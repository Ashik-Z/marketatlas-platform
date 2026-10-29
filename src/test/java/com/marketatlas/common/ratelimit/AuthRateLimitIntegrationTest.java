package com.marketatlas.common.ratelimit;

import com.marketatlas.market.TestPostgresConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "marketatlas.rate-limit.signups-per-hour=2",
        "marketatlas.rate-limit.failed-logins-per-15-minutes=3"
})
@AutoConfigureMockMvc
@Import(TestPostgresConfiguration.class)
@Transactional
class AuthRateLimitIntegrationTest {

    private static final String PASSWORD = "a-long-test-password";
    private static final String ACCOUNT_URL = "/api/accounts/999999";

    @Autowired
    MockMvc mockMvc;

    private static RequestPostProcessor from(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private static String basic(String user, String password) {
        String raw = user + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private int signUp(String ip, String username) throws Exception {
        return mockMvc.perform(post("/api/users").with(from(ip))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andReturn().getResponse().getStatus();
    }

    private void failLoginThreeTimes(String ip) throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get(ACCOUNT_URL).with(from(ip)).header("Authorization", basic("nobody", "wrong-pass")))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void thirdSignUpFromOneIpIsRejectedWith429() throws Exception {
        org.junit.jupiter.api.Assertions.assertEquals(201, signUp("10.0.1.1", "rl.one"));
        org.junit.jupiter.api.Assertions.assertEquals(201, signUp("10.0.1.1", "rl.two"));

        mockMvc.perform(post("/api/users").with(from("10.0.1.1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rl.three\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void signUpLimitIsPerIp() throws Exception {
        signUp("10.0.2.1", "rl.ip1a");
        signUp("10.0.2.1", "rl.ip1b");
        org.junit.jupiter.api.Assertions.assertEquals(429, signUp("10.0.2.1", "rl.ip1c"));

        org.junit.jupiter.api.Assertions.assertEquals(201, signUp("10.0.2.2", "rl.ip2a"));
    }

    @Test
    void repeatedFailedLoginsLockTheIpOut() throws Exception {
        failLoginThreeTimes("10.0.3.1");

        mockMvc.perform(get(ACCOUNT_URL).with(from("10.0.3.1"))
                        .header("Authorization", basic("nobody", "wrong-pass")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void lockoutAppliesEvenToCorrectCredentialsFromThatIpOnly() throws Exception {
        failLoginThreeTimes("10.0.4.1");

        mockMvc.perform(get(ACCOUNT_URL).with(from("10.0.4.1"))
                        .header("Authorization", basic("admin", "test-only-password")))
                .andExpect(status().isTooManyRequests());
        mockMvc.perform(get(ACCOUNT_URL).with(from("10.0.4.2"))
                        .header("Authorization", basic("admin", "test-only-password")))
                .andExpect(status().isNotFound());
    }

    @Test
    void successfulAndAnonymousRequestsDoNotCountAsFailures() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get(ACCOUNT_URL).with(from("10.0.5.1"))
                            .header("Authorization", basic("admin", "test-only-password")))
                    .andExpect(status().isNotFound());
        }
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get(ACCOUNT_URL).with(from("10.0.5.1")))
                    .andExpect(status().isUnauthorized());
        }
        // Still well under the limit: one real failure is just a 401.
        mockMvc.perform(get(ACCOUNT_URL).with(from("10.0.5.1"))
                        .header("Authorization", basic("nobody", "wrong-pass")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forwardedForHeaderCannotDodgeTheLimit() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get(ACCOUNT_URL).with(from("10.0.6.1"))
                            .header("X-Forwarded-For", "203.0.113." + i)
                            .header("Authorization", basic("nobody", "wrong-pass")))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(get(ACCOUNT_URL).with(from("10.0.6.1"))
                        .header("X-Forwarded-For", "203.0.113.99")
                        .header("Authorization", basic("nobody", "wrong-pass")))
                .andExpect(status().isTooManyRequests());
    }
}