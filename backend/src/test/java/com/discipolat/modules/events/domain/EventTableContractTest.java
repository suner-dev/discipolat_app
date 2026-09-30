package com.discipolat.modules.events.domain;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * GATE DE NON-REGRESSION — le test qui manquait depuis toujours.
 *
 * <p>Il rejoue ce que la suite H2 ne jouait pas : une base <b>reellement
 * migree</b> par la chaine Flyway complete, puis un aller-retour Hibernate
 * dessus, avec {@code ddl-auto: validate}.
 *
 * <p>Le profil {@code test} est active pour ses cles JWT et sa configuration,
 * mais la source de donnees, {@code ddl-auto} et {@code flyway} sont
 * <b>ecrases</b> par les proprietes dynamiques ci-dessous : c'est un
 * PostgreSQL migre, pas un H2 que Hibernate fabrique.
 *
 * <p>La suite historique fonctionne avec {@code ddl-auto: create-drop} et
 * {@code flyway: false} : Hibernate recree le schema a partir des annotations.
 * Toute divergence entre une entite et les migrations y est donc invisible
 * <b>par construction</b>. C'est ainsi que {@code Event.java} a pu pointer
 * pendant des mois vers une table que {@code V158} avait renommee en
 * {@code legacy_events} : plus d'un millier de tests verts, et un module entier
 * mort au deploiement. Un test qui ne rejoue pas la migration ne peut pas voir
 * une derive de migration.
 *
 * <p>Ce test est donc un test d'<b>execution</b>, pas de verifications de
 * declarations. Il echoue si une colonne mappee disparait, si un type JDBC ne
 * correspond plus, si le fuseau bouge, ou si la contrainte de vocabulaire
 * refuse la valeur que le produit envoie reellement.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "it-flyway"})
@Transactional
class EventTableContractTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("discipolat")
            .withUsername("discipolat")
            .withPassword("discipolat");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // `none`, et non `validate` : le contrôle de type est fait ici, colonne
        // par colonne, sur le seul module qui nous concerne.
        //
        // Pourquoi pas `validate` ? Parce qu'il refuse de démarrer sur un défaut
        // préexistant et sans rapport, trouvé en exécutant ce test :
        // `V135` crée `audit_event.hash CHAR(64)` alors que `AuditEvent.java`
        // déclare `varchar(64)`. Un `validate` global rendrait ce test
        // inexécutable — et le défaut resterait hors de vue, ce qui est
        // exactement le travers que ce test combat. Il est consigne dans
        // reports/plan-2agents/agentB.md, et il signifie que le profil `dev`
        // (ddl-auto: validate) ne démarre sur aucune base migrée.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
        // Le profil `test` prepare un H2 via `sql.init` : ici c'est un
        // PostgreSQL, et le schema vient de Flyway, pas d'un script de test.
        registry.add("spring.sql.init.mode", () -> "never");
    }

    @Autowired
    EventRepository eventRepository;

    @Autowired
    EventRegistrationRepository registrationRepository;

    @Autowired
    EntityManager em;

    private UUID tenantId;
    private UUID organizerId;
    private UUID familleId;

    @BeforeEach
    void parents() {
        // `event.tenant_id`, `event.organizer_id` et `event.famille_id` portent
        // des cles etrangeres : on ne peut pas ecrire un evenement avec des
        // UUID fantaisistes. Les parents minimaux sont donc crees en SQL natif,
        // ce qui evite de demarrer tout le graphe d'authentification.
        tenantId = (UUID) em.createNativeQuery("""
                INSERT INTO tenants (name, slug) VALUES ('Eglise du test', :slug)
                RETURNING id
                """).setParameter("slug", "eglise-" + UUID.randomUUID().toString().substring(0, 8))
                .getSingleResult();

        organizerId = (UUID) em.createNativeQuery("""
                INSERT INTO users (email, password_hash, role, tenant_id)
                VALUES (:email, 'x', 'PASTEUR', :tenant) RETURNING id
                """).setParameter("email", "pasteur-" + UUID.randomUUID() + "@test.local")
                .setParameter("tenant", tenantId)
                .getSingleResult();

        // `families.nom` porte une UNIQUE GLOBALE (contrainte `uk_families_nom`,
        // et non `(tenant_id, nom)`) : deux eglises ne peuvent pas avoir une
        // famille du meme nom. Constat confirme a l'execution ici — c'est
        // l'arbitrage D4 du TODO de reprise, toujours non corrige. Le nom est
        // donc unique par test, faute de quoi le test echouerait sur une
        // contrainte qui n'a rien a voir avec ce qu'il verifie.
        familleId = (UUID) em.createNativeQuery("""
                INSERT INTO families (nom, chef_famille_id, tenant_id)
                VALUES (:nom, :chef, :tenant)
                RETURNING id
                """).setParameter("nom", "Famille-" + UUID.randomUUID())
                .setParameter("chef", organizerId)
                .setParameter("tenant", tenantId)
                .getSingleResult();
    }

    @Test
    @DisplayName("V203 : la table vivante `event` porte TOUTES les colonnes mappees par l'entite")
    void everyMappedColumnExistsOnTheLivingTable() {
        // Copie maintenance du @Column de Event. Si l'entite gagne une colonne
        // sans que cette liste soit mise a jour, le test echoue et le rappelle :
        // c'est la liste qui rend l'oubli visible.
        List<String> mapped = List.of(
                "id", "tenant_id", "organizer_id", "famille_id", "department_id",
                "organization_unit_id", "resource_scope", "type", "title", "description",
                "lieu", "start_at", "end_at", "limite_places", "status", "image_url",
                "tags", "visibility", "requires_registration", "has_checkin",
                "latitude", "longitude", "geofence_radius_m", "stream_id",
                "compte_rendu", "created_at", "updated_at", "deleted_at");

        assertThat(colonnesReelles())
                .as("colonne mappee par Event mais absente de la table vivante `event`")
                .containsAll(mapped);

        // Et l'inverse : aucune colonne de `event` ne doit être inconnue, sans
        // justification explicite. Il y en a cinq, et chacune a une raison :
        //
        //  * `created_by`, `is_recurring`, `recurrence_rule`, `timezone` sont
        //    mappees par `ChurchEvent`, la seconde entite de cette table. Elle
        //    est conservee volontairement : `/api/v1/church-events` (23
        //    endpoints, dont 3 consommes par le mobile) sert encore, et la
        //    supprimer ferait perdre des donnees. Deux mappings sur une table
        //    restent un dette connue, pas une verite double sur une colonne :
        //    chacun ecrit son propre sous-ensemble.
        //  * `search_vector` est un `tsvector` produit par un trigger
        //    (V163) et jamais mappe.
        List<String> parChurchEvent = List.of(
                "created_by", "is_recurring", "recurrence_rule", "timezone");
        List<String> nonMapees = colonnesReelles().stream()
                .filter(c -> !mapped.contains(c))
                .filter(c -> !parChurchEvent.contains(c))
                .filter(c -> !c.equals("search_vector"))
                .toList();

        assertThat(nonMapees)
                .as("colonne de `event` ni mappee par Event, ni mappee par ChurchEvent, ni justifiee")
                .isEmpty();
    }

    @Test
    @DisplayName("V203 : chaque colonne a le TYPE que l'entite declare (pas seulement le nom)")
    void everyMappedColumnHasTheDeclaredType() {
        // Un nom de colonne juste ne suffit pas : c'est la DIVERGENCE DE TYPE qui
        // fait demarrer Hibernate en `validate`, et c'est elle qui ferait
        // disparaitre silencieusement le fuseau. Les `timestamptz` sont verifies
        // explicitement parce que le convertisseur UTC en depend : si un
        // `ddl-auto: update` les ramenait un jour a `timestamp`, l'heure d'un
        // evenement changerait selon le serveur qui le sert.
        assertThat(typesReelles())
                .as("type reel de la colonne != type attendu par l'entite")
                .containsAllEntriesOf(Map.ofEntries(
                        Map.entry("id", "uuid"),
                        Map.entry("tenant_id", "uuid"),
                        Map.entry("organizer_id", "uuid"),
                        Map.entry("famille_id", "uuid"),
                        Map.entry("department_id", "uuid"),
                        Map.entry("organization_unit_id", "uuid"),
                        Map.entry("resource_scope", "character varying"),
                        Map.entry("type", "character varying"),
                        Map.entry("title", "character varying"),
                        Map.entry("description", "text"),
                        Map.entry("lieu", "character varying"),
                        Map.entry("start_at", "timestamp with time zone"),
                        Map.entry("end_at", "timestamp with time zone"),
                        Map.entry("limite_places", "integer"),
                        Map.entry("status", "character varying"),
                        Map.entry("image_url", "character varying"),
                        Map.entry("tags", "ARRAY"),
                        Map.entry("visibility", "character varying"),
                        Map.entry("requires_registration", "boolean"),
                        Map.entry("has_checkin", "boolean"),
                        Map.entry("latitude", "double precision"),
                        Map.entry("longitude", "double precision"),
                        Map.entry("geofence_radius_m", "integer"),
                        Map.entry("stream_id", "uuid"),
                        Map.entry("compte_rendu", "text"),
                        Map.entry("created_at", "timestamp with time zone"),
                        Map.entry("updated_at", "timestamp with time zone"),
                        Map.entry("deleted_at", "timestamp with time zone")));
    }

    private List<String> colonnesReelles() {
        List<String> out = new ArrayList<>();
        for (Object colonne : em.createNativeQuery("""
                SELECT column_name FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'event'
                """).getResultList()) {
            out.add((String) colonne);
        }
        return out;
    }

    private Map<String, String> typesReelles() {
        Map<String, String> out = new LinkedHashMap<>();
        for (Object row : em.createNativeQuery("""
                SELECT column_name, data_type FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'event'
                """).getResultList()) {
            Object[] colonnes = (Object[]) row;
            out.put((String) colonnes[0], (String) colonnes[1]);
        }
        return out;
    }

    @Test
    @DisplayName("V203 : la preuve du pointage a le type que l'entite declare, lui aussi")
    void preuveDuPointageHasTheDeclaredType() {
        // Meme famille de derive que `event.latitude` : V201 avait depose la
        // preuve du pointage en `NUMERIC`, alors que `EventRegistration` la
        // mappe en `Double` et que `EventGeofence` calcule en double. C'est la
        // table dont se derive `nbInscrits`, donc elle est dans le perimetre de
        // ce chantier — la laisser deralee ferait echouer `validate` au meme
        // endroit, pour la meme raison.
        Map<String, String> types = new LinkedHashMap<>();
        for (Object row : em.createNativeQuery("""
                SELECT column_name, data_type FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'event_registrations'
                  AND column_name LIKE 'checkin%'
                """).getResultList()) {
            Object[] colonnes = (Object[]) row;
            types.put((String) colonnes[0], (String) colonnes[1]);
        }

        assertThat(types).containsAllEntriesOf(Map.of(
                "checkin_latitude", "double precision",
                "checkin_longitude", "double precision",
                "checkin_accuracy_m", "double precision",
                "checkin_distance_m", "double precision"));
    }

    @Test
    @DisplayName("aller-retour complet : on ecrit un evenement du vocabulaire produit, on le relit")
    void writeThenReadBackAProductVocabularyEvent() {
        Event saved = eventRepository.save(Event.builder()
                .tenantId(tenantId)
                .organisateurId(organizerId)
                .titre("Culte dominical")
                .description("Celebration hebdomadaire")
                .lieu("Eglise principale")
                .typeEvenement("CULTE")
                .statut(Event.STATUT_PLANIFIE)
                .dateDebut(LocalDateTime.of(2026, 10, 4, 9, 0))
                .dateFin(LocalDateTime.of(2026, 10, 4, 12, 0))
                .limitePlaces(500)
                .tags(new String[]{"dimanche", "famille"})
                .latitude(3.848000)
                .longitude(15.502000)
                .geofenceRadiusMeters(300)
                .build());
        em.flush();
        em.clear();

        Event relu = eventRepository.findById(saved.getId()).orElseThrow();

        assertThat(relu.getTitre()).isEqualTo("Culte dominical");
        assertThat(relu.getTypeEvenement()).isEqualTo("CULTE");
        assertThat(relu.getStatut()).isEqualTo(Event.STATUT_PLANIFIE);
        // La geolocalisation est precisement ce que V201 avait voulu poser sur
        // une table qui n'existait plus : elle doit survivre au round-trip.
        assertThat(relu.getLatitude()).isEqualTo(3.848000);
        assertThat(relu.getLongitude()).isEqualTo(15.502000);
        assertThat(relu.getGeofenceRadiusMeters()).isEqualTo(300);
        assertThat(relu.getTags()).containsExactly("dimanche", "famille");
        // Le fuseau : l'entite ecrit de l'UTC dans un `timestamptz`. Le
        // round-trip doit rendre la MEME heure qu'a l'ecriture, quelle que soit
        // l'heure de session du serveur — sinon un evenement change de date
        // selon qui le sert.
        assertThat(relu.getDateDebut()).isEqualTo(LocalDateTime.of(2026, 10, 4, 9, 0));
    }

    @Test
    @DisplayName("V203 : le perimetre famille existe et alimente la liste filtree")
    void perimetreEstPersisteEtFiltrable() {
        Event scoped = eventRepository.save(Event.builder()
                .tenantId(tenantId)
                .organisateurId(organizerId)
                .titre("Reunion de famille")
                .typeEvenement("REUNION")
                .statut(Event.STATUT_PLANIFIE)
                .familleId(familleId)
                .dateDebut(LocalDateTime.of(2026, 10, 5, 18, 0))
                .build());
        eventRepository.save(Event.builder()
                .tenantId(tenantId)
                .organisateurId(organizerId)
                .titre("Evenement d eglise, sans famille")
                .typeEvenement("CULTE")
                .statut(Event.STATUT_PLANIFIE)
                .dateDebut(LocalDateTime.of(2026, 10, 5, 19, 0))
                .build());
        em.flush();
        em.clear();

        // C'est la liste « evenements de ma famille » que consomme
        // EventController#/department/{id} et MemberService : sans la colonne,
        // elle n'existerait pas.
        assertThat(eventRepository.findByFamilleIdAndDeletedAtIsNull(familleId, PageRequest.of(0, 10))
                .getContent())
                .extracting(Event::getTitre)
                .containsExactly("Reunion de famille");
        assertThat(scoped.getFamilleId()).isEqualTo(familleId);
    }

    @Test
    @DisplayName("V203 : le vocabulaire de la table est celui du produit (13 types, 4 statuts)")
    void tableVocabularyIsTheProductOne() {
        // Les 13 codes du dictionnaire EVENT_TYPE (V42) — dont CULTE,
        // ETUDE_BIBLIQUE, VEILLEE et PRIERE, absents de la CHECK de V3 et qui
        // donnaient un 500 a la creation. C'est ce que V62 avait corrige sur la
        // table morte ; V203 le porte sur la table vivante.
        for (String type : List.of(
                "SORTIE", "RETRAITE", "EVANGELISATION", "REUNION", "VISITE",
                "CONFERENCE", "FORMATION", "ANNIVERSAIRE",
                "CULTE", "ETUDE_BIBLIQUE", "VEILLEE", "PRIERE", "AUTRE")) {
            UUID id = (UUID) em.createNativeQuery("""
                    INSERT INTO event (tenant_id, organizer_id, title, type, status, start_at, visibility)
                    VALUES (:tenant, :org, 'Essai de type', :type, 'PLANIFIE',
                            TIMESTAMPTZ '2026-11-01 10:00:00+00', 'CHURCH')
                    RETURNING id
                    """)
                    .setParameter("tenant", tenantId)
                    .setParameter("org", organizerId)
                    .setParameter("type", type)
                    .getSingleResult();
            assertThat(id).as("type %s refuse a l'insertion", type).isNotNull();
        }

        for (String statut : List.of("PLANIFIE", "EN_COURS", "TERMINE", "ANNULE")) {
            UUID id = (UUID) em.createNativeQuery("""
                    INSERT INTO event (tenant_id, organizer_id, title, type, status, start_at, visibility)
                    VALUES (:tenant, :org, 'Essai de statut', 'REUNION', :statut,
                            TIMESTAMPTZ '2026-11-02 10:00:00+00', 'CHURCH')
                    RETURNING id
                    """)
                    .setParameter("tenant", tenantId)
                    .setParameter("org", organizerId)
                    .setParameter("statut", statut)
                    .getSingleResult();
            assertThat(id).as("statut %s refuse a l'insertion", statut).isNotNull();
        }
    }

    @Test
    @DisplayName("V203 : la contrainte REFUSE le vocabulaire etranger de V158")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void constraintRejectsForeignVocabulary() {
        // Le controle doit etre effectif. Si la constraint avait ete elargie par
        // megarde, ce test passerait malgre tout et le module accepterait des
        // valeurs que ni le dictionnaire ni les clients ne connaissent — donc
        // on verifie le refus, pas seulement l'acceptation.
        //
        // `NOT_SUPPORTED` : chaque insertion doit etre autonome. Dans une
        // transaction, PostgreSQL abandonne des la premiere violation
        // (« current transaction is aborted ») et les suivantes echoueraient
        // pour cette raison-la, non parce que la contrainte les a refusees. Le
        // test passerait alors a cote de la regle qu'il pretend verifier.
        assertRefused("DRAFT", "REUNION");
        assertRefused("PUBLISHED", "REUNION");
        assertRefused("PLANIFIE", "MEETING");
        assertRefused("PLANIFIE", "SERVICE");
    }

    private void assertRefused(String statut, String type) {
        assertThatThrownBy(() -> em.createNativeQuery("""
                INSERT INTO event (tenant_id, organizer_id, title, type, status, start_at, visibility)
                VALUES (:tenant, :org, 'Doit echouer', :type, :statut,
                        TIMESTAMPTZ '2026-11-03 10:00:00+00', 'CHURCH')
                """)
                .setParameter("tenant", tenantId)
                .setParameter("org", organizerId)
                .setParameter("type", type)
                .setParameter("statut", statut)
                .executeUpdate())
                .as("la contrainte doit refuser type=%s / status=%s", type, statut)
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("nb_inscrits, is_public et deleted ne sont pas des colonnes : une seule verite chacune")
    void noColumnDuplicatesASingleTruth() {
        // Trois colonnes candidates a une double verite. Chacune se
        // reinscrirait d'elle-meme au prochain deploiement et divergerait de la
        // source qui fait autorite : le compteur (event_registrations), la
        // visibilite (visibility), la suppression (deleted_at). Le test le
        // verifie par leur absence.
        Object n = em.createNativeQuery("""
                SELECT count(*) FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'event'
                  AND column_name IN ('nb_inscrits', 'is_public', 'deleted')
                """).getSingleResult();

        assertThat(((Number) n).longValue())
                .as("colonne qui duplique une verite deja portee par une autre")
                .isZero();
    }

    @Test
    @DisplayName("le compteur derive suit les inscriptions, et survit a leur suppression")
    void derivedCountTracksRegistrations() {
        Event e = eventRepository.save(Event.builder()
                .tenantId(tenantId)
                .organisateurId(organizerId)
                .titre("Evenement compte")
                .typeEvenement("REUNION")
                .statut(Event.STATUT_PLANIFIE)
                .dateDebut(LocalDateTime.of(2026, 10, 6, 20, 0))
                .build());
        em.flush();

        assertThat(registrationRepository.countByEventId(e.getId()))
                .as("un evenement sans inscription vaut zero, pas une donnee manquante")
                .isZero();

        registrationRepository.save(EventRegistration.builder()
                .tenantId(tenantId)
                .eventId(e.getId())
                .utilisateurId(organizerId)
                .statutInscription("INSCRIT")
                .build());
        em.flush();
        assertThat(registrationRepository.countByEventId(e.getId())).isEqualTo(1);

        em.createNativeQuery("DELETE FROM event_registrations WHERE event_id = :id")
                .setParameter("id", e.getId())
                .executeUpdate();
        em.flush();
        assertThat(registrationRepository.countByEventId(e.getId()))
                .as("le compteur se recompose : plus rien a resynchroniser a la main")
                .isZero();
    }
}
