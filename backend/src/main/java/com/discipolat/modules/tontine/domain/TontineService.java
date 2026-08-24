package com.discipolat.modules.tontine.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Tontine Numérique — groupes de contribution type tontine.
 *
 * - Création de groupes avec montant par tour et périodicité
 * - Échéancier généré automatiquement pour chaque tour
 * - Suivi des versements et passage de tour
 * - Couplage possible aux objectifs de générosité du module Finances
 */
@Service
@Transactional
public class TontineService {

    private static final Logger log = LoggerFactory.getLogger(TontineService.class);

    private final TontineGroupRepository groupRepository;
    private final TontineMemberRepository memberRepository;
    private final TontineContributionRepository contributionRepository;
    private final EntityPropagationPublisher propagationPublisher;
    private final SecurityUtils securityUtils;
    private final com.discipolat.modules.notifications.domain.NotificationService notificationService;
    private final jakarta.persistence.EntityManager em;

    public TontineService(TontineGroupRepository groupRepository,
                          TontineMemberRepository memberRepository,
                          TontineContributionRepository contributionRepository,
                          EntityPropagationPublisher propagationPublisher,
                          SecurityUtils securityUtils,
                          com.discipolat.modules.notifications.domain.NotificationService notificationService,
                          jakarta.persistence.EntityManager em) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.contributionRepository = contributionRepository;
        this.propagationPublisher = propagationPublisher;
        this.securityUtils = securityUtils;
        this.notificationService = notificationService;
        this.em = em;
    }

    public TontineGroup createGroup(TontineGroup group) {
        group.setTenantId(securityUtils.getCurrentTenantId());
        if (group.getCreatedBy() == null) {
            group.setCreatedBy(securityUtils.getCurrentUserId());
        }
        TontineGroup saved = groupRepository.save(group);
        propagationPublisher.publishCreated("TONTINE_GROUP", saved.getId(),
                Map.of("name", saved.getName()), "Tontine créée: " + saved.getName());
        return saved;
    }

    public TontineMember addMember(UUID groupId, TontineMember member) {
        TontineGroup group = findGroup(groupId);
        member.setGroupId(groupId);
        member.setTenantId(group.getTenantId());
        long existing = memberRepository.countByGroupId(groupId);
        if (member.getOrdrePassage() <= 0) {
            member.setOrdrePassage((int) existing + 1);
        }
        TontineMember saved = memberRepository.save(member);
        // Génère l'échéancier du tour courant pour ce membre
        contributionRepository.save(TontineContribution.builder()
                .groupId(groupId)
                .memberId(saved.getId())
                .tour(group.getTourActuel())
                .montant(group.getMontantParTour())
                .build());
        propagationPublisher.publishCreated("TONTINE_MEMBER", saved.getId(),
                Map.of("groupId", groupId.toString(), "nom", saved.getNom()),
                "Membre ajouté à la tontine: " + saved.getNom());
        return saved;
    }

    /** Enregistre un versement pour le tour courant. */
    public TontineContribution markPaid(UUID groupId, UUID memberId, int tour, String note) {
        TontineGroup group = findGroup(groupId);
        assertManager();
        TontineContribution contribution = contributionRepository
                .findByGroupIdAndMemberIdAndTour(groupId, memberId, tour)
                .orElseGet(() -> {
                    TontineMember member = memberRepository.findById(memberId)
                            .orElseThrow(() -> new EntityNotFoundException("TontineMember", memberId));
                    return TontineContribution.builder()
                            .groupId(groupId)
                            .memberId(memberId)
                            .tour(tour)
                            .montant(group.getMontantParTour())
                            .build();
                });
        contribution.setPaye(true);
        contribution.setDatePaiement(LocalDateTime.now());
        contribution.setNote(note);
        TontineContribution saved = contributionRepository.save(contribution);
        propagationPublisher.publishUpdated("TONTINE_CONTRIBUTION", saved.getId(),
                Map.of("paye", false), Map.of("paye", true),
                "Versement enregistré (tontine " + group.getName() + ", tour " + tour + ")");
        return saved;
    }

    /** Passe au tour suivant : bénéficiaire = prochain membre n'ayant pas reçu son tour. */
    public Map<String, Object> nextRound(UUID groupId) {
        TontineGroup group = findGroup(groupId);
        assertManager();
        List<TontineMember> members = memberRepository.findByGroupIdOrderByOrdrePassageAsc(groupId);
        Optional<TontineMember> beneficiary = members.stream()
                .filter(m -> !m.isARecuTour())
                .findFirst();

        if (beneficiary.isPresent()) {
            TontineMember b = beneficiary.get();
            b.setARecuTour(true);
            memberRepository.save(b);
        }

        boolean cycleComplete = memberRepository.findByGroupIdOrderByOrdrePassageAsc(groupId).stream()
                .allMatch(TontineMember::isARecuTour);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("beneficiary", beneficiary.map(TontineMember::getNom).orElse(null));
        result.put("complete", cycleComplete);
        result.put("newRound", group.getTourActuel());

        if (!cycleComplete && beneficiary.isPresent()) {
            int newRound = group.getTourActuel() + 1;
            // Génère l'échéancier du nouveau tour pour chaque membre
            for (TontineMember m : members) {
                if (contributionRepository.findByGroupIdAndMemberIdAndTour(groupId, m.getId(), newRound).isEmpty()) {
                    contributionRepository.save(TontineContribution.builder()
                            .groupId(groupId).memberId(m.getId()).tour(newRound)
                            .montant(group.getMontantParTour()).build());
                }
            }
            group.setTourActuel(newRound);
            groupRepository.save(group);
            propagationPublisher.publishStatusChanged("TONTINE_GROUP", groupId,
                    String.valueOf(newRound - 1), String.valueOf(newRound),
                    "Nouveau tour de tontine: " + newRound + " — bénéficiaire: "
                            + beneficiary.map(TontineMember::getNom).orElse("?"));
            result.put("newRound", newRound);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<TontineGroup> listGroups() {
        return groupRepository.findByStatutOrderByCreatedAtDesc(TontineGroup.Statut.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> groupDetail(UUID groupId) {
        TontineGroup group = findGroup(groupId);
        List<TontineMember> members = memberRepository.findByGroupIdOrderByOrdrePassageAsc(groupId);
        List<TontineContribution> schedule =
                contributionRepository.findByGroupIdAndTourOrderByMemberIdAsc(groupId, group.getTourActuel());

        Map<UUID, TontineContribution> byMember = new HashMap<>();
        schedule.forEach(c -> byMember.put(c.getMemberId(), c));

        BigDecimal totalCollecte = groupRepository.totalCollecte(groupId);
        BigDecimal totalAttendu = group.getMontantParTour().multiply(BigDecimal.valueOf(Math.max(1, members.size())));

        List<Map<String, Object>> memberViews = new ArrayList<>();
        for (TontineMember m : members) {
            TontineContribution c = byMember.get(m.getId());
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("id", m.getId());
            view.put("nom", m.getNom());
            view.put("ordrePassage", m.getOrdrePassage());
            view.put("aRecuTour", m.isARecuTour());
            view.put("paye", c != null && c.isPaye());
            view.put("datePaiement", c == null ? null : c.getDatePaiement());
            memberViews.add(view);
        }

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("group", group);
        detail.put("members", memberViews);
        detail.put("totalCollecte", totalCollecte);
        detail.put("totalAttendu", totalAttendu);
        detail.put("progressPercent", totalAttendu.signum() > 0
                ? totalCollecte.multiply(BigDecimal.valueOf(100)).divide(totalAttendu, 2, java.math.RoundingMode.HALF_UP).min(BigDecimal.valueOf(100))
                : BigDecimal.ZERO);
        return detail;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("activeGroups", groupRepository.countByStatut(TontineGroup.Statut.ACTIVE));
        return stats;
    }

    /**
     * Dashboard tontine : santé du groupe, contributions, impayés, rotation.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard(UUID groupId) {
        TontineGroup group = findGroup(groupId);
        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("groupId", groupId);
        dashboard.put("groupName", group.getName());
        dashboard.put("statut", group.getStatut().name());
        dashboard.put("montantParTour", group.getMontantParTour());
        dashboard.put("periodicite", group.getPeriodicite().name());

        var members = memberRepository.findByGroupIdOrderByOrdrePassageAsc(groupId);
        dashboard.put("totalMembers", members.size());
        dashboard.put("activeMembers", members.size());
        dashboard.put("nextRecipient", members.stream()
                .filter(m -> !m.isARecuTour())
                .min(Comparator.comparingInt(TontineMember::getOrdrePassage))
                .map(TontineMember::getNom)
                .orElse("Aucun"));

        // Contributions
        var contributions = contributionRepository.findByGroupIdAndTourOrderByMemberIdAsc(groupId, group.getTourActuel());
        dashboard.put("totalContributions", contributions.size());
        long totalCollected = contributions.stream()
                .filter(TontineContribution::isPaye)
                .mapToLong(c -> c.getMontant().longValue())
                .sum();
        dashboard.put("totalCollected", totalCollected);
        dashboard.put("expectedPerPeriod", group.getMontantParTour().longValue() * members.size());

        // Impayés
        var unpaid = contributions.stream().filter(c -> !c.isPaye()).toList();
        dashboard.put("pendingPayments", unpaid.size());
        dashboard.put("overduePayments", 0);

        // Rotation
        dashboard.put("tourActuel", group.getTourActuel());
        long receivedCount = members.stream().filter(TontineMember::isARecuTour).count();
        dashboard.put("toursCompleted", receivedCount);
        dashboard.put("totalTours", members.size());

        return dashboard;
    }

    private TontineGroup findGroup(UUID id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TontineGroup", id));
    }

    /** Gestion : ADMIN, PASTEUR, RESPONSABLE ou créateur. */
    private void assertManager() {
        String role = securityUtils.getCurrentUserRole();
        boolean manager = "ADMIN".equals(role) || "PASTEUR".equals(role) || "RESPONSABLE".equals(role);
        if (!manager) {
            throw new AccessDeniedException("Seuls ADMIN, PASTEUR et RESPONSABLE gèrent les tontines");
        }
    }


    /**
     * P11 — Liste des impayés du tour courant (contributions non réglées).
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> overdue(UUID groupId) {
        UUID tenantId = com.discipolat.common.multitenancy.TenantContext.getTenantId();
        List<Object[]> rows = em.createNativeQuery("""
                SELECT m.nom, m.ordre_passage, c.tour, c.montant
                FROM tontine_contributions c
                JOIN tontine_members m ON m.id = c.member_id
                WHERE c.group_id = :g AND c.tenant_id = :t AND c.paye = false
                ORDER BY c.tour, m.ordre_passage
                """)
                .setParameter("g", groupId)
                .setParameter("t", tenantId)
                .getResultList();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object rowObj : rows) {
            Object[] row = (Object[]) rowObj;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("membre", row[0]);
            m.put("ordrePassage", ((Number) row[1]).intValue());
            m.put("tour", ((Number) row[2]).intValue());
            m.put("montantDu", row[3]);
            out.add(m);
        }
        return out;
    }

    /**
     * P11 — Notifie les membres ayant une contribution impayée au tour courant.
     * Réutilise NotificationService (canal IN_APP/PUSH selon préférences).
     */
    public int notifyDue(UUID groupId) {
        var group = groupRepository.findById(groupId).orElse(null);
        if (group == null) return 0;
        int sent = 0;
        for (var member : memberRepository.findByGroupIdOrderByOrdrePassageAsc(groupId)) {
            boolean paidForCurrentRound = contributionRepository.findByGroupIdAndTourOrderByMemberIdAsc(groupId, group.getTourActuel())
                    .stream().anyMatch(c -> c.getMemberId().equals(member.getId()) && c.isPaye());
            if (!paidForCurrentRound && member.getUserId() != null) {
                notificationService.create(member.getUserId(),
                        com.discipolat.common.enums.TypeNotification.RAPPEL,
                        com.discipolat.common.enums.CanalNotification.PUSH,
                        "Échéance tontine",
                        "Votre contribution pour le tour " + group.getTourActuel() + " de « "
                                + group.getName() + " » est attendue (" + group.getMontantParTour() + ").",
                        groupId, "TONTINE");
                sent++;
            }
        }
        return sent;
    }
}