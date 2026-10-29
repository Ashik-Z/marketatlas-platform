package com.marketatlas.common.config;

import com.marketatlas.market.TestPostgresConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestPostgresConfiguration.class)
class OpenApiDocsIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void specIsPublicAndDescribesTheApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("MarketAtlas API"))
                .andExpect(jsonPath("$.components.securitySchemes.basicAuth.scheme").value("basic"))
                .andExpect(jsonPath("$.paths['/api/accounts/{accountId}/orders']").exists())
                .andExpect(jsonPath("$.paths['/api/assets/{assetId}/real-prices']").exists())
                .andExpect(jsonPath("$.paths['/api/users']").exists());
    }

    @Test
    void orderEndpointDocumentsTheRequiredIdempotencyKeyHeader() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.paths['/api/accounts/{accountId}/orders'].post.parameters[?(@.name=='Idempotency-Key')]")
                        .exists());
    }

    @Test
    void swaggerUiIsReachableWithoutLogin() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}