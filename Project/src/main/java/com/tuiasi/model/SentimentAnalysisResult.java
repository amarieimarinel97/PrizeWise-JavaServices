package com.tuiasi.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SentimentAnalysisResult {
    private double[] sentiment_analysis;
}
