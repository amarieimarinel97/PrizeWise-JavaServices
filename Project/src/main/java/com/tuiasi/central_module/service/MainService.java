package com.tuiasi.central_module.service;


import com.tuiasi.central_module.model.StockAnalysis;
import com.tuiasi.central_module.threading.threads.MainArticlesThread;
import com.tuiasi.crawler_module.model.*;
import com.tuiasi.central_module.model.utils.StockInformationWithTimestamp;
import com.tuiasi.exception.ObjectNotFoundException;
import com.tuiasi.utils.SymbolWithTimestamp;
import com.tuiasi.crawler_module.repository.StockRepository;
import com.tuiasi.central_module.threading.threads.MainThread;
import com.tuiasi.crawler_module.service.ArticleService;
import com.tuiasi.crawler_module.service.StockContextService;
import com.tuiasi.crawler_module.service.StockService;
import com.tuiasi.utils.StockUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MainService {

    private AlgorithmService algorithmService;
    private ArticleService articleService;
    private StockService stockService;
    private StockUtils stockUtils;
    private StockEvolutionService stockEvolutionService;
    private StockRepository stockRepository;
    private StockContextService stockContextService;

    @Autowired
    public MainService(StockContextService stockContextService, StockRepository stockRepository, AlgorithmService algorithmService, ArticleService articleService, StockService stockService, StockUtils stockUtils, StockEvolutionService stockEvolutionService) {
        this.algorithmService = algorithmService;
        this.articleService = articleService;
        this.stockService = stockService;
        this.stockUtils = stockUtils;
        this.stockEvolutionService = stockEvolutionService;
        this.stockRepository = stockRepository;
        this.stockContextService = stockContextService;
        this.dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    }

    public ResponseEntity<StockAnalysis> analyzeStock(String stock, boolean saveInDatabase) {
        try {
            StockAnalysis stockAnalysis = analyzeStockWithCache(stock, saveInDatabase, Optional.empty());
            return new ResponseEntity<>(stockAnalysis, HttpStatus.OK);
        } catch (ObjectNotFoundException e) {
            log.error("Could not find stock " + stock);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
        }
    }

    public ResponseEntity<StockAnalysis> analyzeStockArticles(String stock, boolean saveInDatabase) {
        try {
            StockAnalysis stockAnalysis = analyzeStockArticlesWithCache(stock, saveInDatabase, Optional.empty());
            return new ResponseEntity<>(stockAnalysis, HttpStatus.OK);
        } catch (ObjectNotFoundException e) {
            log.error("Could not find stock " + stock);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
        }
    }

    public List<StockAnalysis> getTopGrowingStocks(int noOfStocks, boolean isDescendingOrder) {
        return stockService.getStocksSortedBy("predictedChange", noOfStocks, isDescendingOrder)
                .stream()
                .map(stock -> StockAnalysis.builder().stock(stock).build())
                .collect(Collectors.toList());
    }


    public List<StockAnalysis> getTopPopularStocks(int noOfStocks) {
        return stockService.getStocksSortedBy("views", noOfStocks, true)
                .stream()
                .map(stock -> StockAnalysis.builder().stock(stock).build())
                .collect(Collectors.toList());
    }


    public List<StockInformationWithTimestamp> getListOfStocks(List<SymbolWithTimestamp> symbolsList, Integer limit) {
        List<StockInformationWithTimestamp> stockInformationWithTimestamps = new ArrayList<>();
        if (!Objects.isNull(symbolsList))
            symbolsList.stream()
                    .limit(limit)
                    .forEach(symbol -> {
                                if (!Objects.isNull(symbol) && symbol.symbol.length() > 0)
                                    stockInformationWithTimestamps.add(
                                            StockInformationWithTimestamp.builder()
                                                    .stockAnalysis(this.analyzeStockWithCache(symbol.symbol, false, Optional.of(ONE_DAY_IN_MILLIS)))
                                                    .localDateTime(symbol.date.toInstant()
                                                            .atZone(ZoneId.of("UTC"))
                                                            .toLocalDateTime())
                                                    .build()
                                    );
                            }
                    );
        stockInformationWithTimestamps.sort(Comparator.comparing(StockInformationWithTimestamp::getLocalDateTime).reversed());
        return stockInformationWithTimestamps;
    }


    private StockAnalysis analyzeStockWithCache(String stock, boolean saveInDatabase, Optional<Long> cacheValidityTimeMillis) {
        String[] stockSymbolAndCompany = stockUtils.searchStockByCompany(stock);
        StockAnalysis stockAnalysis = StockAnalysis.builder()
                .stock(Stock.builder()
                        .symbol(stockSymbolAndCompany[0])
                        .company(stockSymbolAndCompany[1])
                        .build())
                .build();
        MainThread mainThread = new MainThread(stockAnalysis, algorithmService, articleService, stockService, stockUtils, stockEvolutionService, stockRepository, stockContextService);

        try {
            mainThread.run(saveInDatabase, cacheValidityTimeMillis);
        } catch (InterruptedException e) {
            log.error("Could not process stock " + stock);
            e.printStackTrace();
        }
        if (mainThread.allSuccessful)
            return stockAnalysis;
        else
            throw new ObjectNotFoundException("Could not process stock " + stock);
    }


    private StockAnalysis analyzeStockArticlesWithCache(String stock, boolean saveInDatabase, Optional<Long> cacheValidityTimeMillis) {
        String[] stockSymbolAndCompany = stockUtils.searchStockByCompany(stock);
        StockAnalysis stockAnalysis = StockAnalysis.builder()
                .stock(Stock.builder()
                        .symbol(stockSymbolAndCompany[0])
                        .company(stockSymbolAndCompany[1])
                        .build())
                .build();
        MainArticlesThread mainArticlesThread = new MainArticlesThread(stockAnalysis, stockService, algorithmService, articleService, stockRepository);

        try {
            mainArticlesThread.run(saveInDatabase, cacheValidityTimeMillis);
        } catch (InterruptedException e) {
            log.error("Could not process stock " + stock);
            e.printStackTrace();
        }
        if (mainArticlesThread.allSuccessful)
            return stockAnalysis;
        else
            throw new ObjectNotFoundException("Could not process stock " + stock);
    }

    public Long ONE_DAY_IN_MILLIS = (long) 8.64e7;
    private DateTimeFormatter dateTimeFormatter;
    public final String HISTORY_COOKIE_PREFIX = "HIST-";
    public final String WATCHLIST_COOKIE_PREFIX = "WTLS-";

}
