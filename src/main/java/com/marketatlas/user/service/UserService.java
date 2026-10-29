package com.marketatlas.user.service;

import com.marketatlas.common.exception.DuplicateResourceException;
import com.marketatlas.user.model.AppUser;
import com.marketatlas.user.model.UserRole;
import com.marketatlas.user.repository.AppUserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class UserService {

    private static final Pattern USERNAME = Pattern.compile("[a-z0-9._-]{3,50}");
    private static final int MIN_PASSWORD_CHARACTERS = 12;
    // BCrypt only uses the first 72 bytes of a password, so longer ones are refused, not silently cut.
    private static final int MAX_PASSWORD_BYTES = 72;

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    private final String reservedUsername;

    public UserService(AppUserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock,
                       @Value("${marketatlas.security.admin-username:admin}") String adminUsername) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.reservedUsername = adminUsername.trim().toLowerCase(Locale.ROOT);
    }

    /** Sign-up always creates a plain USER; no code path creates an admin. */
    @Transactional
    public AppUser register(String username, String password) {
        String normalized = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        if (!USERNAME.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Username must be 3 to 50 characters: lowercase letters, digits, dot, underscore or hyphen");
        }
        if (password == null || password.length() < MIN_PASSWORD_CHARACTERS) {
            throw new IllegalArgumentException(
                    "Password must be at least " + MIN_PASSWORD_CHARACTERS + " characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new IllegalArgumentException(
                    "Password must be at most " + MAX_PASSWORD_BYTES + " bytes");
        }

        if (normalized.equals(reservedUsername) || userRepository.existsByUsername(normalized)) {
            throw new DuplicateResourceException("Username is already taken");
        }
        try {
            return userRepository.saveAndFlush(new AppUser(
                    normalized, passwordEncoder.encode(password), UserRole.USER, clock.instant()));
        } catch (DataIntegrityViolationException raceOnTheUniqueConstraint) {
            // Two sign-ups for the same name at once: the database constraint is the real guard.
            throw new DuplicateResourceException("Username is already taken");
        }
    }
}