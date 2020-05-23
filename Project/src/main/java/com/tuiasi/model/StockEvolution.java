package com.tuiasi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

import javax.persistence.*;

@Data
@Builder
@Table(name = "stock_evolution")
@Entity
public class StockEvolution {

    @Id
    @Column(name = "stock_id")
    private String stockId;

    @Column
    private Double[] prediction;

    @Column
    private Double[] changes;

    @Column
    private Double[] deviation;

    @Column
    private Double[] history;

    @Column(name="history_days")
    private String[] historyDays;
}
