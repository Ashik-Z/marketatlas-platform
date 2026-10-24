package com.marketatlas.market.dto;

import com.marketatlas.market.entity.Issuer;

public record IssuerResponse(Long id, String name, String country) {

    public static IssuerResponse from(Issuer issuer) {
        return new IssuerResponse(issuer.getId(), issuer.getName(), issuer.getCountry());
    }
}