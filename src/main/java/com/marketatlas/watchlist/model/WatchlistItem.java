package com.marketatlas.watchlist.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "watchlist_item")
public class WatchlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_username", nullable = false, length = 50)
    private String ownerUsername;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    protected WatchlistItem() {
    }

    public WatchlistItem(String ownerUsername, Long assetId) {
        this.ownerUsername = ownerUsername;
        this.assetId = assetId;
        this.addedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getOwnerUsername() { return ownerUsername; }
    public Long getAssetId() { return assetId; }
    public Instant getAddedAt() { return addedAt; }
}
