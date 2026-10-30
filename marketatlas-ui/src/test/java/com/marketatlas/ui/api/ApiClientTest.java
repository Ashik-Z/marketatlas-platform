package com.marketatlas.ui.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class ApiClientTest {

    private static final String PASSWORD = "secret-pass-12";

    private MockRestServiceServer server;
    private ApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://api.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ApiClient(builder.build());
    }

    @Test
    void signUpPostsUsernameAndPasswordAsJson() {
        server.expect(requestTo("http://api.test/api/users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.password").value(PASSWORD))
                .andRespond(withStatus(HttpStatus.CREATED));

        client.signUp("alice", PASSWORD);

        server.verify();
    }

    @Test
    void duplicateUsernameIsReportedAsDuplicate() {
        server.expect(requestTo("http://api.test/api/users")).andRespond(withStatus(HttpStatus.CONFLICT));

        ApiException ex = assertThrows(ApiException.class, () -> client.signUp("alice", PASSWORD));

        assertEquals(ApiException.Kind.DUPLICATE_USER, ex.kind());
    }

    @Test
    void validationFailureCarriesTheApisDetailText() {
        server.expect(requestTo("http://api.test/api/users")).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body("{\"title\":\"Bad Request\",\"status\":400,\"detail\":\"username must not be blank\"}"));

        ApiException ex = assertThrows(ApiException.class, () -> client.signUp("", PASSWORD));

        assertEquals(ApiException.Kind.INVALID_INPUT, ex.kind());
        assertEquals("username must not be blank", ex.getMessage());
    }

    @Test
    void rateLimitCarriesRetryAfterSeconds() {
        server.expect(requestTo("http://api.test/api/users")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, "120"));

        ApiException ex = assertThrows(ApiException.class, () -> client.signUp("alice", PASSWORD));

        assertEquals(ApiException.Kind.RATE_LIMITED, ex.kind());
        assertEquals(120L, ex.retryAfterSeconds());
    }

    @Test
    void rateLimitWithoutHeaderHasNoRetryAfter() {
        server.expect(requestTo("http://api.test/api/users")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        ApiException ex = assertThrows(ApiException.class, () -> client.signUp("alice", PASSWORD));

        assertNull(ex.retryAfterSeconds());
    }

    @Test
    void loginSendsBasicCredentialsToAnAuthenticatedEndpoint() {
        String expected = "Basic " + Base64.getEncoder()
                .encodeToString(("alice:" + PASSWORD).getBytes(StandardCharsets.UTF_8));
        server.expect(requestTo("http://api.test/api/accounts"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, expected))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body("[]"));

        client.checkLogin("alice", PASSWORD);

        server.verify();
    }

    @Test
    void wrongCredentialsAreReportedAsInvalidCredentials() {
        server.expect(requestTo("http://api.test/api/accounts")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        ApiException ex = assertThrows(ApiException.class, () -> client.checkLogin("alice", "nope-nope-nope"));

        assertEquals(ApiException.Kind.INVALID_CREDENTIALS, ex.kind());
    }

    @Test
    void droppedConnectionIsReportedAsUnavailable() {
        server.expect(requestTo("http://api.test/api/accounts")).andRespond(request -> {
            throw new IOException("connection reset");
        });

        ApiException ex = assertThrows(ApiException.class, () -> client.checkLogin("alice", PASSWORD));

        assertEquals(ApiException.Kind.UNAVAILABLE, ex.kind());
    }

    @Test
    void errorMessagesNeverContainThePassword() {
        for (HttpStatus status : new HttpStatus[]{HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED,
                HttpStatus.CONFLICT, HttpStatus.TOO_MANY_REQUESTS, HttpStatus.INTERNAL_SERVER_ERROR}) {
            RestClient.Builder builder = RestClient.builder().baseUrl("http://api.test");
            MockRestServiceServer each = MockRestServiceServer.bindTo(builder).build();
            each.expect(requestTo("http://api.test/api/users")).andRespond(withStatus(status));
            ApiException ex = assertThrows(ApiException.class,
                    () -> new ApiClient(builder.build()).signUp("alice", PASSWORD));
            assertFalse(ex.getMessage().contains(PASSWORD), "message leaked the password for " + status);
        }
    }
}