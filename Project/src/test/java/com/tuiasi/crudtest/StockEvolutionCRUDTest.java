package com.tuiasi.crudtest;


import com.tuiasi.central_module.model.StockEvolution;
import com.tuiasi.central_module.service.StockEvolutionService;
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
public class StockEvolutionCRUDTest {

    private Stock mockStock;
    private String mockSymbol = "TEST";
    private String mockCompany = "Test";
    private Double mockValue = 123.0;
    private StockEvolution mockStockEvolution;

    private Double[] mockPastEvolution = new Double[]{10.0, 11.0, 12.0};
    private Double[] mockDeviation = new Double[]{-10.0, 11.0, -12.0};
    private Double[] mockPredictedEvolution = new Double[]{100.0, 110.0, 120.0};
    private Double[] mockPercentageChanges = new Double[]{0.1, -0.1, 0.5};
    private String[] mockHistoryDays = new String[]{"A", "B", "C"};

    @Autowired
    private StockEvolutionService stockEvolutionService;

    @Autowired
    private StockService stockService;

    @Before
    public void init() {
        this.mockStock = Stock.builder()
                .symbol(mockSymbol)
                .company(mockCompany)
                .price(mockValue)
                .build();
        this.mockStockEvolution = StockEvolution.builder()
                .stockId(mockSymbol)
                .pastEvolution(this.mockPastEvolution)
                .deviation(mockDeviation)
                .predictedEvolution(mockPredictedEvolution)
                .percentageChanges(mockPercentageChanges)
                .pastDays(mockHistoryDays)
                .build();
        this.stockService.add(mockStock);
        this.stockEvolutionService.add(mockStockEvolution);
    }

    @Test
    @Order(1)
    public void testStockEvolutionCreateAndRead() {
        StockEvolution retrievedStockEvolution = this.stockEvolutionService.get(mockSymbol);
        Assert.assertEquals(mockStockEvolution.getStockId(), retrievedStockEvolution.getStockId());
        Assert.assertArrayEquals(mockStockEvolution.getPercentageChanges(), retrievedStockEvolution.getPercentageChanges());
        Assert.assertArrayEquals(mockStockEvolution.getDeviation(), retrievedStockEvolution.getDeviation());
        Assert.assertArrayEquals(mockStockEvolution.getPastDays(), retrievedStockEvolution.getPastDays());
        Assert.assertArrayEquals(mockStockEvolution.getPastEvolution(), retrievedStockEvolution.getPastEvolution());
        Assert.assertArrayEquals(mockStockEvolution.getPredictedEvolution(), retrievedStockEvolution.getPredictedEvolution());
    }

    @Test
    @Order(2)
    public void testStockEvolutionUpdate() {
        final  Double[] mockDeviation = new Double[]{-100.0, 110.0, -120.0};
        mockStockEvolution.setDeviation(mockDeviation);
        this.stockEvolutionService.update(mockStockEvolution, mockSymbol);
        StockEvolution retrievedStockEvolution = this.stockEvolutionService.get(mockSymbol);
        Assert.assertArrayEquals(mockDeviation, retrievedStockEvolution.getDeviation());
    }

    @Test
    @Order(3)
    public void testStockEvolutionDelete() {
        String newMockSymbol = "TEST2";
        mockStockEvolution.setStockId(newMockSymbol);
        this.stockEvolutionService.add(mockStockEvolution);
        this.stockEvolutionService.delete(newMockSymbol);
        try {
            this.stockEvolutionService.get(newMockSymbol);
        } catch (DatabaseConnectionException expected) {
            return;
        }
        Assert.assertTrue("Failed to delete stockEvolution with id " + newMockSymbol, false);
        mockStockEvolution.setStockId(mockSymbol);
    }

    @After
    public void post() {
        this.stockEvolutionService.delete(this.mockSymbol);
        this.stockService.delete(mockSymbol);

    }
}
