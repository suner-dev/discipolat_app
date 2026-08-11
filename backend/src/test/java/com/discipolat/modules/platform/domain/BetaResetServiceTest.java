package com.discipolat.modules.platform.domain;

import com.discipolat.common.exception.BadRequestException;
import com.discipolat.common.infrastructure.config.DataInitializer;
import com.discipolat.modules.audit.domain.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests de sécurité du reset de l'environnement bêta.
 *
 * La réinitialisation supprime TOUTES les données métier : elle doit être
 * strictement impossible en production. Double garde testée ici :
 *  1. environnement = prod → refus (même si le flag était forcé) ;
 *  2. flag `reset-enabled` = false → refus (dev/docker/prod par défaut).
 *
 * NB : le chemin de réussite (TRUNCATE + seed) est couvert par la
 * vérification end-to-end sur Postgres réel (bêta-testing local) — un test
 * d'intégration H2 ne peut pas reproduire `pg_tables`/TRUNCATE CASCADE.
 */
class BetaResetServiceTest {

    private BetaResetService service;
    private JdbcTemplate jdbcTemplate;
    private DataSource dataSource;
    private DataInitializer dataInitializer;
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        dataSource = mock(DataSource.class);
        dataInitializer = mock(DataInitializer.class);
        auditService = mock(AuditService.class);
        service = new BetaResetService(jdbcTemplate, dataSource, dataInitializer, auditService);
    }

    @Test
    void reset_refusesOnProductionEnvironment_evenIfFlagForced() {
        // Pire cas : un opérateur force BETA_RESET_ENABLED=true en prod.
        ReflectionTestUtils.setField(service, "environment", "prod");
        ReflectionTestUtils.setField(service, "resetEnabled", true);

        assertThrows(BadRequestException.class, service::reset, "Le reset doit être refusé en production");

        // Aucune requête destructive ne doit être émise.
        verify(jdbcTemplate, never()).execute(anyString());
        verify(jdbcTemplate, never()).queryForList(anyString(), eq(String.class));
    }

    @Test
    void reset_refusesWhenFlagDisabled() {
        // Environnement bêta mais flag désactivé (défaut hors profil beta).
        ReflectionTestUtils.setField(service, "environment", "beta");
        ReflectionTestUtils.setField(service, "resetEnabled", false);

        assertThrows(BadRequestException.class, service::reset, "Le reset doit être refusé si le flag est désactivé");
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void status_reportsEnvironmentAndFlag() {
        ReflectionTestUtils.setField(service, "environment", "beta");
        ReflectionTestUtils.setField(service, "resetEnabled", true);

        var status = service.status();
        assertEquals("beta", status.get("environment"));
        assertEquals(Boolean.TRUE, status.get("resetEnabled"));

        ReflectionTestUtils.setField(service, "environment", "prod");
        ReflectionTestUtils.setField(service, "resetEnabled", false);
        status = service.status();
        assertEquals("prod", status.get("environment"));
        assertEquals(Boolean.FALSE, status.get("resetEnabled"));
    }
}
