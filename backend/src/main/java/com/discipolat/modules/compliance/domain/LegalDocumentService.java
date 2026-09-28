package com.discipolat.modules.compliance.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service des documents légaux versionnés (support RGPD art. 7 — preuve).
 */
@Service
@Transactional(readOnly = true)
public class LegalDocumentService {

    /** Codes légaux requis à la souscription. */
    public static final List<String> MANDATORY_CODES = List.of("CGU", "PRIVACY", "CONSENT_ART9");

    private final LegalDocumentRepository repository;

    public LegalDocumentService(LegalDocumentRepository repository) {
        this.repository = repository;
    }

    /** Dernière version publiée d'un document (404 métier si absente). */
    public LegalDocument getLatest(String code, String language) {
        String lang = normalizeLanguage(language);
        return repository.findFirstByCodeAndLanguageAndPublishedTrueOrderByVersionDesc(code, lang)
                .orElseThrow(() -> new EntityNotFoundException("LegalDocument:" + code, null));
    }

    /** Version historique précise (recherche de preuve de consentement). */
    public LegalDocument getVersion(String code, Integer version, String language) {
        String lang = normalizeLanguage(language);
        return repository.findByCodeAndVersionAndLanguage(code, version, lang)
                .orElseThrow(() -> new EntityNotFoundException("LegalDocument:" + code + " v" + version, null));
    }

    public Optional<LegalDocument> findVersion(String code, Integer version, String language) {
        return repository.findByCodeAndVersionAndLanguage(code, version, normalizeLanguage(language));
    }

    /** Catalogue public : dernière version de chaque document publié. */
    public List<LegalDocument> listLatest() {
        return repository.findLatestPublished();
    }

    /** Version actuellement exigée à l'inscription (max des versions publiées obligatoires). */
    public String currentTermsVersion() {
        int max = MANDATORY_CODES.stream()
                .map(code -> repository.findFirstByCodeAndLanguageAndPublishedTrueOrderByVersionDesc(code, "fr"))
                .filter(Optional::isPresent)
                .map(o -> o.get().getVersion())
                .max(Integer::compareTo)
                .orElse(1);
        return String.valueOf(max);
    }

    /**
     * Publie une nouvelle version d'un document (Super Admin plateforme).
     * Les versions sont immuables : on n'écrase jamais un texte déjà accepté.
     */
    @Transactional
    public LegalDocument publish(String code, String title, String content, String language) {
        String lang = normalizeLanguage(language);
        int nextVersion = repository.findByCodeAndLanguageOrderByVersionDesc(code, lang).stream()
                .map(LegalDocument::getVersion)
                .max(Integer::compareTo)
                .orElse(0) + 1;
        if (repository.existsByCodeAndVersionAndLanguage(code, nextVersion, lang)) {
            throw new BusinessRuleException("Cette version existe déjà", "LEGAL_VERSION_EXISTS");
        }
        return repository.save(LegalDocument.builder()
                .code(code.toUpperCase())
                .version(nextVersion)
                .language(lang)
                .title(title)
                .content(content)
                .published(true)
                .publishedAt(Instant.now())
                .build());
    }

    /** Résume le document pour l'API (sans surcharger la réponse de contenu sur la liste). */
    public Map<String, Object> toSummary(LegalDocument doc) {
        return Map.of(
                "code", doc.getCode(),
                "version", doc.getVersion(),
                "language", doc.getLanguage(),
                "title", doc.getTitle(),
                "publishedAt", doc.getPublishedAt().toString());
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) return "fr";
        return language.trim().toLowerCase().substring(0, Math.min(2, language.trim().length()));
    }
}
