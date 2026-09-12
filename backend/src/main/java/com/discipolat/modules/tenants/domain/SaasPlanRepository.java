package com.discipolat.modules.tenants.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SaasPlanRepository extends JpaRepository<SaasPlan, String> {

    List<SaasPlan> findByIsActiveTrueOrderBySortOrderAsc();

    Optional<SaasPlan> findByKeyAndIsActiveTrue(String key);

    Optional<SaasPlan> findById(String id);
}