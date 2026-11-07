package com.marketatlas.ui.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MeClientTest {

    private MockRestServiceServer server;
    private ApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://api.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ApiClient(builder.build());
    }

    @Test
    void sendsTheAuthorizationHeaderAndReadsTheRole() {
        server.expect(requestTo("http://api.test/api/me"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Basic abc"))
                .andRespond(withSuccess("{\"username\":\"admin\",\"role\":\"ADMIN\",\"extra\":1}",
                        MediaType.APPLICATION_JSON));

        Me me = client.me("Basic abc");

        assertEquals("admin", me.username());
        assertTrue(me.admin());
        server.verify();
    }

    @Test
    void aUserRoleIsNotAdmin() {
        server.expect(requestTo("http://api.test/api/me"))
                .andRespond(withSuccess("{\"username\":\"carol\",\"role\":\"USER\"}", MediaType.APPLICATION_JSON));

        assertFalse(client.me("Basic abc").admin());
    }

    @Test
    void wrongCredentialsAreMapped() {
        server.expect(requestTo("http://api.test/api/me")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        ApiException e = assertThrows(ApiException.class, () -> client.me("Basic nope"));
        assertEquals(ApiException.Kind.INVALID_CREDENTIALS, e.kind());
    }

    @Test
    void aSleepingApiIsReportedAsWakingUp() {
        for (HttpStatus status : new HttpStatus[] {HttpStatus.BAD_GATEWAY, HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.GATEWAY_TIMEOUT}) {
            server.reset();
            server.expect(requestTo("http://api.test/api/me")).andRespond(withStatus(status));

            ApiException ex = assertThrows(ApiException.class, () -> client.me("Basic abc"));

            assertEquals(ApiException.Kind.UNAVAILABLE, ex.kind());
            assertTrue(ex.getMessage().contains("waking up"));
        }
    }

    @Test
    void warmUpNeverThrows() {
        server.expect(requestTo("http://api.test/api/assets")).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        client.warmUp();

        server.verify();
    }
}
