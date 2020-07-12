package com.tuiasi.crudtest;

import com.tuiasi.configuration.ApplicationConfiguration;
import com.tuiasi.crawler_module.model.Article;
import com.tuiasi.crawler_module.model.Stock;
import com.tuiasi.crawler_module.service.ArticleService;
import com.tuiasi.crawler_module.service.StockService;
import com.tuiasi.exception.DatabaseConnectionException;
import com.tuiasi.exception.ObjectNotFoundException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ArticleCRUDTest {
    private Stock mockStock;
    private String mockSymbol = "TEST";
    private String mockCompany = "Test";
    private Double mockValue = 123.0;

    private Article mockArticle;
    private String mockLink = "test.com";
    private String mockTitle = "Test title";
    private String mockBody = "Test body";
    private Integer articleId;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private StockService stockService;


    @Before
    public void init(){
        this.mockStock= Stock.builder()
                .symbol(mockSymbol)
                .company(mockCompany)
                .price(mockValue)
                .build();
        this.mockArticle= Article.builder()
                .stock(mockStock)
                .lastUpdated(mockValue)
                .link(mockLink)
                .title(mockTitle)
                .body(mockBody)
                .build();
        this.stockService.add(mockStock);
        this.articleId = this.articleService.add(mockArticle).getId();
    }

    @Test
    @Order(1)
    public void testArticleCreateAndRead() {
        Article retrievedArticle = this.articleService.get(articleId);
        Assert.assertEquals(mockArticle.getLink(), retrievedArticle.getLink());
        Assert.assertEquals(mockArticle.getId(), retrievedArticle.getId());
        Assert.assertEquals(mockArticle.getTitle(), retrievedArticle.getTitle());
        Assert.assertEquals(mockArticle.getBody(), retrievedArticle.getBody());
        this.articleId=retrievedArticle.getId();
    }

    @Test
    @Order(2)
    public void testArticleUpdate() {
        final String newTitle = "new Test";
        mockArticle.setTitle(newTitle);
        this.articleService.update(mockArticle, articleId);
        Article retrievedArticle = this.articleService.get(articleId);
        Assert.assertEquals(newTitle, retrievedArticle.getTitle());
    }

    @Test
    @Order(3)
    public void testArticleDelete() {
        Integer id = this.articleService.add(mockArticle).getId();
        this.articleService.delete(id);
        try {
            this.articleService.get(id);
        }catch (DatabaseConnectionException expected){
            return;
        }
        Assert.assertTrue("Failed to delete article with id "+id, false);
    }

    @After
    public void post(){
        this.articleService.delete(this.articleId);
        this.stockService.delete(mockSymbol);

    }
}
