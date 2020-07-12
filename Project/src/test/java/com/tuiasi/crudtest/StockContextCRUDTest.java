package com.tuiasi.crudtest;


import com.tuiasi.crawler_module.model.StockContext;
import com.tuiasi.crawler_module.service.StockContextService;
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
public class StockContextCRUDTest {

    private Stock mockStock;
    private String mockSymbol = "TEST";
    private String mockCompany = "Test";
    private Double mockValue = 123.0;
    private StockContext mockStockContext;

    private String mockSector = "TestSector";

    private Double mockIndicesPrediction = 5.0;
    private Double mockSectorPrediction = 2.0;

    @Autowired
    private StockContextService stockContextService;

    @Autowired
    private StockService stockService;

    @Before
    public void init() {
        this.mockStock = Stock.builder()
                .symbol(mockSymbol)
                .company(mockCompany)
                .price(mockValue)
                .build();
        this.mockStockContext = StockContext.builder()
                .sector(mockSector)
                .symbol(mockSymbol)
                .name(mockCompany)
                .sectorPrediction(mockSectorPrediction)
                .indicesPrediction(mockIndicesPrediction)
                .build();
        this.stockService.add(mockStock);
        this.stockContextService.add(mockStockContext);
    }

    @Test
    @Order(1)
    public void testStockContextCreateAndRead() {
        StockContext retrievedStockContext = this.stockContextService.get(mockSymbol);
        Assert.assertEquals(mockStockContext.getSymbol(), retrievedStockContext.getSymbol());
        Assert.assertEquals(mockStockContext.getSector(), retrievedStockContext.getSector());
        Assert.assertEquals(mockStockContext.getIndicesPrediction(), retrievedStockContext.getIndicesPrediction());
        Assert.assertEquals(mockStockContext.getSectorPrediction(), retrievedStockContext.getSectorPrediction());
        Assert.assertEquals(mockStockContext.getName(), retrievedStockContext.getName());
    }

    @Test
    @Order(2)
    public void testStockContextUpdate() {
        final  Double newMockIndicesPrediction = 9.0;
        mockStockContext.setIndicesPrediction(newMockIndicesPrediction);
        this.stockContextService.update(mockStockContext, mockSymbol);
        StockContext retrievedStockContext = this.stockContextService.get(mockSymbol);
        Assert.assertEquals(newMockIndicesPrediction, retrievedStockContext.getIndicesPrediction());
    }

    @Test
    @Order(3)
    public void testStockContextDelete() {
        String newMockSymbol = "TEST2";
        mockStockContext.setSymbol(newMockSymbol);
        this.stockContextService.add(mockStockContext);
        this.stockContextService.delete(newMockSymbol);
        try {
            this.stockContextService.get(newMockSymbol);
        } catch (DatabaseConnectionException expected) {
            return;
        }
        Assert.assertTrue("Failed to delete stockContext with id " + newMockSymbol, false);
        mockStockContext.setSymbol(mockSymbol);
    }

    @After
    public void post() {
        this.stockContextService.delete(this.mockSymbol);
        this.stockService.delete(mockSymbol);

    }
}
