package com.tuiasi.threading.threads;

import com.tuiasi.model.Stock;
import com.tuiasi.model.StockEvolution;
import com.tuiasi.model.StockInformation;
import com.tuiasi.repository.StockRepository;
import com.tuiasi.service.AlgorithmService;
import com.tuiasi.service.ArticleService;
import com.tuiasi.service.StockEvolutionService;
import com.tuiasi.service.StockService;
import com.tuiasi.threading.NotifyingThread;
import com.tuiasi.threading.ThreadListener;
import com.tuiasi.utils.StockUtils;

import java.util.*;

public class MainThread implements ThreadListener {
    private StockInformation stockInformation;
    private AlgorithmService algorithmService;
    private StockUtils stockUtils;
    private ArticleService articleService;
    private StockService stockService;
    private StockEvolutionService stockEvolutionService;
    private StockRepository stockRepository;
    private boolean saveInDatabase;
    private int noOfAliveThreads;


    public MainThread(StockInformation stockInformation, AlgorithmService algorithmService, ArticleService articleService, StockService stockService, StockUtils stockUtils, StockEvolutionService stockEvolutionService, StockRepository stockRepository) {
        this.stockInformation = stockInformation;
        this.algorithmService = algorithmService;
        this.stockUtils = stockUtils;
        this.articleService = articleService;
        this.stockService = stockService;
        this.noOfAliveThreads = 0;
        this.stockEvolutionService = stockEvolutionService;
        this.stockRepository = stockRepository;
    }

    public void run(boolean saveInDatabase) throws InterruptedException {
        this.saveInDatabase = saveInDatabase;
        Optional<Stock> preexistingStock = stockRepository.get(this.stockInformation.getStock().getSymbol());

        if (preexistingStock.isPresent() && isCacheValid(preexistingStock.get())) {
            this.stockInformation.setStock(preexistingStock.get());
            this.stockInformation.setArticles(
                    articleService.getLastArticlesBySymbol(
                            this.stockInformation.getStock().getSymbol(), NO_OF_ARTICLES_TO_RETRIEVE));
            this.stockInformation.setStockEvolution(stockEvolutionService.get(this.stockInformation.getStock().getSymbol()));
            this.stockInformation.getStock().setHits(this.stockInformation.getStock().getHits() + 1);
            System.gc();
        } else {
            List<NotifyingThread> workers = new ArrayList<>();
            workers.add(new StockInformationWorker(this.stockInformation.getStock()));
            workers.add(new ExpertRecommendationWorker(this.stockInformation.getStock()));
            workers.add(new StockRegressionWorker(this.algorithmService, this.stockInformation));
            workers.add(new ArticlesRetrieveWorker(this.algorithmService, this.stockInformation));
            this.stockInformation.getStock().setHits(preexistingStock.isPresent() ? preexistingStock.get().getHits() + 1 : 1);
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
            Double ERC = this.stockInformation.getStock().getExpertsRecommendationCoefficient();
            Double HOC = this.stockInformation.getStock().getHistoryOptimismCoefficient();
            Double NOC = this.stockInformation.getStock().getNewsOptimismCoefficient();
            double predictedChange = (ERC + HOC + NOC) / 3.0 - 5;
            this.stockInformation.getStock().setPredictedChange(predictedChange);
            this.stockInformation.getStock().setArticles(this.stockInformation.getArticles());
            this.stockInformation.getStockEvolution().setStockId(this.stockInformation.getStock().getSymbol());

            if (saveInDatabase) {
                this.stockService.add(this.stockInformation.getStock());
                this.stockInformation.getArticles().forEach(article -> articleService.add(article));
                this.stockEvolutionService.add(stockInformation.getStockEvolution());
            }
        }
    }

    private final long CACHE_VALIDITY_TIME_MILLIS = 3600000;
    private final int NO_OF_ARTICLES_TO_RETRIEVE = 25;


    private boolean isCacheValid(Stock stock) {
        return stock.getLastUpdated().getTime() >= new Date().getTime() - CACHE_VALIDITY_TIME_MILLIS;
    }
}
