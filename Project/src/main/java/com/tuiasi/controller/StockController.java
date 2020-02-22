package com.tuiasi.controller;

import com.tuiasi.model.Stock;
import com.tuiasi.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

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
    public Stock update(@RequestBody Stock stock) {
        return service.update(stock);
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
