package com.marketatlas.ui.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
class ApiConfig {

    @Bean
    RestClient apiRestClient(@Value("${marketatlas.api.base-url}") String baseUrl,
                             @Value("${marketatlas.api.connect-timeout:5s}") Duration connectTimeout,
                             @Value("${marketatlas.api.read-timeout:10s}") Duration readTimeout) {
        HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(readTimeout);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }
}