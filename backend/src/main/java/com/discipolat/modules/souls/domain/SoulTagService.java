package com.discipolat.modules.souls.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Tags libres sur les âmes : annotation libre pour le CRM et le filtrage.
 * Accès restreint aux rôles d'encadrement (contrôlé au niveau du contrôleur).
 */
@Service
@Transactional
public class SoulTagService {

    private final SoulTagRepository soulTagRepository;
    private final SoulRepository soulRepository;

    public SoulTagService(SoulTagRepository soulTagRepository, SoulRepository soulRepository) {
        this.soulTagRepository = soulTagRepository;
        this.soulRepository = soulRepository;
    }

    @Transactional(readOnly = true)
    public List<String> getTags(UUID soulId) {
        return soulTagRepository.findBySoulIdOrderByTagAsc(soulId)
                .stream().map(SoulTag::getTag).toList();
    }

    /** Ajoute un tag à une âme (normalisé : minuscules, max 3 tags). */
    public List<String> addTag(UUID soulId, String rawTag) {
        ensureSoulExists(soulId);
        String tag = normalize(rawTag);
        if (tag.isBlank()) throw new BadRequestException("Le tag ne peut pas être vide");

        long count = soulTagRepository.findBySoulIdOrderByTagAsc(soulId).size();
        if (count >= 3) {
            throw new BadRequestException("Maximum 3 tags par âme (supprimez un tag avant d'en ajouter un autre)");
        }
        if (soulTagRepository.findBySoulIdAndTagIgnoreCase(soulId, tag).isEmpty()) {
            soulTagRepository.save(SoulTag.builder().soulId(soulId).tag(tag).build());
        }
        return getTags(soulId);
    }

    public List<String> removeTag(UUID soulId, String rawTag) {
        ensureSoulExists(soulId);
        soulTagRepository.deleteBySoulIdAndTagIgnoreCase(soulId, normalize(rawTag));
        return getTags(soulId);
    }

    @Transactional(readOnly = true)
    public List<String> listAllTags() {
        return soulTagRepository.findAllTags();
    }

    private String normalize(String tag) {
        return tag.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private void ensureSoulExists(UUID soulId) {
        if (!soulRepository.existsById(soulId)) {
            throw new EntityNotFoundException("Soul", soulId);
        }
    }
}
