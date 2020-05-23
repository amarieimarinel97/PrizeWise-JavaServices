package com.tuiasi.repository;

import com.tuiasi.exception.ObjectNotFoundException;
import com.tuiasi.model.StockSymbol;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class StockSymbolRepository implements ICrudRepository<StockSymbol, String> {

    private EntityManager entityManager;

    @Autowired
    public StockSymbolRepository(EntityManagerFactory entityManagerFactory) {
        this.entityManager = entityManagerFactory.createEntityManager();
    }

    public boolean checkIfTableIsEmpty() {
        return entityManager.createQuery(SQL_EMPTY_TABLE).setMaxResults(1).getResultList().isEmpty();
    }

    public StockSymbol add(StockSymbol stockSymbol) {

        EntityTransaction entityTransaction = entityManager.getTransaction();
        try {
            entityTransaction.begin();
            entityManager.persist(stockSymbol);
            entityTransaction.commit();
            log.info("Symbol with symbol: " + stockSymbol.getSymbol() + " inserted.");
        } catch (Exception e) {
            log.error("Symbol with symbol: " + stockSymbol.getSymbol() + " could not be inserted.");
            log.error(e.getMessage());
            entityTransaction.rollback();
        }
        return stockSymbol;
    }

    public Optional<StockSymbol> get(String symbol) {
        EntityTransaction entityTransaction = entityManager.getTransaction();
        try {
            entityTransaction.begin();
            StockSymbol result = entityManager.find(StockSymbol.class, symbol);
            entityTransaction.commit();
            log.info("StockSymbol with symbol: " + symbol + " retrieved.");
            return Optional.of(result);
        } catch (Exception e) {
            entityTransaction.rollback();
            throw new ObjectNotFoundException("StockSymbol with symbol: " + symbol + " could not be found.");
        }
    }

    public Optional<StockSymbol> update(StockSymbol stockSymbol, String symbol) {
        EntityTransaction entityTransaction = entityManager.getTransaction();
        try {
            entityTransaction.begin();
            StockSymbol result = entityManager.merge(stockSymbol);
            entityTransaction.commit();
            log.info("StockSymbol with symbol: " + symbol + " updated.");
            return Optional.of(result);
        } catch (Exception e) {
            entityTransaction.rollback();
            log.error("StockSymbol with symbol: " + symbol + " could not be updated.");
            log.error(e.getMessage());
        }
        return Optional.empty();
    }

    public void delete(String symbol) {
        EntityTransaction entityTransaction = entityManager.getTransaction();
        try {
            StockSymbol result = get(symbol).orElseThrow(ObjectNotFoundException::new);
            entityTransaction.begin();
            entityManager.remove(result);
            entityTransaction.commit();
            log.info("StockSymbol with symbol: " + symbol + " deleted.");
        } catch (Exception e) {
            entityTransaction.rollback();
            throw new ObjectNotFoundException("StockSymbol with symbol: " + symbol + " could not be deleted.");
        }
    }

    public List<StockSymbol> getAll() {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<StockSymbol> criteriaQuery = criteriaBuilder.createQuery(StockSymbol.class);
        Root<StockSymbol> rootEntry = criteriaQuery.from(StockSymbol.class);
        CriteriaQuery<StockSymbol> all = criteriaQuery.select(rootEntry);
        TypedQuery<StockSymbol> allQuery = entityManager.createQuery(all);
        return allQuery.getResultList();
    }

    private final String SQL_EMPTY_TABLE = "SELECT 1 FROM StockSymbol";
    ;
}
