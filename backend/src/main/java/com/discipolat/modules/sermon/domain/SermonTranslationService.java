package com.discipolat.modules.sermon.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SermonTranslationService {

    private final SermonTranslationRepository repo;

    public SermonTranslationService(SermonTranslationRepository repo) {
        this.repo = repo;
    }

    public List<SermonTranslation> listBySermon(UUID sermonId) {
        return repo.findBySermonIdOrderByCreeLeDesc(sermonId);
    }

    public List<SermonTranslation> listAll() {
        return repo.findByTenantIdOrderByCreeLeDesc(TenantContext.getCurrentTenantId());
    }

    public SermonTranslation get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("SermonTranslation", id));
    }

    /**
     * Initie la traduction d'un sermon (simule Whisper + LLM en attendant vrai pipeline).
     * En production, appellerait Whisper pour transcription puis LLM pour traduction.
     */
    public SermonTranslation requestTranslation(UUID sermonId, SermonTranslation.Langue langue) {
        SermonTranslation st = new SermonTranslation();
        st.setTenantId(TenantContext.getCurrentTenantId());
        st.setSermonId(sermonId);
        st.setLangueCible(langue);
        st.setStatut(SermonTranslation.Statut.EN_COURS);
        // Simulation: en production on déclencherait Whisper + LLM ici
        st.setConfiance(0.85);
        return repo.save(st);
    }

    /** Simule la fin de traduction (serait déclenché par un job async) */
    public SermonTranslation completeTranslation(UUID id, String translatedText, String subtitlesJson) {
        SermonTranslation st = get(id);
        st.setTraductionTexte(translatedText);
        st.setSubtitlesJson(subtitlesJson);
        st.setStatut(SermonTranslation.Statut.TERMINE);
        st.setTermineLe(LocalDateTime.now());
        return repo.save(st);
    }
}
