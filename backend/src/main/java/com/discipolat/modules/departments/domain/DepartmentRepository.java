package com.discipolat.modules.departments.domain;

import com.discipolat.common.enums.StatutEntite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    Optional<Department> findByNom(String nom);
    Page<Department> findByStatut(StatutEntite statut, Pageable pageable);
    List<Department> findByResponsableId(UUID responsableId);
    long countByStatut(StatutEntite statut);
    Page<Department> findAllByIdIn(java.util.List<UUID> ids, Pageable pageable);

    List<Department> findAllByIdIn(java.util.Collection<UUID> ids);
}
