package com.discipolat.modules.users.domain;

import com.discipolat.common.domain.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    /**
     * Recherche par email INSENSIBLE A LA CASSE (constat B4 / migration V185).
     * C'est la methode de reference pour toute resolution d'identite par email :
     * `findByEmail` est sensible a la casse et peut renvoyer un compte arbitraire
     * si deux emails ne different que par leur casse.
     *
     * <p>Volontairement enrequete NATIVE (comme {@code findGlobalByEmail}) : l'email
     * est une identite GLOBALE, la resolution ne doit donc jamais etre restreinte
     * par le filtre Hibernate multi-tenant {@code tenantFilter} (actif des qu'un
     * TenantContext est pose). Une requete JPQL serait filtree et pourrait echouer
     * sur un email d'un autre tenant, ou pire, depandre de l'absence de contexte.
     */
    @Query(value = "SELECT * FROM users WHERE LOWER(email) = LOWER(:email) AND deleted = false", nativeQuery = true)
    Optional<User> findByEmailIgnoreCase(@Param("email") String email);

    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END FROM users "
            + "WHERE LOWER(email) = LOWER(:email) AND deleted = false", nativeQuery = true)
    boolean existsByEmailIgnoreCase(@Param("email") String email);

    Optional<User> findByTenantIdAndEmail(UUID tenantId, String email);

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND LOWER(u.email) = LOWER(:email) AND u.deleted = false")
    Optional<User> findByTenantIdAndEmailIgnoreCase(@Param("tenantId") UUID tenantId, @Param("email") String email);

    /**
     * Recherche GLOBALE (tous tenants confondus) par email insensible a la casse.
     * Aligne sur l'index unique `uk_users_email_lower` de la migration V185 :
     * seules les lignes actives sont considerees.
     */
    @Query(value = "SELECT * FROM users WHERE LOWER(email) = LOWER(:email) AND deleted = false", nativeQuery = true)
    Optional<User> findGlobalByEmailIgnoreCase(@Param("email") String email);

    @Query(value = "SELECT * FROM users WHERE email = :email", nativeQuery = true)
    Optional<User> findGlobalByEmail(@Param("email") String email);

    // Legacy single-role queries (still work for basic lookups)
    List<User> findByRole(UserRole role);

    Page<User> findByRole(UserRole role, Pageable pageable);

    // Multi-role queries
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role")
    List<User> findByRolesContaining(@Param("role") UserRole role);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role")
    Page<User> findByRolesContaining(@Param("role") UserRole role, Pageable pageable);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r IN :roles")
    List<User> findByRolesIn(@Param("roles") Set<UserRole> roles);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r = :role")
    long countByRolesContaining(@Param("role") UserRole role);

    List<User> findByFamilleGereeId(UUID familleId);

    boolean existsByEmail(String email);

    List<User> findByEstChefDeFamilleTrue();

    long countByRole(UserRole role);

    Optional<User> findByFamilleGereeIdAndEstChefDeFamilleTrue(UUID familleId);

    List<User> findByTenantIdAndWhatsappOptInTrue(UUID tenantId);

    Optional<User> findByTenantIdAndPhone(UUID tenantId, String phone);

    @Query(value = "SELECT COUNT(*) FROM users WHERE tenant_id = :tenantId", nativeQuery = true)
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query(value = "SELECT COUNT(*) FROM users WHERE tenant_id = :tenantId AND deleted = false", nativeQuery = true)
    long countByTenantIdAndDeletedFalse(@Param("tenantId") UUID tenantId);

    /**
     * Constat H7 : dans une requête NATIVE, l'annotation
     * {@code @Enumerated(STRING)} de l'entité ne s'applique pas — Hibernate liait
     * l'ordinal de {@link UserStatus} alors que la colonne {@code statut} est un
     * {@code varchar}, ce que PostgreSQL refusait
     * ({@code operator does not exist: character varying = smallint}).
     *
     * <p>Impact mesuré : les compteurs d'utilisateurs des DEUX tableaux de bord
     * principaux (Super Admin et admin tenant) renvoyaient 500. La comparaison
     * porte donc sur le NOM de l'énumère, et la surcharge garde l'API publique
     * en enum pour tous les appelants existants.
     */
    @Query(value = "SELECT COUNT(*) FROM users WHERE tenant_id = :tenantId AND statut = :status", nativeQuery = true)
    long countByTenantIdAndStatutName(@Param("tenantId") UUID tenantId, @Param("status") String status);

    default long countByTenantIdAndStatut(UUID tenantId, UserStatus status) {
        return countByTenantIdAndStatutName(tenantId, status.name());
    }

    long countByStatut(UserStatus status);

    List<User> findByTenantId(UUID tenantId);

    Page<User> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);
}
