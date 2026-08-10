package com.discipolat.modules.audit.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.discipolat.common.domain.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository, SecurityUtils securityUtils,
                        UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.securityUtils = securityUtils;
        this.userRepository = userRepository;
    }

    public AuditLog findById(UUID id) {
        return auditLogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AuditLog", id));
    }

    public void log(String action, String entiteType, UUID entiteId,
                    Map<String, Object> ancienValeur, Map<String, Object> nouvelleValeur,
                    HttpServletRequest request) {
        AuditLog log = AuditLog.builder()
                .action(action)
                .entiteType(entiteType)
                .entiteId(entiteId)
                .ancienValeur(ancienValeur)
                .nouvelleValeur(nouvelleValeur)
                .adresseIp(request != null ? request.getRemoteAddr() : null)
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .build();

        try {
            log.setUtilisateurId(securityUtils.getCurrentUserId());
        } catch (Exception e) {
            // System operations
        }

        auditLogRepository.save(log);
    }

    public void logSimple(String action, String entiteType, UUID entiteId) {
        AuditLog log = AuditLog.builder()
                .action(action)
                .entiteType(entiteType)
                .entiteId(entiteId)
                .build();

        try {
            log.setUtilisateurId(securityUtils.getCurrentUserId());
        } catch (Exception e) {
            // System operations
        }

        auditLogRepository.save(log);
    }

    /**
     * Recherche combinée : utilisateur, type d'entité et plage de dates.
     * Chaque critère est optionnel (null = pas de filtre).
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> findFiltered(UUID utilisateurId, String entiteType,
                                       LocalDateTime debut, LocalDateTime fin, Pageable pageable) {
        return auditLogRepository.findFiltered(utilisateurId, entiteType, debut, fin, pageable);
    }

    /**
     * Export CSV de l'ensemble des journaux correspondant aux filtres
     * (parcours paginé de toutes les pages, plafonné à 50 000 lignes).
     */
    @Transactional(readOnly = true)
    public byte[] exportCsv(UUID utilisateurId, String entiteType,
                            LocalDateTime debut, LocalDateTime fin) {
        List<AuditLog> all = new ArrayList<>();
        int pageNumber = 0;
        Page<AuditLog> page;
        do {
            page = auditLogRepository.findFiltered(utilisateurId, entiteType, debut, fin,
                    PageRequest.of(pageNumber, 1000, Sort.by(Sort.Direction.DESC, "createdAt")));
            all.addAll(page.getContent());
            pageNumber++;
        } while (page.hasNext() && all.size() < 50_000);

        Map<UUID, String> emails = resolveEmails(all);

        StringBuilder csv = new StringBuilder();
        csv.append("\uFEFF"); // BOM UTF-8 pour Excel
        csv.append("Date;Utilisateur;Action;Entité;Entité ID;Détails;Adresse IP;User-Agent\n");
        for (AuditLog log : all) {
            csv.append(csvField(log.getCreatedAt() != null ? log.getCreatedAt().toString() : "")).append(';')
               .append(csvField(log.getUtilisateurId() != null
                       ? emails.getOrDefault(log.getUtilisateurId(), log.getUtilisateurId().toString())
                       : "")).append(';')
               .append(csvField(log.getAction())).append(';')
               .append(csvField(log.getEntiteType())).append(';')
               .append(csvField(log.getEntiteId() != null ? log.getEntiteId().toString() : "")).append(';')
               .append(csvField(details(log))).append(';')
               .append(csvField(log.getAdresseIp())).append(';')
               .append(csvField(log.getUserAgent())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Map<UUID, String> resolveEmails(List<AuditLog> logs) {
        Set<UUID> ids = logs.stream()
                .map(AuditLog::getUtilisateurId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getEmail));
    }

    private String details(AuditLog log) {
        StringBuilder sb = new StringBuilder();
        if (log.getAncienValeur() != null && !log.getAncienValeur().isEmpty()) {
            sb.append("ancien=").append(log.getAncienValeur());
        }
        if (log.getNouvelleValeur() != null && !log.getNouvelleValeur().isEmpty()) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append("nouveau=").append(log.getNouvelleValeur());
        }
        return sb.toString();
    }

    private String csvField(String value) {
        if (value == null) return "";
        // Neutralise les retours ligne (user-agent, détails) pour ne pas casser la structure CSV.
        String cleaned = value.replace("\r", " ").replace("\n", " ");
        return "\"" + cleaned.replace("\"", "\"\"") + "\"";
    }
}
