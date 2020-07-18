package com.tuiasi.utils;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class SymbolWithTimestamp {
    public String symbol;
    public Date date;
}
