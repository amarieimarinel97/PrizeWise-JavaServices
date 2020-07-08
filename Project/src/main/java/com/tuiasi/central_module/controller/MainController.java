package com.tuiasi.central_module.controller;


import com.tuiasi.central_module.model.StockAnalysis;
import com.tuiasi.central_module.model.utils.StockInformationWithTimestamp;
import com.tuiasi.central_module.service.MainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api")
public class MainController {
    private MainService mainService;

    @Autowired
    public MainController(MainService mainService) {
        this.mainService = mainService;
    }

    @GetMapping("/analyze")
    public StockAnalysis analyzeStock(@RequestParam(name = "stock") String stock,
                                      @RequestParam(name = "save") Optional<Boolean> saveInDatabase,
                                      HttpServletResponse response) {

        StockAnalysis stockAnalysis = this.mainService.analyzeStock(stock, saveInDatabase.orElse(false));
        mainService.handleCookieSetting(stockAnalysis.getStock().getSymbol(), response, mainService.HISTORY_COOKIE_PREFIX);
        return stockAnalysis;
    }

    @GetMapping("/analyze_articles")
    public StockAnalysis analyzeStockArticles(@RequestParam(name = "stock") String stock,
                                              @RequestParam(name = "save") Optional<Boolean> saveInDatabase) {
        return this.mainService.analyzeStockArticles(stock, saveInDatabase.orElse(false));
    }

    @GetMapping("/analyze_context")
    public StockAnalysis analyzeStockContext(@RequestParam(name = "stock") String stock,
                                              @RequestParam(name = "save") Optional<Boolean> saveInDatabase) {
        return this.mainService.analyzeStock(stock, saveInDatabase.orElse(false));
    }

    @GetMapping("/growing")
    public List<StockAnalysis> getTopGrowingStocks(@RequestParam(name = "number") Optional<Integer> numberOfStocks) {
        return mainService.getTopGrowingStocks(numberOfStocks.orElse(DEFAULT_NO_OF_STOCKS), true);
    }

    @GetMapping("/decreasing")
    public List<StockAnalysis> getTopDecreasingStocks(@RequestParam(name = "number") Optional<Integer> numberOfStocks) {
        return mainService.getTopGrowingStocks(numberOfStocks.orElse(DEFAULT_NO_OF_STOCKS), false);
    }

    @GetMapping("/popular")
    public List<StockAnalysis> getTopPopularStocks(@RequestParam(name = "number") Optional<Integer> numberOfStocks) {
        return mainService.getTopPopularStocks(numberOfStocks.orElse(DEFAULT_NO_OF_STOCKS));
    }

    @GetMapping("/history")
    public List<StockInformationWithTimestamp> getHistoryOfStocks(HttpServletRequest request) {
        return mainService.getListOfStocksFromCookies(request, mainService.HISTORY_COOKIE_PREFIX, DEFAULT_NO_OF_STOCKS);
    }

    @GetMapping("/watchlist")
    public List<StockInformationWithTimestamp> getMyWatchlistOfStocks(HttpServletRequest request) {
        return mainService.getListOfStocksFromCookies(request, mainService.WATCHLIST_COOKIE_PREFIX, DEFAULT_NO_OF_STOCKS);
    }

    @GetMapping("/watchlist/add")
    public void addStockToWatchlist(@RequestParam(name = "stock")String stock, HttpServletResponse response) {
        mainService.handleCookieSetting(stock, response, mainService.WATCHLIST_COOKIE_PREFIX );
    }

    @GetMapping("/watchlist/remove")
    public boolean removeStockFromWatchlist(@RequestParam(name = "stock")String stock, HttpServletRequest request, HttpServletResponse response) {
        return mainService.removeStockFromWatchlist(stock, request, response);
    }

    private final Integer DEFAULT_NO_OF_STOCKS = 5;
  }
