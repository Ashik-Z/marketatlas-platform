package com.marketatlas.market.client;

import org.springframework.stereotype.Component;
import com.marketatlas.market.dto.MarketDataDTO;
import com.marketatlas.market.dto.MarketDataResponseDTO;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import com.marketatlas.common.exception.MarketDataProviderException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

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
        MarketDataResponseDTO response;

        try{
            response = restClient
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
            } catch (RestClientResponseException exception) {
                throw new MarketDataProviderException(
                    "Twelve Data returned an HTTP error.",
                    exception
                );
            } catch (ResourceAccessException exception) {
            throw new MarketDataProviderException(
                    "Unable to communicate with Twelve Data.",
                    exception
                );
            }
        
            if (response == null) {
                throw new MarketDataProviderException(
                    "Twelve Data returned an empty response."
                );
            }
        
            if ("error".equalsIgnoreCase(response.getStatus())) {
                throw new MarketDataProviderException(
                    "Twelve Data request failed. Code: "
                        + response.getCode()
                        + ", message: "
                        + response.getMessage()
                );
            }
        
            if (response.getValues() == null) {
                throw new MarketDataProviderException(
                    "Twelve Data response did not contain market data."
                );
            }
        
            return response.getValues();
    }
}
