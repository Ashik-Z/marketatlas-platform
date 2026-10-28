package com.marketatlas.trading.model;

import com.marketatlas.market.entity.Asset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "trade")
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private TradeSide side;

    @Column(nullable = false, precision = 20, scale = 6)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 20, scale = 6)
    private BigDecimal price;

    @Column(nullable = false, precision = 20, scale = 6)
    private BigDecimal commission;

    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    protected Trade() {
    }

    public Trade(Account account, Asset asset, TradeSide side, BigDecimal quantity,
                 BigDecimal price, BigDecimal commission, LocalDate priceDate, Instant executedAt) {
        this.account = account;
        this.asset = asset;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.commission = commission;
        this.priceDate = priceDate;
        this.executedAt = executedAt;
    }

    public Long getId() { return id; }
    public Account getAccount() { return account; }
    public Asset getAsset() { return asset; }
    public TradeSide getSide() { return side; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getCommission() { return commission; }
    public LocalDate getPriceDate() { return priceDate; }
    public Instant getExecutedAt() { return executedAt; }
}