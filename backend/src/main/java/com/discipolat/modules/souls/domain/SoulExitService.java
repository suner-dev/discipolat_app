package com.discipolat.modules.souls.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SoulExitService {

    private final SoulExitRepository soulExitRepository;
    private final SoulRepository soulRepository;
    private final SecurityUtils securityUtils;

    public SoulExitService(SoulExitRepository soulExitRepository,
                            SoulRepository soulRepository,
                            SecurityUtils securityUtils) {
        this.soulExitRepository = soulExitRepository;
        this.soulRepository = soulRepository;
        this.securityUtils = securityUtils;
    }

    /**
     * US-22: Mark a soul as exited with required motif
     */
    public SoulExit markAsExited(UUID ameId, String motif, String motifDetail, boolean peutReintegrer) {
        Soul soul = soulRepository.findById(ameId)
                .orElseThrow(() -> new EntityNotFoundException("Soul", ameId));
        UUID currentUserId = securityUtils.getCurrentUserId();

        // Update soul status
        soul.setStatut(StatutAme.DECROCHE);
        soulRepository.save(soul);

        // Create exit record
        SoulExit exit = SoulExit.builder()
                .ameId(ameId)
                .faiseurId(currentUserId)
                .motif(motif)
                .motifDetail(motifDetail)
                .peutReintegrer(peutReintegrer)
                .dateSortie(LocalDate.now())
                .build();
        return soulExitRepository.save(exit);
    }

    /**
     * US-22: Reintegrate a soul that was previously marked as exited
     */
    public Soul reintegrate(UUID ameId, StatutAme nouveauStatut) {
        Soul soul = soulRepository.findById(ameId)
                .orElseThrow(() -> new EntityNotFoundException("Soul", ameId));
        soul.setStatut(nouveauStatut != null ? nouveauStatut : StatutAme.EN_INTEGRATION);
        return soulRepository.save(soul);
    }

    @Transactional(readOnly = true)
    public SoulExit findLastExit(UUID ameId) {
        return soulExitRepository.findTopByAmeIdOrderByCreatedAtDesc(ameId)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<SoulExit> findExitsForSoul(UUID ameId) {
        return soulExitRepository.findByAmeIdOrderByCreatedAtDesc(ameId);
    }
}
