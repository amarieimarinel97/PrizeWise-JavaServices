package com.tuiasi.crawler_module.service;

import com.tuiasi.exception.DatabaseConnectionException;
import com.tuiasi.exception.ObjectNotFoundException;
import com.tuiasi.crawler_module.model.Article;
import com.tuiasi.crawler_module.repository.ArticleRepository;
import com.tuiasi.crawler_module.repository.ICrudRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class ArticleService implements ICrudService<Article, Integer> {

    private ArticleRepository repository;

    @Autowired
    public ArticleService(ICrudRepository<Article, Integer> articleRepository) {
        this.repository = (ArticleRepository) articleRepository;
    }

    @Override
    public Article add(Article article) {
        try {
            return repository.add(article);
        } catch (Exception e) {
            log.error("Could not add article with id: " + article.getId() + " : " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }

    @Override
    public Article get(Integer id) throws ObjectNotFoundException {
        try {
            Optional<Article> article = repository.get(id);
            return article
                    .orElseThrow(() -> new ObjectNotFoundException("Article with id: " + id + " does not exist"));
        } catch (Exception e) {
            log.error("Could not get article with id: " + id + " : " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }

    }

    @Override
    public Article update(Article article, Integer id) throws ObjectNotFoundException {
        try {
            Optional<Article> result = repository.update(article, id);
            return result
                    .orElseThrow(() -> new ObjectNotFoundException("Article with id: " + article.getId() + " does not exist"));
        } catch (Exception e) {
            log.error("Could not update article with id: " + id + " : " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }

    @Override
    public void delete(Integer id) {
        try {
            repository.delete(id);
        } catch (ObjectNotFoundException e) {
            log.error(e.getMessage());
        } catch (Exception e) {
            log.error("Could not delete article with id: " + id + " : " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }

    public List<Article> getAll() {
        try {
            return repository.getAll();
        } catch (Exception e) {
            log.error("Could not retrieve all articles: " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }

    public Set<Article> getLastArticlesBySymbol(String symbol, int numberOfArticles) {
        try {
            return repository.getLastArticlesBySymbol(symbol, numberOfArticles);
        } catch (Exception e) {
            log.error("Could not retrieve articles: " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }

    public List<Article> getLastArticlesWithBodiesBySymbol(String symbol, int numberOfArticles) {
        try {
            return repository.getLastArticlesWithBodiesBySymbol(symbol, numberOfArticles);
        } catch (Exception e) {
            log.error("Could not retrieve articles: " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }

    public List<Article> getLastArticlesWithBodies(int numberOfArticles) {
        try {
            return repository.getRecentArticles(numberOfArticles);
        } catch (Exception e) {
            log.error("Could not retrieve articles: " + e.getMessage());
            throw new DatabaseConnectionException(e);
        }
    }

}
