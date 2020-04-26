package com.tuiasi.utils.businessinsider;

import com.tuiasi.exception.ObjectNotFoundException;
import com.tuiasi.model.Article;
import com.tuiasi.model.Recommendation;
import com.tuiasi.model.Stock;
import com.tuiasi.model.StockInformation;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class BusinessInsiderCrawler {

    public void crawlStockInfo(StockInformation stockInformation, String[] companyInfo) {
        String symbol = companyInfo[0];
        String companyName = companyInfo[1];

        Document doc = null;
        try {
            doc = Jsoup.connect("http://markets.businessinsider.com/stocks/" + symbol + "-stock").get();
        } catch (IOException e) {
            log.error("Symbol " + symbol + " not found.");
            throw new ObjectNotFoundException("Symbol " + symbol + " not found.");
        }
        Double price = getPriceFromPage(doc);

        Stock stock = Stock.builder()
                .company(companyName)
                .lastUpdated(new Date())
                .symbol(symbol)
                .expertsRecommendationCoefficient(crawlExpertsRecommendationCoefficient(symbol))
                .price(price)
                .build();

        Set<Article> articles = crawlStockArticles(symbol, stock);

        stockInformation.setStock(stock);
        stockInformation.setArticles(articles);
    }

    private double getPriceFromPage(Document doc) {
        return Double.parseDouble(
                doc.select("div > div[data-field=Mid] > span.push-data")
                        .text()
                        .replaceAll("[^0-9.]", ""));
    }

    private boolean isDateFormat(String input) {
        return input.contains("/");
    }

    private Double crawlExpertsRecommendationCoefficient(String symbol) {
        Document document = null;
        try {
            document = Jsoup.connect("http://markets.businessinsider.com/analyst/" + symbol).get();
        } catch (IOException e) {
            log.error("Symbol " + symbol + " not found.");
            throw new ObjectNotFoundException("Symbol " + symbol + " not found.");
        }
        Iterator<Element> recommendationsIterator = document.select("div.box > div.hidden-xs tr > td.h5, div.box > div.hidden-xs tr > td:eq(4)").iterator();

        List<Recommendation> recommendations = new ArrayList<>();
        while (recommendationsIterator.hasNext()) {
            Recommendation recommendation = Recommendation.builder().build();
            String nextElement = recommendationsIterator.next().text();
            if (isDateFormat(nextElement)) {
                recommendation.setDate(getDate(nextElement));
                recommendation.setText(recommendationsIterator.next().text());
            } else {
                recommendation.setText(nextElement);
                recommendation.setDate(getDate(recommendationsIterator.next().text()));
            }
            recommendations.add(recommendation);
        }

        Double ERC1 = computeExpertsRecommendationCoefficient(recommendations);
        ERC1 = ERC1 > 5 ? 5 : ERC1;
        String theirRating = document.select("div.rating-label").text();
        Double ERC2 = theirRating.isEmpty() ? ERC1 : 5 - 2.5 * (Double.parseDouble(theirRating) - 1);

        return theirRating.isEmpty() ? ERC1 * 2 : (ERC1 + ERC2);
    }

    private Date getDate(String dateStr) {
        String[] mmddyy = dateStr.split("/");
        Date result = new Date();
        result.setMonth(Integer.parseInt(mmddyy[0].substring(Math.max(mmddyy[0].length() - 2, 0))) - 1);
        result.setDate(Integer.parseInt(mmddyy[1]));
        result.setYear(100 + Integer.parseInt(mmddyy[2].substring(0, Math.min(mmddyy[2].length(), 2))));
        return result;

    }

    public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillis = date1.getTime() - date2.getTime();
        return timeUnit.convert(diffInMillis, TimeUnit.MILLISECONDS);
    }


    private Double computeExpertsRecommendationCoefficient(List<Recommendation> recommendations) {
        Double totalRecommendationPoints = 0.0;
        Double totalRecommendations = 0.0;

        recommendations.sort(Recommendation.comparatorByDate);
        Date minDate = recommendations.get(0).getDate();

        Date maxDate = new Date();
        long daysInterval = getDateDiff(maxDate, minDate, TimeUnit.DAYS);
        daysInterval = daysInterval == 0 ? 1 : daysInterval;

        for (Recommendation currentRecommendation : recommendations) {
            double currentCoefficient = 0.0;
            switch (currentRecommendation.getText().toLowerCase()) {
                case "upgraded to market outperform":
                case "upgraded to buy":
                    currentCoefficient = 5.0;
                    break;
                case "upgraded to overweight":
                    currentCoefficient = 2.5;
                    break;
                case "hold":
                    currentCoefficient = 0.0;
                    break;
                case "downgraded to underweight":
                    currentCoefficient = -2.5;
                    break;
                case "downgraded to market underperform":
                case "downgraded to sell":
                    currentCoefficient = -5.0;
                    break;
                default:
                    log.info("Recommendation not found.");
                    continue;
            }
            currentRecommendation.setPoints(currentCoefficient * 1.5 * getDateDiff(maxDate, currentRecommendation.getDate(), TimeUnit.DAYS) / daysInterval);
            totalRecommendationPoints += currentRecommendation.getPoints();
            ++totalRecommendations;
        }
        return totalRecommendationPoints / totalRecommendations;
    }

    private Set<Article> crawlStockArticles(String symbol, Stock stock) {
        Set<Article> articles = new TreeSet<>();

        Document doc = null;
        try {
            doc = Jsoup.connect("http://markets.businessinsider.com/news/" + symbol).get();
        } catch (IOException e) {
            log.error("Symbol " + symbol + " not found.");
            throw new ObjectNotFoundException("Symbol " + symbol + " not found.");
        }
        Elements articlesHTML = doc.select("div.news-by-company");
        articlesHTML.select("div.col-md-12").forEach(element ->
                articles.add(Article.builder()
                        .title(element.select(".news-link").text())
                        .lastUpdated(getArticleLastUpdated(element.select("span").text()))
                        .link(getArticleLink(element))
                        .stock(stock)
                        .build())
        );
        crawlImportantRecentArticles(articles, 0);
        return articles;
    }

    private void crawlImportantRecentArticles(Set<Article> articles, int noOfArticles) {
        Set<Article> articlesList = new TreeSet<Article>(articles);
        Iterator<Article> it = articlesList.iterator();
        int articlesCrawled = 1;
        while (it.hasNext() && articlesCrawled <= noOfArticles) {
            Article article = it.next();

            if (article.getLink().startsWith("http://markets.businessinsider.com")/*
                    && (article.getTitle().toLowerCase().contains(article.getStock().getCompany().split("\\W+")[0].toLowerCase())
                    || article.getTitle().toLowerCase().contains(article.getStock().getSymbol().toLowerCase()))*/) {
                crawlArticleBody(article);
                ++articlesCrawled;
            }
        }
    }

    private void crawlArticleBody(Article article) {
        Document doc = null;
        try {
            doc = Jsoup.connect(article.getLink()).get();
        } catch (IOException e) {
            log.error("Article " + article.getTitle() + " not found.");
            return;
        }
        String articleBody = doc.select("div.news-content")
                .text();
        article.setBody(articleBody.substring(0, Math.min(1000, articleBody.length())));
    }

    private String getArticleLink(Element element) {
        String link = element.select("a").attr("href");
        if (link.startsWith("/news/stock"))
            link = "http://markets.businessinsider.com" + link;
        return link;
    }

    private Double getArticleLastUpdated(String element) {
        String[] elements = element.split(" ");
        String timeElapsed = elements[elements.length - 1];
        switch (timeElapsed.charAt(timeElapsed.length() - 1)) {
            case 'y':
                return Double.parseDouble(timeElapsed.replaceAll("[^0-9.]", "")) * 8760;
            case 'd':
                return Double.parseDouble(timeElapsed.replaceAll("[^0-9.]", "")) * 24;
            case 'h':
                return Double.parseDouble(timeElapsed.replaceAll("[^0-9.]", ""));
            case 'm':
                return Double.parseDouble(timeElapsed.replaceAll("[^0-9.]", "")) / 60;
            default:
                return null;
        }
    }

}
