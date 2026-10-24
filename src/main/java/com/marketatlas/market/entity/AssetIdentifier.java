package com.marketatlas.market.entity;

import com.marketatlas.enums.IdentifierScheme;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(
        name = "asset_identifier",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_asset_identifier_scheme_value",
                        columnNames = {"scheme", "identifier_value"}
                ),
                @UniqueConstraint(
                        name = "uk_asset_identifier_asset_scheme",
                        columnNames = {"asset_id", "scheme"}
                )
        }
)
public class AssetIdentifier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private IdentifierScheme scheme;

    @NotBlank
    @Column(name = "identifier_value", nullable = false)
    private String identifierValue;

    public AssetIdentifier(
            Asset asset,
            IdentifierScheme scheme,
            String identifierValue) {
        this.asset = asset;
        this.scheme = scheme;
        this.identifierValue = identifierValue;
    }

    protected AssetIdentifier() {
    }

    public Long getId() {
        return id;
    }

    public Asset getAsset() {
        return asset;
    }

    public IdentifierScheme getScheme() {
        return scheme;
    }

    public String getIdentifierValue() {
        return identifierValue;
    }
}