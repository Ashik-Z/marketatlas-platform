package com.marketatlas.ui.api;

import java.time.Instant;

/** One registered user as returned by GET /api/users (admin only; never carries password data). */
public record UserSummary(Long id, String username, String role, Instant createdAt) {
}
