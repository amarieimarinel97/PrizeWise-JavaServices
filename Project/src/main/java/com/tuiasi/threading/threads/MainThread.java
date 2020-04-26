package com.tuiasi.threading.threads;

import com.tuiasi.model.StockInformation;
import com.tuiasi.service.AlgorithmService;
import com.tuiasi.service.ArticleService;
import com.tuiasi.service.StockService;
import com.tuiasi.threading.NotifyingThread;
import com.tuiasi.threading.ThreadListener;
import com.tuiasi.utils.StockUtils;

import java.util.ArrayList;
import java.util.List;

public class MainThread implements ThreadListener {
    private StockInformation stockInformation;
    private AlgorithmService algorithmService;
    private StockUtils stockUtils;
    private ArticleService articleService;
    private StockService stockService;
    private int noOfAliveThreads;
    private boolean saveInDatabase;

    public MainThread(StockInformation stockInformation,AlgorithmService algorithmService, ArticleService articleService, StockService stockService, StockUtils stockUtils) {
        this.stockInformation = stockInformation;
        this.algorithmService = algorithmService;
        this.stockUtils = stockUtils;
        this.articleService = articleService;
        this.stockService = stockService;
        this.noOfAliveThreads = 0;
    }

    public void run(boolean saveInDatabase) throws InterruptedException {
        this.saveInDatabase = saveInDatabase;

        List<NotifyingThread> workers = new ArrayList<>();
        workers.add(new StockInformationWorker(this.stockInformation.getStock()));
        workers.add(new ExpertRecommendationWorker(this.stockInformation.getStock()));
        workers.add(new StockRegressionWorker(this.algorithmService, this.stockInformation));
        workers.add(new ArticlesRetrieveWorker(this.algorithmService, this.stockInformation));
        this.noOfAliveThreads = workers.size();

        for (NotifyingThread worker : workers) {
            worker.addListener(this);
            worker.start();
        }

        for (NotifyingThread worker : workers)
            worker.join();

    }

    @Override
    public void onThreadComplete(Thread thread) {
        if (noOfAliveThreads == 0) {
            Double ERC = this.stockInformation.getStock().getExpertsRecommendationCoefficient();
            Double HOC = this.stockInformation.getStock().getHistoryOptimismCoefficient();
            Double NOC = this.stockInformation.getStock().getNewsOptimismCoefficient();
            double predictedChange = (ERC + HOC + NOC) / 3.0 + 5;
            this.stockInformation.getStock().setPredictedChange(predictedChange);

            if (saveInDatabase) {
                this.stockService.add(this.stockInformation.getStock());
                this.stockInformation.getArticles().forEach(article -> articleService.add(article));
            }
        }
    }
}
