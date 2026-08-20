package com.discipolat.modules.gdpr.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class GdprService {

    private static final Logger log = LoggerFactory.getLogger(GdprService.class);

    private final GdprRequestRepository gdprRequestRepository;
    private final UserRepository userRepository;
    private final SoulRepository soulRepository;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;

    public GdprService(GdprRequestRepository gdprRequestRepository,
                       UserRepository userRepository,
                       SoulRepository soulRepository,
                       SecurityUtils securityUtils,
                       ObjectMapper objectMapper) {
        this.gdprRequestRepository = gdprRequestRepository;
        this.userRepository = userRepository;
        this.soulRepository = soulRepository;
        this.securityUtils = securityUtils;
        this.objectMapper = objectMapper;
    }

    public GdprRequest requestDataExport(UUID userId) {
        UUID tenantId = TenantContext.requireTenantId();

        GdprRequest request = GdprRequest.builder()
                .tenantId(tenantId)
                .requesterUserId(userId)
                .requestType(GdprRequestType.DATA_EXPORT)
                .status(GdprRequestStatus.COMPLETED)
                .processedBy(securityUtils.getCurrentUserId())
                .processedAt(LocalDateTime.now())
                .build();

        Map<String, Object> exportPayload = new LinkedHashMap<>();
        exportPayload.put("exportDate", LocalDateTime.now().toString());
        exportPayload.put("userId", userId.toString());

        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent()) {
            User u = user.get();
            Map<String, Object> userData = new LinkedHashMap<>();
            userData.put("id", u.getId().toString());
            userData.put("email", u.getEmail());
            userData.put("firstName", u.getFirstName());
            userData.put("lastName", u.getLastName());
            userData.put("phone", u.getPhone());
            userData.put("role", u.getRole() != null ? u.getRole().name() : null);
            userData.put("dateNaissance", u.getDateNaissance() != null ? u.getDateNaissance().toString() : null);
            userData.put("situationFamiliale", u.getSituationFamiliale());
            userData.put("photoUrl", u.getPhotoUrl());
            userData.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);
            exportPayload.put("user", userData);
        }

        List<Soul> souls = soulRepository.findAllByUserId(userId);
        if (!souls.isEmpty()) {
            List<Map<String, Object>> soulsData = new ArrayList<>();
            for (Soul s : souls) {
                Map<String, Object> soulData = new LinkedHashMap<>();
                soulData.put("id", s.getId().toString());
                soulData.put("nom", s.getNom());
                soulData.put("prenom", s.getPrenom());
                soulData.put("email", s.getEmail());
                soulData.put("telephone", s.getTelephone());
                soulData.put("adresse", s.getAdresse());
                soulData.put("dateNaissance", s.getDateNaissance() != null ? s.getDateNaissance().toString() : null);
                soulData.put("profession", s.getProfession());
                soulData.put("situationFamiliale", s.getSituationFamiliale());
                soulData.put("typeDisciple", s.getTypeDisciple() != null ? s.getTypeDisciple().name() : null);
                soulData.put("statut", s.getStatut() != null ? s.getStatut().name() : null);
                soulData.put("etatSpirituel", s.getEtatSpirituel());
                soulData.put("createdAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
                soulsData.add(soulData);
            }
            exportPayload.put("souls", soulsData);
        }

        try {
            request.setExportData(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportPayload));
        } catch (Exception e) {
            log.error("Failed to serialize GDPR export data", e);
            request.setExportData("{\"error\": \"Failed to serialize data\"}");
        }

        return gdprRequestRepository.save(request);
    }

    public GdprRequest requestDataDeletion(UUID userId) {
        UUID tenantId = TenantContext.requireTenantId();

        GdprRequest request = GdprRequest.builder()
                .tenantId(tenantId)
                .requesterUserId(userId)
                .requestType(GdprRequestType.DATA_DELETION)
                .status(GdprRequestStatus.PENDING)
                .build();

        GdprRequest savedRequest = gdprRequestRepository.save(request);

        // Anonymize user data
        userRepository.findById(userId).ifPresent(user -> {
            String pseudonymizedEmail = "deleted_" + userId.toString().substring(0, 8) + "@anonymized.local";
            user.setEmail(pseudonymizedEmail);
            user.setFirstName("Utilisateur supprimé");
            user.setLastName("Utilisateur supprimé");
            user.setPhone(null);
            user.setPasswordHash("DELETED");
            user.setPhotoUrl(null);
            user.setDateNaissance(null);
            user.setSituationFamiliale(null);
            user.setTwoFactorEnabled(false);
            user.setTwoFactorSecret(null);
            user.setTwoFactorBackupCodes(null);
            user.setDeleted(true);
            userRepository.save(user);
        });

        // Anonymize soul data
        List<Soul> souls = soulRepository.findAllByUserId(userId);
        for (Soul soul : souls) {
            soul.setNom("Utilisateur supprimé");
            soul.setPrenom("Utilisateur supprimé");
            soul.setEmail(null);
            soul.setTelephone(null);
            soul.setAdresse(null);
            soul.setDateNaissance(null);
            soul.setProfession(null);
            soul.setNiveauEtude(null);
            soul.setNbEnfants(null);
            soul.setSituationFamiliale(null);
            soul.setNotesPasteur(null);
            soul.setLatitude(null);
            soul.setLongitude(null);
            soul.setDeleted(true);
            soulRepository.save(soul);
        }

        return savedRequest;
    }

    @Transactional(readOnly = true)
    public List<GdprRequest> getRequestsForTenant() {
        UUID tenantId = TenantContext.requireTenantId();
        return gdprRequestRepository.findByTenantIdOrderByRequestedAtDesc(tenantId);
    }

    @Transactional(readOnly = true)
    public List<GdprRequest> getRequestsByUser(UUID userId) {
        return gdprRequestRepository.findByRequesterUserIdOrderByRequestedAtDesc(userId);
    }

    public GdprRequest processDataRequest(UUID id, UUID processorId, String notes) {
        GdprRequest request = gdprRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("GdprRequest", id));

        if (request.getStatus() != GdprRequestStatus.PENDING) {
            throw new IllegalStateException("Request is not in PENDING status");
        }

        request.setStatus(GdprRequestStatus.PROCESSING);
        request.setProcessedBy(processorId);
        request.setProcessedAt(LocalDateTime.now());
        request.setNotes(notes);
        gdprRequestRepository.save(request);

        switch (request.getRequestType()) {
            case DATA_EXPORT -> {
                requestDataExport(request.getRequesterUserId());
                return gdprRequestRepository.findById(id).orElse(request);
            }
            case DATA_DELETION -> {
                requestDataDeletion(request.getRequesterUserId());
                return gdprRequestRepository.findById(id).orElse(request);
            }
            case DATA_PORTABILITY -> {
                requestDataExport(request.getRequesterUserId());
                return gdprRequestRepository.findById(id).orElse(request);
            }
            default -> {
                request.setStatus(GdprRequestStatus.COMPLETED);
                request.setProcessedAt(LocalDateTime.now());
                request.setNotes(notes);
                return gdprRequestRepository.save(request);
            }
        }
    }

    public GdprRequest rejectRequest(UUID id, UUID processorId, String notes) {
        GdprRequest request = gdprRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("GdprRequest", id));

        request.setStatus(GdprRequestStatus.REJECTED);
        request.setProcessedBy(processorId);
        request.setProcessedAt(LocalDateTime.now());
        request.setNotes(notes);
        return gdprRequestRepository.save(request);
    }
}
