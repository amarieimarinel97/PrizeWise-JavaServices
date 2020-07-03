package com.tuiasi.central_module.model;

import com.tuiasi.crawler_module.model.Article;
import com.tuiasi.crawler_module.model.Stock;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class StockAnalysis {
    private Stock stock;
    private Set<Article> articles;
    private StockEvolution stockEvolution;
}
