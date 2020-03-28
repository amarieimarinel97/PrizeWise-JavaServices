package com.tuiasi.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StockPredictionResult {
    private Double[] prediction;
    private Double[] changes;
    private Double[] deviation;
}
