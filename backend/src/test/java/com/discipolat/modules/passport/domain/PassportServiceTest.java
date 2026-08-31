package com.discipolat.modules.passport.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityTestHelper;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PassportService — Passeport spirituel portable et vérifiable")
class PassportServiceTest {

    @Mock private SpiritualPassportRepository passportRepository;
    @Mock private SpiritualPassportEntryRepository entryRepository;
    @Mock private PassportVerificationRepository verificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private SecurityUtils securityUtils;

    private PassportSignatureService signatureService;
    private PassportService service;

    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() throws Exception {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        SecurityTestHelper.loginAs(userId, "MEMBRE");

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        String privB64 = Base64.getEncoder().encodeToString(toPem("PRIVATE KEY", keyPair.getPrivate().getEncoded()).getBytes(StandardCharsets.UTF_8));
        String pubB64 = Base64.getEncoder().encodeToString(toPem("PUBLIC KEY", keyPair.getPublic().getEncoded()).getBytes(StandardCharsets.UTF_8));
        signatureService = new PassportSignatureService(privB64, pubB64, "", "");

        service = new PassportService(passportRepository, entryRepository, verificationRepository,
                signatureService, userRepository, tenantRepository, securityUtils);
    }

    @AfterEach
    void tearDown() {
        SecurityTestHelper.logout();
        TenantContext.clear();
    }

    private static String toPem(String type, byte[] encoded) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(encoded);
        return "-----BEGIN " + type + "-----\n" + body + "\n-----END " + type + "-----\n";
    }

    private User member() {
        User u = new User();
        u.setId(userId);
        u.setFirstName("Jean");
        u.setLastName("Mbarga");
        return u;
    }

    private void stubSaveWithId() {
        when(passportRepository.save(any(SpiritualPassport.class))).thenAnswer(inv -> {
            SpiritualPassport p = inv.getArgument(0);
            if (p.getId() == null) p.setId(UUID.randomUUID());
            return p;
        });
    }

    private SpiritualPassport activePassport() {
        SpiritualPassport p = new SpiritualPassport();
        p.setId(UUID.randomUUID());
        p.setTenantId(tenantId);
        p.setMemberId(userId);
        p.setPassportCode("DP-AAAA-BBBB-CCCC");
        p.setStatus("ACTIVE");
        return p;
    }

    // ======================== ÉMISSION ========================

    @Test
    @DisplayName("issuePassport — émet un passeport signé pour le tenant courant")
    void issuePassport() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(member()));
        when(passportRepository.findByTenantIdAndMemberId(tenantId, userId)).thenReturn(Optional.empty());
        stubSaveWithId();
        when(entryRepository.findByPassportIdOrderByCreatedAtAsc(any())).thenReturn(List.of());

        SpiritualPassport result = service.issuePassport(userId);

        assertEquals(tenantId, result.getTenantId());
        assertEquals(userId, result.getMemberId());
        assertTrue(result.getPassportCode().startsWith("DP-"));
        assertNotNull(result.getPayloadHash());
        assertNotNull(result.getSignature());
    }

    @Test
    @DisplayName("issuePassport — idempotent : un membre a un seul passeport")
    void issuePassport_idempotent() {
        SpiritualPassport existing = activePassport();
        when(userRepository.findById(userId)).thenReturn(Optional.of(member()));
        when(passportRepository.findByTenantIdAndMemberId(tenantId, userId)).thenReturn(Optional.of(existing));
        stubSaveWithId();
        when(entryRepository.findByPassportIdOrderByCreatedAtAsc(any())).thenReturn(List.of());

        SpiritualPassport result = service.issuePassport(userId);

        assertEquals(existing.getId(), result.getId());
        verify(passportRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("issuePassport — membre inexistant refusé")
    void issuePassport_unknownMember() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> service.issuePassport(userId));
    }

    // ======================== ENTRÉES ========================

    @Test
    @DisplayName("addEntry — ajoute une entrée et re-signe le passeport")
    void addEntry() {
        SpiritualPassport passport = activePassport();
        when(passportRepository.findByIdAndTenantId(passport.getId(), tenantId)).thenReturn(Optional.of(passport));
        when(entryRepository.findByPassportIdOrderByCreatedAtAsc(passport.getId())).thenReturn(List.of());
        stubSaveWithId();
        when(entryRepository.save(any(SpiritualPassportEntry.class))).thenAnswer(inv -> {
            SpiritualPassportEntry e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        SpiritualPassportEntry entry = new SpiritualPassportEntry();
        entry.setEntryType("BAPTISM");
        entry.setTitle("Baptême au fleuve");
        entry.setOccurredAt(LocalDate.of(2025, 6, 15));

        SpiritualPassportEntry saved = service.addEntry(passport.getId(), entry);

        assertEquals(tenantId, saved.getTenantId());
        assertEquals(passport.getId(), saved.getPassportId());
        assertEquals(userId, saved.getCreatedByUserId());
        assertNotNull(passport.getSignature());
        verify(passportRepository, atLeastOnce()).save(any(SpiritualPassport.class));
    }

    @Test
    @DisplayName("addEntry — refusé sur un passeport révoqué")
    void addEntry_revoked() {
        SpiritualPassport passport = activePassport();
        passport.setStatus("REVOKED");
        when(passportRepository.findByIdAndTenantId(passport.getId(), tenantId)).thenReturn(Optional.of(passport));

        assertThrows(IllegalStateException.class, () -> service.addEntry(passport.getId(), new SpiritualPassportEntry()));
    }

    // ======================== RÉVOCATION ========================

    @Test
    @DisplayName("revokePassport — révoque et re-signe")
    void revokePassport() {
        SpiritualPassport passport = activePassport();
        when(passportRepository.findByIdAndTenantId(passport.getId(), tenantId)).thenReturn(Optional.of(passport));
        when(entryRepository.findByPassportIdOrderByCreatedAtAsc(passport.getId())).thenReturn(List.of());
        stubSaveWithId();

        SpiritualPassport result = service.revokePassport(passport.getId(), "Sortie de l'église");

        assertEquals("REVOKED", result.getStatus());
        assertNotNull(result.getRevokedAt());
        assertEquals("Sortie de l'église", result.getRevokedReason());
    }

    // ======================== VÉRIFICATION PUBLIQUE ========================

    @Test
    @DisplayName("verifyByCode — VALID pour un passeport actif et correctement signé")
    void verifyByCode_valid() {
        SpiritualPassport passport = activePassport();
        when(entryRepository.findByPassportIdOrderByCreatedAtAsc(passport.getId())).thenReturn(List.of());
        stubSaveWithId();
        service.resign(passport);

        when(passportRepository.findByPassportCode(passport.getPassportCode())).thenReturn(Optional.of(passport));
        when(userRepository.findById(userId)).thenReturn(Optional.of(member()));
        Tenant tenant = new Tenant();
        tenant.setName("Église Bethel");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        PassportService.VerificationResult result =
                service.verifyByCode(passport.getPassportCode(), "127.0.0.1", "TestAgent");

        assertEquals("VALID", result.status());
        assertTrue(result.signatureValid());
        assertEquals("Jean Mbarga", result.holderName());
        assertEquals("Église Bethel", result.issuedBy());
        verify(verificationRepository).save(any(PassportVerification.class));
    }

    @Test
    @DisplayName("verifyByCode — REVOKED pour un passeport révoqué")
    void verifyByCode_revoked() {
        SpiritualPassport passport = activePassport();
        passport.setStatus("REVOKED");
        when(entryRepository.findByPassportIdOrderByCreatedAtAsc(passport.getId())).thenReturn(List.of());
        stubSaveWithId();
        service.resign(passport);
        when(passportRepository.findByPassportCode(passport.getPassportCode())).thenReturn(Optional.of(passport));

        PassportService.VerificationResult result =
                service.verifyByCode(passport.getPassportCode(), "127.0.0.1", null);

        assertEquals("REVOKED", result.status());
        verify(verificationRepository).save(any(PassportVerification.class));
    }

    @Test
    @DisplayName("verifyByCode — EXPIRED après la date d'expiration")
    void verifyByCode_expired() {
        SpiritualPassport passport = activePassport();
        passport.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(entryRepository.findByPassportIdOrderByCreatedAtAsc(passport.getId())).thenReturn(List.of());
        stubSaveWithId();
        service.resign(passport);
        when(passportRepository.findByPassportCode(passport.getPassportCode())).thenReturn(Optional.of(passport));

        PassportService.VerificationResult result =
                service.verifyByCode(passport.getPassportCode(), null, null);

        assertEquals("EXPIRED", result.status());
    }

    @Test
    @DisplayName("verifyByCode — INVALID si le contenu a été falsifié")
    void verifyByCode_tampered() {
        SpiritualPassport passport = activePassport();
        when(entryRepository.findByPassportIdOrderByCreatedAtAsc(passport.getId())).thenReturn(List.of());
        stubSaveWithId();
        service.resign(passport);
        passport.setMemberId(UUID.randomUUID());
        when(passportRepository.findByPassportCode(passport.getPassportCode())).thenReturn(Optional.of(passport));

        PassportService.VerificationResult result =
                service.verifyByCode(passport.getPassportCode(), null, null);

        assertEquals("INVALID", result.status());
        assertFalse(result.signatureValid());
    }

    @Test
    @DisplayName("verifyByCode — NOT_FOUND pour un code inconnu")
    void verifyByCode_notFound() {
        when(passportRepository.findByPassportCode("DP-0000-0000-0000")).thenReturn(Optional.empty());

        PassportService.VerificationResult result =
                service.verifyByCode("DP-0000-0000-0000", null, null);

        assertEquals("NOT_FOUND", result.status());
        verify(verificationRepository).save(any(PassportVerification.class));
    }

    // ======================== CONTRÔLE D'ACCÈS ========================

    @Test
    @DisplayName("getPassportForCurrentUser — le titulaire accède à son passeport")
    void access_ownPassport() {
        SpiritualPassport passport = activePassport();
        when(passportRepository.findByIdAndTenantId(passport.getId(), tenantId)).thenReturn(Optional.of(passport));

        SpiritualPassport result = service.getPassportForCurrentUser(passport.getId());
        assertEquals(passport.getId(), result.getId());
    }

    @Test
    @DisplayName("getPassportForCurrentUser — un MEMBRE ne peut pas lire le passeport d'un autre")
    void access_otherMember_denied() {
        SpiritualPassport passport = activePassport();
        passport.setMemberId(UUID.randomUUID());
        when(passportRepository.findByIdAndTenantId(passport.getId(), tenantId)).thenReturn(Optional.of(passport));
        when(securityUtils.hasActiveRole("ADMIN", "PASTEUR", "RESPONSABLE")).thenReturn(false);

        assertThrows(SecurityException.class, () -> service.getPassportForCurrentUser(passport.getId()));
    }

    @Test
    @DisplayName("getPassportForCurrentUser — un responsable peut lire le passeport d'un membre")
    void access_responsable_allowed() {
        SpiritualPassport passport = activePassport();
        passport.setMemberId(UUID.randomUUID());
        when(passportRepository.findByIdAndTenantId(passport.getId(), tenantId)).thenReturn(Optional.of(passport));
        when(securityUtils.hasActiveRole("ADMIN", "PASTEUR", "RESPONSABLE")).thenReturn(true);

        SpiritualPassport result = service.getPassportForCurrentUser(passport.getId());
        assertEquals(passport.getId(), result.getId());
    }

    @Test
    @DisplayName("getPassportForCurrentUser — passeport d'un autre tenant introuvable")
    void access_crossTenant_isolated() {
        when(passportRepository.findByIdAndTenantId(any(), eq(tenantId))).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> service.getPassportForCurrentUser(UUID.randomUUID()));
    }
}
