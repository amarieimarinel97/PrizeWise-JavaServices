package com.tuiasi.controller;

import com.tuiasi.model.Article;
import com.tuiasi.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
public class ArticleController implements ICrudController<Article> {

    private ArticleService service;

    @Autowired
    public ArticleController(ArticleService service) {
        this.service = service;
    }

    @PostMapping
    @Override
    public Article add(@RequestBody Article article) {
        return service.add(article);
    }

    @DeleteMapping("/{id}")
    @Override
    public void delete(@PathVariable int id) {
        service.delete(id);
    }

    @PutMapping("/{id}")
    @Override
    public Article update(@RequestBody Article article, @PathVariable int id) {
        return service.update(article, id);
    }

    @GetMapping("/{id}")
    @Override
    public Article get(@PathVariable int id) {
        return service.get(id);
    }

    @GetMapping
    public List<Article> getAll() {
        return service.getAll();
    }
}
