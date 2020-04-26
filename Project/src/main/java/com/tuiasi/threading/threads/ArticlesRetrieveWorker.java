package com.tuiasi.threading.threads;

import com.tuiasi.exception.ObjectNotFoundException;
import com.tuiasi.model.Article;
import com.tuiasi.model.StockInformation;
import com.tuiasi.service.AlgorithmService;
import com.tuiasi.threading.NotifyingThread;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class ArticlesRetrieveWorker extends NotifyingThread {

    private AlgorithmService algorithmService;
    private StockInformation stockInformation;

    public ArticlesRetrieveWorker(AlgorithmService algorithmService, StockInformation stockInformation) {
        this.algorithmService = algorithmService;
        this.stockInformation = stockInformation;
    }

    @Override
    public void doRun() {
        handleArticles();
    }

    private void handleArticles() {
        crawlStockArticles(stockInformation);
        algorithmService.getArticlesSentimentAnalysis(stockInformation);
    }

    private void crawlStockArticles(StockInformation stockInformation) {
        Set<Article> articles = new TreeSet<>();

        Document doc = null;
        try {
            doc = Jsoup.connect("http://markets.businessinsider.com/news/" + stockInformation.getStock().getSymbol()).get();
        } catch (IOException e) {
            throw new ObjectNotFoundException("Symbol " + stockInformation.getStock().getSymbol() + " not found.");
        }
        Elements articlesHTML = doc.select("div.news-by-company");
        articlesHTML.select("div.col-md-12").forEach(element ->
                articles.add(Article.builder()
                        .title(element.select(".news-link").text())
                        .lastUpdated(getArticleLastUpdated(element.select("span").text()))
                        .link(getArticleLink(element))
                        .stock(stockInformation.getStock())
                        .build())
        );
//        crawlImportantRecentArticles(articles, 0);
        stockInformation.setArticles(articles);
    }

    private void crawlImportantRecentArticles(Set<Article> articles, int noOfArticles) {
        Set<Article> articlesList = new TreeSet<>(articles);
        Iterator<Article> it = articlesList.iterator();
        int articlesCrawled = 1;
        while (it.hasNext() && articlesCrawled <= noOfArticles) {
            Article article = it.next();

            if (article.getLink().startsWith("http://markets.businessinsider.com")/*
                    && (article.getTitle().toLowerCase().contains(article.getStock().getCompany().split("\\W+")[0].toLowerCase())
                    || article.getTitle().toLowerCase().contains(article.getStock().getSymbol().toLowerCase()))*/
            ) {
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
