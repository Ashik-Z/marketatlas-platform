package com.marketatlas.trading.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "starting_balance", nullable = false, precision = 20, scale = 6)
    private BigDecimal startingBalance;

    @Column(name = "cash_balance", nullable = false, precision = 20, scale = 6)
    private BigDecimal cashBalance;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "owner_username", nullable = false, length = 50)
    private String ownerUsername;

    protected Account() {
    }

    public Account(String name, String currency, BigDecimal startingBalance, Instant createdAt,
                   String ownerUsername) {
        this.name = name;
        this.currency = currency;
        this.startingBalance = startingBalance;
        this.cashBalance = startingBalance;
        this.createdAt = createdAt;
        this.ownerUsername = ownerUsername;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCurrency() { return currency; }
    public BigDecimal getStartingBalance() { return startingBalance; }
    public BigDecimal getCashBalance() { return cashBalance; }
    public Instant getCreatedAt() { return createdAt; }
    public String getOwnerUsername() { return ownerUsername; }

    public void debit(BigDecimal amount) {
        if (amount.signum() < 0 || cashBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Debit would make the cash balance negative");
        }
        cashBalance = cashBalance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        if (amount.signum() < 0) {
            throw new IllegalStateException("Credit must not be negative");
        }
        cashBalance = cashBalance.add(amount);
    }
}