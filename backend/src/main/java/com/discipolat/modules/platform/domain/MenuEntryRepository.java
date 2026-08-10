package com.discipolat.modules.platform.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MenuEntryRepository extends JpaRepository<MenuEntry, UUID> {

    List<MenuEntry> findAllByOrderBySectionAscOrdreAsc();

    List<MenuEntry> findByEnabledTrueOrderBySectionAscOrdreAsc();

    List<MenuEntry> findByModuleKeyInAndEnabledTrue(List<String> moduleKeys);
}
