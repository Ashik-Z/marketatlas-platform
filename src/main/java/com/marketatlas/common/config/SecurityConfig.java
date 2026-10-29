package com.marketatlas.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ProblemDetailSecurityHandlers handlers) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/accounts", "/api/accounts/**").hasRole("ADMIN")
                        .requestMatchers("/api/market-data/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        .anyRequest().hasRole("ADMIN"))
                .httpBasic(basic -> basic.authenticationEntryPoint(handlers))
                .exceptionHandling(errors -> errors.accessDeniedHandler(handlers));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(
            PasswordEncoder encoder,
            @Value("${marketatlas.security.admin-username:admin}") String username,
            @Value("${marketatlas.security.admin-password:}") String password) {
        if (password.isBlank()) {
            throw new IllegalStateException(
                    "marketatlas.security.admin-password must be set (env var ADMIN_PASSWORD)");
        }
        return new InMemoryUserDetailsManager(
                User.withUsername(username)
                        .password(encoder.encode(password))
                        .roles("ADMIN")
                        .build());
    }
}