package com.marketatlas.common.config;

import com.marketatlas.market.refresh.RefreshProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(RefreshProperties.class)
public class SchedulingConfig {
}