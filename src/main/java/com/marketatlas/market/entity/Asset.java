package com.marketatlas.market.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

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
    
    private String symbol;

    @NotNull
    @Column (nullable=false)
    @Enumerated (EnumType.STRING)
    private AssetClass assetClass;

    @ManyToOne 
    @JoinColumn(name = "issuer_id")
    private Issuer issuer;

    public Asset(String name, String symbol, AssetClass assetClass, Issuer issuer) {
        this.name = name;
        this.symbol = symbol;
        this.assetClass = assetClass;
        this.issuer = issuer;
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

    public String getSymbol() {
        return symbol;
    }
}