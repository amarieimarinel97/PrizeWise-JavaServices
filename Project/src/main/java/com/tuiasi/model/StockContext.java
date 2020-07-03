package com.tuiasi.model;

import lombok.Builder;
import lombok.Data;

import javax.persistence.*;

@Data
@Builder
@Entity
@Table(name = "stock_contexts")
public class StockContext {
    @Id
    private String symbol;

    @Column(name="company_name")
    private String name;

    @Column
    private String sector;

    @Column(name="sector_prediction")
    private Double sectorPrediction;

    @Column(name="indices_prediction")
    private Double indicesPrediction;

    @OneToOne
    @MapsId
    @JoinColumn(name = "symbol")
    private Stock stock;
}
