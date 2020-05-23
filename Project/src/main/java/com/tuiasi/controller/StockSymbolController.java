package com.tuiasi.controller;

import com.tuiasi.model.StockSymbol;
import com.tuiasi.service.StockSymbolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/symbols")
public class StockSymbolController implements ICrudController<StockSymbol, String> {

    private StockSymbolService service;

    @Autowired
    public StockSymbolController(StockSymbolService service) {
        this.service = service;
    }

    @PostMapping
    @Override
    public StockSymbol add(@RequestBody StockSymbol stockSymbol) {
        return service.add(stockSymbol);
    }

    @GetMapping("/{id}")
    @Override
    public StockSymbol get(@PathVariable String id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    @Override
    public StockSymbol update(@RequestBody StockSymbol stockSymbol, @PathVariable String id) {
        return service.update(stockSymbol, id);
    }

    @DeleteMapping("/{id}")
    @Override
    public void delete(@PathVariable String id) {
        service.delete(id);
    }

    @GetMapping
    public List<StockSymbol> getAll() {
        return service.getAll();
    }
}
