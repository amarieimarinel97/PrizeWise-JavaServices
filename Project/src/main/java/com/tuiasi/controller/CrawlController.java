package com.tuiasi.controller;


import com.tuiasi.model.Article;
import com.tuiasi.model.StockInformation;
import com.tuiasi.model.utils.StockInformationWithTimestamp;
import com.tuiasi.service.CrawlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/crawl")
public class CrawlController {
    private CrawlService crawlService;

    @Autowired
    public CrawlController(CrawlService crawlService) {
        this.crawlService = crawlService;
    }

    @GetMapping("/subreddit")
    public List<Article> crawlSubreddit(@RequestParam(name = "subreddit") String subReddit,
                                        @RequestParam(name = "save") Optional<Boolean> saveInDatabase,
                                        @RequestParam(name = "pages") Optional<Integer> noOfPages) {
        return this.crawlService.crawlSubreddit(subReddit, saveInDatabase.orElse(false), noOfPages.orElse(1));
    }


    @GetMapping("/bi")
    public StockInformation crawlBusinessInsider(@RequestParam(name = "stock") String stock,
                                                 @RequestParam(name = "save") Optional<Boolean> saveInDatabase,
                                                 HttpServletResponse response) {

        StockInformation stockInformation = this.crawlService.crawlBusinessInsider(stock, saveInDatabase.orElse(false));
        crawlService.handleCookieSetting(stockInformation, response);
        return stockInformation;
    }

    @GetMapping("/mw")
    public StockInformation crawlMarketWatch(@RequestParam(name = "stock") String stock,
                                             @RequestParam(name = "save") Optional<Boolean> saveInDatabase) {
        return this.crawlService.crawlMarketWatch(stock, saveInDatabase.orElse(false));
    }

    @GetMapping("/growing")
    public List<StockInformation> getTopGrowingStocks(@RequestParam(name = "number") Optional<Integer> numberOfStocks) {
        return crawlService.getTopGrowingStocks(numberOfStocks.orElse(DEFAULT_NO_OF_STOCKS), true);
    }

    @GetMapping("/decreasing")
    public List<StockInformation> getTopDecreasingStocks(@RequestParam(name = "number") Optional<Integer> numberOfStocks) {
        return crawlService.getTopGrowingStocks(numberOfStocks.orElse(DEFAULT_NO_OF_STOCKS), false);
    }

    @GetMapping("/popular")
    public List<StockInformation> getTopPopularStocks(@RequestParam(name = "number") Optional<Integer> numberOfStocks) {
        return crawlService.getTopPopularStocks(numberOfStocks.orElse(DEFAULT_NO_OF_STOCKS));
    }


    @GetMapping("/history")
    public List<StockInformationWithTimestamp> getHistoryOfStocks(HttpServletRequest request) {
        return crawlService.getHistoryOfStocks(request);
    }



    private final Integer DEFAULT_NO_OF_STOCKS = 5;
}
