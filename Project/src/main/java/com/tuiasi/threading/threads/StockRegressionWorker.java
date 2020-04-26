package com.tuiasi.threading.threads;

import com.tuiasi.model.StockInformation;
import com.tuiasi.service.AlgorithmService;
import com.tuiasi.threading.NotifyingThread;

public class StockRegressionWorker extends NotifyingThread {

    private AlgorithmService algorithmService;
    private StockInformation stockInformation;

    public StockRegressionWorker(AlgorithmService algorithmService, StockInformation stockInformation) {
        this.algorithmService = algorithmService;
        this.stockInformation = stockInformation;
    }

    @Override
    public void doRun() {
        algorithmService.handlePredictionBasedOnHistory(stockInformation, 3);
    }
}