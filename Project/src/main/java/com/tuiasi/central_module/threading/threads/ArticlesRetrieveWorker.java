package com.tuiasi.central_module.threading.threads;

import com.tuiasi.central_module.model.StockAnalysis;
import com.tuiasi.central_module.service.AlgorithmService;
import com.tuiasi.central_module.threading.NotifyingThread;
import com.tuiasi.central_module.threading.ThreadListener;
import com.tuiasi.crawler_module.model.Article;
import com.tuiasi.exception.ObjectNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;

@Slf4j
public class ArticlesRetrieveWorker extends NotifyingThread implements ThreadListener {



    private class ArticleBodyRetrieveWorker extends NotifyingThread {
        public Article article;

        private ArticleBodyRetrieveWorker(Article article) {
            this.article = article;
        }

        @Override
        public void doRun() {
            this.crawlArticleBody(this.article);
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

    }

    private List<ArticleBodyRetrieveWorker> articlesBodiesRetrieveWorkers;
    private AlgorithmService algorithmService;
    private StockAnalysis stockAnalysis;
    private int noOfArticlesBodiesToCrawl;
    private int articlesBodiesCrawled;


    public ArticlesRetrieveWorker(AlgorithmService algorithmService, StockAnalysis stockAnalysis, Optional<Integer> noOfArticlesToCrawl) {
        this.articlesBodiesRetrieveWorkers = new ArrayList<>();
        this.algorithmService = algorithmService;
        this.stockAnalysis = stockAnalysis;
        this.articlesBodiesCrawled = 0;
        this.noOfArticlesBodiesToCrawl = noOfArticlesToCrawl.orElse(this.DEFAULT_NO_OF_ARTICLES_BODIES_TO_CRAWL);
    }

    @Override
    public void onThreadComplete(Thread thread) {
    }

    @Override
    public void doRun() {
        handleArticles();
    }

    private void handleArticles() {
        crawlStockArticles(stockAnalysis);
        algorithmService.getArticlesSentimentAnalysis(stockAnalysis);
    }

    private void crawlStockArticles(StockAnalysis stockAnalysis) {
        Set<Article> articles = new TreeSet<>();

        Document doc = null;
        try {
            doc = Jsoup.connect(ARTICLES_PAGE_LINK + stockAnalysis.getStock().getSymbol()).get();
        } catch (IOException e) {
            throw new ObjectNotFoundException("Symbol " + stockAnalysis.getStock().getSymbol() + " not found.");
        }
        Elements articlesHTML = doc.select("div.news-by-company");
        articlesHTML.select("div.col-md-12").forEach(element ->
                articles.add(Article.builder()
                        .title(element.select(".news-link").text())
                        .lastUpdated(getArticleLastUpdated(element.select("span").text()))
                        .link(getArticleLink(element))
                        .stock(stockAnalysis.getStock())
                        .build())
        );
        crawlImportantRecentArticles(articles, noOfArticlesBodiesToCrawl);
        stockAnalysis.setArticles(articles);
    }

    private void crawlImportantRecentArticles(Set<Article> articles, int noOfArticles) {
        Set<Article> articlesList = new TreeSet<>(articles);
        Iterator<Article> it = articlesList.iterator();
        while (it.hasNext() && articlesBodiesCrawled <= noOfArticles) {
            Article article = it.next();

            if (article.getLink().startsWith("http://markets.businessinsider.com")///*
                    && (article.getTitle().toLowerCase().contains(article.getStock().getCompany().split("\\W+")[0].toLowerCase())
                    || article.getTitle().toLowerCase().contains(article.getStock().getSymbol().toLowerCase()))//*/
            ) {
                this.articlesBodiesRetrieveWorkers.add(new ArticleBodyRetrieveWorker(article));
                ++articlesBodiesCrawled;
            }
        }

        if (articlesBodiesCrawled < noOfArticles) {
            it = articlesList.iterator();
            while (it.hasNext() && articlesBodiesCrawled <= noOfArticles) {
                Article article = it.next();
                if (article.getLink().startsWith("http://markets.businessinsider.com") && Objects.isNull(article.getBody())) {
                    this.articlesBodiesRetrieveWorkers.add(new ArticleBodyRetrieveWorker(article));
                    ++articlesBodiesCrawled;
                }
            }
        }
        for (ArticleBodyRetrieveWorker worker : articlesBodiesRetrieveWorkers) {
            worker.addListener(this);
            worker.start();
        }

        for (ArticleBodyRetrieveWorker worker : articlesBodiesRetrieveWorkers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                log.error("Could not crawl body for article "+worker.article.getTitle());
            }
        }
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

    private final String ARTICLES_PAGE_LINK = "http://markets.businessinsider.com/news/";
    private final Integer DEFAULT_NO_OF_ARTICLES_BODIES_TO_CRAWL = 0;
}
