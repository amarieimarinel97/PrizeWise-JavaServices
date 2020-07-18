package com.tuiasi.central_module.threading.threads;

import com.tuiasi.central_module.model.StockAnalysis;
import com.tuiasi.central_module.service.AlgorithmService;
import com.tuiasi.central_module.service.StockEvolutionService;
import com.tuiasi.central_module.threading.NotifyingThread;
import com.tuiasi.central_module.threading.ThreadListener;
import com.tuiasi.crawler_module.model.Article;
import com.tuiasi.crawler_module.model.Stock;
import com.tuiasi.crawler_module.repository.StockRepository;
import com.tuiasi.crawler_module.service.ArticleService;
import com.tuiasi.crawler_module.service.StockService;
import com.tuiasi.exception.ObjectNotFoundException;

import java.util.*;

public class MainArticlesThread implements ThreadListener {

    private StockAnalysis stockAnalysis;
    private AlgorithmService algorithmService;
    private ArticleService articleService;
    private StockRepository stockRepository;
    private StockService stockService;
    private boolean saveInDatabase;
    public boolean allSuccessful;

    public MainArticlesThread(StockAnalysis stockAnalysis, StockService stockService, AlgorithmService algorithmService, ArticleService articleService, StockRepository stockRepository) {
        this.stockAnalysis = stockAnalysis;
        this.algorithmService = algorithmService;
        this.articleService = articleService;
        this.stockRepository = stockRepository;
        this.stockService = stockService;
        this.allSuccessful = true;
    }

    private int noOfAliveThreads;


    public void run(boolean saveInDatabase, Optional<Long> cacheValidityTimeMillis) throws InterruptedException {
        this.saveInDatabase = saveInDatabase;
        Optional<Stock> preexistingStock = stockRepository.get(this.stockAnalysis.getStock().getSymbol());
        Set<Article> articlesWithBody = new HashSet<>();
        if (preexistingStock.isPresent())
            articlesWithBody = articleService.getLastArticlesWithBodiesBySymbol(this.stockAnalysis.getStock().getSymbol(), NO_OF_ARTICLES_TO_RETRIEVE);

        if (preexistingStock.isPresent() && isCacheValid(preexistingStock.get(), cacheValidityTimeMillis.orElse(DEFAULT_CACHE_VALIDITY_TIME_MILLIS)) && articlesWithBody.size() >= 4) {
            this.stockAnalysis.setStock(preexistingStock.get());
            this.stockAnalysis.setArticles(
                    articleService.getLastArticlesBySymbol(
                            this.stockAnalysis.getStock().getSymbol(), NO_OF_ARTICLES_TO_RETRIEVE));
            this.stockAnalysis.getStock().setViews(this.stockAnalysis.getStock().getViews() + 1);
            System.gc();
        } else {
            List<NotifyingThread> workers = new ArrayList<>();
            workers.add(new StockInformationWorker(this.stockAnalysis.getStock()));
            workers.add(new ArticlesRetrieveWorker(this.algorithmService, this.stockAnalysis, Optional.of(10)));
            this.stockAnalysis.getStock().setViews(preexistingStock.map(stock -> stock.getViews() + 1).orElse(1));
            this.noOfAliveThreads = workers.size();

            for (NotifyingThread worker : workers) {
                worker.addListener(this);
                worker.start();
            }

            for (NotifyingThread worker : workers)
                worker.join();


        }

    }

    @Override
    public void onThreadComplete(Thread thread, boolean finishedSuccessful) {
        if (finishedSuccessful) {
            if (--noOfAliveThreads <= 0) {
                this.stockAnalysis.getStock().setArticles(this.stockAnalysis.getArticles());
                if (saveInDatabase) {
                    if (stockRepository.get(stockAnalysis.getStock().getSymbol()).isEmpty())
                        this.stockService.add(stockAnalysis.getStock());
                    this.stockAnalysis.getArticles().forEach(article -> articleService.add(article));
                }
            }
        } else {
            this.allSuccessful = false;
        }
    }

    public final long DEFAULT_CACHE_VALIDITY_TIME_MILLIS = 3600000;
    private final int NO_OF_ARTICLES_TO_RETRIEVE = 50;

    private boolean isCacheValid(Stock stock, long cacheValidityMillis) {
        return stock.getLastUpdated().getTime() >= new Date().getTime() - cacheValidityMillis;
    }
}
