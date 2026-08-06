package com.discipolat.modules.families.domain;

import com.discipolat.common.enums.StatutEntite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FamilyRepository extends JpaRepository<Family, UUID> {
    // DepartementId methods REMOVED - families are now independent from departments
    // Use soul_departments table instead for member-department relationships

    Page<Family> findByChefFamilleId(UUID chefFamilleId, Pageable pageable);
    List<Family> findByChefFamilleId(UUID chefFamilleId);
    Page<Family> findByChefAdjointId(UUID chefAdjointId, Pageable pageable);
    List<Family> findByChefAdjointId(UUID chefAdjointId);
    Page<Family> findAllByStatut(StatutEntite statut, Pageable pageable);
    java.util.Optional<Family> findByNom(String nom);
    List<Family> findByUserId(UUID userId);
}
