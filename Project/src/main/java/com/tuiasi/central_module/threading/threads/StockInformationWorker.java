package com.tuiasi.central_module.threading.threads;

import com.tuiasi.exception.ObjectNotFoundException;
import com.tuiasi.crawler_module.model.Stock;
import com.tuiasi.central_module.threading.NotifyingThread;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.Date;

public class StockInformationWorker extends NotifyingThread {
    private Stock stock;

    public StockInformationWorker(Stock stock) {
        this.stock = stock;
    }

    @Override
    public void doRun() {
        crawlStockInfo(this.stock);
    }


    public void crawlStockInfo(Stock stock) {
        Document doc = null;
        try {
            doc = Jsoup.connect("http://markets.businessinsider.com/stocks/" + stock.getSymbol().trim() + "-stock").get();
        } catch (IOException e) {
            throw new ObjectNotFoundException("Symbol " + stock.getSymbol() + " not found.");
        }
        stock.setPrice(getPriceFromPage(doc));
        stock.setLastUpdated(new Date());
    }

    private double getPriceFromPage(Document doc) {
        return Double.parseDouble(
                doc.select("div.price-section__values > span.price-section__current-value")
                        .text()
                        .split(" ")[0]
                        .replaceAll("[^0-9.]", ""));
    }
}