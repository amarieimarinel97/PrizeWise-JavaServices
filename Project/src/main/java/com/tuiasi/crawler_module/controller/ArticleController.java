package com.tuiasi.crawler_module.controller;

import com.tuiasi.crawler_module.model.Article;
import com.tuiasi.crawler_module.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/articles")
public class ArticleController implements ICrudController<Article, Integer> {

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
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }

    @PutMapping("/{id}")
    @Override
    public Article update(@RequestBody Article article, @PathVariable Integer id) {
        return service.update(article, id);
    }

    @GetMapping("/{id}")
    @Override
    public Article get(@PathVariable Integer id) {
        return service.get(id);
    }

    @GetMapping
    public List<Article> getAll() {
        return service.getAll();
    }
}
