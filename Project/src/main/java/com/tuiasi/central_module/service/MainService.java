package com.tuiasi.central_module.service;


import com.tuiasi.central_module.model.Dashboard;
import com.tuiasi.central_module.model.DashboardDTO;
import com.tuiasi.central_module.model.StockAnalysis;
import com.tuiasi.central_module.model.StockEvolution;
import com.tuiasi.central_module.model.utils.StockInformationWithTimestamp;
import com.tuiasi.central_module.threading.threads.MainArticlesThread;
import com.tuiasi.central_module.threading.threads.MainThread;
import com.tuiasi.crawler_module.model.*;
import com.tuiasi.crawler_module.repository.StockRepository;
import com.tuiasi.crawler_module.service.ArticleService;
import com.tuiasi.crawler_module.service.PostService;
import com.tuiasi.crawler_module.service.StockContextService;
import com.tuiasi.crawler_module.service.StockService;
import com.tuiasi.exception.ObjectNotFoundException;
import com.tuiasi.utils.StockUtils;
import com.tuiasi.utils.SymbolWithTimestamp;
import com.tuiasi.utils.SymbolsTimestampedList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.tuiasi.crawler_module.model.SocialNetwork.TWITTER;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

@Service
@Slf4j
public class MainService {

    private final AlgorithmService algorithmService;
    private final PostService postService;
    private final ArticleService articleService;
    private final StockService stockService;
    private final StockUtils stockUtils;
    private final StockEvolutionService stockEvolutionService;
    private final StockRepository stockRepository;
    private final StockContextService stockContextService;

    @Autowired
    public MainService(StockContextService stockContextService, PostService postService, StockRepository stockRepository, AlgorithmService algorithmService, ArticleService articleService, StockService stockService, StockUtils stockUtils, StockEvolutionService stockEvolutionService) {
        this.algorithmService = algorithmService;
        this.articleService = articleService;
        this.postService = postService;
        this.stockService = stockService;
        this.stockUtils = stockUtils;
        this.stockEvolutionService = stockEvolutionService;
        this.stockRepository = stockRepository;
        this.stockContextService = stockContextService;
        this.dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    }

    public ResponseEntity<StockAnalysis> analyzeStock(String stock, boolean saveInDatabase) {
        try {
            StockAnalysis stockAnalysis = analyzeStockWithCache(stock.trim().toUpperCase(), saveInDatabase, Optional.empty());
            return new ResponseEntity<>(stockAnalysis, HttpStatus.OK);
        } catch (ObjectNotFoundException e) {
            log.error("Could not find stock " + stock);
            Arrays.stream(e.getStackTrace()).map(s -> s + "\n").reduce((a, b) -> a + b).ifPresent(log::error);
            log.warn("Exception1:"+ e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.warn("Exception2:"+ e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
        }
    }

    public Dashboard getDashboard(DashboardDTO input) {

        List<StockAnalysis> portfolioResult = getListOfStocks(input.getPortfolio());
        List<StockAnalysis> historyResult = getListOfStocks(input.getHistory());
        List<StockAnalysis> watchListResult = getListOfStocks(input.getWatchList());

        List<StockAnalysis> popularResult = enrichPopularStocksData(getTopPopularStocks(DASHBOARD_LIMIT));
        List<Article> recentArticles = articleService.getLastArticlesWithBodies(DASHBOARD_LIMIT);
        List<Post> recentPosts = postService.getRecentPosts(DASHBOARD_LIMIT, TWITTER);

        return Dashboard.builder()
                .portfolio(portfolioResult)
                .history(historyResult)
                .watchlist(watchListResult)
                .popular(popularResult)
                .recentPosts(recentPosts)
                .recentArticles(recentArticles)
                .build();
    }

    private List<StockAnalysis> enrichPopularStocksData(List<StockAnalysis> popularStocks) {

        popularStocks.forEach(stock -> {
            String symbol = stock.getStock().getSymbol();
            stock.setStockEvolution(stockEvolutionService.get(symbol));
            stock.setStockContext(stockContextService.get(symbol));

            stock.setArticles(articleService.getLastArticlesBySymbol(symbol, DASHBOARD_LIMIT));
            stock.setPosts(postService.getLastPostsBySymbol(symbol, DASHBOARD_LIMIT));
        });
        return popularStocks;
    }

    public ResponseEntity<StockAnalysis> analyzeStockArticles(String stock, boolean saveInDatabase) {
        try {
            StockAnalysis stockAnalysis = analyzeStockArticlesWithCache(stock, saveInDatabase, Optional.empty());
            return new ResponseEntity<>(stockAnalysis, HttpStatus.OK);
        } catch (ObjectNotFoundException e) {
            log.error("Could not find stock " + stock);
            log.error(Arrays.asList(e.getStackTrace()).toString());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
        }
    }

    public List<StockAnalysis> getTopGrowingStocks(int noOfStocks, boolean isDescendingOrder) {
        return stockService.getStocksSortedBy("predictedChange", noOfStocks, isDescendingOrder)
                .stream()
                .map(stock -> StockAnalysis.builder().stock(stock).build())
                .collect(toList());
    }


    public List<StockAnalysis> getTopPopularStocks(int noOfStocks) {
        return stockService.getStocksSortedBy("views", noOfStocks, true)
                .stream()
                .map(stock -> StockAnalysis.builder().stock(stock).build())
                .collect(toList());
    }


    public List<StockInformationWithTimestamp> getListOfStocks(List<SymbolWithTimestamp> symbolsList, Integer limit) {
        List<StockInformationWithTimestamp> stockInformationWithTimestamps = new ArrayList<>();
        if (!isNull(symbolsList))
            symbolsList.stream()
                    .limit(limit)
                    .forEach(symbol -> {
                                if (!isNull(symbol) && symbol.symbol.length() > 0)
                                    stockInformationWithTimestamps.add(
                                            StockInformationWithTimestamp.builder()
                                                    .stockAnalysis(this.analyzeStockWithCache(symbol.symbol, false, of(ONE_DAY_IN_MILLIS)))
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

    public List<StockAnalysis> getListOfStocks(List<String> symbols) {

        List<StockAnalysis> result = new ArrayList<>();
        if (nonNull(symbols))
            symbols.stream().limit(DASHBOARD_LIMIT).forEach(symbol -> {
                if (nonNull(symbol) && !symbol.isEmpty())
                    result.add(getStockAnalysis(symbol));
            });
        return result;
    }

    private StockAnalysis getStockAnalysis(String symbol) {

        Stock stock = stockService.get(symbol);

        StockEvolution evolution = stockEvolutionService.get(symbol);
        StockContext context = stockContextService.get(symbol);

        Set<Article> articles = articleService.getLastArticlesBySymbol(symbol, DASHBOARD_LIMIT);
        List<Post> posts = postService.getLastPostsBySymbol(symbol, DASHBOARD_LIMIT);


        return StockAnalysis.builder().stock(stock).stockEvolution(evolution).stockContext(context).articles(articles).posts(posts).build();
    }


    private StockAnalysis analyzeStockWithCache(String stock, boolean saveInDatabase, Optional<Long> cacheValidityTimeMillis) {
        String[] stockSymbolAndCompany = stockUtils.searchStockByCompany(stock);
        StockAnalysis stockAnalysis = StockAnalysis.builder()
                .stock(Stock.builder()
                        .symbol(stockSymbolAndCompany[0])
                        .company(stockSymbolAndCompany[1])
                        .build())
                .build();
        MainThread mainThread = new MainThread(stockAnalysis, algorithmService, articleService, postService, stockService, stockUtils, stockEvolutionService, stockRepository, stockContextService);

        try {
            mainThread.run(saveInDatabase, cacheValidityTimeMillis);
        } catch (InterruptedException e) {
            log.error("Could not process stock " + stock);
            log.error(Arrays.asList(e.getStackTrace()).toString());
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
            log.error(Arrays.asList(e.getStackTrace()).toString());
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
    private final static int DASHBOARD_LIMIT = 6;


}
