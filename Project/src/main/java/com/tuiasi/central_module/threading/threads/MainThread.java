package com.tuiasi.central_module.threading.threads;

import com.tuiasi.crawler_module.model.Stock;
import com.tuiasi.central_module.model.StockAnalysis;
import com.tuiasi.crawler_module.repository.StockRepository;
import com.tuiasi.central_module.service.AlgorithmService;
import com.tuiasi.crawler_module.service.ArticleService;
import com.tuiasi.central_module.service.StockEvolutionService;
import com.tuiasi.crawler_module.service.StockContextService;
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
    private StockContextService stockContextService;
    private boolean saveInDatabase;
    private int noOfAliveThreads;


    public MainThread(StockAnalysis stockAnalysis, AlgorithmService algorithmService, ArticleService articleService, StockService stockService, StockUtils stockUtils, StockEvolutionService stockEvolutionService, StockRepository stockRepository, StockContextService stockContextService) {
        this.stockAnalysis = stockAnalysis;
        this.algorithmService = algorithmService;
        this.stockUtils = stockUtils;
        this.articleService = articleService;
        this.stockService = stockService;
        this.noOfAliveThreads = 0;
        this.stockEvolutionService = stockEvolutionService;
        this.stockRepository = stockRepository;
        this.stockContextService = stockContextService;
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
            this.stockAnalysis.setStockContext(stockContextService.get(this.stockAnalysis.getStock().getSymbol()));
            this.stockAnalysis.getStock().setViews(this.stockAnalysis.getStock().getViews() + 1);
            System.gc();
        } else {
            List<NotifyingThread> workers = new ArrayList<>();
            workers.add(new StockInformationWorker(this.stockAnalysis.getStock()));
            workers.add(new ExpertRecommendationWorker(this.stockAnalysis.getStock()));
            workers.add(new StockRegressionWorker(this.algorithmService, this.stockAnalysis));
            workers.add(new StockContextWorker(this.algorithmService, this.stockAnalysis));
            workers.add(new ArticlesRetrieveWorker(this.algorithmService, this.stockAnalysis, Optional.empty()));
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
            Double indicesCoefficient = this.stockAnalysis.getStockContext().getIndicesPrediction();
            Double sectorCoefficient = this.stockAnalysis.getStockContext().getSectorPrediction();
            double predictedChange = (0.25 * ERC + 0.25 * HOC + 0.25 * NOC + 0.125 * indicesCoefficient + 0.125 * sectorCoefficient) - 5;
            this.stockAnalysis.getStock().setPredictedChange(predictedChange);
            this.stockAnalysis.getStock().setArticles(this.stockAnalysis.getArticles());
            this.stockAnalysis.getStockEvolution().setStockId(this.stockAnalysis.getStock().getSymbol());
            this.stockAnalysis.getStockContext().setName(this.stockAnalysis.getStock().getCompany());
            this.stockAnalysis.getStockContext().setSymbol(this.stockAnalysis.getStock().getSymbol());



            if (saveInDatabase) {
                this.stockService.add(this.stockAnalysis.getStock());
                this.stockAnalysis.getArticles().forEach(article -> articleService.add(article));
                this.stockEvolutionService.add(stockAnalysis.getStockEvolution());
                this.stockContextService.add(stockAnalysis.getStockContext());
            }
        }
    }

    public final long DEFAULT_CACHE_VALIDITY_TIME_MILLIS = 3600000;
    private final int NO_OF_ARTICLES_TO_RETRIEVE = 50;


    private boolean isCacheValid(Stock stock, long cacheValidityMillis) {
        return stock.getLastUpdated().getTime() >= new Date().getTime() - cacheValidityMillis;
    }
}
