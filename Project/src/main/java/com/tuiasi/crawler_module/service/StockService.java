package com.tuiasi.crawler_module.service;


import com.tuiasi.exception.DatabaseConnectionException;
import com.tuiasi.exception.ObjectNotFoundException;
import com.tuiasi.crawler_module.model.Stock;
import com.tuiasi.crawler_module.repository.StockRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class StockService implements ICrudService<Stock, String> {

    private StockRepository repository;

    @Autowired
    public StockService(StockRepository repository) {
        this.repository = repository;
    }

    public Stock add(Stock stock) {
        try {
            return repository.add(stock);
        } catch (Exception e) {
            log.error("Could not add stock with symbol: " + stock.getSymbol() + " : " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }

    public Stock get(String symbol) {
        try {
            Optional<Stock> stock = repository.get(symbol);
            return stock
                    .orElseThrow(() -> new ObjectNotFoundException("Stock with symbol: " + symbol + " does not exist"));
        } catch (Exception e) {
            log.error("Could not get stock with symbol: " + symbol + " : " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }

    public Stock update(Stock stock, String symbol) {
        try {
            Optional<Stock> result = repository.update(stock, symbol);
            return result
                    .orElseThrow(() -> new ObjectNotFoundException("Stock with symbol: " + stock.getSymbol() + " does not exist"));
        } catch (Exception e) {
            log.error("Could not update stock with symbol: " + stock.getSymbol() + " : " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }

    public void delete(String symbol) {
        try {
            repository.delete(symbol);
        } catch (ObjectNotFoundException e) {
            log.error(e.getMessage());
        } catch (Exception e) {
            log.error("Could not delete stock with symbol: " + symbol + " : " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }

    public List<Stock> getAll() {
        try {
            return repository.getAll();
        } catch (Exception e) {
            log.error("Could not retrieve all stocks: " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }

    public List<Stock> getStocksSortedBy(String criteria, Integer limit, boolean descendingOrder){
        try {
            return repository.getStocksSortedBy(criteria, limit, descendingOrder);
        } catch (Exception e) {
            log.error("Could not retrieve all stocks: " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }
}
