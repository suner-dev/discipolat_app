package com.discipolat.security;

import com.discipolat.common.infrastructure.config.SecurityHeadersFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("P2 #66 — SecurityHeadersFilter")
class SecurityHeadersFilterTest {

    private SecurityHeadersFilter filter;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new SecurityHeadersFilter();
    }

    @Test
    @DisplayName("Should add Content-Security-Policy header")
    void shouldAddCSPHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        String csp = response.getHeader("Content-Security-Policy");
        assertNotNull(csp, "CSP header should be present");
        assertTrue(csp.contains("default-src 'self'"), "CSP should have default-src 'self'");
        assertTrue(csp.contains("frame-ancestors 'none'"), "CSP should block framing");
        assertTrue(csp.contains("base-uri 'self'"), "CSP should restrict base-uri");
    }

    @Test
    @DisplayName("Should add X-Content-Type-Options header")
    void shouldAddXContentTypeOptions() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
    }

    @Test
    @DisplayName("Should add X-Frame-Options DENY")
    void shouldAddXFrameOptions() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals("DENY", response.getHeader("X-Frame-Options"));
    }

    @Test
    @DisplayName("Should add X-XSS-Protection header")
    void shouldAddXXSSProtection() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals("1; mode=block", response.getHeader("X-XSS-Protection"));
    }

    @Test
    @DisplayName("Should add Strict-Transport-Security header")
    void shouldAddHSTS() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        String hsts = response.getHeader("Strict-Transport-Security");
        assertNotNull(hsts);
        assertTrue(hsts.contains("max-age=31536000"));
        assertTrue(hsts.contains("includeSubDomains"));
    }

    @Test
    @DisplayName("Should add Referrer-Policy header")
    void shouldAddReferrerPolicy() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals("strict-origin-when-cross-origin", response.getHeader("Referrer-Policy"));
    }

    @Test
    @DisplayName("Should add Permissions-Policy header")
    void shouldAddPermissionsPolicy() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        String pp = response.getHeader("Permissions-Policy");
        assertNotNull(pp);
        assertTrue(pp.contains("camera=()"), "Camera should be disabled");
        assertTrue(pp.contains("geolocation=()"), "Geolocation should be disabled");
    }

    @Test
    @DisplayName("Should always call filter chain next")
    void shouldAlwaysCallFilterChain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }
}
