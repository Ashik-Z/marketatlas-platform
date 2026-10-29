package com.marketatlas.common.config;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.marketatlas.market.TestPostgresConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestPostgresConfiguration.class)
class OpenApiSecurityConsistencyTest {

    private static final List<String> METHODS = List.of("get", "post", "put", "delete", "patch");

    @Autowired
    MockMvc mockMvc;

    @Test
    void documentedSecurityMatchesWhatAnAnonymousCallerActuallyGets() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        var doc = JsonPath.using(Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL))
                .parse(json);

        Map<String, Object> paths = doc.read("$.paths");
        int checked = 0;
        for (String path : paths.keySet()) {
            Map<String, Object> item = doc.read("$.paths['" + path + "']");
            for (String method : item.keySet()) {
                if (!METHODS.contains(method)) {
                    continue;
                }
                List<Object> security = doc.read("$.paths['" + path + "']." + method + ".security");
                boolean documentedSecured = security != null && !security.isEmpty();

                String url = path.replaceAll("\\{[^}]+}", "1");
                int status = mockMvc.perform(request(HttpMethod.valueOf(method.toUpperCase()), url)
                                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                        .andReturn().getResponse().getStatus();

                String label = method.toUpperCase() + " " + path + " -> " + status;
                if (documentedSecured) {
                    assertEquals(401, status, "documented as secured but anonymous got: " + label);
                } else {
                    assertNotEquals(401, status, "documented as public but anonymous got: " + label);
                    assertNotEquals(403, status, "documented as public but anonymous got: " + label);
                }
                checked++;
            }
        }
        assertTrue(checked >= 18, "expected to check every operation, checked only " + checked);
    }
}