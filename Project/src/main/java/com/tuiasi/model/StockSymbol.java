package com.tuiasi.model;

import lombok.Builder;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Builder
@Entity
@Table(name = "symbols")
public class StockSymbol {
    @Column
    private String name;

    @Id
    private String symbol;

    @Column
    private String sector;
}
