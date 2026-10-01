package com.marketatlas.market.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "issuer")
public class Issuer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String country;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    
    public String getCountry() {
        return country;
    }

    public Issuer(String name, String country) {
        this.name = name;
        this.country = country;
    }

    protected Issuer() {
    }
}