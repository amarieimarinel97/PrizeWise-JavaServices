package com.tuiasi.service;


import com.tuiasi.model.Article;
import com.tuiasi.model.Site;
import com.tuiasi.model.StockInformation;
import com.tuiasi.utils.marketwatch.MarketwatchCrawler;
import com.tuiasi.utils.reddit.RedditCrawler;
import com.tuiasi.utils.businessinsider.BusinessInsiderCrawler;
import com.tuiasi.utils.StockUtils;
import com.tuiasi.utils.yahoofinance.YahooFinanceCrawler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class CrawlService {

    private AlgorithmService algorithmService;
    private RedditCrawler redditCrawler;
    private ArticleService articleService;
    private BusinessInsiderCrawler businessInsiderCrawler;
    private StockService stockService;
    private StockUtils stockUtils;
    private MarketwatchCrawler marketwatchCrawler;
    private YahooFinanceCrawler yahooFinanceCrawler;

    @Autowired
    public CrawlService(AlgorithmService algorithmService, RedditCrawler redditCrawler, ArticleService articleService, BusinessInsiderCrawler businessInsiderCrawler, StockService stockService, StockUtils stockUtils, MarketwatchCrawler marketwatchCrawler, YahooFinanceCrawler yahooFinanceCrawler) {
        this.algorithmService = algorithmService;
        this.redditCrawler = redditCrawler;
        this.articleService = articleService;
        this.businessInsiderCrawler = businessInsiderCrawler;
        this.stockService = stockService;
        this.stockUtils = stockUtils;
        this.marketwatchCrawler = marketwatchCrawler;
        this.yahooFinanceCrawler = yahooFinanceCrawler;
    }

    public List<Article> crawlSubreddit(String subreddit, boolean saveInDatabase, int noOfPages) {
        List<Article> articles = new ArrayList<>(redditCrawler.crawlSubReddit(subreddit, noOfPages));
        Site site = Site.builder()
                .domain("http://www.reddit.com/")
                .path("r/" + subreddit)
                .build();
        if (saveInDatabase) {
            List<Article> storedArticles = new ArrayList<>();
            articles.forEach(article -> storedArticles.add(articleService.add(article)));
            return storedArticles;
        }
        return articles;
    }

    public StockInformation crawlBusinessInsider(String stock, boolean saveInDatabase) {
        long start;
        float elapsedTimeSec;

        start = System.currentTimeMillis();
        String[] stockSymbolAndCompany = stockUtils.searchStockByCompany(stock);
        elapsedTimeSec = (System.currentTimeMillis() - start) / 1000F;
        System.out.println("Time elapsed getting symbol: " + elapsedTimeSec + "s.");

        start = System.currentTimeMillis();
        StockInformation stockInfo = businessInsiderCrawler.crawlStockInfo(stockSymbolAndCompany);
        elapsedTimeSec = (System.currentTimeMillis() - start) / 1000F;
        System.out.println("Time elapsed crawling stock and articles: " + elapsedTimeSec + "s.");

        start = System.currentTimeMillis();
        double historyOptimismCoefficient = algorithmService.getPredictionBasedOnHistory(stockInfo, 3);
        stockInfo.getStock().setHistoryOptimismCoefficient(historyOptimismCoefficient);
        elapsedTimeSec = (System.currentTimeMillis() - start) / 1000F;
        System.out.println("Time elapsed getting stock regression prediction: " + elapsedTimeSec + "s.");

        start = System.currentTimeMillis();
        stockInfo.getStock().setNewsOptimismCoefficient(
                algorithmService.getArticlesSentimentAnalysis(stockInfo.getArticles(), false) * 10);
//        stockInfo.setArticles(stockUtils.sortBySentimentAnalysis(stockInfo.getArticles()));
        elapsedTimeSec = (System.currentTimeMillis() - start) / 1000F;
        System.out.println("Time elapsed getting sentiment analysis results(array): " + elapsedTimeSec + "s.");

        stockInfo.getStock().setPredictedChange(
                ((stockInfo.getStock().getExpertsRecommendationCoefficient())
                        +
                        stockInfo.getStock().getHistoryOptimismCoefficient()
                        +
                        stockInfo.getStock().getNewsOptimismCoefficient()
                ) / 3.0 - 5
        );
        if (saveInDatabase) {
            start = System.currentTimeMillis();
            stockService.add(stockInfo.getStock());
            stockInfo.getArticles().forEach(article -> articleService.add(article));
            elapsedTimeSec = (System.currentTimeMillis() - start) / 1000F;
            System.out.println("Time elapsed storing in db: " + elapsedTimeSec + "s.");
        }
        System.out.println("--------------------------------------");
        return stockInfo;
    }



    public StockInformation crawlMarketWatch(String stock, boolean saveInDatabase) {
        StockInformation stockInfo = marketwatchCrawler.crawlStockInfo(stockUtils.searchStockByCompany(stock));
        if (saveInDatabase) {
            stockService.add(stockInfo.getStock());
            stockInfo.getArticles().forEach(article -> articleService.add(article));
        }
        return stockInfo;
    }

    public StockInformation crawlYahooFinance(String stock, boolean saveInDatabase) {
        StockInformation stockInfo = yahooFinanceCrawler.crawlStockInfo(stockUtils.searchStockByCompany(stock));
        if (saveInDatabase) {
            stockService.add(stockInfo.getStock());
            stockInfo.getArticles().forEach(article -> articleService.add(article));
        }
        return stockInfo;
    }
}
