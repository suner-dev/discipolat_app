package com.discipolat.modules.mentoring.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MentoringServiceTest {

    @Mock
    private MentorSuggestionRepository repository;

    private MentoringService mentoringService;
    private UUID tenantId;
    private UUID chefId;

    @BeforeEach
    void setUp() {
        mentoringService = new MentoringService(repository);
        tenantId = UUID.randomUUID();
        chefId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ── LIST ───────────────────────────────────────────────────

    @Test
    void listSuggestions_shouldReturnPage() {
        MentorSuggestion suggestion = buildSuggestion(UUID.randomUUID(), "Charge élevée", MentorSuggestion.Statut.ACTIVE);
        when(repository.findByChefDeFamilleIdAndStatutOrderByPrioritéAscCreatedAtDesc(
                eq(chefId), eq(MentorSuggestion.Statut.ACTIVE), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(suggestion)));

        Page<MentorSuggestion> result = mentoringService.listSuggestions(chefId, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void listAllSuggestions_shouldReturnAll() {
        MentorSuggestion s1 = buildSuggestion(UUID.randomUUID(), "Alerte 1", MentorSuggestion.Statut.ACTIVE);
        MentorSuggestion s2 = buildSuggestion(UUID.randomUUID(), "Alerte 2", MentorSuggestion.Statut.LUE);
        when(repository.findByChefDeFamilleIdOrderByPrioritéAscCreatedAtDesc(chefId))
                .thenReturn(List.of(s1, s2));

        List<MentorSuggestion> result = mentoringService.listAllSuggestions(chefId);

        assertEquals(2, result.size());
    }

    @Test
    void getById_existing_shouldReturn() {
        UUID id = UUID.randomUUID();
        MentorSuggestion suggestion = buildSuggestion(id, "Test", MentorSuggestion.Statut.ACTIVE);
        when(repository.findById(id)).thenReturn(Optional.of(suggestion));

        MentorSuggestion result = mentoringService.getById(id);

        assertEquals(id, result.getId());
    }

    @Test
    void getById_notFound_shouldThrow() {
        when(repository.findById(any())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> mentoringService.getById(UUID.randomUUID()));
    }

    // ── AI SUGGESTION GENERATION ───────────────────────────────

    @Test
    void generateSuggestions_highLoad_shouldCreateDelegationSuggestion() {
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Map<String, Object>> faiseurs = List.of(
                Map.of("id", UUID.randomUUID().toString(), "nom", "David", "disciples", 10, "rapportsSoumis", 5, "scoreMoyen", 75.0, "formationsSuivies", 3, "joursDepuisDernierContact", 5)
        );

        List<MentorSuggestion> result = mentoringService.generateSuggestions(chefId, faiseurs);

        assertFalse(result.isEmpty());
        Optional<MentorSuggestion> delegation = result.stream()
                .filter(s -> s.getCatégorie() == MentorSuggestion.Catégorie.DÉLÉGATION)
                .findFirst();
        assertTrue(delegation.isPresent());
        assertTrue(delegation.get().getTitre().contains("David"));
        assertEquals(MentorSuggestion.Priorité.HAUTE, delegation.get().getPriorité());
        assertTrue(delegation.get().getConfiance() >= 0.8);
    }

    @Test
    void generateSuggestions_lowScore_shouldCreateWarning() {
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Map<String, Object>> faiseurs = List.of(
                Map.of("id", UUID.randomUUID().toString(), "nom", "Marie", "disciples", 3, "rapportsSoumis", 5, "scoreMoyen", 30.0, "formationsSuivies", 5, "joursDepuisDernierContact", 3)
        );

        List<MentorSuggestion> result = mentoringService.generateSuggestions(chefId, faiseurs);

        Optional<MentorSuggestion> warning = result.stream()
                .filter(s -> s.getCatégorie() == MentorSuggestion.Catégorie.MISE_EN_GARDIEN)
                .findFirst();
        assertTrue(warning.isPresent());
        assertTrue(warning.get().getTitre().contains("Score spirituel bas"));
    }

    @Test
    void generateSuggestions_fewReports_shouldFlagAccompaniment() {
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Map<String, Object>> faiseurs = List.of(
                Map.of("id", UUID.randomUUID().toString(), "nom", "Paul", "disciples", 5, "rapportsSoumis", 1, "scoreMoyen", 60.0, "formationsSuivies", 3, "joursDepuisDernierContact", 7)
        );

        List<MentorSuggestion> result = mentoringService.generateSuggestions(chefId, faiseurs);

        Optional<MentorSuggestion> accompaniment = result.stream()
                .filter(s -> s.getCatégorie() == MentorSuggestion.Catégorie.ACCOMPAGNEMENT)
                .filter(s -> s.getTitre().contains("Rapports"))
                .findFirst();
        assertTrue(accompaniment.isPresent());
    }

    @Test
    void generateSuggestions_noFormations_shouldFlagFormation() {
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Map<String, Object>> faiseurs = List.of(
                Map.of("id", UUID.randomUUID().toString(), "nom", "Grace", "disciples", 4, "rapportsSoumis", 3, "scoreMoyen", 55.0, "formationsSuivies", 0, "joursDepuisDernierContact", 5)
        );

        List<MentorSuggestion> result = mentoringService.generateSuggestions(chefId, faiseurs);

        Optional<MentorSuggestion> formation = result.stream()
                .filter(s -> s.getCatégorie() == MentorSuggestion.Catégorie.FORMATION)
                .findFirst();
        assertTrue(formation.isPresent());
    }

    @Test
    void generateSuggestions_distantContact_shouldFlagAccompaniment() {
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Map<String, Object>> faiseurs = List.of(
                Map.of("id", UUID.randomUUID().toString(), "nom", "Jean", "disciples", 3, "rapportsSoumis", 3, "scoreMoyen", 65.0, "formationsSuivies", 3, "joursDepuisDernierContact", 20)
        );

        List<MentorSuggestion> result = mentoringService.generateSuggestions(chefId, faiseurs);

        Optional<MentorSuggestion> distant = result.stream()
                .filter(s -> s.getTitre().contains("Pas de contact"))
                .findFirst();
        assertTrue(distant.isPresent());
        assertTrue(distant.get().getAnalyse().contains("20 jours"));
    }

    @Test
    void generateSuggestions_highPerformer_shouldCreateRecognition() {
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Map<String, Object>> faiseurs = List.of(
                Map.of("id", UUID.randomUUID().toString(), "nom", "Sarah", "disciples", 7, "rapportsSoumis", 4, "scoreMoyen", 85.0, "formationsSuivies", 3, "joursDepuisDernierContact", 2)
        );

        List<MentorSuggestion> result = mentoringService.generateSuggestions(chefId, faiseurs);

        Optional<MentorSuggestion> recognition = result.stream()
                .filter(s -> s.getCatégorie() == MentorSuggestion.Catégorie.RECONNAISSANCE)
                .findFirst();
        assertTrue(recognition.isPresent());
        assertEquals(MentorSuggestion.Priorité.BASSE, recognition.get().getPriorité());
    }

    @Test
    void generateSuggestions_multipleFaiseurs_shouldGenerateMultipleSuggestions() {
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Map<String, Object>> faiseurs = List.of(
                Map.of("id", UUID.randomUUID().toString(), "nom", "Overloaded", "disciples", 12, "rapportsSoumis", 1, "scoreMoyen", 35.0, "formationsSuivies", 0, "joursDepuisDernierContact", 25),
                Map.of("id", UUID.randomUUID().toString(), "nom", "Star", "disciples", 6, "rapportsSoumis", 5, "scoreMoyen", 90.0, "formationsSuivies", 4, "joursDepuisDernierContact", 1)
        );

        List<MentorSuggestion> result = mentoringService.generateSuggestions(chefId, faiseurs);

        assertTrue(result.size() >= 3); // At least delegation + warning + recognition
    }

    @Test
    void generateSuggestions_faiseurOkNoIssues_shouldNotCreateNegativeSuggestions() {
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Map<String, Object>> faiseurs = List.of(
                Map.of("id", UUID.randomUUID().toString(), "nom", "Perfect", "disciples", 5, "rapportsSoumis", 4, "scoreMoyen", 65.0, "formationsSuivies", 3, "joursDepuisDernierContact", 3)
        );

        List<MentorSuggestion> result = mentoringService.generateSuggestions(chefId, faiseurs);

        // A normal faiseur (5 disciples, 4 reports, 65 score, 3 formations, 3 days)
        // Should NOT generate negative suggestions, only possibly recognition (score < 70)
        boolean hasDelegation = result.stream().anyMatch(s -> s.getCatégorie() == MentorSuggestion.Catégorie.DÉLÉGATION);
        boolean hasMiseEnGarde = result.stream().anyMatch(s -> s.getCatégorie() == MentorSuggestion.Catégorie.MISE_EN_GARDIEN);
        assertFalse(hasDelegation);
        assertFalse(hasMiseEnGarde);
    }

    // ── MARK AS READ / ARCHIVE ─────────────────────────────────

    @Test
    void markAsRead_shouldSetStatusLue() {
        UUID id = UUID.randomUUID();
        MentorSuggestion suggestion = buildSuggestion(id, "Test", MentorSuggestion.Statut.ACTIVE);
        when(repository.findById(id)).thenReturn(Optional.of(suggestion));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        mentoringService.markAsRead(id);

        assertEquals(MentorSuggestion.Statut.LUE, suggestion.getStatut());
    }

    @Test
    void archive_shouldSetStatusArchivée() {
        UUID id = UUID.randomUUID();
        MentorSuggestion suggestion = buildSuggestion(id, "Test", MentorSuggestion.Statut.ACTIVE);
        when(repository.findById(id)).thenReturn(Optional.of(suggestion));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        mentoringService.archive(id);

        assertEquals(MentorSuggestion.Statut.ARCHIVÉE, suggestion.getStatut());
    }

    // ── STATS ──────────────────────────────────────────────────

    @Test
    void getStats_shouldReturnCounts() {
        when(repository.countByChefDeFamilleIdAndStatut(chefId, MentorSuggestion.Statut.ACTIVE)).thenReturn(5L);
        when(repository.countByChefDeFamilleIdAndStatut(chefId, MentorSuggestion.Statut.LUE)).thenReturn(3L);
        when(repository.countByChefDeFamilleIdAndStatut(chefId, MentorSuggestion.Statut.ARCHIVÉE)).thenReturn(1L);

        Map<String, Object> stats = mentoringService.getStats(chefId);

        assertEquals(5L, stats.get("actives"));
        assertEquals(3L, stats.get("lues"));
        assertEquals(1L, stats.get("archivées"));
    }

    // ── Helpers ────────────────────────────────────────────────

    private MentorSuggestion buildSuggestion(UUID id, String titre, MentorSuggestion.Statut statut) {
        MentorSuggestion s = new MentorSuggestion();
        s.setId(id);
        s.setTenantId(tenantId);
        s.setChefDeFamilleId(chefId);
        s.setTitre(titre);
        s.setStatut(statut);
        s.setPriorité(MoyennePriorité(statut));
        s.setConfiance(0.8);
        return s;
    }

    private MentorSuggestion.Priorité MoyennePriorité(MentorSuggestion.Statut statut) {
        return MentorSuggestion.Priorité.MOYENNE;
    }
}
