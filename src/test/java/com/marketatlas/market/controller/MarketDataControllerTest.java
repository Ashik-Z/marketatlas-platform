package com.marketatlas.market.controller;

import com.marketatlas.market.dto.MarketDataHistoryResponseDTO;
import com.marketatlas.market.service.MarketDataService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.mockito.Mockito.verifyNoInteractions;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MarketDataController.class)
class MarketDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MarketDataService marketDataService;

    @Test
    void shouldGetMarketHistory() throws Exception {

        MarketDataHistoryResponseDTO firstResponse =
            new MarketDataHistoryResponseDTO(
                "AAPL",
                254.80,
                255.30,
                254.60,
                255.10,
                183421.0,
                LocalDate.of(2026, 10, 1)
            );

        MarketDataHistoryResponseDTO secondResponse =
            new MarketDataHistoryResponseDTO(
                "AAPL",
                255.10,
                255.50,
                254.90,
                255.42,
                142310.0,
                LocalDate.of(2026, 10, 2)
            );

        List<MarketDataHistoryResponseDTO> response =
            List.of(firstResponse, secondResponse);

        when(marketDataService.fetchHistoricalData(
            "AAPL",
            LocalDate.of(2026, 9, 29),
            LocalDate.of(2026, 10, 2),
            "1day"
        )).thenReturn(response);

        mockMvc.perform(
            get("/api/market-data/history")
                .param("symbol", "AAPL")
                .param("startDate", "2026-09-29")
                .param("endDate", "2026-10-02")
                .param("interval", "1day")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].symbol").value("AAPL"))
            .andExpect(jsonPath("$[0].open").value(254.80))
            .andExpect(jsonPath("$[0].high").value(255.30))
            .andExpect(jsonPath("$[0].low").value(254.60))
            .andExpect(jsonPath("$[0].close").value(255.10))
            .andExpect(jsonPath("$[0].volume").value(183421.0))
            .andExpect(jsonPath("$[0].timestamp").value("2026-10-01"))
            .andExpect(jsonPath("$[1].symbol").value("AAPL"))
            .andExpect(jsonPath("$[1].timestamp").value("2026-10-02"));
    }

    @Test
    void shouldRejectInvalidDateRange() throws Exception {
        mockMvc.perform(
            get("/api/market-data/history")
                .param("symbol", "AAPL")
                .param("startDate", "2026-10-05")
                .param("endDate", "2026-10-01")
                .param("interval", "1day")
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Bad Request"))
        .andExpect(jsonPath("$.detail").value(
            "startDate must be before or equal to endDate"
        ));

        verifyNoInteractions(marketDataService);
    }

    @Test
    void shouldRejectBlankSymbol() throws Exception {
        mockMvc.perform(
            get("/api/market-data/history")
                .param("symbol", "   ")
                .param("startDate", "2026-10-01")
                .param("endDate", "2026-10-05")
                .param("interval", "1day")
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Bad Request"))
        .andExpect(jsonPath("$.detail").value(
            "One or more request parameters are invalid."
        ));

        verifyNoInteractions(marketDataService);
    }

    @Test
    void shouldRejectBlankInterval() throws Exception {
        mockMvc.perform(
            get("/api/market-data/history")
                .param("symbol", "AAPL")
                .param("startDate", "2026-10-01")
                .param("endDate", "2026-10-05")
                .param("interval", "    ")
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Bad Request"))
        .andExpect(jsonPath("$.detail").value(
            "One or more request parameters are invalid."
        ));

        verifyNoInteractions(marketDataService);
    }
    
    @Test
    void shouldRejectMissingStartDate() throws Exception {
        mockMvc.perform(
                        get("/api/market-data/history")
                                .param("symbol", "AAPL")
                                .param("endDate", "2026-10-05")
                                .param("interval", "1day")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value(
                        "Required request parameter 'startDate' is missing."
                ));

        verifyNoInteractions(marketDataService);
    }

    @Test
    void shouldRejectMalformedStartDate() throws Exception {
        mockMvc.perform(
                        get("/api/market-data/history")
                                .param("symbol", "AAPL")
                                .param("startDate", "not-a-date")
                                .param("endDate", "2026-10-05")
                                .param("interval", "1day")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(marketDataService);
    }

}