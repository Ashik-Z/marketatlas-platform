package com.marketatlas.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProblemDetailSecurityHandlersTest {

    private final ProblemDetailSecurityHandlers handlers = new ProblemDetailSecurityHandlers();

    @Test
    void shouldAnswer403AsAProblemDetailWithoutEchoingTheReason() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handlers.handle(new MockHttpServletRequest(), response, new AccessDeniedException("secret reason"));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/problem+json"));
        assertTrue(response.getContentAsString().contains("\"title\":\"Forbidden\""));
        assertFalse(response.getContentAsString().contains("secret reason"));
    }

    @Test
    void shouldAnswer401WithABasicChallengeWithoutEchoingTheReason() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handlers.commence(new MockHttpServletRequest(), response, new BadCredentialsException("no such user"));

        assertEquals(401, response.getStatus());
        assertTrue(response.getHeader("WWW-Authenticate").startsWith("Basic"));
        assertTrue(response.getContentAsString().contains("\"title\":\"Unauthorized\""));
        assertFalse(response.getContentAsString().contains("no such user"));
    }
}