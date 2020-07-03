package com.tuiasi.crawler_module.controller;

import com.tuiasi.crawler_module.model.Stock;
import com.tuiasi.crawler_module.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/stocks")
public class StockController implements ICrudController<Stock, String> {

    private StockService service;

    @Autowired
    public StockController(StockService service) {
        this.service = service;
    }

    @PostMapping
    public Stock add(@RequestBody Stock stock) {
        return service.add(stock);
    }


    @DeleteMapping("/{symbol}")
    public void delete(@PathVariable String symbol) {
        service.delete(symbol);
    }

    @PutMapping
    public Stock update(@RequestBody Stock stock, String id) {
        return service.update(stock, id);
    }

    @GetMapping("/{symbol}")
    public Stock get(@PathVariable String symbol) {
        return service.get(symbol);
    }

    @GetMapping
    public List<Stock> getAll() {
        return service.getAll();
    }
}
