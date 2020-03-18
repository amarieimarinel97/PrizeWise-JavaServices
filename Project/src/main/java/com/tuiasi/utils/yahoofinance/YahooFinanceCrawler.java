package com.tuiasi.utils.yahoofinance;

import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.tuiasi.exception.ObjectNotFoundException;
import com.tuiasi.model.Article;
import com.tuiasi.model.Stock;
import com.tuiasi.model.StockInformation;
import com.tuiasi.utils.StockUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


@Component
@Slf4j
public class YahooFinanceCrawler {

    protected class SilentHtmlUnitDriver extends HtmlUnitDriver {
        SilentHtmlUnitDriver(Boolean enableJavaScript) {
            super(enableJavaScript);
            this.getWebClient().setCssErrorHandler(new SilentCssErrorHandler());
        }

        SilentHtmlUnitDriver() {
            super();
            this.getWebClient().setCssErrorHandler(new SilentCssErrorHandler());
        }
    }

    public StockInformation crawlStockInfo(String[] companyInfo) {
        String symbol = companyInfo[0];
        String companyName = companyInfo[1];

        Document doc = null;
        try {
            WebDriver driver = new SilentHtmlUnitDriver(false);
            driver.get("https://finance.yahoo.com/quote/" + symbol);
            driver.findElement(By.className("primary")).click();

            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("window.scrollBy(0,1000)");
            doc = Jsoup.parse(driver.getPageSource());

        } catch (Exception e) {
            log.error("Symbol " + symbol + " not found - error :" + e.getMessage());
            throw new ObjectNotFoundException("Symbol " + symbol + " not found.");
        }
        StockUtils.writeToFile("yf.html", doc.html(), false);

        Double price = 0.1;
//                Double.parseDouble(
//                doc.select("div#quote-header-info > div > div > div > span[data-reactid=34]").text() //here
//                        .replaceAll("[^0-9.]", ""));
        System.out.println("PRICE IS : ");
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

    private double crawlExpertsRecommendationCoefficient(String symbol){
        Document doc = null;
        try {
            WebDriver driver = new SilentHtmlUnitDriver();
            driver.get("https://finance.yahoo.com/quote/" + symbol);
            driver.findElement(By.className("primary")).click();
            doc = Jsoup.parse(driver.getPageSource());

        } catch (Exception e) {
            log.error("Symbol " + symbol + " not found - error :" + e.getMessage());
            throw new ObjectNotFoundException("Symbol " + symbol + " not found.");
        }
        StockUtils.writeToFile("yf-efc.html", doc.html(), false);
        return 0.0;
    }


    private Set<Article> crawlStockArticles(Document document, Stock stock) {
        Set<Article> articles = new HashSet<>();

        Elements articlesHTML = document.select("div#quoteNewsStream-0-Stream > ul > li");
        double noOfArticles = 0;
        for (Iterator<Element> it = articlesHTML.iterator(); it.hasNext(); ) {
            Element currentArticle = it.next();
            noOfArticles++;
        }
        System.out.println("FOUND " + noOfArticles + " ARTICLES");
        articlesHTML.forEach(el ->
                articles.add(
                        Article.builder()
                                .title(el.select("h3[class^=Mb]").text())
                                .link(el.select("h3[class^=Mb] > a").attr("href"))
//                                .lastUpdated(getArticleLastUpdated(el.select("div[data-reactid=14] > span:eq(1)").attr("data-est")))
                                .body(el.select("p[data-reactid]").text())
                                .stock(stock)
                                .build()
                ));

        return articles;
    }
}
