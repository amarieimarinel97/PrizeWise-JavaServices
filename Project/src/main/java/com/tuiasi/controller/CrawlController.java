package com.tuiasi.controller;


import com.tuiasi.model.Article;
import com.tuiasi.model.StockInformation;
import com.tuiasi.service.AlgorithmService;
import com.tuiasi.service.CrawlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/crawl")
public class CrawlController {
    private CrawlService crawlService;

    @Autowired
    public CrawlController(CrawlService crawlService) {
        this.crawlService = crawlService;
    }

    @GetMapping("/subreddit")
    public List<Article> crawlSubreddit(@RequestParam(name="subreddit")String subReddit,
                                        @RequestParam(name="save")Optional<Boolean> saveInDatabase,
                                        @RequestParam(name="pages")Optional<Integer> noOfPages){
        return this.crawlService.crawlSubreddit(subReddit, saveInDatabase.orElse(false), noOfPages.orElse(1));
    }



    @GetMapping("/bi")
    public StockInformation crawlBusinessInsider(@RequestParam(name="stock")String stock,
                                                  @RequestParam(name="save")Optional<Boolean> saveInDatabase){
        return this.crawlService.crawlBusinessInsider(stock, saveInDatabase.orElse(false));
    }

    @GetMapping("/mw")
    public StockInformation crawlMarketWatch(@RequestParam(name="stock")String stock,
                                                  @RequestParam(name="save")Optional<Boolean> saveInDatabase){
        return this.crawlService.crawlMarketWatch(stock, saveInDatabase.orElse(false));
    }

    @GetMapping("/yf")
    public StockInformation crawlYahooFinance(@RequestParam(name="stock")String stock,
                                             @RequestParam(name="save")Optional<Boolean> saveInDatabase){
        return this.crawlService.crawlYahooFinance(stock, saveInDatabase.orElse(false));
    }



}
