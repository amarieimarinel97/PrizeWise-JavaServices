package com.tuiasi.controller;

import com.tuiasi.model.StockContext;
import com.tuiasi.service.StockContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/contexts")
public class StockContextController implements ICrudController<StockContext, String> {

    private StockContextService service;

    @Autowired
    public StockContextController(StockContextService service) {
        this.service = service;
    }

    @PostMapping
    @Override
    public StockContext add(@RequestBody StockContext stockContext) {
        return service.add(stockContext);
    }

    @GetMapping("/{id}")
    @Override
    public StockContext get(@PathVariable String id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    @Override
    public StockContext update(@RequestBody StockContext stockContext, @PathVariable String id) {
        return service.update(stockContext, id);
    }

    @DeleteMapping("/{id}")
    @Override
    public void delete(@PathVariable String id) {
        service.delete(id);
    }

    @GetMapping
    public List<StockContext> getAll() {
        return service.getAll();
    }
}
