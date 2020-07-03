package com.tuiasi.central_module.threading.threads;

import com.tuiasi.central_module.model.StockInformation;
import com.tuiasi.central_module.service.AlgorithmService;
import com.tuiasi.central_module.threading.NotifyingThread;

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