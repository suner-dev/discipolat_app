package com.discipolat.modules.tenants.domain;

import com.discipolat.common.exception.DomainException;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.modules.audit.domain.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SPEC_ORGANISATION_DENOMINATION_V2 §7.1 / T-B2 — <b>gouvernance traçable</b>
 * (faille F11).
 *
 * <p>L'IHM de gouvernance annonce « chaque action est auditée » et propose un
 * champ « motif ». Le service, lui, acceptait un motif vide et ne l'écrivait
 * nulle part : un bannissement restait inexplicable à la relecture. Ce test
 * verrouille les trois propriétés qui rendent la traçabilité réelle —
 * <b>motif obligatoire</b>, <b>motif persistant</b>, <b>motif journalisé</b> —
 * plus la contrainte de longueur, dont dépendait silencieusement
 * l'insertabilité du message.
 *
 * <p><b>Note de conception.</b> {@code AuditService},
 * {@code EntityPropagationPublisher} et {@code ApplicationEventPublisher} sont
 * des <i>classes</i> : le mock inline de Byte Buddy refuse de les
 * instrumenter au-delà des JDK qu'il supporte (le runtime de référence du
 * projet est JDK 21), ce qui rendrait ce test inexécutable ailleurs. On les
 * remplace donc par des doublures, et on ne mocke que les interfaces.
 */
@ExtendWith(MockitoExtension.class)
class TenantGovernanceServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private TenantWarningRepository warningRepository;
    @Mock private TenantDisputeRepository disputeRepository;
    /** Interface : un mock Mockito y est sans risque (cf. note de conception). */
    @Mock private ApplicationEventPublisher eventPublisher;

    private final RecordingAuditService auditService = new RecordingAuditService();

    private TenantGovernanceService service;
    private UUID tenantId;
    private UUID actorId;

    /** Faux qui enregistre les actions d'audit (classe concrète). */
    private static final class RecordingAuditService extends AuditService {
        private final List<String> actions = new ArrayList<>();
        private final List<Map<String, Object>> payloads = new ArrayList<>();

        RecordingAuditService() {
            super(null, null, null, null);
        }

        @Override
        public void log(UUID actorId, UUID tenantId, String action, String resourceType,
                        UUID resourceId, String result, Map<String, Object> metadata,
                        String ipAddress, String userAgent, HttpServletRequest request) {
            actions.add(action);
            payloads.add(metadata);
        }

        @Override
        public void logSimple(String action, String entiteType, UUID entiteId) {
            actions.add(action);
            payloads.add(Map.of());
        }
    }

    /**
     * Doublure de {@code EntityPropagationPublisher} (classe concrète, donc
     * non mockable hors JDK 21) : on neutralise la seule méthode appelée par
     * le chemin testé. La publication d'évènement est vérifiée par ailleurs,
     * dans les tests d'intégration — ici elle ne ferait que du bruit.
     */
    private static final class SilentPropagationPublisher extends EntityPropagationPublisher {
        SilentPropagationPublisher() {
            super(null, null);
        }

        @Override
        public void publishStatusChanged(String entityType, UUID entityId,
                                         String oldStatus, String newStatus, String description) {
            // no-op volontaire : voir le commentaire de la classe.
        }
    }

    @BeforeEach
    void setUp() {
        service = new TenantGovernanceService(tenantRepository, warningRepository,
                disputeRepository, auditService,
                new SilentPropagationPublisher(), eventPublisher);
        tenantId = UUID.randomUUID();
        actorId = UUID.randomUUID();
    }

    /** Le statut HTTP réellement produit par le ProblemDetail — le contrat vu par le client. */
    private static int httpStatus(DomainException failure) {
        return failure.toProblemDetail().getStatus();
    }

    private Tenant activeTenant() {        return Tenant.builder()
                .id(tenantId)
                .name("Église Bethel")
                .slug("bethel")
                .status(TenantStatus.ACTIVE)
                .plan("free")
                .build();
    }

    private Tenant tenantWithStatus(TenantStatus status) {
        return Tenant.builder()
                .id(tenantId)
                .name("Église Bethel")
                .slug("bethel")
                .status(status)
                .plan("free")
                .build();
    }

    // ========================================================================
    // MOTIF OBLIGATOIRE (F11)
    // ========================================================================

    @Test
    @DisplayName("Blocage sans motif → 400 et AUCUNE écriture (ni statut, ni trace, ni audit)")
    void blockWithoutReasonIsRejected() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(activeTenant()));

        for (String blank : new String[] { null, "", "   " }) {
            DomainException failure = assertThrows(DomainException.class,
                    () -> service.block(tenantId, blank, actorId));
            assertEquals("GOVERNANCE_REASON_REQUIRED", failure.getCode());
            assertEquals(400, httpStatus(failure));
        }

        verify(tenantRepository, never()).save(any());
        verify(warningRepository, never()).save(any());
        assertTrue(auditService.actions.isEmpty(), "une action refusée ne doit rien écrire");
    }

    @Test
    @DisplayName("Bannissement sans motif → refus identique : la décision la plus grave exige la trace")
    void banWithoutReasonIsRejected() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(activeTenant()));

        DomainException failure = assertThrows(DomainException.class,
                () -> service.ban(tenantId, "  ", actorId));

        assertEquals("GOVERNANCE_REASON_REQUIRED", failure.getCode());
        verify(tenantRepository, never()).save(any());
    }

    // ========================================================================
    // MOTIF PERSISTÉ + JOURNALISÉ
    // ========================================================================

    @Test
    @DisplayName("Blocage : le motif est conservé dans tenant_warnings ET dans l'audit")
    void blockPersistsAndAuditsTheReason() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(activeTenant()));
        when(tenantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Tenant blocked = service.block(tenantId, "  Contenu non conforme  ", actorId);

        assertEquals(TenantStatus.SUSPENDED, blocked.getStatus());

        // (1) Trace persistée, motif présent et nettoyé.
        ArgumentCaptor<TenantWarning> warning = ArgumentCaptor.forClass(TenantWarning.class);
        verify(warningRepository).save(warning.capture());
        assertTrue(warning.getValue().getMessage().contains("Contenu non conforme"),
                "le motif doit être lisible dans la trace : " + warning.getValue().getMessage());
        assertEquals(TenantWarning.Severity.FORMAL, warning.getValue().getSeverity());

        // (2) Journal d'audit chaîné : l'action ET son motif.
        assertEquals(List.of("TENANT_BLOCKED"), auditService.actions);
        assertNotNull(auditService.payloads.get(0).get("reason"));
        assertEquals("Contenu non conforme", auditService.payloads.get(0).get("reason"));
        assertEquals("SUSPENDED", auditService.payloads.get(0).get("newStatus"));
        assertEquals("ACTIVE", auditService.payloads.get(0).get("previousStatus"));
    }

    @Test
    @DisplayName("Bannissement : gravité FINAL — c'est la trace la plus lourde")
    void banUsesFinalSeverity() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(activeTenant()));
        when(tenantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.ban(tenantId, "Vols répétés", actorId);

        ArgumentCaptor<TenantWarning> warning = ArgumentCaptor.forClass(TenantWarning.class);
        verify(warningRepository).save(warning.capture());
        assertEquals(TenantWarning.Severity.FINAL, warning.getValue().getSeverity());
    }

    // ========================================================================
    // LONGUEUR : le message doit TOUJOURS tenir dans VARCHAR(1000)
    // ========================================================================

    @Test
    @DisplayName("Motif à la longueur maximale → accepté, et le message composé tient dans la colonne")
    void maximalReasonStillFitsTheColumn() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(activeTenant()));
        when(tenantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String maxReason = "m".repeat(TenantGovernanceService.MAX_REASON_LENGTH);
        service.block(tenantId, maxReason, actorId);

        ArgumentCaptor<TenantWarning> warning = ArgumentCaptor.forClass(TenantWarning.class);
        verify(warningRepository).save(warning.capture());
        String stored = warning.getValue().getMessage();
        assertTrue(stored.length() <= TenantGovernanceService.MAX_MESSAGE_LENGTH,
                "le message composé (" + stored.length()
                        + ") dépasserait tenant_warnings.message VARCHAR("
                        + TenantGovernanceService.MAX_MESSAGE_LENGTH + ")");
    }

    @Test
    @DisplayName("Motif trop long → 400 explicite, pas une violation de colonne en 500")
    void overlongReasonIsRejectedCleanly() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(activeTenant()));

        String tooLong = "m".repeat(TenantGovernanceService.MAX_REASON_LENGTH + 1);
        DomainException failure = assertThrows(DomainException.class,
                () -> service.block(tenantId, tooLong, actorId));

        assertEquals("GOVERNANCE_REASON_TOO_LONG", failure.getCode());
        assertEquals(400, httpStatus(failure));
        verify(warningRepository, never()).save(any());
    }

    // ========================================================================
    // GARDES DE TRANSITION (inchangées, mais verrouillées)
    // ========================================================================

    @Test
    @DisplayName("Débloquer une église active est refusé — sinon la trace dirait n'importe quoi")
    void unblockRequiresSuspendedState() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenantWithStatus(TenantStatus.ACTIVE)));

        DomainException failure = assertThrows(DomainException.class,
                () -> service.unblock(tenantId, "Par erreur", actorId));

        assertEquals("TENANT_NOT_BLOCKED", failure.getCode());
    }

    @Test
    @DisplayName("Bloquer une église déjà bannie → le remède est la réintégration, pas le blocage")
    void blockRefusesBannedTenant() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenantWithStatus(TenantStatus.CANCELLED)));

        DomainException failure = assertThrows(DomainException.class,
                () -> service.block(tenantId, "Encore", actorId));

        assertEquals("TENANT_BANNED", failure.getCode());
    }

    @Test
    @DisplayName("Réintégration : motif tracé à gravité INFO — c'est une remise en route, pas une sanction")
    void unbanRecordsAnInformationalTrace() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenantWithStatus(TenantStatus.CANCELLED)));
        when(tenantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Tenant restored = service.unban(tenantId, "Dossier régularisé", actorId);

        assertEquals(TenantStatus.ACTIVE, restored.getStatus());
        ArgumentCaptor<TenantWarning> warning = ArgumentCaptor.forClass(TenantWarning.class);
        verify(warningRepository).save(warning.capture());
        assertEquals(TenantWarning.Severity.INFO, warning.getValue().getSeverity());
    }
}
