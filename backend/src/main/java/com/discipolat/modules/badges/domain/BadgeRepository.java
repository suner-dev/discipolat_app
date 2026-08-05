package com.discipolat.modules.badges.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BadgeRepository extends JpaRepository<Badge, UUID> {
    List<Badge> findByActifTrueOrderBySeuilAsc();
    List<Badge> findByCritereOrderBySeuilAsc(Badge.Critere critere);
}
