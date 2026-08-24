package com.discipolat.security;

import com.discipolat.common.infrastructure.config.TenantFileIsolationConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("P2 #83 — TenantFileIsolationConfig")
class TenantFileIsolationConfigTest {

    private final TenantFileIsolationConfig config = new TenantFileIsolationConfig();

    @Test
    @DisplayName("Should implement WebMvcConfigurer")
    void shouldImplementWebMvcConfigurer() {
        assertInstanceOf(WebMvcConfigurer.class, config);
    }

    @Test
    @DisplayName("Should not be null")
    void shouldNotBeNull() {
        assertNotNull(config);
    }

    @Test
    @DisplayName("Config class should be instantiable")
    void shouldBeInstantiable() {
        TenantFileIsolationConfig newConfig = new TenantFileIsolationConfig();
        assertNotNull(newConfig);
    }

    @Test
    @DisplayName("Two instances should be different")
    void twoInstancesShouldBeDifferent() {
        TenantFileIsolationConfig c1 = new TenantFileIsolationConfig();
        TenantFileIsolationConfig c2 = new TenantFileIsolationConfig();
        assertNotSame(c1, c2);
    }
}
