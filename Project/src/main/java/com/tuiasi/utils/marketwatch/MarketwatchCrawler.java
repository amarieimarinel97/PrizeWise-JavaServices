package com.tuiasi.utils.marketwatch;

import com.tuiasi.exception.ObjectNotFoundException;
import com.tuiasi.model.Article;
import com.tuiasi.model.utils.Recommendation;
import com.tuiasi.model.Stock;
import com.tuiasi.model.StockInformation;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.tuiasi.utils.businessinsider.BusinessInsiderCrawler.getDateDiff;


@Component
@Slf4j
public class MarketwatchCrawler {
    public StockInformation crawlStockInfo(String[] companyInfo) {
        String symbol = companyInfo[0];
        String companyName = companyInfo[1];

        Document doc = null;
        try {
            doc = Jsoup.connect("https://www.marketwatch.com/investing/stock/" + symbol)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                    .referrer("http://www.google.com")
                    .timeout(12000)
                    .followRedirects(true)
                    .execute().parse();
        } catch (Exception e) {
            log.error("Symbol " + symbol + " not found.");
            throw new ObjectNotFoundException("Symbol " + symbol + " not found.");
        }

        Double price = Double.parseDouble(
                doc.select("bg-quote.value").text()
                        .replaceAll("[^0-9.]", ""));

        Stock stock = Stock.builder()
                .company(companyName)
                .lastUpdated(new Date())
                .symbol(symbol)
                .expertsRecommendationCoefficient(crawlExpertsRecommendationCoefficient(symbol))
                .price(price)
                .build();

        Set<Article> articles = crawlStockArticles(doc, stock);

        return StockInformation.builder()
                .articles(articles)
                .stock(stock)
                .build();
    }

    private Set<Article> crawlStockArticles(Document document, Stock stock) {
        Set<Article> articles = new HashSet<>();

        Elements articlesHTML = document.select("mw-scrollable-news > div.collection__list[data-type=MarketWatch] > div");

        articlesHTML.forEach(el ->
                articles.add(
                        Article.builder()
                                .title(el.select("a.link").text())
                                .link(el.select("a.link").attr("href"))
                                .lastUpdated(getArticleLastUpdated(el.select("span.article__timestamp").attr("data-est")))
                                .stock(stock)
                                .build()
                ));

        return articles;
    }

    private Double getArticleLastUpdated(String text){
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date articleDate=null;
        try {
            articleDate = format.parse(text);
        } catch (ParseException e) {
            log.error("Date could not be parsed for given article");
            return 0.0;
        }
        return (double)getDateDiff(new Date(), articleDate, TimeUnit.HOURS);
    }


    private Double crawlExpertsRecommendationCoefficient(String symbol) {
        Document doc = null;
        try {
            doc = Jsoup.connect("https://www.marketwatch.com/investing/stock/" + symbol + "/analystestimates").get();
        } catch (Exception e) {
            log.error("Symbol " + symbol + " not found.");
            throw new ObjectNotFoundException("Symbol " + symbol + " not found.");
        }
        StringBuilder sb = new StringBuilder();

        Iterator<Element> recommendationsIterator = doc.select("table.ratings > tbody > tr").iterator();
        List<Recommendation> recommendationList = new ArrayList<>();
        while (recommendationsIterator.hasNext()) {
            recommendationList.addAll(generateRecomendationsFromElement(recommendationsIterator.next()));
        }

        return computeRecommendation(recommendationList);
    }

    public Double computeRecommendation(List<Recommendation> recommendations) {
        double sumOfRecommendationPoints = 0.0;
        int noOfRecommendations = 0;
        for (Recommendation recommendation : recommendations) {
            double currentCoefficient = 0;
            switch (recommendation.getText()) {
                case Recommendation.BUY:
                    currentCoefficient = 5.0;
                    break;
                case Recommendation.OVERWEIGHT:
                    currentCoefficient = 2.5;
                    break;
                case Recommendation.HOLD:
                    currentCoefficient = 0.0;
                    break;
                case Recommendation.UNDERWEIGHT:
                    currentCoefficient = -2.5;
                    break;
                case Recommendation.SELL:
                    currentCoefficient = -5.0;
                    break;
            }

            double importanceCoefficient = 0;
            long daysOld = getDateDiff(new Date(), recommendation.getDate(), TimeUnit.DAYS);
            if (daysOld < 10)
                importanceCoefficient = 1.25;
            else if (daysOld < 20)
                importanceCoefficient = 1.00;
            else
                importanceCoefficient = 0.75;
            sumOfRecommendationPoints += currentCoefficient * recommendation.getPoints() * importanceCoefficient;
            noOfRecommendations += recommendation.getPoints();

        }
        return sumOfRecommendationPoints / noOfRecommendations;
    }

    public List<Recommendation> generateRecomendationsFromElement(Element element) {
        List<Recommendation> recommendationList = new ArrayList<>();

        String[] recommendationString = element.text().split(" ");
        if (recommendationString[0].toLowerCase().equals("mean"))
            return recommendationList;

        String recommendationType = recommendationString[0];
        Double currentRecommendationNo = recommendationString[1].equals("N\\A") ? 0 : Double.parseDouble(recommendationString[1]);
        Double oneMonthAgoRecommendationNo = recommendationString[2].equals("N\\A") ? 0 : Double.parseDouble(recommendationString[2]);
        Double threeMonthsAgoRecommendationNo = recommendationString[3].equals("N\\A") ? 0 : Double.parseDouble(recommendationString[3]);

        recommendationList.add(
                Recommendation.builder()
                        .date(new Date())
                        .text(recommendationType)
                        .points(currentRecommendationNo)
                        .build()
        );
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -1);
        recommendationList.add(
                Recommendation.builder()
                        .date(cal.getTime())
                        .text(recommendationType)
                        .points(oneMonthAgoRecommendationNo)
                        .build()
        );
        cal.add(Calendar.MONTH, -2);
        recommendationList.add(
                Recommendation.builder()
                        .date(cal.getTime())
                        .text(recommendationType)
                        .points(threeMonthsAgoRecommendationNo)
                        .build()
        );


        return recommendationList;
    }
}
