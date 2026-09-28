package com.discipolat.modules.tenants.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invitation i WHERE i.id = :id")
    Optional<Invitation> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invitation i WHERE i.tokenHash = :tokenHash")
    Optional<Invitation> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    Optional<Invitation> findByEmailAndTenantIdAndStatus(String email, UUID tenantId, InvitationStatus status);

    Optional<Invitation> findByTenantIdAndEmailAndStatus(UUID tenantId, String email, InvitationStatus status);

    List<Invitation> findByTenantIdAndStatusIn(UUID tenantId, List<InvitationStatus> statuses);

    List<Invitation> findByTenantId(UUID tenantId);

    Optional<Invitation> findByEmailAndTenantId(String email, UUID tenantId);

    long countByTenantIdAndStatus(UUID tenantId, InvitationStatus status);

    List<Invitation> findByExpiresAtAfter(Instant date);

    List<Invitation> findByTenantIdAndExpiresAtBefore(UUID tenantId, Instant date);

    /**
     * Constat M4 — invitations PENDING dont l'expiration tombe dans la fenêtre
     * [from, to). Utilisé par le scheduler de relance (J-3 et J-1).
     */
    List<Invitation> findByStatusAndExpiresAtBetween(InvitationStatus status, Instant from, Instant to);

    /**
     * Recherche paginée des invitations d'un tenant, avec filtres optionnels
     * (statut, recherche partielle sur l'email).
     *
     * <p>Requête native explicite plutôt qu'un {@code Specification} : les
     * filtres sont combinés par COALESCE pour qu'un filtre nul n'écarte aucune
     * ligne, et la recherche email est normalisée en minuscules comme partout
     * ailleurs dans l'application.
     *
     * @param status statut filtré, ou {@code null} pour tous
     * @param query  fragment d'email recherché, ou {@code null}
     */
    @Query(value = """
            SELECT i.* FROM invitations i
            WHERE i.tenant_id = :tenantId
              AND (CAST(:status AS VARCHAR) IS NULL OR i.status = :status)
              AND (CAST(:query AS VARCHAR) IS NULL OR LOWER(i.email) LIKE LOWER('%' || :query || '%'))
            ORDER BY i.created_at DESC
            """, countQuery = """
            SELECT COUNT(*) FROM invitations i
            WHERE i.tenant_id = :tenantId
              AND (CAST(:status AS VARCHAR) IS NULL OR i.status = :status)
              AND (CAST(:query AS VARCHAR) IS NULL OR LOWER(i.email) LIKE LOWER('%' || :query || '%'))
            """, nativeQuery = true)
    Page<Invitation> searchForAdmin(@Param("tenantId") UUID tenantId,
                                    @Param("status") String status,
                                    @Param("query") String query,
                                    Pageable pageable);
}
