package com.tuiasi.central_module.threading.threads;

import com.tuiasi.central_module.model.StockAnalysis;
import com.tuiasi.central_module.service.AlgorithmService;
import com.tuiasi.central_module.threading.NotifyingThread;

public class StockContextWorker extends NotifyingThread {

    private AlgorithmService algorithmService;
    private StockAnalysis stockAnalysis;

    public StockContextWorker(AlgorithmService algorithmService, StockAnalysis stockAnalysis) {
        this.algorithmService = algorithmService;
        this.stockAnalysis = stockAnalysis;
    }

    @Override
    public void doRun() {
        algorithmService.handleStockContextPrediction(stockAnalysis, 3);
    }
}
