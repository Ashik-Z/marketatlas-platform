package com.marketatlas.user.dto;

import com.marketatlas.user.model.AppUser;
import com.marketatlas.user.model.UserRole;

import java.time.Instant;

public record UserResponse(Long id, String username, UserRole role, Instant createdAt) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole(), user.getCreatedAt());
    }
}