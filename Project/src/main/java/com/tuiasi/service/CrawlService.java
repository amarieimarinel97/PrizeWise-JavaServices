package com.tuiasi.service;


import com.tuiasi.model.Article;
import com.tuiasi.model.Site;
import com.tuiasi.model.Stock;
import com.tuiasi.model.StockInformation;
import com.tuiasi.utils.Crawler;
import com.tuiasi.utils.symbols.NewsCrawler;
import com.tuiasi.utils.symbols.StockUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class CrawlService {

    private Crawler crawler;
    private ArticleService articleService;
    private NewsCrawler newsCrawler;
    private StockService stockService;
    private StockUtils stockUtils;

    @Autowired
    public CrawlService(StockUtils stockUtils, Crawler crawler, ArticleService articleService, StockService stockService, NewsCrawler newsCrawler) {
        this.crawler = crawler;
        this.articleService = articleService;
        this.stockService = stockService;
        this.newsCrawler = newsCrawler;
        this.stockUtils = stockUtils;
    }

    public List<Article> crawlSubreddit(String subreddit, boolean saveInDatabase, int noOfPages) {
        List<Article> articles = new ArrayList<>(crawler.crawlSubReddit(subreddit, noOfPages));
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

    public StockInformation crawlStock(String stock, boolean saveInDatabase) {
        StockInformation stockInfo = newsCrawler.crawlStockInfo(stockUtils.searchCompanyAndStock(stock));
        if (saveInDatabase) {
            stockService.add(stockInfo.getStock());
            stockInfo.getArticles().forEach(article -> articleService.add(article));
        }
        return stockInfo;
    }
}
