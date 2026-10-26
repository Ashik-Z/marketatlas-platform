package com.marketatlas.market.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import com.marketatlas.enums.AssetClass;
import com.marketatlas.market.validation.ValidAsset;

@ValidAsset 
@Entity 
@Table(name = "asset")
public class Asset {
    @Id 
    @GeneratedValue (strategy=GenerationType.IDENTITY)
    private Long id;

    @NotBlank 
    @Column (nullable=false)
    private String name;


    @NotNull
    @Column (nullable=false)
    @Enumerated (EnumType.STRING)
    private AssetClass assetClass;

    @ManyToOne 
    @JoinColumn(name = "issuer_id")
    private Issuer issuer;

    @Pattern(regexp = "[A-Z]{3}")
    @Column(length = 3)
    private String currency;

    @Column(length = 50)
    private String unit;

    public Asset(String name, AssetClass assetClass, Issuer issuer) {
        this.name = name;
        this.assetClass = assetClass;
        this.issuer = issuer;
    }

    public Asset(String name, AssetClass assetClass, Issuer issuer, String currency, String unit) {
        this(name, assetClass, issuer);
        this.currency = currency;
        this.unit = unit;
    }

    protected Asset() {
        
    }

    public AssetClass getAssetClass() {
        return assetClass;
    }

    public Issuer getIssuer() {
        return issuer;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCurrency() {
        return currency;
    }

    public String getUnit() {
        return unit;
    }

}