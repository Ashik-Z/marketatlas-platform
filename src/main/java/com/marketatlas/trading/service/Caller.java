package com.marketatlas.trading.service;

import org.springframework.security.core.Authentication;

public record Caller(String username, boolean admin) {

    public static Caller from(Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return new Caller(authentication.getName(), admin);
    }
}