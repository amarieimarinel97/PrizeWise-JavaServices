package com.tuiasi.crudtest;

import com.tuiasi.crawler_module.model.Stock;
import com.tuiasi.crawler_module.model.Stock;
import com.tuiasi.crawler_module.service.StockService;
import com.tuiasi.exception.DatabaseConnectionException;
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
public class StockCRUDTest {
    private Stock mockStock;
    private String mockSymbol = "TEST";
    private String mockCompany = "Test";
    private Double mockValue = 123.0;
    private Double mockERC = 2.0;
    private Double mockHOC = 3.0;
    private Double mockNOC = 9.0;
    private Integer mockIntValue = 100;
    private Double mockPrediction = 5.0;

    @Autowired
    private StockService stockService;


    @Before
    public void init() {
        this.mockStock = Stock.builder()
                .symbol(mockSymbol)
                .company(mockCompany)
                .price(mockValue)
                .expertsRecommendationCoefficient(mockERC)
                .newsOptimismCoefficient(mockNOC)
                .historyOptimismCoefficient(mockHOC)
                .views(mockIntValue)
                .predictedChange(mockPrediction)
                .build();
        this.stockService.add(mockStock);
    }

    @Test
    @Order(1)
    public void testStockCreateAndRead() {
        Stock retrievedStock = this.stockService.get(mockSymbol);
        Assert.assertEquals(mockStock.getSymbol(), retrievedStock.getSymbol());
        Assert.assertEquals(mockStock.getCompany(), retrievedStock.getCompany());
        Assert.assertEquals(mockStock.getViews(), retrievedStock.getViews());
        Assert.assertEquals(mockStock.getExpertsRecommendationCoefficient(), retrievedStock.getExpertsRecommendationCoefficient());
        Assert.assertEquals(mockStock.getHistoryOptimismCoefficient(), retrievedStock.getHistoryOptimismCoefficient());
        Assert.assertEquals(mockStock.getNewsOptimismCoefficient(), retrievedStock.getNewsOptimismCoefficient());
        Assert.assertEquals(mockStock.getPredictedChange(), retrievedStock.getPredictedChange());
    }

    @Test
    @Order(2)
    public void testStockUpdate() {
        final String newTitle = "new Test";
        mockStock.setCompany(newTitle);
        this.stockService.update(mockStock, mockSymbol);
        Stock retrievedStock = this.stockService.get(mockSymbol);
        Assert.assertEquals(newTitle, retrievedStock.getCompany());
    }

    @Test
    @Order(3)
    public void testStockDelete() {
        this.stockService.add(mockStock);
        this.stockService.delete(mockSymbol);
        try {
            this.stockService.get(mockSymbol);
        } catch (DatabaseConnectionException expected) {
            return;
        }
        Assert.assertTrue("Failed to delete stock with id " + mockSymbol, false);
    }

    @After
    public void post() {
        this.stockService.delete(mockSymbol);

    }
}
