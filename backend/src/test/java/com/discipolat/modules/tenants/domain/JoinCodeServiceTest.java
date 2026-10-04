package com.discipolat.modules.tenants.domain;

import com.discipolat.common.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SPEC_ONBOARDING_FLOWS (BE-1) — cycle de vie des codes de rejointure.
 * Vérifie le format D2, la résolution publique sans PII et la garde cross-tenant.
 */
@ExtendWith(MockitoExtension.class)
class JoinCodeServiceTest {

    @Mock
    private TenantJoinCodeRepository joinCodeRepository;
    @Mock
    private TenantRepository tenantRepository;

    private JoinCodeService service;

    @BeforeEach
    void setUp() {
        service = new JoinCodeService(joinCodeRepository, tenantRepository);
    }

    private Tenant tenant(UUID id, String slug, TenantStatus status) {
        return Tenant.builder()
                .id(id)
                .name("Église Bethel")
                .slug(slug)
                .status(status)
                .plan("STARTER")
                .createdAt(Instant.now())
                .build();
    }

    // ── normalize (saisie utilisateur) ──────────────────────────────
    @Test
    void normalizeUppercasesCollapsesSpacesAndTrimsDashes() {
        assertEquals("BETHEL-7K2M", JoinCodeService.normalize("  bethel 7k2m "));
        assertEquals("BETHEL-7K2M", JoinCodeService.normalize("bethel--7k2m"));
        assertEquals("BETHEL", JoinCodeService.normalize("-bethel-"));
        assertNullValue(JoinCodeService.normalize("   "));
        assertNullValue(JoinCodeService.normalize(null));
    }

    private static void assertNullValue(String v) {
        org.junit.jupiter.api.Assertions.assertNull(v);
    }

    // ── generate : format PREFIXE-XXXX + persistance active ─────────
    @Test
    void generateBuildsUnambiguousCodeFromSlugAndPersistsActive() {
        UUID tenantId = UUID.randomUUID();
        when(tenantRepository.findById(tenantId))
                .thenReturn(Optional.of(tenant(tenantId, "bethel", TenantStatus.ACTIVE)));
        when(joinCodeRepository.existsByCodeAndIsActiveTrue(anyString())).thenReturn(false);
        when(joinCodeRepository.save(any(TenantJoinCode.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TenantJoinCode saved = service.generate(tenantId, null, null, null, UUID.randomUUID());

        assertNotNull(saved.getCode());
        // Préfixe = slug purgé des lettres ambiguës (I/L/O/U/0/1) : "BETHEL" -> "BETHE".
        assertTrue(saved.getCode().startsWith("BETHE-"),
                "code attendu de la forme BETHE-XXXX mais " + saved.getCode());
        String suffix = saved.getCode().substring("BETHE-".length());
        assertEquals(4, suffix.length());
        for (char c : suffix.toCharArray()) {
            assertTrue(JoinCodeService.ALPHABET.indexOf(c) >= 0,
                    "suffixe hors alphabet non ambigu : " + c);
        }
        assertTrue(saved.isActive());
        assertEquals(JoinMode.OPEN, saved.getJoinMode(), "défaut OPEN quand mode non précisé");
        verify(joinCodeRepository).save(any(TenantJoinCode.class));
    }

    @Test
    void generateRetriesUntilUniqueCandidateFound() {
        UUID tenantId = UUID.randomUUID();
        when(tenantRepository.findById(tenantId))
                .thenReturn(Optional.of(tenant(tenantId, "bethel", TenantStatus.ACTIVE)));
        // Premier candidat collisionné, second libre.
        when(joinCodeRepository.existsByCodeAndIsActiveTrue(anyString()))
                .thenReturn(true, false);
        when(joinCodeRepository.save(any(TenantJoinCode.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.generate(tenantId, null, null, JoinMode.APPROVAL, UUID.randomUUID());

        verify(joinCodeRepository, times(2)).existsByCodeAndIsActiveTrue(anyString());
        verify(joinCodeRepository, times(1)).save(any(TenantJoinCode.class));
    }

    @Test
    void generateFailsWhenTenantMissing() {
        UUID tenantId = UUID.randomUUID();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThrows(DomainException.class,
                () -> service.generate(tenantId, null, null, JoinMode.OPEN, null));
        verify(joinCodeRepository, never()).save(any());
    }

    // ── lookup public : vitrine, jamais le code ni la PII ───────────
    @Test
    void lookupByCodeReturnsVitrineWhenTenantActive() {
        UUID tenantId = UUID.randomUUID();
        TenantJoinCode code = TenantJoinCode.builder()
                .tenantId(tenantId)
                .code("BETHE-9K2M")
                .label(null)
                .joinMode(JoinMode.OPEN)
                .isActive(true)
                .build();
        when(joinCodeRepository.findByCodeAndIsActiveTrue("BETHEL-9K2M"))
                .thenReturn(Optional.of(code));
        when(tenantRepository.findById(tenantId))
                .thenReturn(Optional.of(tenant(tenantId, "bethel", TenantStatus.ACTIVE)));

        JoinCodeService.JoinLookup view = service.lookupByCode("bethel 9k2m");

        assertTrue(view.found());
        assertEquals("Église Bethel", view.churchName());
        assertEquals("bethel", view.slug());
        assertFalse(view.requiresApproval());
    }

    @Test
    void lookupByCodeHiddenWhenTenantNotActive() {
        UUID tenantId = UUID.randomUUID();
        TenantJoinCode code = TenantJoinCode.builder()
                .tenantId(tenantId)
                .code("BETHE-9K2M")
                .joinMode(JoinMode.OPEN)
                .isActive(true)
                .build();
        when(joinCodeRepository.findByCodeAndIsActiveTrue("BETHE-9K2M"))
                .thenReturn(Optional.of(code));
        when(tenantRepository.findById(tenantId))
                .thenReturn(Optional.of(tenant(tenantId, "bethel", TenantStatus.SUSPENDED)));

        assertFalse(service.lookupByCode("BETHE-9K2M").found());
    }

    @Test
    void lookupByCodeNotFoundWhenNoActiveCode() {
        when(joinCodeRepository.findByCodeAndIsActiveTrue("EGLISE-0000"))
                .thenReturn(Optional.empty());
        assertFalse(service.lookupByCode("eglise 0000").found());
    }

    // ── garde cross-tenant : un code d'un autre tenant est un 404 ───
    @Test
    void updateRejectsCodeBelongingToAnotherTenant() {
        UUID codeId = UUID.randomUUID();
        UUID ownerTenant = UUID.randomUUID();
        UUID attackerTenant = UUID.randomUUID();
        TenantJoinCode code = TenantJoinCode.builder()
                .tenantId(ownerTenant)
                .code("BETHE-9K2M")
                .joinMode(JoinMode.OPEN)
                .isActive(true)
                .build();
        when(joinCodeRepository.findById(codeId)).thenReturn(Optional.of(code));

        DomainException thrown = assertThrows(DomainException.class,
                () -> service.update(codeId, attackerTenant, "Renommé", JoinMode.APPROVAL, true));
        assertEquals("JOIN_CODE_NOT_FOUND", thrown.getCode());
        verify(joinCodeRepository, never()).save(any());
    }
}
