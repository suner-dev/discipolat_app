package com.discipolat.modules.souls.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SoulNoteService {

    private final SoulNoteRepository soulNoteRepository;
    private final SecurityUtils securityUtils;
    private final SoulService soulService;

    public SoulNoteService(SoulNoteRepository soulNoteRepository, SecurityUtils securityUtils, SoulService soulService) {
        this.soulNoteRepository = soulNoteRepository;
        this.securityUtils = securityUtils;
        this.soulService = soulService;
    }

    public SoulNote create(SoulNote note) {
        soulService.assertAccessible(note.getAmeId());
        note.setAuteurId(securityUtils.getCurrentUserId());
        return soulNoteRepository.save(note);
    }

    @Transactional(readOnly = true)
    public SoulNote findById(UUID id) {
        return soulNoteRepository.findById(id)
                .filter(n -> !n.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException("SoulNote", id));
    }

    @Transactional(readOnly = true)
    public List<SoulNote> findByAmeId(UUID ameId) {
        soulService.assertAccessible(ameId);
        return soulNoteRepository.findByAmeIdAndDeletedFalseOrderByCreatedAtDesc(ameId);
    }

    @Transactional(readOnly = true)
    public Page<SoulNote> findByAuteurId(UUID auteurId, Pageable pageable) {
        return soulNoteRepository.findByAuteurIdAndDeletedFalse(auteurId, pageable);
    }

    public SoulNote update(UUID id, String contenu) {
        SoulNote note = findById(id);
        soulService.assertAccessible(note.getAmeId());
        note.setContenu(contenu);
        return soulNoteRepository.save(note);
    }

    public void delete(UUID id) {
        SoulNote note = findById(id);
        soulService.assertAccessible(note.getAmeId());
        note.setDeleted(true);
        soulNoteRepository.save(note);
    }
}
