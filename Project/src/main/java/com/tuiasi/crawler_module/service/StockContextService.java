package com.tuiasi.crawler_module.service;

import com.tuiasi.exception.DatabaseConnectionException;
import com.tuiasi.exception.ObjectNotFoundException;
import com.tuiasi.crawler_module.model.StockContext;
import com.tuiasi.crawler_module.repository.ICrudRepository;
import com.tuiasi.crawler_module.repository.StockContextRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class StockContextService implements ICrudService<StockContext, String> {


    private StockContextRepository repository;

    @Autowired
    public StockContextService(ICrudRepository<StockContext, String> stockSymbolRepository) {
        this.repository= (StockContextRepository) stockSymbolRepository;
    }
    
    @Override
    public StockContext add(StockContext stockContext) {
        try {
            return repository.add(stockContext);
        } catch (Exception e) {
            log.error("Could not add StockSymbol with id: " + stockContext.getSymbol() + " : " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }

    @Override
    public StockContext get(String symbol) {
        try {
            Optional<StockContext> stockSymbol = repository.get(symbol);
            return stockSymbol
                    .orElseThrow(() -> new ObjectNotFoundException("StockSymbol with symbol: " + symbol + " does not exist"));
        } catch (Exception e) {
            log.error("Could not get stockSymbol with symbol: " + symbol + " : " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }

    @Override
    public StockContext update(StockContext stockContext, String symbol) {
        try {
            Optional<StockContext> result = repository.update(stockContext, symbol);
            return result
                    .orElseThrow(() -> new ObjectNotFoundException("StockSymbol with symbol: " + stockContext.getSymbol() + " does not exist"));
        } catch (Exception e) {
            log.error("Could not update stockSymbol with symbol: " + symbol + " : " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }

    @Override
    public void delete(String symbol) {
        try {
            repository.delete(symbol);
        } catch (ObjectNotFoundException e) {
            log.error(e.getMessage());
        } catch (Exception e) {
            log.error("Could not delete stockSymbol with symbol: " + symbol + " : " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }

    public List<StockContext> getAll() {
        try {
            return repository.getAll();
        } catch (Exception e) {
            log.error("Could not retrieve all stockSymbols: " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }
}
