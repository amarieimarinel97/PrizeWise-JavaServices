package com.tuiasi.service;


import com.tuiasi.model.*;
import com.tuiasi.repository.StockRepository;
import com.tuiasi.repository.StockSymbolRepository;
import com.tuiasi.threading.threads.MainThread;
import com.tuiasi.utils.StockUtils;
import com.tuiasi.utils.businessinsider.BusinessInsiderCrawler;
import com.tuiasi.utils.marketwatch.MarketwatchCrawler;
import com.tuiasi.utils.reddit.RedditCrawler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

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
    private StockEvolutionService stockEvolutionService;
    private StockRepository stockRepository;
    private StockSymbolRepository stockSymbolRepository;

    @Autowired
    public CrawlService(StockSymbolRepository stockSymbolRepository, StockRepository stockRepository, AlgorithmService algorithmService, RedditCrawler redditCrawler, ArticleService articleService, BusinessInsiderCrawler businessInsiderCrawler, StockService stockService, StockUtils stockUtils, MarketwatchCrawler marketwatchCrawler, StockEvolutionService stockEvolutionService
    ) {
        this.algorithmService = algorithmService;
        this.redditCrawler = redditCrawler;
        this.articleService = articleService;
        this.businessInsiderCrawler = businessInsiderCrawler;
        this.stockService = stockService;
        this.stockUtils = stockUtils;
        this.marketwatchCrawler = marketwatchCrawler;
        this.stockEvolutionService = stockEvolutionService;
        this.stockRepository = stockRepository;
        this.stockSymbolRepository = stockSymbolRepository;
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
        MainThread mainThread = new MainThread(stockInformation, algorithmService, articleService, stockService, stockUtils, stockEvolutionService, stockRepository);

        try {
            mainThread.run(saveInDatabase);
        } catch (InterruptedException e) {
            log.error("Could not process stock " + stock);
            e.printStackTrace();
        }
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

    public List<StockInformation> getTopGrowingStocks(int noOfStocks, boolean isDescendingOrder) {
        Map<Double, StockInformation> map = isDescendingOrder? new TreeMap<>(Collections.reverseOrder()): new TreeMap<>();

        for (Stock stock : stockService.getAll()) {
            map.put(
                    stock.getPredictedChange(),
                    StockInformation.builder()
                            .stock(stock)
                            .articles(articleService.getLastArticlesBySymbol(stock.getSymbol(), 25))
                            .stockEvolution(stockEvolutionService.get(stock.getSymbol()))
                            .build()
            );
        }

        return new ArrayList<>(map.values()).subList(0, noOfStocks);
    }


    public List<StockInformation> getTopPopularStocks(int noOfStocks) {
        Map<Integer, StockInformation> map = new TreeMap<>(Collections.reverseOrder());

        for (Stock stock : stockService.getAll()) {
            map.put(
                    stock.getHits(),
                    StockInformation.builder()
                            .stock(stock)
                            .articles(articleService.getLastArticlesBySymbol(stock.getSymbol(), 25))
                            .stockEvolution(stockEvolutionService.get(stock.getSymbol()))
                            .build()
            );
        }

        return new ArrayList<>(map.values()).subList(0, noOfStocks);
    }

}
