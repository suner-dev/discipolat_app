package com.discipolat.modules.encouragements.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.encouragements.domain.Encouragement;
import com.discipolat.modules.encouragements.domain.EncouragementRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * P3 #115 — Encouragements entre membres de famille/équipe.
 */
@RestController
@RequestMapping("/api/v1/encouragements")
@PreAuthorize("isAuthenticated()")
public class EncouragementController {

    private final EncouragementRepository repository;
    private final jakarta.persistence.EntityManager em;

    public EncouragementController(EncouragementRepository repository,
                                   jakarta.persistence.EntityManager em) {
        this.repository = repository;
        this.em = em;
    }

    @PostMapping
    public ResponseEntity<Encouragement> send(@RequestBody Map<String, String> body) {
        UUID fromUserId = SecurityUtils.getCurrentUserId();
        Encouragement e = new Encouragement();
        e.setTenantId(TenantContext.getCurrentTenantId());
        e.setFromUserId(fromUserId);
        e.setToUserId(UUID.fromString(body.get("toUserId")));
        if (body.get("kind") != null) {
            try { e.setKind(Encouragement.Kind.valueOf(body.get("kind"))); }
            catch (IllegalArgumentException ignored) { }
        }
        e.setMessage(body.get("message"));
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(e));
    }

    /** Encouragements reçus par l'utilisateur courant. */
    @GetMapping("/received")
    public ResponseEntity<List<Encouragement>> received() {
        return ResponseEntity.ok(repository.findByToUserIdOrderByCreatedAtDesc(SecurityUtils.getCurrentUserId()));
    }

    /** Encouragements envoyés par l'utilisateur courant. */
    @GetMapping("/sent")
    public ResponseEntity<List<Encouragement>> sent() {
        return ResponseEntity.ok(repository.findByFromUserIdOrderByCreatedAtDesc(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/count/{userId}")
    public ResponseEntity<Long> countFor(@PathVariable UUID userId) {
        return ResponseEntity.ok(repository.countByToUserId(userId));
    }

    // ======================== P3 #115 — MON ÉQUIPE / MA FAMILLE ========================

    /** Membres de la famille spirituelle de l'utilisateur courant + encouragements reçus. */
    @GetMapping("/my-team")
    public ResponseEntity<List<Map<String, Object>>> myTeam() {
        UUID me = SecurityUtils.getCurrentUserId();
        List<?> famRows = em.createNativeQuery(
                        "SELECT famille_id FROM souls WHERE user_id = :me AND deleted = false AND famille_id IS NOT NULL LIMIT 1")
                .setParameter("me", me)
                .getResultList();
        if (famRows.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        UUID familleId = (UUID) famRows.get(0);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT s.id, s.user_id, s.nom, s.prenom, s.etat_spirituel, s.date_dernier_contact,
                       f.nom AS famille_nom
                FROM souls s
                LEFT JOIN families f ON f.id = s.famille_id
                WHERE s.famille_id = :fid AND s.deleted = false
                ORDER BY s.nom
                """)
                .setParameter("fid", familleId)
                .getResultList();

        List<Map<String, Object>> team = new java.util.ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("soulId", row[0]);
            m.put("userId", row[1]);
            m.put("nom", row[2]);
            m.put("prenom", row[3]);
            m.put("etatSpirituel", row[4]);
            m.put("dernierContact", row[5]);
            m.put("familleNom", row[6]);
            long encouragements = row[1] == null ? 0 : repository.countByToUserId((UUID) row[1]);
            m.put("encouragementsRecus", encouragements);
            m.put("estMoi", me.equals(row[1]));
            team.add(m);
        }
        return ResponseEntity.ok(team);
    }
}
