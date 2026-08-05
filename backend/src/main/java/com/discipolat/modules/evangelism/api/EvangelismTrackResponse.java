package com.discipolat.modules.evangelism.api;

import com.discipolat.modules.evangelism.domain.EvangelismEtape;
import com.discipolat.modules.evangelism.domain.EvangelismTrack;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record EvangelismTrackResponse(
        UUID id,
        UUID soulId,
        String soulNom,
        EvangelismEtape etape,
        LocalDate dateEtape,
        String note,
        UUID creePar,
        String creeParNom,
        LocalDateTime creeLe,
        LocalDateTime majLe
) {
    public static EvangelismTrackResponse from(EvangelismTrack t, String soulNom, String creeParNom) {
        return new EvangelismTrackResponse(
                t.getId(), t.getSoulId(), soulNom, t.getEtape(),
                t.getDateEtape(), t.getNote(), t.getCreePar(),
                creeParNom, t.getCreeLe(), t.getMajLe());
    }
}
