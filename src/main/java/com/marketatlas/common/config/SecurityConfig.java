package com.marketatlas.common.config;

import com.marketatlas.common.ratelimit.AuthRateLimitFilter;
import com.marketatlas.common.ratelimit.RateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import java.time.Clock;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ProblemDetailSecurityHandlers handlers,
            Clock clock,
            @Value("${marketatlas.rate-limit.signups-per-hour:5}") int signupsPerHour,
            @Value("${marketatlas.rate-limit.failed-logins-per-15-minutes:10}") int failedLogins)
            throws Exception {
        var rateLimitFilter = new AuthRateLimitFilter(
                new RateLimiter(signupsPerHour, Duration.ofHours(1), 100_000, clock),
                new RateLimiter(failedLogins, Duration.ofMinutes(15), 100_000, clock));
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/v3/api-docs", "/v3/api-docs/**",
                                "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        .requestMatchers("/api/accounts", "/api/accounts/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/assets/*/backtest").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/market-data/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        .anyRequest().hasRole("ADMIN"))
                .httpBasic(basic -> basic.authenticationEntryPoint(handlers))
                .exceptionHandling(errors -> errors.accessDeniedHandler(handlers))
                .addFilterBefore(rateLimitFilter, BasicAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}