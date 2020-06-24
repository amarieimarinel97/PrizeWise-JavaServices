package com.tuiasi.service;


import com.tuiasi.model.*;
import com.tuiasi.model.utils.StockInformationWithTimestamp;
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
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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
        this.dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
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
        return crawlBusinessInsiderWithCache(stock, saveInDatabase, Optional.empty());
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
        return stockService.getStocksSortedBy("predictedChange", noOfStocks, isDescendingOrder)
                .stream()
                .map(stock -> StockInformation.builder().stock(stock).build())
                .collect(Collectors.toList());
    }


    public List<StockInformation> getTopPopularStocks(int noOfStocks) {
        return stockService.getStocksSortedBy("hits", noOfStocks, true)
                .stream()
                .map(stock -> StockInformation.builder().stock(stock).build())
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
                                                    .stockInformation(this.crawlBusinessInsiderWithCache(cookie.getName().substring(cookieValuePrefix.length()), false, Optional.of(ONE_DAY_IN_MILLIS)))
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


    private StockInformation crawlBusinessInsiderWithCache(String stock, boolean saveInDatabase, Optional<Long> cacheValidityTimeMillis) {
        String[] stockSymbolAndCompany = stockUtils.searchStockByCompany(stock);
        StockInformation stockInformation = StockInformation.builder()
                .stock(Stock.builder()
                        .symbol(stockSymbolAndCompany[0])
                        .company(stockSymbolAndCompany[1])
                        .build())
                .build();
        MainThread mainThread = new MainThread(stockInformation, algorithmService, articleService, stockService, stockUtils, stockEvolutionService, stockRepository);

        try {
            mainThread.run(saveInDatabase, cacheValidityTimeMillis);
        } catch (InterruptedException e) {
            log.error("Could not process stock " + stock);
            e.printStackTrace();
        }
        return stockInformation;
    }

    public Long ONE_DAY_IN_MILLIS = (long) 8.64e7;
    private DateTimeFormatter dateTimeFormatter;
    public final String HISTORY_COOKIE_PREFIX = "HIST-";
    public final String WATCHLIST_COOKIE_PREFIX = "WTLS-";

}
