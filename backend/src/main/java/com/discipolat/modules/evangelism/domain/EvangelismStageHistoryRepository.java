package com.discipolat.modules.evangelism.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EvangelismStageHistoryRepository extends JpaRepository<EvangelismStageHistory, UUID> {
    List<EvangelismStageHistory> findByTrackIdOrderByCreeLeDesc(UUID trackId);

    /** Nombre de franchissements d'étapes pour un pipeline (scoring de conversion). */
    long countByTrackId(UUID trackId);

    /** Nombre de franchissements d'une étape par un utilisateur (attribution de badges). */
    long countByEtapeAndCreePar(EvangelismEtape etape, UUID creePar);
}
