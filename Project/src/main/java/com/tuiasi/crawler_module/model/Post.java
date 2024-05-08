package com.tuiasi.crawler_module.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Builder
@Entity
@Getter
@Setter
@AllArgsConstructor
@Table(name = "posts")
public class Post {

    public Post() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column
    private String title;

    @Column(length = 1000)
    private String body;

    @Column(name = "sentiment_analysis")
    private Double sentimentAnalysis;

    @Column(name = "social_network")
    private SocialNetwork socialNetwork;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "stock_symbol")
    private Stock stock;

}
