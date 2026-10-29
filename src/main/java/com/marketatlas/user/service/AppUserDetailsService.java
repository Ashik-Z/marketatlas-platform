package com.marketatlas.user.service;

import com.marketatlas.user.model.AppUser;
import com.marketatlas.user.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

/** Logs in the bootstrap admin from the environment first, then users from the database. */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;
    private final String adminUsername;
    private final String adminPasswordHash;

    public AppUserDetailsService(AppUserRepository userRepository,
                                 PasswordEncoder encoder,
                                 @Value("${marketatlas.security.admin-username:admin}") String adminUsername,
                                 @Value("${marketatlas.security.admin-password:}") String adminPassword) {
        if (adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "marketatlas.security.admin-password must be set (env var ADMIN_PASSWORD)");
        }
        this.userRepository = userRepository;
        this.adminUsername = adminUsername.trim().toLowerCase(Locale.ROOT);
        this.adminPasswordHash = encoder.encode(adminPassword);
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        String normalized = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);

        // A fresh object every time: Spring erases the password of the object it authenticated.
        if (normalized.equals(adminUsername)) {
            return User.withUsername(adminUsername).password(adminPasswordHash).roles("ADMIN").build();
        }

        AppUser user = userRepository.findByUsername(normalized)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .build();
    }
}