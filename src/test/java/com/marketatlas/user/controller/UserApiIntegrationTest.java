package com.marketatlas.user.controller;

import com.marketatlas.market.TestPostgresConfiguration;
import com.marketatlas.user.model.AppUser;
import com.marketatlas.user.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestPostgresConfiguration.class)
@Transactional
class UserApiIntegrationTest {

    private static final String PASSWORD = "correct horse battery";

    @Autowired private MockMvc mockMvc;
    @Autowired private AppUserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // Sign-up is public, so none of these requests carries credentials.
    private static MockHttpServletRequestBuilder signUp(String json) {
        return MockMvcRequestBuilders.post("/api/users")
                .contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private static String body(String username, String password) {
        return "{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}";
    }

    @Test
    void shouldSignUpAUserWithoutAnyLoginAndNeverReturnThePassword() throws Exception {
        mockMvc.perform(signUp(body("  Alice.Smith  ", PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice.smith"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void shouldStoreOnlyABcryptHashOfThePassword() throws Exception {
        mockMvc.perform(signUp(body("bob", PASSWORD))).andExpect(status().isCreated());

        AppUser stored = userRepository.findByUsername("bob").orElseThrow();
        assertTrue(stored.getPasswordHash().startsWith("$2"));
        assertFalse(stored.getPasswordHash().contains(PASSWORD));
        assertTrue(passwordEncoder.matches(PASSWORD, stored.getPasswordHash()));
    }

    @Test
    void shouldRejectADuplicateUsernameIgnoringCase() throws Exception {
        mockMvc.perform(signUp(body("carol", PASSWORD))).andExpect(status().isCreated());

        mockMvc.perform(signUp(body("CAROL", PASSWORD)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectInvalidUsernamesAndPasswords() throws Exception {
        mockMvc.perform(signUp(body("ab", PASSWORD))).andExpect(status().isBadRequest());
        mockMvc.perform(signUp(body("bad name!", PASSWORD))).andExpect(status().isBadRequest());
        mockMvc.perform(signUp(body("dave", "tooshort"))).andExpect(status().isBadRequest());
        // 73 bytes: BCrypt would silently ignore the tail, so it is refused instead
        mockMvc.perform(signUp(body("dave", "a".repeat(73)))).andExpect(status().isBadRequest());
        mockMvc.perform(signUp("{\"username\": \"dave\"}")).andExpect(status().isBadRequest());
        mockMvc.perform(signUp("{\"password\": \"" + PASSWORD + "\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void shouldIgnoreARoleSentByTheClient() throws Exception {
        mockMvc.perform(signUp("{\"username\": \"mallory\", \"password\": \"" + PASSWORD
                        + "\", \"role\": \"ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));
    }
}