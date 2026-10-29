package com.marketatlas.common.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Limits sign-ups per client IP and blocks an IP that keeps failing Basic authentication. */
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String BODY = "{\"type\":\"about:blank\",\"title\":\"Too Many Requests\","
            + "\"status\":429,\"detail\":\"Too many requests. Try again later.\"}";

    private final RateLimiter signupLimiter;
    private final RateLimiter failedLoginLimiter;

    public AuthRateLimitFilter(RateLimiter signupLimiter, RateLimiter failedLoginLimiter) {
        this.signupLimiter = signupLimiter;
        this.failedLoginLimiter = failedLoginLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // getRemoteAddr only: X-Forwarded-For is client-controlled and would let callers dodge the limit.
        String ip = request.getRemoteAddr();
        boolean hasBasic = hasBasicCredentials(request);

        if ("POST".equals(request.getMethod()) && "/api/users".equals(request.getRequestURI())
                && !signupLimiter.tryAcquire(ip)) {
            reject(response, signupLimiter.retryAfterSeconds(ip));
            return;
        }
        if (hasBasic && failedLoginLimiter.isBlocked(ip)) {
            reject(response, failedLoginLimiter.retryAfterSeconds(ip));
            return;
        }

        chain.doFilter(request, response);

        if (hasBasic && response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            failedLoginLimiter.record(ip);
        }
    }

    private static boolean hasBasicCredentials(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && header.regionMatches(true, 0, "Basic ", 0, 6);
    }

    private static void reject(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(BODY);
    }
}