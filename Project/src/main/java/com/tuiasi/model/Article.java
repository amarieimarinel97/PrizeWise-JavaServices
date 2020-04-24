package com.tuiasi.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

import javax.persistence.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Builder
@Entity
@Data
@Table(name = "articles")
public class Article implements Comparable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column
    private String title;

    @Column(length = 1000)
    private String body;

    @Column(name = "last_updated")
    private Double lastUpdated; //hours since last update

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "stock_id")
    private Stock stock;

    @Column(unique = true)
    private String link;

    @Column(name = "sentiment_analysis")
    private Double sentimentAnalysis;


    @JsonAnyGetter
    private Map<String, String> getStockInfo() {
        Map<String, String> stockInfo = new HashMap<>();
        stockInfo.put("stock", stock.getCompany());
        return stockInfo;
    }



    @Override
    public String toString() {
        return "Article{" +
                "\nId: " + id +
                "\nTitle: " + title +
                "\nBody: " + (Objects.isNull(body) ? null : body.substring(0, Math.min(body.length(), 100))) +
                "\nLast updated: " + lastUpdated + " h" +
                "\nLink: " + link +
                "\nStock: " + (Objects.isNull(stock) ? null : stock.getCompany()) +
                '}';
    }

    @Override
    public int compareTo(Object o) {
        if(!(o instanceof Article))
            return -1;
        if(this.lastUpdated<((Article)o).lastUpdated)
            return -1;
        else return 1;
    }
}
