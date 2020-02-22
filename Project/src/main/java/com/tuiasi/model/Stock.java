package com.tuiasi.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Builder
@Entity
@Data
@Table(name = "stocks")
public class Stock {

    @Id
    @Column(unique = true)
    private String symbol;

    @Column
    private String company;

    @Column(name = "last_updated")
    @Temporal(TemporalType.DATE)
    private Date lastUpdated;

    @Column(name = "NOC")
    private Double newsOptimismCoefficient;

    @Column(name = "HOC")
    private Double historyOptimismCoefficient;

    @Column(name = "ERC")
    private Double expertsRecommendationCoefficient;

    @Column(name = "predicted_change")
    private Double predictedChange;

    @Column
    private Double price;

    @Transient
    @JsonIgnore
    private Set<Article> articles;
}
