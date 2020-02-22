package com.tuiasi.model;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class StockInformation {
    Stock stock;
    Set<Article> articles;
}
