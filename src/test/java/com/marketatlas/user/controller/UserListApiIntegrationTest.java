package com.marketatlas.user.controller;

import com.marketatlas.market.TestPostgresConfiguration;
import org.junit.jupiter.api.BeforeEach;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestPostgresConfiguration.class)
@Transactional
class UserListApiIntegrationTest {

    private static final String PASSWORD = "a-long-test-password";

    @Autowired
    MockMvc mockMvc;

    private static String basic(String user, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setUp() throws Exception {
        for (String name : new String[] {"alice", "bob"}) {
            mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"" + name + "\",\"password\":\"" + PASSWORD + "\"}"))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    void theAdminSeesRegisteredUsersWithoutAnyPasswordData() throws Exception {
        mockMvc.perform(get("/api/users").header("Authorization", basic("admin", "test-only-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", hasItems("alice", "bob")))
                .andExpect(jsonPath("$[0].role").value("USER"))
                .andExpect(content().string(not(containsString("assword"))));
    }

    @Test
    void aRegularUserCannotListUsers() throws Exception {
        mockMvc.perform(get("/api/users").header("Authorization", basic("alice", PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousCallersAreRefused() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
    }
}
