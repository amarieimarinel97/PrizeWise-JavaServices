package com.tuiasi.crawler_module.repository;

import com.tuiasi.crawler_module.model.Article;
import com.tuiasi.crawler_module.model.Post;
import com.tuiasi.crawler_module.model.SocialNetwork;
import com.tuiasi.exception.ObjectNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import java.util.*;

@Slf4j
@Repository
public class PostRepository implements ICrudRepository<Post, Integer> {

    private EntityManager entityManager;

    @Autowired
    public PostRepository(EntityManagerFactory entityManagerFactory) {
        this.entityManager = entityManagerFactory.createEntityManager();
    }

    @Override
    public Post add(Post post) {
        EntityTransaction entityTransaction = entityManager.getTransaction();
        try {
            entityTransaction.begin();
            entityManager.persist(post);
            entityTransaction.commit();
            log.info("Post with id: " + post.getId() + " inserted.");
        } catch (Exception e) {
            log.error("Post with id: " + post.getId() + " could not be inserted.");
            log.error(e.getMessage());
            entityTransaction.rollback();
        }
        return post;
    }

    @Override
    public Optional<Post> get(Integer id) {

        EntityTransaction entityTransaction = entityManager.getTransaction();
        try {
            entityTransaction.begin();
            Post result = entityManager.find(Post.class, id);
            entityTransaction.commit();
            log.info("Post with id: " + id + " retrieved.");
            return Optional.of(result);
        } catch (Exception e) {
            entityTransaction.rollback();
            throw new ObjectNotFoundException("Post with id: " + id + " could not be found.");
        }
    }

    @Override
    public Optional<Post> update(Post post, Integer id) {
        EntityTransaction entityTransaction = entityManager.getTransaction();
        try {
            entityTransaction.begin();
            Post result = entityManager.merge(post);
            entityTransaction.commit();
            log.info("Post with id: " + id + " updated.");
            return Optional.of(result);
        } catch (Exception e) {
            entityTransaction.rollback();
            log.error("Post with id: " + id + " could not be updated.");
            log.error(e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public void delete(Integer id) throws ObjectNotFoundException {
        EntityTransaction entityTransaction = entityManager.getTransaction();
        try {
            Post result = get(id).orElseThrow(ObjectNotFoundException::new);
            entityTransaction.begin();
            entityManager.remove(result);
            entityTransaction.commit();
            log.info("Post with id: " + id + " deleted.");
        } catch (Exception e) {
            entityTransaction.rollback();
            throw new ObjectNotFoundException("Post with id: " + id + " could not be deleted.");
        }
    }

    public List<Post> getLastPostsBySymbol(String symbol, int numberOfPosts){
        Query query = entityManager.createQuery(SQL_SELECT_POSTS_BY_SYMBOL);
        query.setParameter("symbol", symbol);
        return new ArrayList<>(query.setMaxResults(numberOfPosts).getResultList());
    }

    public List<Post> getRecentPosts(int numberOfPosts, SocialNetwork type){
        Query query = entityManager.createQuery(SQL_SELECT_RECENT_POSTS);
        query.setParameter("social_network", type);
        return query.setMaxResults(numberOfPosts).getResultList();
    }

    private final String SQL_SELECT_POSTS_BY_SYMBOL = "SELECT p FROM Post p JOIN FETCH p.stock WHERE p.stock.symbol = :symbol ORDER BY p.id DESC";
    private final String SQL_SELECT_RECENT_POSTS = "SELECT p FROM Post p WHERE p.socialNetwork = :social_network ORDER BY p.id DESC";

}
