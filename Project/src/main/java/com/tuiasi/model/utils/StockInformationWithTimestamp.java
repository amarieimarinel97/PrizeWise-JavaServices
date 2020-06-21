package com.tuiasi.model.utils;

import com.tuiasi.model.StockInformation;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class StockInformationWithTimestamp {
    StockInformation stockInformation;
    LocalDateTime localDateTime;
}
