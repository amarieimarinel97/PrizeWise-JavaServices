package com.tuiasi.service;


import com.tuiasi.exception.ObjectNotFoundException;
import com.tuiasi.model.Article;
import com.tuiasi.model.Site;
import com.tuiasi.model.Stock;
import com.tuiasi.model.StockInformation;
import com.tuiasi.threading.threads.MainThread;
import com.tuiasi.utils.marketwatch.MarketwatchCrawler;
import com.tuiasi.utils.reddit.RedditCrawler;
import com.tuiasi.utils.businessinsider.BusinessInsiderCrawler;
import com.tuiasi.utils.StockUtils;
//import com.tuiasi.utils.yahoofinance.YahooFinanceCrawler;
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
//    private YahooFinanceCrawler yahooFinanceCrawler;

    @Autowired
    public CrawlService(AlgorithmService algorithmService, RedditCrawler redditCrawler, ArticleService articleService, BusinessInsiderCrawler businessInsiderCrawler, StockService stockService, StockUtils stockUtils, MarketwatchCrawler marketwatchCrawler//, YahooFinanceCrawler yahooFinanceCrawler
    ) {
        this.algorithmService = algorithmService;
        this.redditCrawler = redditCrawler;
        this.articleService = articleService;
        this.businessInsiderCrawler = businessInsiderCrawler;
        this.stockService = stockService;
        this.stockUtils = stockUtils;
        this.marketwatchCrawler = marketwatchCrawler;
//        this.yahooFinanceCrawler = yahooFinanceCrawler;
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
        String[] stockSymbolAndCompany = stockUtils.searchStockByCompany(stock);
        StockInformation stockInformation = StockInformation.builder()
                .stock(Stock.builder()
                        .symbol(stockSymbolAndCompany[0])
                        .company(stockSymbolAndCompany[1])
                        .build())
                .build();
        MainThread mainThread = new MainThread(stockInformation, algorithmService, articleService, stockService, stockUtils);

        try {
            mainThread.run(saveInDatabase);
        } catch (InterruptedException e) {
            log.error("Could not process stock " + stock);
            e.printStackTrace();
        }

        Double ERC = stockInformation.getStock().getExpertsRecommendationCoefficient();
        Double HOC = stockInformation.getStock().getHistoryOptimismCoefficient();
        Double NOC = stockInformation.getStock().getNewsOptimismCoefficient();
        double predictedChange = (ERC + HOC + NOC) / 3.0 - 5;
        stockInformation.getStock().setPredictedChange(predictedChange);
        return stockInformation;
    }


    public StockInformation crawlMarketWatch(String stock, boolean saveInDatabase) {
        StockInformation stockInfo = marketwatchCrawler.crawlStockInfo(stockUtils.searchStockByCompany(stock));
        if (saveInDatabase) {
            stockService.add(stockInfo.getStock());
            stockInfo.getArticles().forEach(article -> articleService.add(article));
        }
        return stockInfo;
    }

//    public StockInformation crawlYahooFinance(String stock, boolean saveInDatabase) {
//        StockInformation stockInfo = yahooFinanceCrawler.crawlStockInfo(stockUtils.searchStockByCompany(stock));
//        if (saveInDatabase) {
//            stockService.add(stockInfo.getStock());
//            stockInfo.getArticles().forEach(article -> articleService.add(article));
//        }
//        return stockInfo;
//    }
}
