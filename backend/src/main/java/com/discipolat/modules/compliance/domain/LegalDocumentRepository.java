package com.discipolat.modules.compliance.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LegalDocumentRepository extends JpaRepository<LegalDocument, UUID> {

    Optional<LegalDocument> findFirstByCodeAndLanguageAndPublishedTrueOrderByVersionDesc(String code, String language);

    Optional<LegalDocument> findByCodeAndVersionAndLanguage(String code, Integer version, String language);

    @Query("SELECT d FROM LegalDocument d WHERE d.published = true "
            + "AND d.version = (SELECT MAX(d2.version) FROM LegalDocument d2 "
            + "WHERE d2.code = d.code AND d2.language = d.language AND d2.published = true) "
            + "ORDER BY d.code ASC")
    List<LegalDocument> findLatestPublished();

    List<LegalDocument> findByCodeAndLanguageOrderByVersionDesc(String code, String language);

    boolean existsByCodeAndVersionAndLanguage(String code, Integer version, String language);
}
