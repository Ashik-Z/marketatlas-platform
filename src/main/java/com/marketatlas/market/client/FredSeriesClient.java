package com.marketatlas.market.client;

import com.marketatlas.common.exception.MarketDataProviderException;
import com.marketatlas.market.dto.FredObservationsResponse;
import com.marketatlas.market.dto.SeriesObservationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class FredSeriesClient implements SeriesClient {

    private static final String MISSING_VALUE = ".";

    private final RestClient restClient;
    private final String apiKey;

    @Autowired
    public FredSeriesClient(
            @Value("${fred.base-url:https://api.stlouisfed.org/fred}") String baseUrl,
            @Value("${fred.api-key:}") String apiKey,
            @Value("${fred.connect-timeout:5s}") Duration connectTimeout,
            @Value("${fred.response-timeout:10s}") Duration responseTimeout) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(responseTimeout);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.apiKey = apiKey;
    }

    FredSeriesClient(RestClient restClient, String apiKey) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    @Override
    public List<SeriesObservationDTO> getObservations(
            String externalId, LocalDate startDate, LocalDate endDate) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new MarketDataProviderException("FRED API key is not configured.");
        }

        FredObservationsResponse response;
        try {
            response = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/series/observations")
                            .queryParam("series_id", externalId)
                            .queryParam("api_key", apiKey)
                            .queryParam("file_type", "json")
                            .queryParam("observation_start", startDate)
                            .queryParam("observation_end", endDate)
                            .build())
                    .header("Accept", "application/json")
                    .retrieve()
                    .body(FredObservationsResponse.class);
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            if (status == 429) {
                throw new MarketDataProviderException("FRED rate limit exceeded (HTTP 429).");
            }
            String body = exception.getResponseBodyAsString();
            if (body.length() > 300) {
                body = body.substring(0, 300);
            }
            throw new MarketDataProviderException(
                    "FRED returned an HTTP error: " + status + " " + body);
        } catch (ResourceAccessException exception) {
            // The exception message contains the full request URL, including the API key,
            // so neither its message nor the exception itself is passed on.
            throw new MarketDataProviderException("Unable to communicate with FRED.");
        }

        if (response == null) {
            throw new MarketDataProviderException("FRED returned an empty response.");
        }
        if (response.errorCode() != null) {
            throw new MarketDataProviderException("FRED request failed. Code: "
                    + response.errorCode() + ", message: " + response.errorMessage());
        }
        if (response.observations() == null) {
            throw new MarketDataProviderException("FRED response did not contain observations.");
        }

        List<SeriesObservationDTO> result = new ArrayList<>();
        for (FredObservationsResponse.Observation raw : response.observations()) {
            SeriesObservationDTO observation = toObservation(raw);
            if (!observation.date().isBefore(startDate) && !observation.date().isAfter(endDate)) {
                result.add(observation);
            }
        }
        result.sort((a, b) -> a.date().compareTo(b.date()));
        return result;
    }

    private static SeriesObservationDTO toObservation(FredObservationsResponse.Observation raw) {
        if (raw == null || raw.date() == null || raw.value() == null) {
            throw new MarketDataProviderException(
                    "FRED response contained an invalid observation: a required field was missing.");
        }
        try {
            LocalDate date = LocalDate.parse(raw.date());
            BigDecimal value = MISSING_VALUE.equals(raw.value().trim())
                    ? null
                    : new BigDecimal(raw.value().trim());
            return new SeriesObservationDTO(date, value);
        } catch (DateTimeParseException | NumberFormatException exception) {
            throw new MarketDataProviderException(
                    "FRED response contained an invalid observation: unparseable date or value.");
        }
    }
}