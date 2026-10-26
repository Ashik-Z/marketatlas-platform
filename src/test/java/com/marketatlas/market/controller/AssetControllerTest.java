package com.marketatlas.market.controller;

import com.marketatlas.common.exception.DuplicateResourceException;
import com.marketatlas.common.exception.ResourceNotFoundException;
import com.marketatlas.enums.AssetClass;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.Issuer;
import com.marketatlas.market.service.AssetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssetController.class)
class AssetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssetService assetService;

    private Asset assetWithId(Long id, String name, AssetClass assetClass, Issuer issuer) {
        Asset asset = new Asset(name, assetClass, issuer);
        ReflectionTestUtils.setField(asset, "id", id);
        return asset;
    }

    private Issuer issuerWithId(Long id) {
        Issuer issuer = new Issuer("Apple Inc.", "United States");
        ReflectionTestUtils.setField(issuer, "id", id);
        return issuer;
    }

    @Test
    void shouldCreateAsset() throws Exception {
        Asset apple = assetWithId(1L, "Apple", AssetClass.EQUITY, issuerWithId(5L));
        when(assetService.createAsset("Apple", AssetClass.EQUITY, 5L, "aapl", null, null))
                .thenReturn(apple);
        when(assetService.findSymbolsByAssetIds(List.of(1L)))
                .thenReturn(Map.of(1L, "AAPL"));

        mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Apple", "assetClass": "EQUITY",
                                 "issuerId": 5, "symbol": "aapl"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/assets/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.assetClass").value("EQUITY"))
                .andExpect(jsonPath("$.issuerId").value(5))
                .andExpect(jsonPath("$.symbol").value("AAPL"));
    }

    @Test
    void shouldRejectBlankName() throws Exception {
        mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": " ", "assetClass": "COMMODITY"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());

        verifyNoInteractions(assetService);
    }

    @Test
    void shouldRejectMissingAssetClass() throws Exception {
        mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Gold"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.assetClass").exists());

        verifyNoInteractions(assetService);
    }

    @Test
    void shouldMapBusinessRuleViolationTo400() throws Exception {
        when(assetService.createAsset(anyString(), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("An equity asset requires an issuer."));

        mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Apple", "assetClass": "EQUITY", "symbol": "AAPL"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("An equity asset requires an issuer."));
    }

    @Test
    void shouldMapUnknownIssuerTo404() throws Exception {
        when(assetService.createAsset(anyString(), any(), anyLong(), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Issuer 99 was not found."));

        mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Apple", "assetClass": "EQUITY",
                                 "issuerId": 99, "symbol": "AAPL"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldMapDuplicateSymbolTo409() throws Exception {
        when(assetService.createAsset(anyString(), any(), anyLong(), anyString(), any(), any()))
                .thenThrow(new DuplicateResourceException("An asset with symbol AAPL already exists."));

        mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Apple", "assetClass": "EQUITY",
                                 "issuerId": 5, "symbol": "AAPL"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    void shouldListAssetsByClassWithSymbols() throws Exception {
        Asset gold = assetWithId(2L, "Gold", AssetClass.COMMODITY, null);
        when(assetService.findAssetsByClass(AssetClass.COMMODITY)).thenReturn(List.of(gold));
        when(assetService.findSymbolsByAssetIds(List.of(2L))).thenReturn(Map.of());

        mockMvc.perform(get("/api/assets").param("assetClass", "COMMODITY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Gold"))
                .andExpect(jsonPath("$[0].issuerId").doesNotExist())
                .andExpect(jsonPath("$[0].symbol").doesNotExist());
    }

    @Test
    void shouldFindBySymbolAfterNormalizingIt() throws Exception {
        Asset apple = assetWithId(1L, "Apple", AssetClass.EQUITY, issuerWithId(5L));
        when(assetService.findAssetBySymbol("AAPL")).thenReturn(Optional.of(apple));
        when(assetService.findSymbolsByAssetIds(List.of(1L))).thenReturn(Map.of(1L, "AAPL"));

        mockMvc.perform(get("/api/assets").param("symbol", " aapl "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"));

        verify(assetService).findAssetBySymbol(eq("AAPL"));
    }

    @Test
    void shouldReturnEmptyListForUnknownSymbol() throws Exception {
        when(assetService.findAssetBySymbol("NOPE")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/assets").param("symbol", "nope"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldRejectUnknownAssetClassFilter() throws Exception {
        mockMvc.perform(get("/api/assets").param("assetClass", "BOGUS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetAssetById() throws Exception {
        Asset apple = assetWithId(1L, "Apple", AssetClass.EQUITY, issuerWithId(5L));
        when(assetService.findAssetById(1L)).thenReturn(Optional.of(apple));
        when(assetService.findSymbolsByAssetIds(List.of(1L))).thenReturn(Map.of(1L, "AAPL"));

        mockMvc.perform(get("/api/assets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Apple"))
                .andExpect(jsonPath("$.symbol").value("AAPL"));
    }

    @Test
    void shouldReturnNotFoundForUnknownAsset() throws Exception {
        when(assetService.findAssetById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/assets/99"))
                .andExpect(status().isNotFound());
    }
}