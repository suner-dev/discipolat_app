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

    Optional<Person> findByTenantIdAndPhoneNormalizedAndDeletedAtIsNull(UUID tenantId, String phone);

    List<Person> findByTenantIdAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc(UUID tenantId);

    Page<Person> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    @Query("SELECT p FROM Person p WHERE p.tenantId = :tenantId AND p.deletedAt IS NULL AND (LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.emailNormalized) LIKE LOWER(CONCAT('%', :search, '%'))) ORDER BY p.lastName, p.firstName")
    Page<Person> search(@Param("tenantId") UUID tenantId, @Param("search") String search, Pageable pageable);

    List<Person> findByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, String status);

    Page<Person> findByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, String status, Pageable pageable);

    long countByTenantIdAndDeletedAtIsNull(UUID tenantId);
}