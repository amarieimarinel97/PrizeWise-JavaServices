package com.tuiasi.central_module.model;

import lombok.Builder;
import lombok.Data;

import javax.persistence.*;

@Data
@Builder
@Table(name = "stock_evolutions")
@Entity
public class StockEvolution {

    @Id
    @Column(name = "stock_symbol")
    private String stockId;

    @Column(name="predicted_evolution")
    private Double[] predictedEvolution;

    @Column(name="percentage_changes")
    private Double[] percentageChanges;

    @Column
    private Double[] deviation;

    @Column(name="past_evolution")
    private Double[] pastEvolution;

    @Column(name="past_days")
    private String[] pastDays;
}
