package com.discipolat.modules.people.repository;

import com.discipolat.modules.people.domain.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PersonRepository extends JpaRepository<Person, UUID> {

    Optional<Person> findByTenantIdAndEmailNormalizedAndDeletedAtIsNull(UUID tenantId, String email);

    Optional<Person> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Person> findByTenantIdAndPhoneNormalizedAndDeletedAtIsNull(UUID tenantId, String phone);

    /**
     * §G5.9 — Résolution cross-tenant par numéro (point d'entrée unique USSD/WhatsApp
     * avant connaissance du tenant). Requête NATIVE : volontairement hors du filtre
     * Hibernate tenantFilter, bornée par les variantes de numéro normalisées fournies.
     * L'appelant refuse l'ambiguïté (plusieurs tenants ⇒ refus).
     */
    @org.springframework.data.jpa.repository.Query(
            value = "SELECT * FROM person WHERE deleted_at IS NULL AND phone_normalized IN (:variants)",
            nativeQuery = true)
    java.util.List<Person> findLowBandByPhoneVariants(
            @org.springframework.data.repository.query.Param("variants") java.util.Collection<String> variants);

    List<Person> findByTenantIdAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc(UUID tenantId);

    Page<Person> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    @Query("SELECT p FROM Person p WHERE p.tenantId = :tenantId AND p.deletedAt IS NULL AND (LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.emailNormalized) LIKE LOWER(CONCAT('%', :search, '%'))) ORDER BY p.lastName, p.firstName")
    Page<Person> search(@Param("tenantId") UUID tenantId, @Param("search") String search, Pageable pageable);

    /**
     * §G6.5 — « sans espace » en UNE requête. L'implantation précédente
     * chargeait les 10 000 personnes puis interrogeait space_membership pour
     * chacune (N+1 : ~10 001 requêtes, plusieurs secondes en charge).
     * S'appuie sur idx_sm_person_status (V176).
     */
    @Query("""
            SELECT p FROM Person p
            WHERE p.tenantId = :tenantId AND p.deletedAt IS NULL
            AND NOT EXISTS (
                SELECT 1 FROM SpaceMembership sm
                WHERE sm.personId = p.id AND sm.status = 'ACTIVE')
            ORDER BY p.lastName ASC, p.firstName ASC
            """)
    List<Person> findWithoutActiveSpace(@Param("tenantId") UUID tenantId);

    List<Person> findByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, String status);

    Page<Person> findByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, String status, Pageable pageable);

    /**
     * §G6.4/§G3.1 — Filtres réels « sans espace » / « sans famille » (l'ancien
     * TODO ignorait les flags : les listes d'affectation étaient fausses).
     * « En famille » = le compte lié (email) chef ou adjoint d'une famille active.
     */
    @Query("""
            SELECT p FROM Person p
            WHERE p.tenantId = :tenantId AND p.deletedAt IS NULL
            AND (:withoutSpace = false OR NOT EXISTS (
                SELECT sm FROM SpaceMembership sm
                WHERE sm.personId = p.id AND sm.status = 'ACTIVE'))
            AND (:withoutFamily = false OR (
                NOT EXISTS (
                    SELECT 1 FROM com.discipolat.modules.families.domain.Family f
                    JOIN com.discipolat.modules.users.domain.User uc ON uc.id = f.chefFamilleId
                    WHERE f.deleted = false AND LOWER(uc.email) = LOWER(p.emailNormalized))
                AND NOT EXISTS (
                    SELECT 1 FROM com.discipolat.modules.families.domain.Family f2
                    JOIN com.discipolat.modules.users.domain.User ua ON ua.id = f2.chefAdjointId
                    WHERE f2.deleted = false AND LOWER(ua.email) = LOWER(p.emailNormalized))))
            ORDER BY p.lastName ASC, p.firstName ASC
            """)
    Page<Person> findFiltered(@Param("tenantId") UUID tenantId,
                              @Param("withoutSpace") boolean withoutSpace,
                              @Param("withoutFamily") boolean withoutFamily,
                              Pageable pageable);

    long countByTenantIdAndDeletedAtIsNull(UUID tenantId);
}