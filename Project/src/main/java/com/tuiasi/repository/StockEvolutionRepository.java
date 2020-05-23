package com.tuiasi.repository;

import com.tuiasi.exception.ObjectNotFoundException;
import com.tuiasi.model.StockEvolution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.util.Optional;

@Slf4j
@Repository
public class StockEvolutionRepository implements ICrudRepository<StockEvolution, String> {

    private EntityManager entityManager;

    @Autowired
    public StockEvolutionRepository(EntityManagerFactory entityManagerFactory) {
        this.entityManager = entityManagerFactory.createEntityManager();
    }

    @Override
    public StockEvolution add(StockEvolution stockEvolution) {
        Optional<StockEvolution> preexistingEntry = get(stockEvolution.getStockId());
        if (preexistingEntry.isPresent()) {
            log.info("Stock evolution entry already existing. Updating preexising entry");
            return this.update(preexistingEntry.get(), preexistingEntry.get().getStockId())
                    .orElseThrow(() -> new ObjectNotFoundException("Stock evolution of stock: " + stockEvolution.getStockId() + " could not be inserted."));
        }

        EntityTransaction entityTransaction = entityManager.getTransaction();
        try {
            entityTransaction.begin();
            entityManager.persist(stockEvolution);
            entityTransaction.commit();
            log.info("Stock evolution of stock: " + stockEvolution.getStockId() + " inserted.");
        } catch (Exception e) {
            log.error("Stock evolution of stock: " + stockEvolution.getStockId() + " could not be inserted.");
            log.error(e.getMessage());
            entityTransaction.rollback();
        }
        return stockEvolution;
    }

    @Override
    public Optional<StockEvolution> get(String symbol) {
        EntityTransaction entityTransaction = entityManager.getTransaction();
        try {
            entityTransaction.begin();
            StockEvolution result = entityManager.find(StockEvolution.class, symbol);
            entityTransaction.commit();
            log.info("Stock evolution of stock: " + symbol + " retrieved.");
            return Optional.of(result);
        } catch (Exception e) {
            entityTransaction.rollback();
            log.warn("Stock evolution of stock: " + symbol + " could not be found.");
            return Optional.empty();
        }
    }

    @Override
    public Optional<StockEvolution> update(StockEvolution stockEvolution, String symbol) {
        EntityTransaction entityTransaction = entityManager.getTransaction();
        try {
            entityTransaction.begin();
            StockEvolution result = entityManager.merge(stockEvolution);
            entityTransaction.commit();
            log.info("Stock evolution of stock: " + symbol + " updated.");
            return Optional.of(result);
        } catch (Exception e) {
            entityTransaction.rollback();
            log.error("Stock evolution of stock: " + symbol + " could not be updated.");
            log.error(e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public void delete(String symbol) {
        EntityTransaction entityTransaction = entityManager.getTransaction();
        try {
            StockEvolution result = get(symbol).orElseThrow(ObjectNotFoundException::new);
            entityTransaction.begin();
            entityManager.remove(result);
            entityTransaction.commit();
            log.info("Stock evolution of stock: " + symbol + " deleted.");
        } catch (Exception e) {
            entityTransaction.rollback();
            throw new ObjectNotFoundException("Stock evolution of stock: " + symbol + " could not be deleted.");
        }
    }
}
