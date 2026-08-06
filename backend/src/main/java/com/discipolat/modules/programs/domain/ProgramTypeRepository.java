package com.discipolat.modules.programs.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProgramTypeRepository extends JpaRepository<ProgramType, UUID> {
    List<ProgramType> findByActifTrueOrderByOrdreAsc();
    List<ProgramType> findAllByOrderByOrdreAsc();
    Optional<ProgramType> findByCode(String code);
}
