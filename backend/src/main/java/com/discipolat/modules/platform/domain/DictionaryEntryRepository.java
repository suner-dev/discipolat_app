package com.discipolat.modules.platform.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DictionaryEntryRepository extends JpaRepository<DictionaryEntry, UUID> {

    List<DictionaryEntry> findAllByOrderByDictKeyAscOrdreAsc();

    List<DictionaryEntry> findByActifTrueOrderByDictKeyAscOrdreAsc();

    Optional<DictionaryEntry> findByDictKeyAndCode(String dictKey, String code);

    List<DictionaryEntry> findByDictKeyOrderByOrdreAsc(String dictKey);
}
