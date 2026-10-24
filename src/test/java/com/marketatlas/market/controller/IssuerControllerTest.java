package com.marketatlas.market.controller;

import com.marketatlas.market.entity.Issuer;
import com.marketatlas.market.service.IssuerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IssuerController.class)
class IssuerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IssuerService issuerService;

    private Issuer issuerWithId(Long id, String name, String country) {
        Issuer issuer = new Issuer(name, country);
        ReflectionTestUtils.setField(issuer, "id", id);
        return issuer;
    }

    @Test
    void shouldCreateIssuer() throws Exception {
        when(issuerService.createIssuer(any(Issuer.class)))
                .thenReturn(issuerWithId(1L, "Apple Inc.", "United States"));

        mockMvc.perform(post("/api/issuers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "  Apple Inc.  ", "country": "United States"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/issuers/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Apple Inc."))
                .andExpect(jsonPath("$.country").value("United States"));
    }

    @Test
    void shouldRejectBlankName() throws Exception {
        mockMvc.perform(post("/api/issuers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "   ", "country": "United States"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());

        verifyNoInteractions(issuerService);
    }

    @Test
    void shouldRejectMissingCountry() throws Exception {
        mockMvc.perform(post("/api/issuers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Apple Inc."}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.country").exists());

        verifyNoInteractions(issuerService);
    }

    @Test
    void shouldListIssuers() throws Exception {
        when(issuerService.findAllIssuers()).thenReturn(List.of(
                issuerWithId(1L, "Apple Inc.", "United States"),
                issuerWithId(2L, "Tesla Inc.", "United States")));

        mockMvc.perform(get("/api/issuers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].name").value("Tesla Inc."));
    }

    @Test
    void shouldGetIssuerById() throws Exception {
        when(issuerService.findIssuerById(1L))
                .thenReturn(Optional.of(issuerWithId(1L, "Apple Inc.", "United States")));

        mockMvc.perform(get("/api/issuers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Apple Inc."));
    }

    @Test
    void shouldReturnNotFoundForUnknownIssuer() throws Exception {
        when(issuerService.findIssuerById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/issuers/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not Found"));
    }
}