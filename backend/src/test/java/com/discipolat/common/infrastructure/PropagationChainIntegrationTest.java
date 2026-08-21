package com.discipolat.common.infrastructure;

import com.discipolat.DiscipolatApplication;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.TypeDisciple;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.audit.domain.AuditLog;
import com.discipolat.modules.audit.domain.AuditLogRepository;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.families.domain.FamilyService;
import com.discipolat.modules.departments.domain.DepartmentService;
import com.discipolat.modules.notifications.domain.Notification;
import com.discipolat.modules.notifications.domain.NotificationRepository;
import com.discipolat.modules.souls.api.UpdateSoulRequest;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulHistory;
import com.discipolat.modules.souls.domain.SoulHistoryRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.souls.domain.SoulService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import com.discipolat.modules.users.domain.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TESTS DE CHAÎNE DE PROPAGATION INTER-ENTITÉS
 *
 * Vérifie le principe « UNE ENTITÉ = UNE SOURCE DE VÉRITÉ » pour chaque
 * entité critique : modification → notification → historique → audit trail.
 *
 * Ces tests utilisent le contexte Spring complet (H2 embarquée) avec les
 * services RÉELS : aucun mock — les assertions se font sur les données
 * réellement persistées dans la base.
 *
 * Chaînes testées :
 *   1. Soul → faiseur notification + soul history + audit log
 *   2. Soul → état spirituel → faiseur notification + history + audit
 *   3. Soul → réaffectation faiseur → notifications + history + audit
 *   4. Family → chef change → notifications + chief history + audit
 *   5. Department → responsable change → notifications + audit
 *   6. User → promote to faiseur → notification + audit
 *   7. User → status change → notification + audit
 *   8. Cross-entity : Soul update triggers consistent state across all views
 */
@SpringBootTest(classes = DiscipolatApplication.class)
@ActiveProfiles("test")
class PropagationChainIntegrationTest {

    private static final UUID DEFAULT_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired private SoulService soulService;
    @Autowired private UserService userService;
    @Autowired private FamilyService familyService;
    @Autowired private DepartmentService departmentService;

    @Autowired private SoulRepository soulRepository;
    @Autowired private SoulHistoryRepository soulHistoryRepository;
    @Autowired private FamilyRepository familyRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID pasteurId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        for (String table : List.of(
                "audit_logs", "notifications", "soul_history",
                "souls", "families", "departments", "users", "user_roles")) {
            jdbcTemplate.execute("TRUNCATE TABLE " + table);
        }
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        TenantContext.setTenantId(DEFAULT_TENANT_ID);
        pasteurId = saveUser("pasteur@test", UserRole.PASTEUR, "Pierre", "Pasteur").getId();
        login(pasteurId, "PASTEUR");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    // ========================================================================
    // 1. SOUL → FAISEUR NOTIFICATION + SOUL HISTORY + AUDIT LOG
    // ========================================================================

    @Test
    @DisplayName("Modifier le statut d'une âme : notification au faiseur + historique âme + journal d'audit")
    void soulStatusChange_chainComplete() {
        User faiseur = saveUser("faiseur@test", UserRole.FAISEUR, "Fabrice", "Faiseur");
        Soul disciple = saveSoul("Kouassi", "Aya", faiseur.getId(), null, StatutAme.ACTIF);

        // Modify the soul status
        soulService.update(disciple.getId(), new UpdateSoulRequest(
                "Kouassi", "Aya", null, null, null, null, null, null,
                null, null, StatutAme.DECROCHE, null, null, null,
                null, null, null));

        // 1. Source of truth: the soul has the new status
        Soul reloaded = soulRepository.findById(disciple.getId()).orElseThrow();
        assertThat(reloaded.getStatut()).isEqualTo(StatutAme.DECROCHE);

        // 2. Soul history: status change tracked
        SoulHistory statutHistory = findHistory(disciple.getId(), "CHANGEMENT_STATUT");
        assertThat(statutHistory).isNotNull();
        assertThat(statutHistory.getAncienStatut()).isEqualTo("ACTIF");
        assertThat(statutHistory.getNouveauStatut()).isEqualTo("DECROCHE");

        // 3. Notification to the faiseur
        assertThat(hasNotification(faiseur.getId(), "Statut disciple modifié")).isTrue();
        assertThat(hasNotification(faiseur.getId(), "ACTIF")).isTrue();
        assertThat(hasNotification(faiseur.getId(), "DECROCHE")).isTrue();

        // 4. Audit trail: SOUL_STATUS_CHANGED logged
        AuditLog audit = findAudit("SOUL_STATUS_CHANGED", "SOUL", disciple.getId());
        assertThat(audit).isNotNull();
        assertThat(audit.getUtilisateurId()).isEqualTo(pasteurId);
    }

    // ========================================================================
    // 2. SOUL → ÉTAT SPIRITUEL → FAISEUR NOTIFICATION + HISTORY + AUDIT
    // ========================================================================

    @Test
    @DisplayName("Modifier l'état spirituel d'une âme : notification au faiseur + historique + audit")
    void soulEtatSpirituelChange_chainComplete() {
        User faiseur = saveUser("faiseur@test", UserRole.FAISEUR, "Fabrice", "Faiseur");
        Soul disciple = saveSoul("Konan", "Emmanuel", faiseur.getId(), null, StatutAme.ACTIF);

        // L'âme est créée avec l'état « EN_CROISSANCE » (cf. saveSoul) :
        // on change vers une valeur DIFFÉRENTE pour déclencher la propagation.
        soulService.update(disciple.getId(), new UpdateSoulRequest(
                "Konan", "Emmanuel", null, null, null, null, null, null,
                null, null, null, null, null, null,
                "EN_DIFFICULTE", null, null));

        // History: état spirituel change tracked
        SoulHistory h = findHistory(disciple.getId(), "CHANGEMENT_ETAT_SPIRITUEL");
        assertThat(h).isNotNull();
        assertThat(h.getDescription()).contains("EN_DIFFICULTE");

        // Notification to the faiseur
        assertThat(hasNotification(faiseur.getId(), "État spirituel modifié")).isTrue();
        assertThat(hasNotification(faiseur.getId(), "EN_DIFFICULTE")).isTrue();

        // Audit trail
        AuditLog audit = findAudit("SOUL_UPDATED", "SOUL", disciple.getId());
        assertThat(audit).isNotNull();
    }

    // ========================================================================
    // 3. SOUL → RÉAFFECTATION FAISEUR → NOTIFICATIONS + HISTORY + AUDIT
    // ========================================================================

    @Test
    @DisplayName("Réaffecter un disciple à un autre faiseur : notifications aux deux faiseurs + historique + audit")
    void soulReassignFaiseur_chainComplete() {
        User ancienFaiseur = saveUser("faiseur.ancien@test", UserRole.FAISEUR, "Alpha", "Ancien");
        User nouveauFaiseur = saveUser("faiseur.nouveau@test", UserRole.FAISEUR, "Béa", "Nouvelle");
        Soul disciple = saveSoul("Kouassi", "Aya", ancienFaiseur.getId(), null, StatutAme.ACTIF);

        soulService.update(disciple.getId(), new UpdateSoulRequest(
                "Kouassi", "Aya", null, null, null, null, null, null,
                null, null, null, nouveauFaiseur.getId(), null, null,
                null, null, null));

        // Source of truth: faiseur changed
        Soul reloaded = soulRepository.findById(disciple.getId()).orElseThrow();
        assertThat(reloaded.getFaiseurId()).isEqualTo(nouveauFaiseur.getId());

        // Soul history: reassignment tracked
        SoulHistory h = findHistory(disciple.getId(), "REAFFECTATION");
        assertThat(h).isNotNull();
        assertThat(h.getAncienFaiseurId()).isEqualTo(ancienFaiseur.getId());
        assertThat(h.getNouveauFaiseurId()).isEqualTo(nouveauFaiseur.getId());

        // Notifications: both faiseurs notified
        assertThat(hasNotification(ancienFaiseur.getId(), "ne fait plus partie de vos disciples")).isTrue();
        assertThat(hasNotification(nouveauFaiseur.getId(), "vous a été affecté")).isTrue();

        // Audit trail
        AuditLog audit = findAudit("SOUL_REASSIGNED", "SOUL", disciple.getId());
        assertThat(audit).isNotNull();
    }

    // ========================================================================
    // 4. FAMILY → CHEF CHANGE → NOTIFICATIONS + CHIEF HISTORY + AUDIT
    // ========================================================================

    @Test
    @DisplayName("Changer le chef de famille : notifications + historique chefs + audit")
    void familyChefChange_chainComplete() {
        User ancienChef = saveUser("chef.ancien@test", UserRole.FAISEUR, "Jean", "Ancien");
        User nouveauChef = saveUser("chef.nouveau@test", UserRole.FAISEUR, "Julie", "Nouvelle");
        Family famille = saveFamily("Famille Timothée", ancienChef.getId());

        // Update the family chef via the service
        familyService.update(famille.getId(),
                new com.discipolat.modules.families.api.UpdateFamilyRequest(
                        "Famille Timothée", nouveauChef.getId(), null));

        // Source of truth: family has new chef
        Family reloaded = familyRepository.findById(famille.getId()).orElseThrow();
        assertThat(reloaded.getChefFamilleId()).isEqualTo(nouveauChef.getId());

        // Notifications: both chefs notified
        assertThat(hasNotification(ancienChef.getId(), "n'êtes plus chef de famille")).isTrue();
        assertThat(hasNotification(nouveauChef.getId(), "chef de la famille")).isTrue();

        // Audit trail
        AuditLog audit = findAudit("FAMILY_UPDATED", "FAMILY", famille.getId());
        assertThat(audit).isNotNull();
    }

    // ========================================================================
    // 5. DEPARTMENT → RESPONSABLE CHANGE → NOTIFICATIONS + AUDIT
    // ========================================================================

    @Test
    @DisplayName("Changer le responsable d'un département : notifications aux deux responsables + audit")
    void departmentResponsableChange_chainComplete() {
        User ancienResp = saveUser("resp.ancien@test", UserRole.RESPONSABLE, "Rachel", "Ancienne");
        User nouveauResp = saveUser("resp.nouveau@test", UserRole.RESPONSABLE, "Roger", "Nouveau");
        Department dept = saveDepartment("Jeunesse", ancienResp.getId());

        // Update via service
        Department updated = Department.builder()
                .id(dept.getId()).tenantId(DEFAULT_TENANT_ID)
                .nom("Jeunesse").responsableId(nouveauResp.getId()).build();
        departmentService.update(updated);

        // Source of truth: department has new responsable
        assertThat(departmentRepository.findById(dept.getId()).orElseThrow().getResponsableId())
                .isEqualTo(nouveauResp.getId());

        // Notifications: both responsables notified
        assertThat(hasNotification(ancienResp.getId(), "n'êtes plus responsable")).isTrue();
        assertThat(hasNotification(nouveauResp.getId(), "responsable du département")).isTrue();

        // Audit trail
        AuditLog audit = findAudit("DEPARTMENT_UPDATED", "DEPARTMENT", dept.getId());
        assertThat(audit).isNotNull();
    }

    // ========================================================================
    // 6. USER → PROMOTE TO FAISEUR → AUDIT
    // ========================================================================

    @Test
    @DisplayName("Promouvoir un utilisateur Faiseur : rôle modifié + audit trail")
    void userPromoteFaiseur_chainComplete() {
        User user = saveUser("membre@test", UserRole.MEMBRE, "Marie", "Membre");

        userService.promoteToFaiseur(user.getId());

        // Source of truth: user has FAISEUR role
        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getRoles()).contains(UserRole.FAISEUR);
        assertThat(reloaded.getRole()).isEqualTo(UserRole.FAISEUR);

        // Audit trail
        AuditLog audit = findAudit("USER_UPDATED", "USER", user.getId());
        assertThat(audit).isNotNull();
        assertThat(audit.getUtilisateurId()).isEqualTo(pasteurId);
    }

    // ========================================================================
    // 7. USER → STATUS CHANGE → AUDIT
    // ========================================================================

    @Test
    @DisplayName("Désactiver un utilisateur : statut changé + audit trail")
    void userDeactivate_chainComplete() {
        User user = saveUser("membre@test", UserRole.MEMBRE, "Marie", "Membre");

        userService.deactivate(user.getId());

        // Source of truth
        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getStatut()).isEqualTo(UserStatus.INACTIVE);

        // Audit trail
        AuditLog audit = findAudit("USER_STATUS_CHANGED", "USER", user.getId());
        assertThat(audit).isNotNull();
    }

    // ========================================================================
    // 8. CROSS-ENTITY: SOUL UPDATE TRIGGERS CONSISTENT STATE ACROSS ALL VIEWS
    // ========================================================================

    @Test
    @DisplayName("Modifier une âme : toutes les vues (source, historique, notifications, audit) sont cohérentes")
    void soulUpdate_consistentAcrossAllViews() {
        User faiseur = saveUser("faiseur@test", UserRole.FAISEUR, "Fabrice", "Faiseur");
        Soul disciple = saveSoul("Kouassi", "Aya", faiseur.getId(), null, StatutAme.ACTIF);

        // Modify statut + etat spirituel + niveau croissance in one call
        soulService.update(disciple.getId(), new UpdateSoulRequest(
                "Kouassi", "Aya", null, null, null, null, null, null,
                null, null, StatutAme.DECROCHE, null, null, null,
                "EN_DIFFICULTE", 3, null));

        // Source of truth: soul has ALL new values
        Soul reloaded = soulRepository.findById(disciple.getId()).orElseThrow();
        assertThat(reloaded.getStatut()).isEqualTo(StatutAme.DECROCHE);
        assertThat(reloaded.getEtatSpirituel()).isEqualTo("EN_DIFFICULTE");
        assertThat(reloaded.getNiveauCroissance()).isEqualTo(3);

        // History: statut change tracked
        SoulHistory statutH = findHistory(disciple.getId(), "CHANGEMENT_STATUT");
        assertThat(statutH).isNotNull();
        assertThat(statutH.getAncienStatut()).isEqualTo("ACTIF");
        assertThat(statutH.getNouveauStatut()).isEqualTo("DECROCHE");

        // History: etat spirituel change tracked
        SoulHistory etatH = findHistory(disciple.getId(), "CHANGEMENT_ETAT_SPIRITUEL");
        assertThat(etatH).isNotNull();
        assertThat(etatH.getDescription()).contains("EN_DIFFICULTE");

        // History: niveau croissance change tracked
        SoulHistory niveauH = findHistory(disciple.getId(), "CHANGEMENT_NIVEAU_CROISSANCE");
        assertThat(niveauH).isNotNull();

        // Notification: faiseur notified of statut change
        assertThat(hasNotification(faiseur.getId(), "Statut disciple modifié")).isTrue();

        // Notification: faiseur notified of etat spirituel change
        assertThat(hasNotification(faiseur.getId(), "État spirituel modifié")).isTrue();

        // Audit: multiple audit entries for the same entity
        List<AuditLog> audits = auditLogRepository.findAll().stream()
                .filter(a -> a.getEntiteType().equals("SOUL") && a.getEntiteId().equals(disciple.getId()))
                .toList();
        assertThat(audits).isNotEmpty();
        assertThat(audits).anyMatch(a -> a.getAction().contains("SOUL"));
    }

    // ========================================================================
    // HELPERS
    // ========================================================================

    private void login(UUID userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userId, "token", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private User saveUser(String email, UserRole role, String firstName, String lastName) {
        return userRepository.save(User.builder()
                .tenantId(DEFAULT_TENANT_ID)
                .email(email).passwordHash("PLACEHOLDER")
                .firstName(firstName).lastName(lastName)
                .role(role).roles(Set.of(role)).activeRole(role)
                .statut(UserStatus.ACTIVE).build());
    }

    private Soul saveSoul(String nom, String prenom, UUID faiseurId, UUID familleId, StatutAme statut) {
        return soulRepository.save(Soul.builder()
                .tenantId(DEFAULT_TENANT_ID)
                .nom(nom).prenom(prenom).email(nom.toLowerCase() + "@test")
                .typeDisciple(TypeDisciple.NOUVEL_ARRIVANT)
                .dateIntegration(LocalDate.now())
                .statut(statut)
                .faiseurId(faiseurId).familleId(familleId)
                .etatSpirituel("NOUVEAU").niveauCroissance(1)
                .build());
    }

    private Family saveFamily(String nom, UUID chefId) {
        return familyRepository.save(Family.builder()
                .tenantId(DEFAULT_TENANT_ID).nom(nom)
                .chefFamilleId(chefId).dateCreation(LocalDate.now()).build());
    }

    private Department saveDepartment(String nom, UUID responsableId) {
        return departmentRepository.save(Department.builder()
                .tenantId(DEFAULT_TENANT_ID).nom(nom)
                .responsableId(responsableId).build());
    }

    private boolean hasNotification(UUID userId, String contenu) {
        // Le contenu recherché peut être dans le titre OU le message de la
        // notification (ex. « Statut disciple modifié » est un titre).
        return notificationRepository.findAll().stream()
                .filter(n -> n.getDestinataireId().equals(userId))
                .anyMatch(n -> (n.getTitre() != null && n.getTitre().contains(contenu))
                        || (n.getMessage() != null && n.getMessage().contains(contenu)));
    }

    private SoulHistory findHistory(UUID ameId, String type) {
        return soulHistoryRepository.findByAmeIdOrderByCreatedAtDesc(ameId).stream()
                .filter(h -> h.getTypeEvenement().equals(type))
                .findFirst().orElse(null);
    }

    private AuditLog findAudit(String action, String entityType, UUID entityId) {
        return auditLogRepository.findAll().stream()
                .filter(a -> a.getAction().equals(action)
                        && a.getEntiteType().equals(entityType)
                        && a.getEntiteId().equals(entityId))
                .findFirst().orElse(null);
    }
}
