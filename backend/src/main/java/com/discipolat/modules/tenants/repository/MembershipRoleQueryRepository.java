package com.discipolat.modules.tenants.repository;

import com.discipolat.modules.tenants.domain.MembershipStatus;
import com.discipolat.modules.tenants.domain.TenantMembership;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

/**
 * PARAMÉTRISATION PAR ÉGLISE (Phase 9) — lectures d'appartenances guidées par le
 * RÔLE, publiées par le contexte {@code tenants}.
 *
 * <p><strong>Pourquoi ce dépôt vit ici et non dans {@code tenants.domain}.</strong>
 * Les règles R1/R4 d'ArchUnit gèlent la dette du domaine <em>par compte</em> : le
 * fichier {@code src/test/resources/architecture/architecture-freeze.txt} est un
 * plafond, pas un blanc-seing. Or toute méthode annotée {@code @Query}/{@code
 * @Param} dans un paquetage {@code ..domain..} ajoute des dépendances de
 * Spring&nbsp;Data à du domaine, donc des violations nouvelles. Ces trois lectures
 * étant exactement cela — du câblage de persistance, pas du métier — elles sont
 * déposées dans la couche adaptateur du même contexte, ce qui laisse le compteur
 * du domaine inchangé et suit la direction visée par l'ADR-002 (les dépôts
 * appartiennent aux adaptateurs). Le domaine {@code RoleManagementService} reste
 * seul appelant, donc aucun nouveau bord entre contextes n'est créé (règle R3).
 *
 * <p>Interface en lecture seule héritant de {@link Repository} : Spring Data ne
 * publie que les méthodes déclarées ici, aucun CRUD accidentuel n'est exposé.
 */
public interface MembershipRoleQueryRepository extends Repository<TenantMembership, UUID> {

    /**
     * Les comptes dont la matrice vient de changer pour une raison liée à un RÔLE,
     * dans un tenant donné.
     *
     * <p>La sélection se fait sur la <b>clé</b> du rôle et non sur son identifiant,
     * parce que c'est la clé que résout le moteur d'autorisation : un membre
     * rattaché à la ligne globale {@code ADMIN} et un membre rattaché à la
     * surcouche locale homonyme de la même église ont tous deux leurs droits
     * redéfinis quand l'église édite sa copie. Un filtre par identifiant en
     * oublierait systématiquement la moitié.
     */
    @Query("SELECT DISTINCT tm.userId FROM TenantMembership tm WHERE tm.tenantId = :tenantId "
            + "AND tm.status = :status AND tm.role.key = :roleKey")
    List<UUID> findActiveUserIdsByTenantAndRoleKey(@Param("tenantId") UUID tenantId,
                                                   @Param("status") MembershipStatus status,
                                                   @Param("roleKey") String roleKey);

    /**
     * Idem, sans restriction de tenant : l'édition d'un rôle GLOBAL
     * ({@code tenant_id IS NULL}) touche d'un coup toutes les organisations qui
     * l'utilisent. Paire {@code (tenantId, userId)}.
     */
    @Query("SELECT DISTINCT tm.tenantId, tm.userId FROM TenantMembership tm WHERE tm.status = :status "
            + "AND tm.role.key = :roleKey ORDER BY tm.tenantId, tm.userId")
    List<Object[]> findActiveHoldersByRoleKey(@Param("status") MembershipStatus status,
                                              @Param("roleKey") String roleKey);

    /**
     * Appartenances référencant une ligne {@code roles} précise : nécessaire pour
     * ré-ancrer les porteurs sur la ligne globale quand l'église supprime sa
     * surcouche (sinon la FK {@code tenant_memberships.role_id} devient orpheline
     * et la suppression est refusée par {@code ROLE_IN_USE}).
     */
    @Query("SELECT tm FROM TenantMembership tm WHERE tm.role.id = :roleId")
    List<TenantMembership> findAllByRoleId(@Param("roleId") UUID roleId);
}
