package com.tuiasi.service;


import com.tuiasi.model.Article;
import com.tuiasi.model.Site;
import com.tuiasi.model.StockInformation;
import com.tuiasi.utils.marketwatch.MarketwatchCrawler;
import com.tuiasi.utils.reddit.RedditCrawler;
import com.tuiasi.utils.businessinsider.BusinessInsiderCrawler;
import com.tuiasi.utils.StockUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class CrawlService {

    private RedditCrawler redditCrawler;
    private ArticleService articleService;
    private BusinessInsiderCrawler businessInsiderCrawler;
    private StockService stockService;
    private StockUtils stockUtils;
    private MarketwatchCrawler marketwatchCrawler;

    @Autowired
    public CrawlService(RedditCrawler redditCrawler, ArticleService articleService, BusinessInsiderCrawler businessInsiderCrawler, StockService stockService, StockUtils stockUtils, MarketwatchCrawler marketwatchCrawler) {
        this.redditCrawler = redditCrawler;
        this.articleService = articleService;
        this.businessInsiderCrawler = businessInsiderCrawler;
        this.stockService = stockService;
        this.stockUtils = stockUtils;
        this.marketwatchCrawler = marketwatchCrawler;
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

    public StockInformation crawlBussinessInsider(String stock, boolean saveInDatabase) {
        StockInformation stockInfo = businessInsiderCrawler.crawlStockInfo(stockUtils.searchStockByCompany(stock));
        if (saveInDatabase) {
            stockService.add(stockInfo.getStock());
            stockInfo.getArticles().forEach(article -> articleService.add(article));
        }
        return stockInfo;
    }

    public StockInformation crawlMarketWatch(String stock, boolean saveInDatabase){
        StockInformation stockInfo = marketwatchCrawler.crawlStockInfo(stockUtils.searchStockByCompany(stock));
        if (saveInDatabase) {
            stockService.add(stockInfo.getStock());
            stockInfo.getArticles().forEach(article -> articleService.add(article));
        }
        return stockInfo;
    }
}
