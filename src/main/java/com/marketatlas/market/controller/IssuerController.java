package com.marketatlas.market.controller;

import com.marketatlas.common.exception.ResourceNotFoundException;
import com.marketatlas.market.dto.CreateIssuerRequest;
import com.marketatlas.market.dto.IssuerResponse;
import com.marketatlas.market.entity.Issuer;
import com.marketatlas.market.service.IssuerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/issuers")
public class IssuerController {

    private final IssuerService issuerService;

    public IssuerController(IssuerService issuerService) {
        this.issuerService = issuerService;
    }

    @PostMapping
    public ResponseEntity<IssuerResponse> createIssuer(
            @Valid @RequestBody CreateIssuerRequest request) {

        Issuer saved = issuerService.createIssuer(
                new Issuer(request.name().trim(), request.country().trim())
        );

        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();

        return ResponseEntity.created(location).body(IssuerResponse.from(saved));
    }

    @GetMapping
    public List<IssuerResponse> listIssuers() {
        return issuerService.findAllIssuers().stream()
                .map(IssuerResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public IssuerResponse getIssuer(@PathVariable Long id) {
        return issuerService.findIssuerById(id)
                .map(IssuerResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Issuer " + id + " was not found."));
    }
}