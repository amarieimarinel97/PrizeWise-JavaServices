package com.tuiasi.crawler_module.repository;

import com.tuiasi.exception.ObjectNotFoundException;
import com.tuiasi.crawler_module.model.StockContext;
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
public class StockContextRepository implements ICrudRepository<StockContext, String> {

    private EntityManager entityManager;

    @Autowired
    public StockContextRepository(EntityManagerFactory entityManagerFactory) {
        this.entityManager = entityManagerFactory.createEntityManager();
    }

    public boolean checkIfTableIsEmpty() {
        return entityManager.createQuery(SQL_EMPTY_TABLE).setMaxResults(1).getResultList().isEmpty();
    }

    public StockContext add(StockContext stockContext) {

        EntityTransaction entityTransaction = entityManager.getTransaction();
        try {
            entityTransaction.begin();
            entityManager.persist(stockContext);
            entityTransaction.commit();
            log.info("Sector with sector: " + stockContext.getSector() + " inserted.");
        } catch (Exception e) {
            log.error("Sector with sector: " + stockContext.getSector() + " could not be inserted.");
            log.error(e.getMessage());
            entityTransaction.rollback();
        }
        return stockContext;
    }

    public Optional<StockContext> get(String sector) {
        EntityTransaction entityTransaction = entityManager.getTransaction();
        try {
            entityTransaction.begin();
            StockContext result = entityManager.find(StockContext.class, sector);
            entityTransaction.commit();
            log.info("StockSector with sector: " + sector + " retrieved.");
            return Optional.of(result);
        } catch (Exception e) {
            entityTransaction.rollback();
            throw new ObjectNotFoundException("StockSector with sector: " + sector + " could not be found.");
        }
    }

    public Optional<StockContext> update(StockContext stockContext, String sector) {
        EntityTransaction entityTransaction = entityManager.getTransaction();
        try {
            entityTransaction.begin();
            StockContext result = entityManager.merge(stockContext);
            entityTransaction.commit();
            log.info("StockSector with sector: " + sector + " updated.");
            return Optional.of(result);
        } catch (Exception e) {
            entityTransaction.rollback();
            log.error("StockSector with sector: " + sector + " could not be updated.");
            log.error(e.getMessage());
        }
        return Optional.empty();
    }

    public void delete(String sector) {
        EntityTransaction entityTransaction = entityManager.getTransaction();
        try {
            StockContext result = get(sector).orElseThrow(ObjectNotFoundException::new);
            entityTransaction.begin();
            entityManager.remove(result);
            entityTransaction.commit();
            log.info("StockSector with sector: " + sector + " deleted.");
        } catch (Exception e) {
            entityTransaction.rollback();
            throw new ObjectNotFoundException("StockSector with sector: " + sector + " could not be deleted.");
        }
    }

    public List<StockContext> getAll() {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<StockContext> criteriaQuery = criteriaBuilder.createQuery(StockContext.class);
        Root<StockContext> rootEntry = criteriaQuery.from(StockContext.class);
        CriteriaQuery<StockContext> all = criteriaQuery.select(rootEntry);
        TypedQuery<StockContext> allQuery = entityManager.createQuery(all);
        return allQuery.getResultList();
    }

    private final String SQL_EMPTY_TABLE = "SELECT 1 FROM StockSector";
    ;
}
