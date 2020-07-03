package com.tuiasi.central_module.model.utils;

import com.tuiasi.central_module.model.StockAnalysis;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class StockInformationWithTimestamp {
    StockAnalysis stockAnalysis;
    LocalDateTime localDateTime;
}
