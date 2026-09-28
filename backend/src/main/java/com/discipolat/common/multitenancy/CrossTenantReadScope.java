package com.discipolat.common.multitenancy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Exécution d'une lecture <b>cross-tenant</b> explicitement déclarée.
 *
 * <h2>Pourquoi ce composant existe (constat H4)</h2>
 *
 * <p>{@link TenantFilter} active pour chaque requête HTTP un filtre Hibernate
 * {@code tenantFilter} qui ajoute {@code AND tenant_id = :tenantId} à <b>toutes</b>
 * les requêtes sur les entités qui l'ont déclaré. C'est exactement le comportement
 * voulu pour les données d'un tenant — mais le filtre s'applique aussi aux
 * <b>contrôles de sécurité qui doivent, eux, traverser les tenants</b>.
 *
 * <p>Conséquence mesurée sur le sélecteur d'organisation : la vérification
 * d'accès de {@code POST /api/v1/tenant-switcher/switch} produit
 *
 * <pre>
 * select tm1_0.id from tenant_memberships tm1_0
 *  where tm1_0.tenant_id = ?   -- tenant COURANT, injecté par le filtre
 *    and tm1_0.user_id   = ?
 *    and tm1_0.tenant_id = ?   -- le tenant DEMANDÉ
 *    and tm1_0.status    = ?
 * </pre>
 *
 * <p>Le prédicat ne peut donc être vrai que si le tenant demandé est <b>déjà</b>
 * le tenant courant : basculer vers une autre église était structurellement
 * impossible, et le sélecteur d'organisation du constat B2 n'était pas delivered.
 *
 * <h2>Contrat de sécurité — à lire avant d'utiliser ce composant</h2>
 *
 * <p>Désactiver le filtre élargit l'ensemble des lignes lues. Ce n'est acceptable
 * que si la requête est <b>paramétrée par l'identifiant de l'utilisateur
 * authentifié</b> : l'élargissement porte alors sur les seules lignes qui
 * appartiennent à ce même utilisateur, et n'ouvre aucun accès aux données d'un
 * tiers. Ce composant ne doit donc <b>jamais</b> servir à lire les données d'un
 * tenant pour le compte d'un simple identifiant de tenant fourni par le client :
 * ce cas doit passer par un contrôle d'accès explicite, pas par une désactivation
 * du filtre.
 *
 * <p>Le filtre est rétabli dans un {@code finally}, y compris en cas d'exception :
 * une fuite du contexte filtré vers la suite de la requête est impossible.
 *
 * <p>Enfin, la portée est <b>limitée au bloc exécuté</b> et non à la requête
 * entière : dès que le bloc rend la main, l'isolation multi-tenant est de nouveau
 * active pour tout le reste du traitement.
 */
@Component
public class CrossTenantReadScope {

    private static final Logger log = LoggerFactory.getLogger(CrossTenantReadScope.class);
    private static final String FILTER_NAME = "tenantFilter";
    private static final String TENANT_ID_PARAM = "tenantId";

    /**
     * Résolution directe de l'EntityManager : comme pour {@link TenantFilter},
     * on ne passe pas par un proxy {@code @PersistenceContext} dont la session
     * pourrait différer de celle utilisée par les repositories. Ici le point de
     * départ est le contrôleur, donc <b>après</b> tous les intercepteurs : le
     * proxy serait valide, mais la résolution explicite reste la plus sûr si
     * l'ordre d'exécution change un jour.
     */
    @PersistenceContext
    private EntityManager entityManager;

    /** Exécute {@code read} avec le filtre multi-tenant désactivé. */
    public <T> T call(Supplier<T> read) {
        SuspendedFilter suspended = suspendFilter();
        try {
            return read.get();
        } finally {
            resumeFilter(suspended);
        }
    }

    /**
     * Filtre temporairement désactivé : le tenant à lui rendre, et un drapeau
     * indiquant s'il y en avait un. {@code null} = rien à rétablir.
     */
    private record SuspendedFilter(UUID tenantId) {
        static final SuspendedFilter NONE = new SuspendedFilter(null);
    }

    private SuspendedFilter suspendFilter() {
        Session session = entityManager.unwrap(Session.class);
        if (session.getEnabledFilter(FILTER_NAME) == null) {
            return SuspendedFilter.NONE;
        }
        session.disableFilter(FILTER_NAME);
        log.debug("Filtre multi-tenant suspendu pour une lecture cross-tenant (tenant courant {})",
                TenantContext.getTenantId());
        return new SuspendedFilter(TenantContext.getTenantId());
    }

    private void resumeFilter(SuspendedFilter suspended) {
        if (suspended.tenantId() == null) {
            return;
        }
        try {
            entityManager.unwrap(Session.class)
                    .enableFilter(FILTER_NAME)
                    .setParameter(TENANT_ID_PARAM, suspended.tenantId());
        } catch (RuntimeException failure) {
            // La session peut être close si le bloc a ouvert une transaction
            // rollbackée : le filtre meurt avec elle, il n'y a rien à rétablir.
            log.debug("Filtre multi-tenant non rétabli (session terminée) : {}", failure.getMessage());
        }
    }
}
