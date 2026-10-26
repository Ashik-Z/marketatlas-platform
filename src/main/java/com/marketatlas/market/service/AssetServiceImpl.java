package com.marketatlas.market.service;

import com.marketatlas.common.exception.DuplicateResourceException;
import com.marketatlas.common.exception.ResourceNotFoundException;
import com.marketatlas.enums.AssetClass;
import com.marketatlas.enums.IdentifierScheme;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.AssetIdentifier;
import com.marketatlas.market.entity.Issuer;
import com.marketatlas.market.repository.AssetIdentifierRepository;
import com.marketatlas.market.repository.AssetRepository;
import com.marketatlas.market.repository.IssuerRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;
    private final IssuerRepository issuerRepository;
    private final AssetIdentifierRepository assetIdentifierRepository;

    public AssetServiceImpl(AssetRepository assetRepository,
                            IssuerRepository issuerRepository,
                            AssetIdentifierRepository assetIdentifierRepository) {
        this.assetRepository = assetRepository;
        this.issuerRepository = issuerRepository;
        this.assetIdentifierRepository = assetIdentifierRepository;
    }

    @Override
    @Transactional
    public Asset createAsset(String name, AssetClass assetClass,
                             Long issuerId, String symbol,
                             String currency, String unit) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Asset name must not be blank.");
        }
        if (assetClass == null) {
            throw new IllegalArgumentException("Asset class must not be null.");
        }
        if (assetClass == AssetClass.EQUITY && issuerId == null) {
            throw new IllegalArgumentException("An equity asset requires an issuer.");
        }
        if (assetClass == AssetClass.COMMODITY && issuerId != null) {
            throw new IllegalArgumentException("A commodity asset must not have an issuer.");
        }

        Issuer issuer = null;
        if (issuerId != null) {
            issuer = issuerRepository.findById(issuerId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Issuer " + issuerId + " was not found."));
        }

        String normalizedSymbol = normalizeSymbol(symbol);

        String normalizedCurrency = normalizeCurrency(currency);
        String normalizedUnit = normalizeUnit(unit);

        if (normalizedSymbol != null
                && assetRepository.findByIdentifier(
                IdentifierScheme.TWELVE_DATA_SYMBOL, normalizedSymbol).isPresent()) {
            throw new DuplicateResourceException(
                    "An asset with symbol " + normalizedSymbol + " already exists.");
        }

        Asset asset = assetRepository.save(
                new Asset(name.trim(), assetClass, issuer, normalizedCurrency, normalizedUnit));

        if (normalizedSymbol != null) {
            try {
                assetIdentifierRepository.saveAndFlush(new AssetIdentifier(
                        asset, IdentifierScheme.TWELVE_DATA_SYMBOL, normalizedSymbol));
            } catch (DataIntegrityViolationException e) {
                // A concurrent request won the race past the check above;
                // the unique constraint is the real guard.
                throw new DuplicateResourceException(
                        "An asset with symbol " + normalizedSymbol + " already exists.");
            }
        }

        return asset;
    }

    @Override
    public List<Asset> findAssetsByClass(AssetClass assetClass) {
        return assetRepository.findByAssetClass(assetClass);
    }

    @Override
    public Optional<Asset> findAssetById(Long id) {
        return assetRepository.findById(id);
    }

    @Override
    public List<Asset> findAllAssets() {
        return assetRepository.findAll();
    }

    @Override
    public Map<Long, String> findSymbolsByAssetIds(Collection<Long> assetIds) {
        if (assetIds.isEmpty()) {
            return Map.of();
        }
        return assetIdentifierRepository
                .findByAssetIdsAndScheme(assetIds, IdentifierScheme.TWELVE_DATA_SYMBOL)
                .stream()
                .collect(Collectors.toMap(
                        identifier -> identifier.getAsset().getId(),
                        AssetIdentifier::getIdentifierValue));
    }

    @Override
    public Optional<Asset> findAssetBySymbol(String symbol) {
        return assetRepository.findByIdentifier(
                IdentifierScheme.TWELVE_DATA_SYMBOL,
                symbol
        );
    }

    @Override
    public List<String> findAllProviderSymbols() {
        return assetIdentifierRepository
                .findIdentifierValuesByScheme(IdentifierScheme.TWELVE_DATA_SYMBOL);
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return null;
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return null;
        }
        String normalized = currency.trim().toUpperCase(java.util.Locale.ROOT);
        if (!normalized.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Currency must be a 3-letter ISO 4217 code.");
        }
        return normalized;
    }

    private static String normalizeUnit(String unit) {
        if (unit == null || unit.isBlank()) {
            return null;
        }
        String normalized = unit.trim();
        if (normalized.length() > 50) {
            throw new IllegalArgumentException("Unit must be at most 50 characters.");
        }
        return normalized;
    }
}