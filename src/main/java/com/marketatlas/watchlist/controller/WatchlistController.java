package com.marketatlas.watchlist.controller;

import com.marketatlas.trading.service.Caller;
import com.marketatlas.watchlist.dto.WatchlistEntryResponse;
import com.marketatlas.watchlist.service.WatchlistService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping
    public List<WatchlistEntryResponse> list(Authentication authentication) {
        return watchlistService.list(Caller.from(authentication));
    }

    @PutMapping("/{assetId}")
    public ResponseEntity<Void> add(@PathVariable Long assetId, Authentication authentication) {
        watchlistService.add(Caller.from(authentication), assetId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{assetId}")
    public ResponseEntity<Void> remove(@PathVariable Long assetId, Authentication authentication) {
        watchlistService.remove(Caller.from(authentication), assetId);
        return ResponseEntity.noContent().build();
    }
}
