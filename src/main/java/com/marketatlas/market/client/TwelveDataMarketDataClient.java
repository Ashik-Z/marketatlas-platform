package com.marketatlas.market.client;

import org.springframework.stereotype.Component;
import com.marketatlas.market.dto.MarketDataDTO;
import com.marketatlas.market.dto.MarketDataResponseDTO;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.util.List;

@Component
public class TwelveDataMarketDataClient implements MarketDataClient {

    @Value("${twelvedata.base-url}")
    private String baseUrl;
    
    @Value("${twelvedata.api-key}")
    private String apiKey;
    
    private final RestClient restClient;

    public TwelveDataMarketDataClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<MarketDataDTO> getHistoricalData(
        String symbol,
        LocalDate startDate,
        LocalDate endDate,
        String interval
    ) {
        // return restClient
        //     .get()
        //     .uri(uriBuilder -> uriBuilder
        //         .path("/time_series")
        //         .queryParam("symbol", symbol)
        //         .queryParam("interval", interval)
        //         .queryParam("start_date", startDate)
        //         .queryParam("end_date", endDate)
        //         .queryParam("apikey", apiKey)
        //         .build())
        //     .header("Accept", "application/json")
        //     .retrieve()
        //     .body(MarketDataResponseDTO.class)
        //     .getValues();
        // 
        MarketDataResponseDTO response = restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                    .path("/time_series")
                    .queryParam("symbol", symbol)
                    .queryParam("interval", interval)
                    .queryParam("start_date", startDate)
                    .queryParam("end_date", endDate)
                    .queryParam("apikey", apiKey)
                    .build())
                .header("Accept", "application/json")
                .retrieve()
                .body(MarketDataResponseDTO.class);
        
            if (response == null) {
                throw new IllegalStateException(
                    "Twelve Data returned an empty response."
                );
            }
        
            if ("error".equalsIgnoreCase(response.getStatus())) {
                throw new IllegalStateException(
                    "Twelve Data request failed. Code: "
                        + response.getCode()
                        + ", message: "
                        + response.getMessage()
                );
            }
        
            if (response.getValues() == null) {
                throw new IllegalStateException(
                    "Twelve Data response did not contain market data."
                );
            }
        
            return response.getValues();
    }
}
