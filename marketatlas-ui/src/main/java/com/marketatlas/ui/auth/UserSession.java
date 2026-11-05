package com.marketatlas.ui.auth;

import com.marketatlas.ui.api.ApiClient;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import org.springframework.stereotype.Component;

/** The logged-in user, kept in the server-side Vaadin session only. */
@Component
@VaadinSessionScope
public class UserSession {

    private String username;
    private String authHeader;
    private boolean admin;

    public void login(String username, String password) {
        this.username = username;
        this.authHeader = ApiClient.basicHeader(username, password);
        this.admin = false;
    }

    public void logout() {
        this.username = null;
        this.authHeader = null;
        this.admin = false;
    }

    public void login(String username, String password, boolean admin) {
        login(username, password);
        this.admin = admin;
    }

    public boolean isAdmin() {
        return admin;
    }

    public boolean isLoggedIn() {
        return authHeader != null;
    }

    public String username() {
        return username;
    }

    public String authHeader() {
        return authHeader;
    }
}