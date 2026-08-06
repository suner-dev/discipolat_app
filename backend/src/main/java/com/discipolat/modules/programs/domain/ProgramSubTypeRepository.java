package com.discipolat.modules.programs.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProgramSubTypeRepository extends JpaRepository<ProgramSubType, UUID> {
    List<ProgramSubType> findByProgramTypeIdAndActifTrueOrderByOrdreAsc(UUID programTypeId);
    List<ProgramSubType> findByProgramTypeIdOrderByOrdreAsc(UUID programTypeId);
    void deleteByProgramTypeId(UUID programTypeId);
}
