package com.discipolat.common.infrastructure;

import com.discipolat.DiscipolatApplication;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.TransferType;
import com.discipolat.common.enums.TypeDisciple;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyChiefHistory;
import com.discipolat.modules.families.domain.FamilyChiefHistoryRepository;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.members.domain.MemberDepartment;
import com.discipolat.modules.members.domain.MemberDepartmentRepository;
import com.discipolat.modules.notifications.domain.Notification;
import com.discipolat.modules.notifications.domain.NotificationRepository;
import com.discipolat.modules.search.domain.SearchService;
import com.discipolat.modules.souls.api.UpdateSoulRequest;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulDepartment;
import com.discipolat.modules.souls.domain.SoulDepartmentRepository;
import com.discipolat.modules.souls.domain.SoulHistory;
import com.discipolat.modules.souls.domain.SoulHistoryRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.souls.domain.SoulService;
import com.discipolat.modules.transfers.domain.TransferExecutor;
import com.discipolat.modules.transfers.domain.TransferRequest;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserDepartment;
import com.discipolat.modules.users.domain.UserDepartmentRepository;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TESTS DE PROPAGATION — phase « cohérence / synchronisation ».
 *
 * Objectif : prouver le principe UNE ENTITÉ = UNE SOURCE DE VÉRITÉ.
 * Une modification opérée par un service (moteur de transfert, service
 * d'âmes, …) doit être visible partout ailleurs sans duplication :
 * CRM faiseur, famille, département/responsable, historique, notifications,
 * recherche et statistiques lisent la MÊME donnée.
 *
 * Les scénarios exercent le moteur réel {@link TransferExecutor} (workflow
 * complet exécuté) et {@link SoulService} sur une base H2 embarquée avec le
 * contexte Spring complet : aucun mock — les assertions se font sur les
 * données réellement persistées.
 */
@SpringBootTest(classes = DiscipolatApplication.class)
@ActiveProfiles("test")
class PropagationConsistencyTest {

    private static final UUID DEFAULT_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired private TransferExecutor transferExecutor;
    @Autowired private SoulService soulService;
    @Autowired private SearchService searchService;

    @Autowired private SoulRepository soulRepository;
    @Autowired private SoulHistoryRepository soulHistoryRepository;
    @Autowired private SoulDepartmentRepository soulDepartmentRepository;
    @Autowired private MemberDepartmentRepository memberDepartmentRepository;
    @Autowired private FamilyRepository familyRepository;
    @Autowired private FamilyChiefHistoryRepository chiefHistoryRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserDepartmentRepository userDepartmentRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID pasteurId;

    @BeforeEach
    void setUp() {
        // Base propre entre chaque test : les compteurs et listes doivent
        // refléter UNIQUEMENT les données du scénario courant.
        // H2 : une table par TRUNCATE et pas de CASCADE → on coupe l'intégrité
        // référentielle le temps du nettoyage (UUID : pas de séquence à réinitialiser).
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        for (String table : List.of(
                "notifications", "soul_history", "soul_departments",
                "member_departments", "family_chief_history", "transfer_requests",
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
    // HELPERS
    // ========================================================================

    /** Pose un contexte de sécurité réel : l'utilisateur courant est l'UUID. */
    private void login(UUID userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userId, "token", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private User saveUser(String email, UserRole role, String firstName, String lastName) {
        return userRepository.save(User.builder()
                .tenantId(DEFAULT_TENANT_ID)
                .email(email)
                .passwordHash("PLACEHOLDER")
                .firstName(firstName)
                .lastName(lastName)
                .role(role)
                .roles(Set.of(role))
                .activeRole(role)
                .statut(UserStatus.ACTIVE)
                .build());
    }

    private Soul saveSoul(String nom, String prenom, UUID faiseurId, UUID familleId) {
        return soulRepository.save(Soul.builder()
                .tenantId(DEFAULT_TENANT_ID)
                .nom(nom).prenom(prenom).email(nom.toLowerCase() + "@test")
                .typeDisciple(TypeDisciple.NOUVEL_ARRIVANT)
                .dateIntegration(LocalDate.now())
                .statut(StatutAme.ACTIF)
                .faiseurId(faiseurId)
                .familleId(familleId)
                .etatSpirituel("EN_CROISSANCE")
                .niveauCroissance(2)
                .build());
    }

    private Family saveFamily(String nom, UUID chefId) {
        return familyRepository.save(Family.builder()
                .tenantId(DEFAULT_TENANT_ID).nom(nom).chefFamilleId(chefId).dateCreation(LocalDate.now()).build());
    }

    private Department saveDepartment(String nom, UUID responsableId) {
        return departmentRepository.save(Department.builder()
                .tenantId(DEFAULT_TENANT_ID).nom(nom).responsableId(responsableId).build());
    }

    private Map<String, Object> affectation(String type, UUID id, String nom) {
        return Map.of("type", type, "id", id, "nom", nom);
    }

    /** Exécute un transfert via le moteur réel (workflow exécuté). */
    private void executeTransfer(TransferType type, UUID personneId, String personneType,
                                 Map<String, Object> ancienne, Map<String, Object> nouvelle,
                                 Map<String, Object> regles) {
        TransferRequest req = TransferRequest.builder()
                .tenantId(DEFAULT_TENANT_ID)
                .type(type)
                .statut(com.discipolat.common.enums.TransferStatus.VALIDE)
                .personneId(personneId)
                .personneType(personneType)
                .ancienneAffectation(ancienne)
                .nouvelleAffectation(nouvelle)
                .demandeurId(pasteurId)
                .justification("Test de propagation")
                .build();
        transferExecutor.execute(req, regles);
    }

    private List<Notification> notificationsFor(UUID userId) {
        return notificationRepository.findAll().stream()
                .filter(n -> n.getDestinataireId().equals(userId))
                .toList();
    }

    private boolean hasNotification(UUID userId, String contenu) {
        return notificationsFor(userId).stream()
                .anyMatch(n -> n.getMessage().contains(contenu));
    }

    private SoulHistory history(UUID ameId, String type) {
        return soulHistoryRepository.findByAmeIdOrderByCreatedAtDesc(ameId).stream()
                .filter(h -> h.getTypeEvenement().equals(type))
                .findFirst()
                .orElse(null);
    }

    /** Valeur de métadonnée JSONB (UUID persisté → relecture éventuellement string). */
    private String meta(SoulHistory h, String key) {
        Object v = h != null && h.getMetadata() != null ? h.getMetadata().get(key) : null;
        return v != null ? String.valueOf(v) : null;
    }

    // ========================================================================
    // 1. CHANGEMENT DE FAISEUR (membre → faiseur → historique → notifications → CRM → recherche)
    // ========================================================================

    @Test
    @DisplayName("Changer le faiseur d'un disciple propage partout : âme, historique, notifications, CRM des deux faiseurs, recherche")
    void changeDiscipleFaiseur_propageSurTousLesConsommateurs() {
        User faiseurA = saveUser("faiseur.a@test", UserRole.FAISEUR, "Alpha", "Faiseur");
        User faiseurB = saveUser("faiseur.b@test", UserRole.FAISEUR, "Béa", "Faiseur");
        Family famille = saveFamily("Famille Timothée", pasteurId);
        Soul disciple = saveSoul("Kouassi", "Aya", faiseurA.getId(), famille.getId());

        executeTransfer(TransferType.FAISEUR_DISCIPLE_CHANGEMENT,
                disciple.getId(), "SOUL",
                affectation("FAISEUR", faiseurA.getId(), "A"),
                affectation("FAISEUR", faiseurB.getId(), "B"),
                Map.of());

        // Source de vérité : l'âme.
        assertThat(soulRepository.findById(disciple.getId()).orElseThrow().getFaiseurId())
                .isEqualTo(faiseurB.getId());

        // Historique métier : ancien → nouveau faiseur.
        SoulHistory h = history(disciple.getId(), "REAFFECTATION_FAISEUR");
        assertThat(h).isNotNull();
        assertThat(meta(h, "ancienFaiseurId")).isEqualTo(faiseurA.getId().toString());
        assertThat(meta(h, "nouveauFaiseurId")).isEqualTo(faiseurB.getId().toString());

        // Notifications : le disciple, l'ancien faiseur, le nouveau faiseur.
        assertThat(hasNotification(faiseurB.getId(), "affectée pour suivi")).isTrue();
        assertThat(hasNotification(faiseurA.getId(), "retirée de votre suivi")).isTrue();
        assertThat(notificationsFor(faiseurB.getId()).stream()
                .allMatch(n -> n.getType() == TypeNotification.TRANSFERT_EXECUTEE
                        && n.getCanal() == CanalNotification.IN_APP)).isTrue();

        // CRM faiseur : le disciple a changé de charge.
        assertThat(soulRepository.findAllByFaiseurId(faiseurA.getId())).isEmpty();
        assertThat(soulRepository.findAllByFaiseurId(faiseurB.getId()))
                .extracting(Soul::getId).contains(disciple.getId());

        // Recherche (rôle Pasteur) : toujours la même entité, nom inchangé.
        assertThat(searchService.search("Kouassi", PageRequest.of(0, 20)).getContent())
                .extracting(r -> r.get("id"))
                .contains(disciple.getId());
    }

    // ========================================================================
    // 2. TRANSFERT DE FAMILLE (âme → famille → historique → notifications)
    // ========================================================================

    @Test
    @DisplayName("Transférer une âme vers une autre famille : une seule mise à jour, visible côté familles et historique")
    void transferDiscipleFamily_propageSurLaFamille() {
        User faiseur = saveUser("faiseur@test", UserRole.FAISEUR, "Fabrice", "Faiseur");
        Family f1 = saveFamily("Famille Timothée", pasteurId);
        Family f2 = saveFamily("Famille Tite", pasteurId);
        Soul disciple = saveSoul("Konan", "Emmanuel", faiseur.getId(), f1.getId());

        executeTransfer(TransferType.DISCIPLE_FAMILLE_TRANSFERT,
                disciple.getId(), "SOUL",
                affectation("FAMILLE", f1.getId(), f1.getNom()),
                affectation("FAMILLE", f2.getId(), f2.getNom()),
                Map.of());

        // Source de vérité : l'âme (familleId), et toutes les lectures s'alignent.
        assertThat(soulRepository.findById(disciple.getId()).orElseThrow().getFamilleId())
                .isEqualTo(f2.getId());
        assertThat(soulRepository.findAllByFamilleId(f1.getId())).isEmpty();
        assertThat(soulRepository.findAllByFamilleId(f2.getId()))
                .extracting(Soul::getId).contains(disciple.getId());

        SoulHistory h = history(disciple.getId(), "TRANSFERT_FAMILLE");
        assertThat(h).isNotNull();
        assertThat(meta(h, "ancienneFamilleId")).isEqualTo(f1.getId().toString());
        assertThat(meta(h, "nouvelleFamilleId")).isEqualTo(f2.getId().toString());

        assertThat(notificationRepository.findAll().stream()
                .anyMatch(n -> n.getMessage().contains("famille « Famille Tite »"))).isTrue();
    }

    // ========================================================================
    // 3. TRANSFERT DE DÉPARTEMENT (membre → département → responsable)
    // ========================================================================

    @Test
    @DisplayName("Transférer un membre d'un département à un autre : désaffectation ancien, affectation nouveau, notification au responsable")
    void transferMemberDepartment_propageSurLesDeuxDepartementsEtLeResponsable() {
        User faiseur = saveUser("faiseur@test", UserRole.FAISEUR, "Fabrice", "Faiseur");
        User respA = saveUser("resp.a@test", UserRole.RESPONSABLE, "Rachel", "A");
        User respB = saveUser("resp.b@test", UserRole.RESPONSABLE, "Roger", "B");
        Department deptA = saveDepartment("Jeunesse", respA.getId());
        Department deptB = saveDepartment("Chorale", respB.getId());
        Soul membre = saveSoul("Aka", "Marie", faiseur.getId(), null);

        soulDepartmentRepository.save(SoulDepartment.builder()
                .tenantId(DEFAULT_TENANT_ID)
                .soulId(membre.getId()).departmentId(deptA.getId())
                .actif(true).dateAffectation(LocalDateTime.now())
                .createdBy(pasteurId).origine("TEST").build());
        memberDepartmentRepository.save(MemberDepartment.builder()
                .tenantId(DEFAULT_TENANT_ID)
                .soulId(membre.getId()).departmentId(deptA.getId()).build());

        executeTransfer(TransferType.MEMBRE_DEPARTEMENT_TRANSFERT,
                membre.getId(), "SOUL",
                affectation("DEPARTEMENT", deptA.getId(), deptA.getNom()),
                affectation("DEPARTEMENT", deptB.getId(), deptB.getNom()),
                Map.of());

        // Ancien département : désaffecté (liaison inactive + retiré du membre).
        SoulDepartment oldLink = soulDepartmentRepository
                .findBySoulIdAndDepartmentId(membre.getId(), deptA.getId()).get(0);
        assertThat(oldLink.isActif()).isFalse();
        assertThat(memberDepartmentRepository.existsBySoulIdAndDepartmentId(membre.getId(), deptA.getId())).isFalse();
        // Nouveau département : affecté.
        SoulDepartment newLink = soulDepartmentRepository
                .findBySoulIdAndDepartmentId(membre.getId(), deptB.getId()).get(0);
        assertThat(newLink.isActif()).isTrue();
        assertThat(memberDepartmentRepository.existsBySoulIdAndDepartmentId(membre.getId(), deptB.getId())).isTrue();

        // Vue du responsable B : le membre apparaît dans SON département, plus dans celui de A.
        assertThat(soulDepartmentRepository.findByDepartmentIdAndActifTrue(deptB.getId()))
                .extracting(SoulDepartment::getSoulId).contains(membre.getId());
        assertThat(soulDepartmentRepository.findByDepartmentIdAndActifTrue(deptA.getId()))
                .extracting(SoulDepartment::getSoulId).doesNotContain(membre.getId());

        // Historique + notifications (membre + responsable du nouveau département).
        assertThat(history(membre.getId(), "TRANSFERT_DEPARTEMENT")).isNotNull();
        assertThat(hasNotification(respB.getId(), "transféré vers votre département : Marie Aka")).isTrue();
        assertThat(hasNotification(membre.getId(), "département « Chorale »")).isTrue();
    }

    // ========================================================================
    // 4. CHANGEMENT DE CHEF DE FAMILLE (chef → famille → historique → notifications)
    // ========================================================================

    @Test
    @DisplayName("Changer le chef de famille : famille, ancien chef, nouveau chef et historique sont tous synchronisés")
    void changeChefFamille_propageSurLaFamilleEtLesChefs() {
        User ancienChef = saveUser("chef.ancien@test", UserRole.FAISEUR, "Jean", "Ancien");
        User nouveauChef = saveUser("chef.nouveau@test", UserRole.FAISEUR, "Julie", "Nouvelle");
        Family famille = saveFamily("Famille Timothée", ancienChef.getId());

        executeTransfer(TransferType.CHEF_FAMILLE_TRANSFERT,
                nouveauChef.getId(), "USER",
                affectation("FAMILLE", famille.getId(), famille.getNom()),
                affectation("FAMILLE", famille.getId(), famille.getNom()),
                Map.of());

        // Famille : nouveau chef (id + userId).
        Family reloaded = familyRepository.findById(famille.getId()).orElseThrow();
        assertThat(reloaded.getChefFamilleId()).isEqualTo(nouveauChef.getId());
        assertThat(reloaded.getUserId()).isEqualTo(nouveauChef.getId());

        // Ancien chef : déchu ; nouveau chef : investi.
        User old = userRepository.findById(ancienChef.getId()).orElseThrow();
        assertThat(old.isEstChefDeFamille()).isFalse();
        assertThat(old.getFamilleGereeId()).isNull();
        User fresh = userRepository.findById(nouveauChef.getId()).orElseThrow();
        assertThat(fresh.isEstChefDeFamille()).isTrue();
        assertThat(fresh.getFamilleGereeId()).isEqualTo(famille.getId());

        // Historique professionnel des chefs.
        List<FamilyChiefHistory> hist = chiefHistoryRepository.findByFamilleIdOrderByCreatedAtDesc(famille.getId());
        assertThat(hist).isNotEmpty();
        assertThat(hist.get(0).getAncienChefId()).isEqualTo(ancienChef.getId());
        assertThat(hist.get(0).getNouveauChefId()).isEqualTo(nouveauChef.getId());

        assertThat(hasNotification(nouveauChef.getId(), "chef de la famille « Famille Timothée »")).isTrue();
    }

    // ========================================================================
    // 5. TRANSFERT DE FAISEUR AVEC / SANS SES ÂMES (faiseur → famille → âmes)
    // ========================================================================

    @Test
    @DisplayName("Transférer un faiseur de famille avec ses disciples (règle transfererAmes) : faiseur ET âmes changent de famille")
    void transferFaiseurFamily_avecTransfertDesAmes() {
        User faiseur = saveUser("faiseur@test", UserRole.FAISEUR, "Fabrice", "Faiseur");
        Family f1 = saveFamily("Famille Timothée", pasteurId);
        Family f2 = saveFamily("Famille Tite", pasteurId);
        Soul d1 = saveSoul("Konan", "Emmanuel", faiseur.getId(), f1.getId());
        Soul d2 = saveSoul("Kouassi", "Aya", faiseur.getId(), f1.getId());

        executeTransfer(TransferType.FAISEUR_FAMILLE_TRANSFERT,
                faiseur.getId(), "USER",
                affectation("FAMILLE", f1.getId(), f1.getNom()),
                affectation("FAMILLE", f2.getId(), f2.getNom()),
                Map.of("transfererAmes", true));

        // Faiseur ET disciples : une seule source de vérité (familleId).
        assertThat(userRepository.findById(faiseur.getId()).orElseThrow().getFamilleGereeId())
                .isEqualTo(f2.getId());
        assertThat(soulRepository.findById(d1.getId()).orElseThrow().getFamilleId()).isEqualTo(f2.getId());
        assertThat(soulRepository.findById(d2.getId()).orElseThrow().getFamilleId()).isEqualTo(f2.getId());
        assertThat(soulRepository.findAllByFamilleId(f1.getId())).isEmpty();
        assertThat(soulRepository.findAllByFamilleId(f2.getId())).hasSize(2);

        // Historique sur chaque disciple transféré.
        assertThat(history(d1.getId(), "TRANSFERT_FAMILLE")).isNotNull();
        assertThat(history(d2.getId(), "TRANSFERT_FAMILLE")).isNotNull();
    }

    @Test
    @DisplayName("Transférer un faiseur de famille SANS ses disciples : seuls le faiseur change de famille, les âmes restent")
    void transferFaiseurFamily_sansTransfertDesAmes() {
        User faiseur = saveUser("faiseur@test", UserRole.FAISEUR, "Fabrice", "Faiseur");
        Family f1 = saveFamily("Famille Timothée", pasteurId);
        Family f2 = saveFamily("Famille Tite", pasteurId);
        Soul disciple = saveSoul("Konan", "Emmanuel", faiseur.getId(), f1.getId());

        executeTransfer(TransferType.FAISEUR_FAMILLE_TRANSFERT,
                faiseur.getId(), "USER",
                affectation("FAMILLE", f1.getId(), f1.getNom()),
                affectation("FAMILLE", f2.getId(), f2.getNom()),
                Map.of("transfererAmes", false));

        assertThat(userRepository.findById(faiseur.getId()).orElseThrow().getFamilleGereeId())
                .isEqualTo(f2.getId());
        assertThat(soulRepository.findById(disciple.getId()).orElseThrow().getFamilleId())
                .isEqualTo(f1.getId());
        assertThat(history(disciple.getId(), "TRANSFERT_FAMILLE")).isNull();
    }

    // ========================================================================
    // 6. CHANGEMENT DE RESPONSABLE DE DÉPARTEMENT (responsable → liaison → notifications)
    // ========================================================================

    @Test
    @DisplayName("Changer le responsable d'un département : département, liaison user_departments et notifications des deux responsables")
    void changeDepartementResponsable_propageSurLaLiaisonEtLesResponsables() {
        User ancienResp = saveUser("resp.ancien@test", UserRole.RESPONSABLE, "Rachel", "Ancienne");
        User nouveauResp = saveUser("resp.nouveau@test", UserRole.RESPONSABLE, "Roger", "Nouveau");
        Department dept = saveDepartment("Jeunesse", ancienResp.getId());
        userDepartmentRepository.save(UserDepartment.builder()
                .tenantId(DEFAULT_TENANT_ID)
                .userId(ancienResp.getId()).departmentId(dept.getId())
                .roleDansDept("RESPONSABLE").build());

        executeTransfer(TransferType.RESPONSABLE_DEPARTEMENT_CHANGEMENT,
                nouveauResp.getId(), "USER",
                affectation("DEPARTEMENT", dept.getId(), dept.getNom()),
                affectation("DEPARTEMENT", dept.getId(), dept.getNom()),
                Map.of());

        // Département : nouveau responsable.
        assertThat(departmentRepository.findById(dept.getId()).orElseThrow().getResponsableId())
                .isEqualTo(nouveauResp.getId());

        // Liaison user_departments : l'ancien responsable n'a plus le rôle, le nouveau l'a.
        assertThat(userDepartmentRepository.findByDepartmentId(dept.getId()))
                .extracting(UserDepartment::getUserId)
                .containsExactly(nouveauResp.getId());
        assertThat(userDepartmentRepository.findByDepartmentId(dept.getId()).get(0).getRoleDansDept())
                .isEqualTo("RESPONSABLE");

        assertThat(hasNotification(nouveauResp.getId(), "responsable du département « Jeunesse »")).isTrue();
        assertThat(hasNotification(ancienResp.getId(), "n'êtes plus responsable du département « Jeunesse »")).isTrue();
    }

    // ========================================================================
    // 7. MODIFICATION D'ÂME (source de vérité → recherche → statistiques)
    // ========================================================================

    @Test
    @DisplayName("Modifier une âme : une seule écriture, visible dans l'historique, la recherche et les statistiques")
    void soulUpdate_sourceDeVeriteVisibleEnRechercheEtStatistiques() {
        User faiseur = saveUser("faiseur@test", UserRole.FAISEUR, "Fabrice", "Faiseur");
        Soul active = saveSoul("Kouassi", "Aya", faiseur.getId(), null);
        saveSoul("Konan", "Emmanuel", faiseur.getId(), null);

        soulService.update(active.getId(), new UpdateSoulRequest(
                "Kouassi", "Aya", null, null, null, null, null, null,
                null, null, StatutAme.DECROCHE, null, null, null,
                "EN_DIFFICULTE", 1, null));

        // Source de vérité : l'âme porte le nouveau statut.
        Soul reloaded = soulRepository.findById(active.getId()).orElseThrow();
        assertThat(reloaded.getStatut()).isEqualTo(StatutAme.DECROCHE);
        assertThat(reloaded.getEtatSpirituel()).isEqualTo("EN_DIFFICULTE");

        // Historique : changements de statut et d'état spirituel tracés.
        SoulHistory statutHist = history(active.getId(), "CHANGEMENT_STATUT");
        assertThat(statutHist.getAncienStatut()).isEqualTo("ACTIF");
        assertThat(statutHist.getNouveauStatut()).isEqualTo("DECROCHE");
        assertThat(history(active.getId(), "CHANGEMENT_ETAT_SPIRITUEL")).isNotNull();

        // Recherche : l'âme reste trouvable (même entité).
        assertThat(searchService.search("Kouassi", PageRequest.of(0, 20)).getContent())
                .extracting(r -> r.get("id")).contains(active.getId());

        // Statistiques réelles : les compteurs par statut reflètent la mise à jour.
        long decroches = soulRepository.findAll().stream()
                .filter(s -> s.getStatut() == StatutAme.DECROCHE).count();
        long actifs = soulRepository.findAll().stream()
                .filter(s -> s.getStatut() == StatutAme.ACTIF).count();
        assertThat(decroches).isEqualTo(1);
        assertThat(actifs).isEqualTo(1);
    }
}
