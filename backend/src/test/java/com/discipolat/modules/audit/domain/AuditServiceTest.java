package com.discipolat.modules.audit.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private UserRepository userRepository;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditLogRepository, securityUtils, userRepository);
    }

    @Test
    void findFiltered_ShouldDelegateWithAllCriteria() {
        UUID userId = UUID.randomUUID();
        LocalDateTime debut = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 8, 7, 23, 59);
        Pageable pageable = PageRequest.of(0, 20);

        when(auditLogRepository.findFiltered(userId, "SOUL", debut, fin, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        auditService.findFiltered(userId, "SOUL", debut, fin, pageable);

        verify(auditLogRepository).findFiltered(userId, "SOUL", debut, fin, pageable);
    }

    @Test
    void findFiltered_WithNulls_ShouldPassNullCriteria() {
        Pageable pageable = PageRequest.of(0, 20);

        when(auditLogRepository.findFiltered(isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        auditService.findFiltered(null, null, null, null, pageable);

        verify(auditLogRepository).findFiltered(null, null, null, null, pageable);
    }

    @Test
    void exportCsv_ShouldBuildHeaderAndRowsWithEmails() {
        UUID userId = UUID.randomUUID();
        AuditLog log = AuditLog.builder()
                .id(UUID.randomUUID())
                .utilisateurId(userId)
                .action("CREER_SOUL")
                .entiteType("SOUL")
                .ancienValeur(Map.of("statut", "NOUVEAU"))
                .nouvelleValeur(Map.of("statut", "ACTIF"))
                .adresseIp("127.0.0.1")
                .userAgent("test-agent")
                .build();
        log.setCreatedAt(LocalDateTime.of(2026, 8, 5, 10, 30));

        when(auditLogRepository.findFiltered(eq(userId), isNull(), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(log)));
        when(userRepository.findAllById(Set.of(userId)))
                .thenReturn(List.of(User.builder().id(userId).email("pasteur@discipolat.com").build()));

        byte[] csv = auditService.exportCsv(userId, null, null, null);
        String content = new String(csv, StandardCharsets.UTF_8);

        assertTrue(content.startsWith("\uFEFF"));
        assertTrue(content.contains("Date;Utilisateur;Action;Entité"));
        assertTrue(content.contains("pasteur@discipolat.com"));
        assertTrue(content.contains("CREER_SOUL"));
        assertTrue(content.contains("ancien={statut=NOUVEAU}"));
        assertTrue(content.contains("nouveau={statut=ACTIF}"));
        assertTrue(content.contains("127.0.0.1"));
    }

    @Test
    void exportCsv_WithoutUsers_ShouldFallbackToUuid() {
        UUID userId = UUID.randomUUID();
        AuditLog log = AuditLog.builder()
                .id(UUID.randomUUID())
                .utilisateurId(userId)
                .action("LOGIN")
                .entiteType("USER")
                .build();
        log.setCreatedAt(LocalDateTime.of(2026, 8, 5, 10, 30));

        when(auditLogRepository.findFiltered(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(log)));
        when(userRepository.findAllById(any())).thenReturn(List.of());

        byte[] csv = auditService.exportCsv(null, null, null, null);
        String content = new String(csv, StandardCharsets.UTF_8);

        assertTrue(content.contains(userId.toString()));
        assertTrue(content.contains("LOGIN"));
    }

    @Test
    void exportCsv_WhenEmpty_ShouldReturnHeaderOnly() {
        when(auditLogRepository.findFiltered(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        byte[] csv = auditService.exportCsv(null, null, null, null);
        String content = new String(csv, StandardCharsets.UTF_8);

        assertTrue(content.startsWith("\uFEFF"));
        assertTrue(content.contains("Date;Utilisateur;Action;Entité"));
        assertTrue(content.endsWith("\n"));
    }
}
