package com.discipolat.modules.tenants.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * LOT 2 §GR — repository des affectations d'entrées de menu à un groupe.
 */
public interface NavigationGroupItemRepository extends JpaRepository<NavigationGroupItem, UUID> {

    List<NavigationGroupItem> findByGroupIdOrderByDisplayOrderAsc(UUID groupId);

    List<NavigationGroupItem> findByGroupIdInOrderByDisplayOrderAsc(List<UUID> groupIds);

    void deleteByGroupId(UUID groupId);
}