package com.tuiasi.central_module.controller;


import com.tuiasi.central_module.model.StockAnalysis;
import com.tuiasi.central_module.model.utils.StockInformationWithTimestamp;
import com.tuiasi.central_module.service.MainService;
import com.tuiasi.utils.SymbolsTimestampedList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<StockAnalysis> analyzeStock(@RequestParam(name = "stock") String stock,
                                       @RequestParam(name = "save") Optional<Boolean> saveInDatabase,
                                       HttpServletResponse response) {
        return this.mainService.analyzeStock(stock, saveInDatabase.orElse(false));
    }

    @GetMapping("/analyze_articles")
    public ResponseEntity<StockAnalysis> analyzeStockArticles(@RequestParam(name = "stock") String stock,
                                              @RequestParam(name = "save") Optional<Boolean> saveInDatabase) {
        return this.mainService.analyzeStockArticles(stock, saveInDatabase.orElse(false));
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

    @PostMapping("/history")
    public List<StockInformationWithTimestamp> getHistoryOfStocks(@RequestBody SymbolsTimestampedList symbolsTimestampedList) {
        return mainService.getListOfStocks(symbolsTimestampedList.getSymbols(), DEFAULT_NO_OF_STOCKS);
    }

    @PostMapping("/watchlist")
    public List<StockInformationWithTimestamp> getMyWatchlistOfStocks(@RequestBody SymbolsTimestampedList symbolsTimestampedList) {
        return mainService.getListOfStocks(symbolsTimestampedList.getSymbols(), DEFAULT_NO_OF_STOCKS);
    }

    private final Integer DEFAULT_NO_OF_STOCKS = 5;
  }
