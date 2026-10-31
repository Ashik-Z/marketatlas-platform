package com.marketatlas.ui.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AssetListClientTest {

    private MockRestServiceServer server;
    private ApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://api.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ApiClient(builder.build());
    }

    @Test
    void listAssetsParsesRowsAndIgnoresUnknownFields() {
        String json = "[{\"id\":1,\"name\":\"Gold Spot\",\"assetClass\":\"COMMODITY\",\"issuerId\":null,"
                + "\"symbol\":\"XAU/USD\",\"currency\":\"USD\",\"unit\":\"troy ounce\",\"extra\":true}]";
        server.expect(requestTo("http://api.test/api/assets"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<AssetSummary> assets = client.listAssets();

        assertEquals(1, assets.size());
        assertEquals(new AssetSummary(1L, "Gold Spot", "COMMODITY", "XAU/USD", "USD", "troy ounce"), assets.get(0));
        server.verify();
    }

    @Test
    void emptyListIsReturnedAsEmpty() {
        server.expect(requestTo("http://api.test/api/assets"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertTrue(client.listAssets().isEmpty());
    }

    @Test
    void serverErrorIsReportedAsUnavailable() {
        server.expect(requestTo("http://api.test/api/assets")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        ApiException ex = assertThrows(ApiException.class, () -> client.listAssets());

        assertEquals(ApiException.Kind.UNAVAILABLE, ex.kind());
    }
}