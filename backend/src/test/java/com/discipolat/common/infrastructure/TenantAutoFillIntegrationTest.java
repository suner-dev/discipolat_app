package com.discipolat.common.infrastructure;

import com.discipolat.DiscipolatApplication;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.TypeDisciple;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AUTO-FILL DU TENANT À L'INSERT (intégrateur Hibernate — TenantFilterIntegrator).
 *
 * Objectif : prouver que la contrainte {@code tenant_id NOT NULL} de V70 ne
 * fait JAMAIS tomber un insert — même quand le service n'a pas fixé de
 * tenantId explicite :
 * - en contexte de requête HTTP : le tenant du JWT (TenantContext) est écrit
 *   dans la ligne insérée ;
 * - hors contexte (jobs planifiés, initialiseurs, tâches système) : repli sur
 *   le tenant par défaut créé par V70 (comportement historique conservé).
 *
 * NB : le listener Hibernate remplit la valeur de la colonne au moment du
 * flush (tableau d'état du persister) — l'entité en mémoire, elle, garde sa
 * valeur d'origine tant qu'elle n'est pas rechargée. Les assertions se font
 * donc sur la valeur réellement persistée en base (JdbcTemplate) puis sur la
 * relecture via le repository (filtre tenant actif).
 */
@SpringBootTest(classes = DiscipolatApplication.class)
@ActiveProfiles("test")
class TenantAutoFillIntegrationTest {

    private static final UUID DEFAULT_TENANT_ID = TenantContext.DEFAULT_TENANT_ID;
    private static final UUID TENANT_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired private SoulRepository soulRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        for (String table : List.of("soul_history", "soul_departments", "soul_notes", "soul_tags", "souls")) {
            jdbcTemplate.execute("TRUNCATE TABLE " + table);
        }
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        TenantContext.clear();
    }

    private Soul.SoulBuilder soulBuilder() {
        return Soul.builder()
                .nom("AutoFill").prenom("Test").email("autofill@test")
                .typeDisciple(TypeDisciple.NOUVEL_ARRIVANT)
                .dateIntegration(LocalDate.now())
                .statut(StatutAme.ACTIF)
                .faiseurId(UUID.randomUUID())
                .etatSpirituel("EN_CROISSANCE")
                .niveauCroissance(2);
    }

    private UUID persistedTenantId(UUID soulId) {
        return jdbcTemplate.queryForObject(
                "SELECT tenant_id FROM souls WHERE id = ?", UUID.class, soulId);
    }

    @Test
    @DisplayName("Contexte tenant actif : la ligne insérée hérite du tenant du JWT")
    void insert_SansTenantExplicite_HeriteDuTenantDuContexte() {
        TenantContext.setTenantId(TENANT_B);
        try {
            Soul soul = soulRepository.save(soulBuilder().build());
            assertThat(persistedTenantId(soul.getId())).isEqualTo(TENANT_B);
            // Relecture par le repository : le filtre tenant retrouve la ligne.
            assertThat(soulRepository.findById(soul.getId()))
                    .isPresent()
                    .get()
                    .extracting(Soul::getTenantId)
                    .isEqualTo(TENANT_B);
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("Hors contexte (job/seed) : repli sur le tenant par défaut — jamais de crash NOT NULL")
    void insert_HorsContexte_RepliSurTenantParDefaut() {
        Soul soul = soulRepository.save(soulBuilder().build());
        assertThat(persistedTenantId(soul.getId())).isEqualTo(DEFAULT_TENANT_ID);
        // Sans contexte tenant, la lecture par clé primaire retombe sur le
        // comportement standard (super.findById) et retrouve la ligne.
        assertThat(soulRepository.findById(soul.getId())).isPresent();
    }

    @Test
    @DisplayName("Un tenantId déjà fixé par le service n'est jamais écrasé")
    void insert_TenantIdExplicite_EstConserve() {
        UUID tenantExplicite = UUID.randomUUID();
        Soul soul = soulRepository.save(soulBuilder().tenantId(tenantExplicite).build());
        assertThat(persistedTenantId(soul.getId())).isEqualTo(tenantExplicite);
    }
}
