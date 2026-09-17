package com.discipolat.modules.config.repository;

import com.discipolat.modules.config.domain.SpaceTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpaceTemplateRepository extends JpaRepository<SpaceTemplate, UUID> {

    Optional<SpaceTemplate> findByCode(String code);

    boolean existsByCode(String code);

    List<SpaceTemplate> findAllByOrderByNameAsc();
}