package com.tuiasi.service;

import com.tuiasi.exception.DatabaseConnectionException;
import com.tuiasi.exception.ObjectNotFoundException;
import com.tuiasi.model.StockSymbol;
import com.tuiasi.repository.ICrudRepository;
import com.tuiasi.repository.StockSymbolRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class StockSymbolService implements ICrudService<StockSymbol, String> {


    private StockSymbolRepository repository;

    @Autowired
    public StockSymbolService(ICrudRepository<StockSymbol, String> stockSymbolRepository) {
        this.repository= (StockSymbolRepository) stockSymbolRepository;
    }
    
    @Override
    public StockSymbol add(StockSymbol stockSymbol) {
        try {
            return repository.add(stockSymbol);
        } catch (Exception e) {
            log.error("Could not add StockSymbol with id: " + stockSymbol.getSymbol() + " : " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }

    @Override
    public StockSymbol get(String symbol) {
        try {
            Optional<StockSymbol> stockSymbol = repository.get(symbol);
            return stockSymbol
                    .orElseThrow(() -> new ObjectNotFoundException("StockSymbol with symbol: " + symbol + " does not exist"));
        } catch (Exception e) {
            log.error("Could not get stockSymbol with symbol: " + symbol + " : " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }

    @Override
    public StockSymbol update(StockSymbol stockSymbol, String symbol) {
        try {
            Optional<StockSymbol> result = repository.update(stockSymbol, symbol);
            return result
                    .orElseThrow(() -> new ObjectNotFoundException("StockSymbol with symbol: " + stockSymbol.getSymbol() + " does not exist"));
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

    public List<StockSymbol> getAll() {
        try {
            return repository.getAll();
        } catch (Exception e) {
            log.error("Could not retrieve all stockSymbols: " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }
}
