package com.tuiasi.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Builder
@Entity
@Getter
@Setter
@AllArgsConstructor
@Table(name = "stocks")
public class Stock {
    public Stock() {}
    @Id
    @Column(unique = true)
    private String symbol;

    @Column
    private String company;

    @Column(name = "last_updated")
    private Date lastUpdated;

    @Column(name = "news_coefficient")
    private Double newsOptimismCoefficient;

    @Column(name = "history_coefficient")
    private Double historyOptimismCoefficient;

    @Column(name = "experts_coefficient")
    private Double expertsRecommendationCoefficient;

    @Column(name = "predicted_change")
    private Double predictedChange;

    @Column
    private Double price;

    @Column
    private Integer views;

    @Transient
    @JsonIgnore
    private Set<Article> articles;
}
