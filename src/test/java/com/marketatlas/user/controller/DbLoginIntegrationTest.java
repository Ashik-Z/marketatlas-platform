package com.marketatlas.user.controller;

import com.marketatlas.market.TestPostgresConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestPostgresConfiguration.class)
@Transactional
class DbLoginIntegrationTest {

    private static final String PASSWORD = "a-long-test-password";

    @Autowired
    MockMvc mockMvc;

    private static String basic(String user, String password) {
        String raw = user + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private void signUp(String username) throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void signedUpUserCanLogInButIsNotAdmin() throws Exception {
        signUp("login.user");

        // 403 (not 401) proves authentication worked and only the role is insufficient.
        mockMvc.perform(get("/api/accounts/999999").header("Authorization", basic("login.user", PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void loginIsCaseInsensitiveOnUsername() throws Exception {
        signUp("case.user");

        mockMvc.perform(get("/api/accounts/999999").header("Authorization", basic("CASE.User", PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void wrongPasswordIsUnauthorized() throws Exception {
        signUp("wrong.pass");

        mockMvc.perform(get("/api/accounts/999999").header("Authorization", basic("wrong.pass", "not-the-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownUserIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/accounts/999999").header("Authorization", basic("nobody.here", PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotCreateAccounts() throws Exception {
        signUp("no.create");

        mockMvc.perform(post("/api/accounts")
                        .header("Authorization", basic("no.create", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void bootstrapAdminCanLogInRepeatedly() throws Exception {
        // Twice on purpose: guards against Spring erasing a shared admin object's password.
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(get("/api/accounts/999999").header("Authorization", basic("admin", "test-only-password")))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void adminUsernameCannotBeSignedUp() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Admin\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isConflict());
    }
}