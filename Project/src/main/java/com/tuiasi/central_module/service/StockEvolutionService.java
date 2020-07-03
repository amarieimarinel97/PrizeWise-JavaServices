package com.tuiasi.central_module.service;

import com.tuiasi.exception.DatabaseConnectionException;
import com.tuiasi.exception.ObjectNotFoundException;
import com.tuiasi.central_module.model.StockEvolution;
import com.tuiasi.crawler_module.repository.ICrudRepository;
import com.tuiasi.central_module.repository.StockEvolutionRepository;
import com.tuiasi.crawler_module.service.ICrudService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class StockEvolutionService implements ICrudService<StockEvolution, String> {

    private StockEvolutionRepository repository;

    @Autowired
    public StockEvolutionService(ICrudRepository<StockEvolution, String> stockSymbolRepository) {
        this.repository= (StockEvolutionRepository) stockSymbolRepository;
    }

    @Override
    public StockEvolution add(StockEvolution object) {
        try {
            return repository.add(object);
        } catch (Exception e) {
            log.error("Could not add evolution of stock: " + object.getStockId() + " : " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }

    @Override
    public StockEvolution get(String id) {
        try {
            Optional<StockEvolution> stockEvolution = repository.get(id);
            return stockEvolution
                    .orElseThrow(() -> new ObjectNotFoundException("Stock evolution of stock: " + id + " does not exist"));
        } catch (Exception e) {
            log.error("Could not get Stock evolution of stock: " + id + " : " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }

    @Override
    public StockEvolution update(StockEvolution object, String id) {
        try {
            Optional<StockEvolution> result = repository.update(object, id);
            return result
                    .orElseThrow(() -> new ObjectNotFoundException("Stock evolution of stock: " + id + " does not exist"));
        } catch (Exception e) {
            log.error("Could not update Stock evolution of stock: " + id + " : " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }

    @Override
    public void delete(String id) {
        try {
            repository.delete(id);
        } catch (ObjectNotFoundException e) {
            log.error(e.getMessage());
        } catch (Exception e) {
            log.error("Could not delete Stock evolution of stock: " + id + " : " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }
}
