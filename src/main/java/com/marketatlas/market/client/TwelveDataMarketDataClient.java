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

    private static final String NO_DATA_MESSAGE = "No data is available";

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
                        .queryParam("end_date", endDate.plusDays(1))            // Twelve Data treats end_date as exclusive for daily bars
                        .build())
                    .header("Authorization", "apikey " + apiKey)
                    .header("Accept", "application/json")
                    .retrieve()
                    .body(MarketDataResponseDTO.class);
            } catch (RestClientResponseException exception) {
                String body = exception.getResponseBodyAsString();
                if (body.length() > 300) {
                    body = body.substring(0, 300);
                }
                if (exception.getStatusCode().value() == 400
                        && body.contains(NO_DATA_MESSAGE)) {
                    return List.of();
                }

                throw new MarketDataProviderException(
                        "Twelve Data returned an HTTP error: "
                                + exception.getStatusCode().value() + " " + body,
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

            if ("error".equalsIgnoreCase(response.getStatus())
                    && response.getMessage() != null
                    && response.getMessage().contains(NO_DATA_MESSAGE)) {
                return List.of();
            }

            if ("error".equalsIgnoreCase(response.getStatus())) {
                throw new MarketDataProviderException(
                        "Twelve Data request failed. Code: "
                                + response.getCode()
                                + ", message: "
                                + response.getMessage()
                );
            }

            if (!"ok".equalsIgnoreCase(response.getStatus())) {
                throw new MarketDataProviderException(
                        "Twelve Data response contained an unexpected status: "
                                + response.getStatus()
                );
            }

            if (response.getValues() == null) {
                throw new MarketDataProviderException(
                        "Twelve Data response did not contain market data."
                );
            }

            for (MarketDataDTO record : response.getValues()) {
                if (record == null
                        || record.getOpen() == null
                        || record.getHigh() == null
                        || record.getLow() == null
                        || record.getClose() == null
                        || record.getTimestamp() == null) {
                    throw new MarketDataProviderException(
                            "Twelve Data response contained an invalid market data record: "
                                    + "a required field was missing."
                    );
                }

                double open = record.getOpen();
                double high = record.getHigh();
                double low = record.getLow();
                double close = record.getClose();

                if (!Double.isFinite(open)
                        || !Double.isFinite(high)
                        || !Double.isFinite(low)
                        || !Double.isFinite(close)
                        || open <= 0
                        || high <= 0
                        || low <= 0
                        || close <= 0
                        || high < low
                        || high < open
                        || high < close
                        || low > open
                        || low > close) {
                    throw new MarketDataProviderException(
                            "Twelve Data response contained an invalid market data record: "
                                    + "OHLC values were invalid."
                    );
                }
            }

            return response.getValues().stream()
                    .filter(record -> !record.getTimestamp().isBefore(startDate)
                            && !record.getTimestamp().isAfter(endDate))
                    .toList();
    }
}
