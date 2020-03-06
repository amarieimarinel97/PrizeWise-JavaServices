package com.tuiasi.utils.yahoofinance;

import com.tuiasi.exception.ObjectNotFoundException;
import com.tuiasi.model.Article;
import com.tuiasi.model.Stock;
import com.tuiasi.model.StockInformation;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;


@Component
@Slf4j
public class YahooFinanceCrawler {
    public StockInformation crawlStockInfo(String[] companyInfo) {
        String symbol = companyInfo[0];
        String companyName = companyInfo[1];

        Document doc = null;
        try {
            Connection.Response response = Jsoup.connect("https://finance.yahoo.com/quote/" + symbol)
                    .method(Connection.Method.GET)
                    .followRedirects(true)
                    .execute();
            System.out.println("HERE"+response.url());
            System.out.println("Session id: "+response.url().toString().matches("(?<=sessionId=)(.*)(?=&lang=)"));
//            String sessionId = response.cookie("sessionId");
            doc = Jsoup.connect("https://finance.yahoo.com/quote/"+ symbol)
                    .get();
        } catch (Exception e) {
            log.error("Symbol " + symbol + " not found." + e.getMessage());
            throw new ObjectNotFoundException("Symbol " + symbol + " not found.");
        }
        System.out.println(doc.text());
       doc.select("div#quote-header-info > div > div > div > span").forEach(el-> System.out.println("-------------\n"+el.html()));
        Double price = Double.parseDouble(
                doc.select("div#quote-header-info > div > div > div > span").text() //here
                        .replaceAll("[^0-9.]", ""));

        Stock stock = Stock.builder()
                .company(companyName)
                .lastUpdated(new Date())
                .symbol(symbol)
//                .expertsRecommendationCoefficient(crawlExpertsRecommendationCoefficient(symbol))
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

        Elements articlesHTML = document.select("div#quoteNewsStream-0-Stream");
        articlesHTML.forEach(el -> System.out.println("---------------------\n"+el.html()+"\n"));
//        articlesHTML.forEach(el ->
//                articles.add(
//                        Article.builder()
//                                .title(el.select("a.link").text())
//                                .link(el.select("a.link").attr("href"))
//                                .lastUpdated(getArticleLastUpdated(el.select("span.article__timestamp").attr("data-est")))
//                                .stock(stock)
//                                .build()
//                ));

        return articles;
    }
}
