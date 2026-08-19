package com.discipolat.modules.files.domain;

import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BulkImportServiceTest {

    @Mock
    private FamilyRepository familyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SoulRepository soulRepository;

    private BulkImportService bulkImportService;

    private UUID departementId;
    private UUID chefFamilleId;
    private UUID faiseurId;

    @BeforeEach
    void setUp() {
        bulkImportService = new BulkImportService(familyRepository, userRepository, soulRepository);
        departementId = UUID.randomUUID();
        chefFamilleId = UUID.randomUUID();
        faiseurId = UUID.randomUUID();
    }

    // ================ importFamilies ================

    @Test
    void importFamilies_WithValidData_ShouldImportAll() {
        List<Map<String, String>> families = List.of(
                Map.of("nom", "Famille A", "chefFamilleId", chefFamilleId.toString()),
                Map.of("nom", "Famille B", "chefFamilleId", chefFamilleId.toString())
        );

        when(familyRepository.findByNom(anyString())).thenReturn(Optional.empty());
        when(familyRepository.save(any(Family.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = bulkImportService.importFamilies(families);

        assertEquals(2, result.get("imported"));
        assertEquals(0, result.get("skipped"));
        assertEquals(0, ((List<?>) result.get("errors")).size());
        verify(familyRepository, times(2)).save(any(Family.class));
    }

    @Test
    void importFamilies_WithDuplicateName_ShouldSkipDuplicates() {
        String existingName = "Famille Existante";
        List<Map<String, String>> families = List.of(
                Map.of("nom", existingName, "chefFamilleId", chefFamilleId.toString()),
                Map.of("nom", "Famille Nouvelle", "chefFamilleId", chefFamilleId.toString())
        );

        when(familyRepository.findByNom(existingName)).thenReturn(Optional.of(Family.builder().nom(existingName).build()));
        when(familyRepository.findByNom("Famille Nouvelle")).thenReturn(Optional.empty());
        when(familyRepository.save(any(Family.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = bulkImportService.importFamilies(families);

        assertEquals(1, result.get("imported"));
        assertEquals(1, result.get("skipped"));
        verify(familyRepository, times(1)).save(any(Family.class));
    }

    @Test
    void importFamilies_WithInvalidUUID_ShouldCaptureError() {
        List<Map<String, String>> families = List.of(
                Map.of("nom", "Famille C", "chefFamilleId", "not-a-uuid")
        );

        Map<String, Object> result = bulkImportService.importFamilies(families);

        assertEquals(0, result.get("imported"));
        assertEquals(0, result.get("skipped"));
        assertEquals(1, ((List<?>) result.get("errors")).size());
        assertTrue(((String) ((List<?>) result.get("errors")).get(0)).contains("Ligne 1"));
    }

    @Test
    void importFamilies_WithEmptyList_ShouldImportNothing() {
        Map<String, Object> result = bulkImportService.importFamilies(List.of());

        assertEquals(0, result.get("imported"));
        assertEquals(0, result.get("skipped"));
        assertEquals(0, ((List<?>) result.get("errors")).size());
        verify(familyRepository, never()).save(any(Family.class));
    }

    @Test
    void importFamilies_WithMissingFields_ShouldCaptureError() {
        List<Map<String, String>> families = List.of(
                Map.of("nom", "Famille D")
                // Missing chefFamilleId
        );

        Map<String, Object> result = bulkImportService.importFamilies(families);

        assertEquals(0, result.get("imported"));
        assertEquals(0, result.get("skipped"));
        assertEquals(1, ((List<?>) result.get("errors")).size());
    }

    // ================ importUsers ================

    @Test
    void importUsers_WithValidData_ShouldImportAll() {
        List<Map<String, String>> users = List.of(
                Map.of("email", "user1@email.com", "firstName", "Jean", "lastName", "Dupont", "role", "FAISEUR"),
                Map.of("email", "user2@email.com", "firstName", "Marie", "lastName", "Martin", "role", "RESPONSABLE")
        );

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = bulkImportService.importUsers(users);

        assertEquals(2, result.get("imported"));
        assertEquals(0, result.get("skipped"));
        assertEquals(0, ((List<?>) result.get("errors")).size());
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    void importUsers_WithDuplicateEmail_ShouldSkipDuplicates() {
        String existingEmail = "existing@email.com";
        List<Map<String, String>> users = List.of(
                Map.of("email", existingEmail, "firstName", "Déjà", "lastName", "Présent"),
                Map.of("email", "new@email.com", "firstName", "Nouveau", "lastName", "Utilisateur")
        );

        when(userRepository.existsByEmail(existingEmail)).thenReturn(true);
        when(userRepository.existsByEmail("new@email.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = bulkImportService.importUsers(users);

        assertEquals(1, result.get("imported"));
        assertEquals(1, result.get("skipped"));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void importUsers_WithDefaultRole_ShouldUseFAISEUR() {
        List<Map<String, String>> users = List.of(
                Map.of("email", "default@email.com", "firstName", "Sans", "lastName", "Role")
                // No "role" field
        );

        when(userRepository.existsByEmail("default@email.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = bulkImportService.importUsers(users);

        assertEquals(1, result.get("imported"));
        verify(userRepository).save(argThat(u -> u.getRole().name().equals("FAISEUR")));
    }

    @Test
    void importUsers_WithMinimalFields_ShouldUseDefaults() {
        List<Map<String, String>> users = List.of(
                Map.of("email", "minimal@email.com")
                // No firstName, lastName, role — should use defaults
        );

        when(userRepository.existsByEmail("minimal@email.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = bulkImportService.importUsers(users);

        assertEquals(1, result.get("imported"));
        assertEquals(0, ((List<?>) result.get("errors")).size());
        verify(userRepository).save(argThat(u ->
                u.getRole().name().equals("FAISEUR") &&
                u.getStatut().name().equals("PENDING_ACTIVATION")
        ));
    }

    @Test
    void importUsers_WithInvalidRole_ShouldCaptureError() {
        List<Map<String, String>> users = List.of(
                Map.of("email", "badrole@email.com", "role", "INEXISTANT")
        );

        when(userRepository.existsByEmail("badrole@email.com")).thenReturn(false);

        Map<String, Object> result = bulkImportService.importUsers(users);

        assertEquals(0, result.get("imported"));
        assertEquals(1, ((List<?>) result.get("errors")).size());
    }

    // ================ importSouls ================

    @Test
    void importSouls_WithValidData_ShouldImportAll() {
        List<Map<String, String>> souls = List.of(
                Map.of("nom", "Petit", "prenom", "Pierre", "faiseurId", faiseurId.toString()),
                Map.of("nom", "Robert", "prenom", "Anne", "faiseurId", faiseurId.toString())
        );

        when(soulRepository.save(any(Soul.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = bulkImportService.importSouls(souls);

        assertEquals(2, result.get("imported"));
        assertEquals(0, result.get("skipped"));
        assertEquals(0, ((List<?>) result.get("errors")).size());
        verify(soulRepository, times(2)).save(any(Soul.class));
    }

    @Test
    void importSouls_WithDefaultValues_ShouldSetDefaults() {
        List<Map<String, String>> souls = List.of(
                Map.of("nom", "Dupont", "faiseurId", faiseurId.toString())
                // No "prenom", "email", "telephone"
        );

        when(soulRepository.save(any(Soul.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = bulkImportService.importSouls(souls);

        assertEquals(1, result.get("imported"));
        verify(soulRepository).save(argThat(s ->
                "Dupont".equals(s.getNom()) &&
                "".equals(s.getPrenom()) &&
                s.getTypeDisciple().name().equals("NOUVEL_ARRIVANT") &&
                s.getStatut().name().equals("EN_INTEGRATION")
        ));
    }

    @Test
    void importSouls_WithExtraFields_ShouldIgnoreUnknownFields() {
        List<Map<String, String>> souls = List.of(
                Map.of(
                        "nom", "Martin",
                        "prenom", "Sophie",
                        "email", "sophie@email.com",
                        "telephone", "0123456789",
                        "faiseurId", faiseurId.toString()
                )
        );

        when(soulRepository.save(any(Soul.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = bulkImportService.importSouls(souls);

        assertEquals(1, result.get("imported"));
        assertEquals(0, ((List<?>) result.get("errors")).size());
        verify(soulRepository).save(argThat(s ->
                "Martin".equals(s.getNom()) &&
                "Sophie".equals(s.getPrenom()) &&
                "sophie@email.com".equals(s.getEmail())
        ));
    }

    @Test
    void importSouls_WithInvalidFaiseurId_ShouldCaptureError() {
        List<Map<String, String>> souls = List.of(
                Map.of("nom", "SansFaiseur", "faiseurId", "not-a-uuid")
        );

        Map<String, Object> result = bulkImportService.importSouls(souls);

        assertEquals(0, result.get("imported"));
        assertEquals(1, ((List<?>) result.get("errors")).size());
    }

    @Test
    void importSouls_WithEmptyList_ShouldImportNothing() {
        Map<String, Object> result = bulkImportService.importSouls(List.of());

        assertEquals(0, result.get("imported"));
        assertEquals(0, result.get("skipped"));
        assertEquals(0, ((List<?>) result.get("errors")).size());
        verify(soulRepository, never()).save(any(Soul.class));
    }
}
