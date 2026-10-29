package com.marketatlas.user.dto;

import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(@NotNull String username, @NotNull String password) {

    @Override
    public String toString() {
        return "CreateUserRequest[username=" + username + ", password=****]";
    }
}