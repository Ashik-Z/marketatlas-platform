package com.marketatlas.market.controller;

import com.marketatlas.common.exception.ResourceNotFoundException;
import com.marketatlas.enums.AssetClass;
import com.marketatlas.market.dto.AssetResponse;
import com.marketatlas.market.dto.CreateAssetRequest;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.service.AssetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @PostMapping
    public ResponseEntity<AssetResponse> createAsset(
            @Valid @RequestBody CreateAssetRequest request) {

        Asset saved = assetService.createAsset(
                request.name(), request.assetClass(),
                request.issuerId(), request.symbol());

        Map<Long, String> symbols =
                assetService.findSymbolsByAssetIds(List.of(saved.getId()));

        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();

        return ResponseEntity.created(location)
                .body(AssetResponse.from(saved, symbols.get(saved.getId())));
    }

    @GetMapping
    public List<AssetResponse> listAssets(
            @RequestParam(required = false) AssetClass assetClass,
            @RequestParam(required = false) String symbol) {

        List<Asset> assets;

        if (symbol != null && !symbol.isBlank()) {
            assets = assetService
                    .findAssetBySymbol(symbol.trim().toUpperCase(Locale.ROOT))
                    .filter(asset -> assetClass == null
                            || asset.getAssetClass() == assetClass)
                    .map(List::of)
                    .orElseGet(List::of);
        } else if (assetClass != null) {
            assets = assetService.findAssetsByClass(assetClass);
        } else {
            assets = assetService.findAllAssets();
        }

        return toResponses(assets);
    }

    @GetMapping("/{id}")
    public AssetResponse getAsset(@PathVariable Long id) {
        Asset asset = assetService.findAssetById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Asset " + id + " was not found."));

        return toResponses(List.of(asset)).get(0);
    }

    private List<AssetResponse> toResponses(List<Asset> assets) {
        Map<Long, String> symbols = assetService.findSymbolsByAssetIds(
                assets.stream().map(Asset::getId).toList());

        return assets.stream()
                .map(asset -> AssetResponse.from(asset, symbols.get(asset.getId())))
                .toList();
    }
}