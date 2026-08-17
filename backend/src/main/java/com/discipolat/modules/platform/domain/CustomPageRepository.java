package com.discipolat.modules.platform.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomPageRepository extends JpaRepository<CustomPage, UUID> {

    Optional<CustomPage> findBySlug(String slug);

    Optional<CustomPage> findByKey(String key);

    List<CustomPage> findAllByOrderByTitleAsc();

    List<CustomPage> findByEnabledTrueAndPublishedTrueOrderByTitleAsc();

    boolean existsBySlug(String slug);

    boolean existsByKey(String key);
}
