package com.tuiasi.central_module.threading.threads;

import com.tuiasi.crawler_module.model.Stock;
import com.tuiasi.central_module.model.StockAnalysis;
import com.tuiasi.crawler_module.repository.StockRepository;
import com.tuiasi.central_module.service.AlgorithmService;
import com.tuiasi.crawler_module.service.ArticleService;
import com.tuiasi.central_module.service.StockEvolutionService;
import com.tuiasi.crawler_module.service.StockService;
import com.tuiasi.central_module.threading.NotifyingThread;
import com.tuiasi.central_module.threading.ThreadListener;
import com.tuiasi.utils.StockUtils;

import java.util.*;

public class MainThread implements ThreadListener {
    private StockAnalysis stockAnalysis;
    private AlgorithmService algorithmService;
    private StockUtils stockUtils;
    private ArticleService articleService;
    private StockService stockService;
    private StockEvolutionService stockEvolutionService;
    private StockRepository stockRepository;
    private boolean saveInDatabase;
    private int noOfAliveThreads;


    public MainThread(StockAnalysis stockAnalysis, AlgorithmService algorithmService, ArticleService articleService, StockService stockService, StockUtils stockUtils, StockEvolutionService stockEvolutionService, StockRepository stockRepository) {
        this.stockAnalysis = stockAnalysis;
        this.algorithmService = algorithmService;
        this.stockUtils = stockUtils;
        this.articleService = articleService;
        this.stockService = stockService;
        this.noOfAliveThreads = 0;
        this.stockEvolutionService = stockEvolutionService;
        this.stockRepository = stockRepository;
    }

    public void run(boolean saveInDatabase, Optional<Long> cacheValidityTimeMillis) throws InterruptedException {
        this.saveInDatabase = saveInDatabase;
        Optional<Stock> preexistingStock = stockRepository.get(this.stockAnalysis.getStock().getSymbol());

        if (preexistingStock.isPresent() && isCacheValid(preexistingStock.get(), cacheValidityTimeMillis.orElse(DEFAULT_CACHE_VALIDITY_TIME_MILLIS))) {
            this.stockAnalysis.setStock(preexistingStock.get());
            this.stockAnalysis.setArticles(
                    articleService.getLastArticlesBySymbol(
                            this.stockAnalysis.getStock().getSymbol(), NO_OF_ARTICLES_TO_RETRIEVE));
            this.stockAnalysis.setStockEvolution(stockEvolutionService.get(this.stockAnalysis.getStock().getSymbol()));
            this.stockAnalysis.getStock().setViews(this.stockAnalysis.getStock().getViews() + 1);
            System.gc();
        } else {
            List<NotifyingThread> workers = new ArrayList<>();
            workers.add(new StockInformationWorker(this.stockAnalysis.getStock()));
            workers.add(new ExpertRecommendationWorker(this.stockAnalysis.getStock()));
            workers.add(new StockRegressionWorker(this.algorithmService, this.stockAnalysis));
            workers.add(new ArticlesRetrieveWorker(this.algorithmService, this.stockAnalysis));
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
    public void onThreadComplete(Thread thread) {
        if (--noOfAliveThreads == 0) {
            Double ERC = this.stockAnalysis.getStock().getExpertsRecommendationCoefficient();
            Double HOC = this.stockAnalysis.getStock().getHistoryOptimismCoefficient();
            Double NOC = this.stockAnalysis.getStock().getNewsOptimismCoefficient();
            double predictedChange = (ERC + HOC + NOC) / 3.0 - 5;
            this.stockAnalysis.getStock().setPredictedChange(predictedChange);
            this.stockAnalysis.getStock().setArticles(this.stockAnalysis.getArticles());
            this.stockAnalysis.getStockEvolution().setStockId(this.stockAnalysis.getStock().getSymbol());

            if (saveInDatabase) {
                this.stockService.add(this.stockAnalysis.getStock());
                this.stockAnalysis.getArticles().forEach(article -> articleService.add(article));
                this.stockEvolutionService.add(stockAnalysis.getStockEvolution());
            }
        }
    }

    public final long DEFAULT_CACHE_VALIDITY_TIME_MILLIS = 3600000;
    private final int NO_OF_ARTICLES_TO_RETRIEVE = 25;


    private boolean isCacheValid(Stock stock, long cacheValidityMillis) {
        return stock.getLastUpdated().getTime() >= new Date().getTime() - cacheValidityMillis;
    }
}
