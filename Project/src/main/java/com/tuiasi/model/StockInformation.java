package com.tuiasi.model;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class StockInformation {
    private Stock stock;
    private Set<Article> articles;
    private StockEvolution stockEvolution;
}
