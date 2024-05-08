package com.tuiasi.central_module.model;

import com.tuiasi.crawler_module.model.Article;
import com.tuiasi.crawler_module.model.Post;
import com.tuiasi.crawler_module.model.Stock;
import com.tuiasi.crawler_module.model.StockContext;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
@Builder
public class StockAnalysis {

    private Stock stock;

    private StockEvolution stockEvolution;
    private StockContext stockContext;

    private Set<Article> articles;
    private List<Post> posts;
}
