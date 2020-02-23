package com.tuiasi.repository;

import com.tuiasi.exception.ObjectNotFoundException;
import com.tuiasi.model.Article;
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
public class ArticleRepository implements ICrudRepository<Article> {

    private EntityManager entityManager;

    @Autowired
    public ArticleRepository(EntityManagerFactory entityManagerFactory) {
        this.entityManager = entityManagerFactory.createEntityManager();
    }

    private Optional<Article> getByLink(String link) {
        try {
            Article result = (Article) entityManager.createQuery("SELECT a FROM Article a where a.link = :LINK")
                    .setParameter("LINK", link).getSingleResult();
            return Optional.of(result);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public Article add(Article article) {
        Optional<Article> preexistingArticle = getByLink(article.getLink());
        if (preexistingArticle.isPresent()) {
            article.setId(preexistingArticle.get().getId());
            log.info("Article already existing. Updating preexising entry");
            return this.update(article, article.getId())
                    .orElseThrow(() -> new ObjectNotFoundException("Article with id: " + article.getId() + " could not be inserted."));
        }

        EntityTransaction entityTransaction = entityManager.getTransaction();
        try {
            entityTransaction.begin();
            entityManager.persist(article);
            entityTransaction.commit();
            log.info("Article with id: " + article.getId() + " inserted.");
        } catch (Exception e) {
            log.error("Article with id: " + article.getId() + " could not be inserted.");
            log.error(e.getMessage());
            entityTransaction.rollback();
        }
        return article;
    }

    public Optional<Article> get(int id) {
        EntityTransaction entityTransaction = entityManager.getTransaction();
        try {
            entityTransaction.begin();
            Article result = entityManager.find(Article.class, id);
            entityTransaction.commit();
            log.info("Article with id: " + id + " retrieved.");
            return Optional.of(result);
        } catch (Exception e) {
            entityTransaction.rollback();
            throw new ObjectNotFoundException("Article with id: " + id + " could not be found.");
        }
    }

    public Optional<Article> update(Article article, int id) {
        EntityTransaction entityTransaction = entityManager.getTransaction();
        try {
            entityTransaction.begin();
            Article result = entityManager.merge(article);
            entityTransaction.commit();
            log.info("Article with id: " + id + " updated.");
            return Optional.of(result);
        } catch (Exception e) {
            entityTransaction.rollback();
            log.error("Article with id: " + id + " could not be updated.");
            log.error(e.getMessage());
        }
        return Optional.empty();
    }


    public void delete(int id) throws ObjectNotFoundException {
        EntityTransaction entityTransaction = entityManager.getTransaction();
        try {
            Article result = get(id).orElseThrow(ObjectNotFoundException::new);
            entityTransaction.begin();
            entityManager.remove(result);
            entityTransaction.commit();
            log.info("Article with id: " + id + " deleted.");
        } catch (Exception e) {
            entityTransaction.rollback();
            throw new ObjectNotFoundException("Article with id: " + id + " could not be deleted.");
        }
    }

    public List<Article> getAll() {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Article> criteriaQuery = criteriaBuilder.createQuery(Article.class);
        Root<Article> rootEntry = criteriaQuery.from(Article.class);
        CriteriaQuery<Article> all = criteriaQuery.select(rootEntry);
        TypedQuery<Article> allQuery = entityManager.createQuery(all);
        return allQuery.getResultList();
    }
}