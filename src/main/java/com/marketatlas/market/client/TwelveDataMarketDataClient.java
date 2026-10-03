package com.marketatlas.market.client;

import org.springframework.stereotype.Component;
import com.marketatlas.market.dto.MarketDataDTO;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

@Component
public class TwelveDataMarketDataClient implements MarketDataClient {
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
        return null;
    }
}
