package com.discipolat.common.infrastructure.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.discipolat.common.infrastructure.security.JwtAuthenticationFilter;
import com.discipolat.common.infrastructure.security.JwtTokenProvider;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.environment:dev}")
    private String environment;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String[] allowedOrigins;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"status\":401,\"title\":\"Unauthorized\",\"detail\":\"Authentication required or token expired\"}"
                    );
                })
                .accessDeniedHandler((request, response, deniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"status\":403,\"title\":\"Forbidden\",\"detail\":\"Insufficient permissions\"}"
                    );
                })
            )
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers("/api/v1/auth/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/public/**").permitAll()
                    // Demande de démonstration depuis la landing (public, rate-limitée
                    // par IP : 3 req / 10 min — cf. PerIpRateLimiter.tryConsumeDemoRequest)
                    .requestMatchers(HttpMethod.POST, "/api/v1/public/demo-requests").permitAll()
                    // Webhook opérateur Mobile Money : sécurisé par la référence unique
                    // de l'intention + (en production) signature HMAC opérateur / IP allowlist.
                    // Webhook endpoint moved behind authentication — webhook secret is now mandatory in production.
                    // .requestMatchers(HttpMethod.POST, "/api/v1/payments/webhook").permitAll()
                    // Actuator: health public (for load balancer / Render healthcheck), details only when authenticated
                    .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                    .requestMatchers("/actuator/**").hasAnyRole("ADMIN", "PASTEUR");

                // Swagger: public in dev/docker, restricted to ADMIN/PASTEUR in prod/beta
                if ("dev".equals(environment) || "docker".equals(environment)) {
                    auth.requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                } else {
                    auth.requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").hasAnyRole("ADMIN", "PASTEUR");
                }

                auth.anyRequest().authenticated();
            })
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class)
            // Security headers
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; frame-ancestors 'none'; script-src 'self'")
                )
                .frameOptions(frame -> frame.deny())
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
            );

        return http.build();
    }

@Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "X-Webhook-Secret", "X-Hub-Signature-256"));
        configuration.setExposedHeaders(List.of(
                "Authorization",
                "X-RateLimit-Remaining",
                "Retry-After"
        ));
        // Credentials allowed only for non-wildcard origins to prevent tunnel-based MITM attacks
        boolean hasWildcard = Arrays.stream(allowedOrigins).anyMatch(o -> o.contains("*"));
        configuration.setAllowCredentials(!hasWildcard);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
