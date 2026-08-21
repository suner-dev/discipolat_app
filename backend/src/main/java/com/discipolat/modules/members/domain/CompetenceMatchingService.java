package com.discipolat.modules.members.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Matching Membres ↔ Compétences pour Équipe.
 *
 * Chaque membre profile ses compétences/intérêts, et le système
 * propose des matches avec les besoins des équipes.
 */
@Service
@Transactional
public class CompetenceMatchingService {

    private static final Logger log = LoggerFactory.getLogger(CompetenceMatchingService.class);

    private final MemberCompetenceRepository competenceRepository;
    private final UserRepository userRepository;
    private final EntityPropagationPublisher propagationPublisher;
    private final SecurityUtils securityUtils;

    public CompetenceMatchingService(MemberCompetenceRepository competenceRepository,
                                     UserRepository userRepository,
                                     EntityPropagationPublisher propagationPublisher,
                                     SecurityUtils securityUtils) {
        this.competenceRepository = competenceRepository;
        this.userRepository = userRepository;
        this.propagationPublisher = propagationPublisher;
        this.securityUtils = securityUtils;
    }

    public MemberCompetence addCompetence(MemberCompetence competence) {
        competence.setUserId(securityUtils.getCurrentUserId());
        MemberCompetence saved = competenceRepository.save(competence);
        propagationPublisher.publishCreated("MEMBER_COMPETENCE", saved.getId(),
                Map.of("competenceName", saved.getCompetenceName(),
                        "category", saved.getCategory() != null ? saved.getCategory() : ""),
                "Compétence ajoutée: " + saved.getCompetenceName());
        return saved;
    }

    public MemberCompetence updateCompetence(UUID id, MemberCompetence updated) {
        MemberCompetence existing = competenceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MemberCompetence", id));
        existing.setCompetenceName(updated.getCompetenceName());
        existing.setCategory(updated.getCategory());
        existing.setLevel(updated.getLevel());
        existing.setInterestLevel(updated.getInterestLevel());
        existing.setNotes(updated.getNotes());
        return competenceRepository.save(existing);
    }

    public void deleteCompetence(UUID id) {
        MemberCompetence competence = competenceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MemberCompetence", id));
        competenceRepository.delete(competence);
    }

    @Transactional(readOnly = true)
    public List<MemberCompetence> getMyCompetences() {
        return competenceRepository.findByUserIdOrderByCompetenceNameAsc(
                securityUtils.getCurrentUserId());
    }

    @Transactional(readOnly = true)
    public List<MemberCompetence> getUserCompetences(UUID userId) {
        return competenceRepository.findByUserIdOrderByCompetenceNameAsc(userId);
    }

    /**
     * Trouve les membres qui ont une compétence spécifique, triés par niveau.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> findMembersWithCompetence(String competenceName) {
        List<MemberCompetence> matches = competenceRepository
                .findByCompetenceNameContainingIgnoreCaseOrderByLevelDesc(competenceName);

        return matches.stream().map(mc -> {
            Map<String, Object> result = new LinkedHashMap<>();
            Optional<User> user = userRepository.findById(mc.getUserId());
            result.put("userId", mc.getUserId());
            result.put("userName", user.map(u -> u.getFirstName() + " " + u.getLastName()).orElse("—"));
            result.put("competenceName", mc.getCompetenceName());
            result.put("level", mc.getLevel());
            result.put("interestLevel", mc.getInterestLevel());
            result.put("category", mc.getCategory());
            return result;
        }).toList();
    }

    /**
     * Matching : trouve les membres les plus adaptés pour un ensemble de compétences requises.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> findBestMatches(List<String> requiredCompetences, int minLevel) {
        List<MemberCompetence> all = competenceRepository.findAll();

        // Group by user
        Map<UUID, List<MemberCompetence>> byUser = all.stream()
                .collect(Collectors.groupingBy(MemberCompetence::getUserId));

        // Score each user
        return byUser.entrySet().stream()
                .map(entry -> {
                    UUID userId = entry.getKey();
                    List<MemberCompetence> userComps = entry.getValue();

                    double score = 0;
                    int matched = 0;
                    for (String required : requiredCompetences) {
                        Optional<MemberCompetence> match = userComps.stream()
                                .filter(mc -> mc.getCompetenceName().equalsIgnoreCase(required)
                                        && mc.getLevel() >= minLevel)
                                .findFirst();
                        if (match.isPresent()) {
                            score += match.get().getLevel() + match.get().getInterestLevel() * 0.5;
                            matched++;
                        }
                    }

                    Optional<User> user = userRepository.findById(userId);
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("userId", userId);
                    result.put("userName", user.map(u -> u.getFirstName() + " " + u.getLastName()).orElse("—"));
                    result.put("matchScore", Math.round(score * 100.0) / 100.0);
                    result.put("matchedCompetences", matched);
                    result.put("totalRequired", requiredCompetences.size());
                    result.put("matchPercentage", requiredCompetences.isEmpty() ? 0 :
                            Math.round((double) matched / requiredCompetences.size() * 100));
                    return result;
                })
                .filter(r -> ((Number) r.get("matchedCompetences")).intValue() > 0)
                .sorted(Comparator.comparingDouble(r -> -((Number) r.get("matchScore")).doubleValue()))
                .limit(20)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalCompetences", competenceRepository.count());
        Map<String, Long> byCategory = competenceRepository.findAll().stream()
                .filter(mc -> mc.getCategory() != null)
                .collect(Collectors.groupingBy(MemberCompetence::getCategory, Collectors.counting()));
        stats.put("byCategory", byCategory);
        return stats;
    }
}
