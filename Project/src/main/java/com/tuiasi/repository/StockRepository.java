package com.tuiasi.repository;

import com.tuiasi.exception.ObjectNotFoundException;
import com.tuiasi.model.Stock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class StockRepository {

    private EntityManager entityManager;

    @Autowired
    public StockRepository(EntityManagerFactory entityManagerFactory) {
        this.entityManager = entityManagerFactory.createEntityManager();
    }


    public Stock add(Stock stock) {
        try {
            if (this.get(stock.getSymbol()).isPresent()) {
                log.info("Article already existing. Updating preexising entry");
                return this.update(stock)
                        .orElseThrow(() -> new ObjectNotFoundException("Stock with id: " + stock.getSymbol() + " could not be inserted."));
            }
        } catch (ObjectNotFoundException e) {
            log.info("Stock with symbol: " + stock.getSymbol() + " is not in database.");
        }

        EntityTransaction entityTransaction = entityManager.getTransaction();
        try {
            entityTransaction.begin();
            entityManager.persist(stock);
            entityTransaction.commit();
            log.info("Stock with id: " + stock.getSymbol() + " inserted.");
        } catch (Exception e) {
            log.error("Stock with id: " + stock.getSymbol() + " could not be inserted.");
            log.error(e.getMessage());
            entityTransaction.rollback();
        }
        return stock;
    }

    public Optional<Stock> get(String symbol) {
        EntityTransaction entityTransaction = entityManager.getTransaction();
        try {
            entityTransaction.begin();
            Stock result = entityManager.find(Stock.class, symbol);
            entityTransaction.commit();
            log.info("Stock with symbol: " + symbol + " retrieved.");
            return Optional.of(result);
        } catch (Exception e) {
            entityTransaction.rollback();
            throw new ObjectNotFoundException("Stock with symbol: " + symbol + " could not be found.");
        }
    }

    public Optional<Stock> update(Stock stock) {
        EntityTransaction entityTransaction = entityManager.getTransaction();
        try {
            entityTransaction.begin();
            Stock result = entityManager.merge(stock);
            entityTransaction.commit();
            log.info("Stock with symbol: " + stock.getSymbol() + " updated.");
            return Optional.of(result);
        } catch (Exception e) {
            entityTransaction.rollback();
            log.error("Stock with symbol: " + stock.getSymbol() + " could not be updated.");
            log.error(e.getMessage());
        }
        return Optional.empty();
    }

    public void delete(String symbol) {
        EntityTransaction entityTransaction = entityManager.getTransaction();
        try {
            Stock result = get(symbol).orElseThrow(ObjectNotFoundException::new);
            entityTransaction.begin();
            entityManager.remove(result);
            entityTransaction.commit();
            log.info("Stock with symbol: " + symbol + " deleted.");
        } catch (Exception e) {
            entityTransaction.rollback();
            throw new ObjectNotFoundException("Stock with symbol: " + symbol + " could not be deleted.");
        }
    }

    public List<Stock> getAll() {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Stock> criteriaQuery = criteriaBuilder.createQuery(Stock.class);
        Root<Stock> rootEntry = criteriaQuery.from(Stock.class);
        CriteriaQuery<Stock> all = criteriaQuery.select(rootEntry);
        TypedQuery<Stock> allQuery = entityManager.createQuery(all);
        return allQuery.getResultList();
    }
}
