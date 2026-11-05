package com.marketatlas.ui.api;

/** Response of GET /api/me: who the credentials belong to. */
public record Me(String username, String role) {

    public boolean admin() {
        return "ADMIN".equals(role);
    }
}
