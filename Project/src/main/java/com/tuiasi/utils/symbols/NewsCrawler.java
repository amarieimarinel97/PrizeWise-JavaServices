package com.tuiasi.utils.symbols;

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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class NewsCrawler {

    private static String convertXPathToJsoupSyntax(String xpath) {
        if (xpath.charAt(0) == '/')
            xpath = xpath.substring(1);

        StringBuilder result = new StringBuilder();
        for (char c : xpath.toCharArray()) {
            switch (c) {
                case '[':
                    result.append(":eq(");
                    break;
                case ']':
                    result.append(")");
                    break;
                case '/':
                    result.append(" > ");
                    break;
                default:
                    result.append(c);
            }
        }
        return result.toString();
    }

    public StockInformation crawlStockInfo(String[] companyInfo) {
        String symbol = companyInfo[0];
        String companyName = companyInfo[1];

        Document doc = null;
        try {
            doc = Jsoup.connect("http://markets.businessinsider.com/stocks/" + symbol + "-stock").get();
        } catch (IOException e) {
            log.error("Symbol " + symbol + " not found.");
            throw new ObjectNotFoundException("Symbol " + symbol + " not found.");
        }

        Double price = Double.parseDouble(
                doc.select("#site > div > div:nth-child(2) > div.row.equalheights.greyBorder > div > div:nth-child(3) > div.col-sm-10.no-padding > div:nth-child(3) > div:nth-child(2) > span")
                        .text()
                        .replaceAll("[^0-9.]", ""));

        Stock stock = Stock.builder()
                .company(companyName)
                .lastUpdated(new Date())
                .symbol(symbol)
                .expertsRecommendationCoefficient(crawlExpertsRecommendationCoefficient(symbol))
                .price(price)
                .build();

        Set<Article> articles = crawlStockArticles(symbol, stock);

        return StockInformation.builder()
                .stock(stock)
                .articles(articles)
                .build();
    }

    private Double crawlExpertsRecommendationCoefficient(String symbol) {
        Document document = null;
        try {
            document = Jsoup.connect("http://markets.businessinsider.com/analyst/" + symbol).get();
        } catch (IOException e) {
            log.error("Symbol " + symbol + " not found.");
            throw new ObjectNotFoundException("Symbol " + symbol + " not found.");
        }
        StringBuilder recommendationsString = new StringBuilder();
        Elements recommendationsHTML = document.select("div.box > div.hidden-xs tr > td:eq(4)");
        Iterator<Element> recommendationsIterator = document.select("div.box > div.hidden-xs tr > td.h5, div.box > div.hidden-xs tr > td:eq(4)").iterator();

        List<Recommendation> recommendations = new ArrayList<>();
        while (recommendationsIterator.hasNext()) {
            recommendations.add(Recommendation.builder()
                    .date(getDate(recommendationsIterator.next().text()))
                    .text(recommendationsIterator.next().text())
                    .build());
        }

        Double ERC1 = computeExpertsRecommendationCoefficient(recommendations);
        ERC1 = ERC1 > 5 ? 5 : ERC1;
        String theirRating = document.select("div.rating-label").text();
        Double ERC2 = theirRating.isEmpty() ? ERC1 : 5 - 2.5 * (Double.parseDouble(theirRating) - 1);

        return theirRating.isEmpty() ? ERC1 : (ERC1 + ERC2) / 2;
    }

    private Date getDate(String dateStr) {
        String[] mmddyy = dateStr.split("/");
        Date result = new Date();
        result.setMonth(Integer.parseInt(mmddyy[0]) - 1);
        result.setDate(Integer.parseInt(mmddyy[1]));
        result.setYear(100 + Integer.parseInt(mmddyy[2]));
        return result;

    }

    public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillis = date2.getTime() - date1.getTime();
        return timeUnit.convert(diffInMillis, TimeUnit.MILLISECONDS);
    }


    private Double computeExpertsRecommendationCoefficient(List<Recommendation> recommendations) {
        Double totalRecommendationPoints = 0.0;
        Double totalRecommendations = 0.0;

        recommendations.sort(Recommendation.comparatorByDate);
        Date minDate = recommendations.get(0).getDate();

        Date maxDate = new Date();
        long daysInterval = getDateDiff(minDate, maxDate, TimeUnit.DAYS);
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
            currentRecommendation.setPoints(currentCoefficient * 1.5 * getDateDiff(currentRecommendation.getDate(), maxDate, TimeUnit.DAYS) / daysInterval);
            totalRecommendationPoints += currentRecommendation.getPoints();
            ++totalRecommendations;
        }
        return totalRecommendationPoints / totalRecommendations;
    }

    private Set<Article> crawlStockArticles(String symbol, Stock stock) {
        Set<Article> articles = new HashSet<>();

        Document doc = null;
        try {
            doc = Jsoup.connect("http://markets.businessinsider.com/news/" + symbol).get();
        } catch (IOException e) {
            log.error("Symbol " + symbol + " not found.");
            throw new ObjectNotFoundException("Symbol " + symbol + " not found.");
        }
        Elements articlesHTML = doc.select("#site > div > div:nth-child(2) > div.row.equalheights.greyBorder > div > div.box > div.box.news-by-company");
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
        String articleBody = doc.select("#site > div > div:nth-child(3) > div:nth-child(5) > div.col-md-8.col-xs-12 > div.row > div.col-xs-12.news-content.no-padding")
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

    private void writeToFile(String fileName, String str, boolean append) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(fileName, append));
            writer.write(str);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
