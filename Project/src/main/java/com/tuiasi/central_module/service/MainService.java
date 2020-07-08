package com.tuiasi.central_module.service;


import com.tuiasi.central_module.model.StockAnalysis;
import com.tuiasi.central_module.threading.threads.MainArticlesThread;
import com.tuiasi.crawler_module.model.*;
import com.tuiasi.central_module.model.utils.StockInformationWithTimestamp;
import com.tuiasi.crawler_module.repository.StockRepository;
import com.tuiasi.crawler_module.repository.StockContextRepository;
import com.tuiasi.central_module.threading.threads.MainThread;
import com.tuiasi.crawler_module.service.ArticleService;
import com.tuiasi.crawler_module.service.StockContextService;
import com.tuiasi.crawler_module.service.StockService;
import com.tuiasi.utils.StockUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
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

    public StockAnalysis analyzeStock(String stock, boolean saveInDatabase) {
        return analyzeStockWithCache(stock, saveInDatabase, Optional.empty());
    }

    public StockAnalysis analyzeStockArticles(String stock, boolean saveInDatabase) {
        return analyzeStockArticlesWithCache(stock, saveInDatabase, Optional.empty());
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

    public void handleCookieSetting(String stock, HttpServletResponse response, String cookieValuePrefix) {
        String formattedDateTime = LocalDateTime.now().format(dateTimeFormatter);
        Cookie cookie = new Cookie(cookieValuePrefix + stock, formattedDateTime);
        cookie.setMaxAge(80000);
//        cookie.setDomain("127.0.0.1");
        response.addCookie(cookie);
    }

    public List<StockInformationWithTimestamp> getListOfStocksFromCookies(HttpServletRequest request,
                                                                          String cookieValuePrefix, Integer limit) {
        Cookie[] cookies = request.getCookies();
        List<StockInformationWithTimestamp> stockInformationWithTimestamps = new ArrayList<>();
        if (!Objects.isNull(cookies))
            Arrays.stream(cookies)
                    .limit(limit)
                    .forEach(cookie -> {
                                if (cookie.getName().startsWith(cookieValuePrefix))
                                    stockInformationWithTimestamps.add(
                                            StockInformationWithTimestamp.builder()
                                                    .stockAnalysis(this.analyzeStockWithCache(cookie.getName().substring(cookieValuePrefix.length()), false, Optional.of(ONE_DAY_IN_MILLIS)))
                                                    .localDateTime(LocalDateTime.parse(cookie.getValue(), this.dateTimeFormatter))
                                                    .build()
                                    );
                            }
                    );
        stockInformationWithTimestamps.sort(Comparator.comparing(StockInformationWithTimestamp::getLocalDateTime).reversed());
        return stockInformationWithTimestamps;
    }

    public boolean removeStockFromWatchlist(String stock, HttpServletRequest request, HttpServletResponse response) {
        String cookieValue = WATCHLIST_COOKIE_PREFIX + stock;

        boolean isCookieFound = false;
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals(cookieValue)) {
                isCookieFound = true;
                cookie.setMaxAge(0);
                response.addCookie(cookie);
            }
        }
        return isCookieFound;
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
        return stockAnalysis;
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
        return stockAnalysis;
    }

    public Long ONE_DAY_IN_MILLIS = (long) 8.64e7;
    private DateTimeFormatter dateTimeFormatter;
    public final String HISTORY_COOKIE_PREFIX = "HIST-";
    public final String WATCHLIST_COOKIE_PREFIX = "WTLS-";

}