package com.tuiasi.central_module.model.utils;

import com.tuiasi.central_module.model.StockInformation;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class StockInformationWithTimestamp {
    StockInformation stockInformation;
    LocalDateTime localDateTime;
}
