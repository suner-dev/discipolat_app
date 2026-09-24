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

    Optional<User> findByTenantIdAndEmail(UUID tenantId, String email);

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

    @Query(value = "SELECT COUNT(*) FROM users WHERE tenant_id = :tenantId AND statut = :status", nativeQuery = true)
    long countByTenantIdAndStatut(@Param("tenantId") UUID tenantId, @Param("status") UserStatus status);

    long countByStatut(UserStatus status);

    List<User> findByTenantId(UUID tenantId);

    Page<User> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);
}
