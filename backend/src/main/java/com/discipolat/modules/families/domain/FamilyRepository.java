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
    Page<Family> findByDepartementId(UUID departementId, Pageable pageable);
    Page<Family> findByDepartementIdAndStatut(UUID departementId, StatutEntite statut, Pageable pageable);
    List<Family> findByDepartementId(UUID departementId);
    List<Family> findByChefFamilleId(UUID chefFamilleId);
    long countByDepartementIdAndStatut(UUID departementId, StatutEntite statut);
    Page<Family> findAllByStatut(StatutEntite statut, Pageable pageable);
    java.util.Optional<Family> findByNom(String nom);
}
