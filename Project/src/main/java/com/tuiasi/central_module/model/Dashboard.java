package com.tuiasi.central_module.model;

import com.tuiasi.crawler_module.model.Article;
import com.tuiasi.crawler_module.model.Post;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class Dashboard {

    private final List<StockAnalysis> history;
    private final List<StockAnalysis> portfolio;
    private final List<StockAnalysis> watchlist;

    private final List<StockAnalysis> popular;
    private final List<Article> recentArticles;
    private final List<Post> recentPosts;
}
