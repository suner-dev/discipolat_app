package com.discipolat.modules.platform.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlatformModuleRepository extends JpaRepository<PlatformModule, String> {

    List<PlatformModule> findAllByOrderByOrdreAsc();

    List<PlatformModule> findByEnabledFalse();
}
