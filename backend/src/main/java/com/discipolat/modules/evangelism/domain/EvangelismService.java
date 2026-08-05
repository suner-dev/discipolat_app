package com.discipolat.modules.evangelism.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.evangelism.api.EvangelismStatsResponse;
import com.discipolat.modules.evangelism.api.EvangelismTrackResponse;
import com.discipolat.modules.evangelism.api.UpdateEvangelismRequest;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Suivi d'évangélisation : chaque âme progresse dans un pipeline d'étapes
 * (nouvelle âme → premier contact → visite → invitation → premier culte →
 * suivi → baptême → département → famille → discipolat → leader).
 * Chaque franchissement d'étape est historisé.
 */
@Service
@Transactional
public class EvangelismService {

    private final EvangelismTrackRepository trackRepository;
    private final EvangelismStageHistoryRepository historyRepository;
    private final SoulRepository soulRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    public EvangelismService(EvangelismTrackRepository trackRepository,
                             EvangelismStageHistoryRepository historyRepository,
                             SoulRepository soulRepository,
                             UserRepository userRepository,
                             SecurityUtils securityUtils) {
        this.trackRepository = trackRepository;
        this.historyRepository = historyRepository;
        this.soulRepository = soulRepository;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
    }

    /** Récupère ou initialise le track d'une âme (démarre à NOUVELLE_AME). */
    @Transactional
    public EvangelismTrackResponse getOrCreate(UUID soulId) {
        return trackRepository.findBySoulId(soulId)
                .map(this::toResponse)
                .orElseGet(() -> {
                    if (!soulRepository.existsById(soulId)) {
                        throw new EntityNotFoundException("Soul", soulId);
                    }
                    EvangelismTrack track = EvangelismTrack.builder()
                            .soulId(soulId)
                            .etape(EvangelismEtape.NOUVELLE_AME)
                            .dateEtape(LocalDate.now())
                            .creePar(securityUtils.getCurrentUserId())
                            .build();
                    trackRepository.save(track);
                    historyRepository.save(EvangelismStageHistory.builder()
                            .trackId(track.getId())
                            .etape(EvangelismEtape.NOUVELLE_AME)
                            .creePar(track.getCreePar())
                            .build());
                    return toResponse(track);
                });
    }

    /** Avance ou recule l'âme dans le pipeline (avec traçabilité). */
    public EvangelismTrackResponse updateStage(UUID soulId, UpdateEvangelismRequest request) {
        EvangelismTrack track = trackRepository.findBySoulId(soulId)
                .orElseGet(() -> {
                    if (!soulRepository.existsById(soulId)) {
                        throw new EntityNotFoundException("Soul", soulId);
                    }
                    return EvangelismTrack.builder()
                            .soulId(soulId)
                            .etape(EvangelismEtape.NOUVELLE_AME)
                            .dateEtape(LocalDate.now())
                            .creePar(securityUtils.getCurrentUserId())
                            .build();
                });

        boolean changed = track.getEtape() != request.etape();
        track.setEtape(request.etape());
        track.setDateEtape(LocalDate.now());
        track.setNote(request.note() != null ? request.note() : track.getNote());

        EvangelismTrack saved = trackRepository.save(track);

        // Uniquement les franchissements réels sont historisés
        if (changed) {
            historyRepository.save(EvangelismStageHistory.builder()
                    .trackId(saved.getId())
                    .etape(request.etape())
                    .creePar(securityUtils.getCurrentUserId())
                    .build());
        }
        return toResponse(saved);
    }

    /** Liste des âmes à une étape donnée du pipeline (ou toutes si null). */
    @Transactional(readOnly = true)
    public List<EvangelismTrackResponse> findAll(EvangelismEtape etape, String search) {
        List<EvangelismTrack> tracks = etape != null
                ? trackRepository.findByEtapeOrderByDateEtapeDesc(etape)
                : trackRepository.findAll();
        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase().trim();
            tracks = tracks.stream().filter(t -> {
                String nom = soulName(t.getSoulId());
                return nom != null && nom.toLowerCase().contains(q);
            }).toList();
        }
        return tracks.stream().map(this::toResponse).toList();
    }

    /** Nombre d'âmes par étape (funnel du pipeline). */
    @Transactional(readOnly = true)
    public EvangelismStatsResponse stats() {
        Map<String, Long> parEtape = new LinkedHashMap<>();
        for (EvangelismEtape e : EvangelismEtape.values()) {
            parEtape.put(e.name(), trackRepository.countByEtape(e));
        }
        return new EvangelismStatsResponse(trackRepository.count(), parEtape);
    }

    /** Historique des franchissements d'étapes d'une âme. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> history(UUID soulId) {
        return trackRepository.findBySoulId(soulId)
                .map(t -> historyRepository.findByTrackIdOrderByCreeLeDesc(t.getId()).stream()
                        .map(h -> {
                            Map<String, Object> m = new LinkedHashMap<>();
                            m.put("etape", h.getEtape());
                            m.put("creePar", h.getCreePar());
                            m.put("creeLe", h.getCreeLe());
                            return m;
                        })
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    private EvangelismTrackResponse toResponse(EvangelismTrack t) {
        return EvangelismTrackResponse.from(
                t,
                soulName(t.getSoulId()),
                t.getCreePar() != null
                        ? userRepository.findById(t.getCreePar())
                                .map(u -> u.getFirstName() + " " + u.getLastName()).orElse(null)
                        : null);
    }

    private String soulName(UUID soulId) {
        return soulRepository.findById(soulId)
                .map(s -> (s.getPrenom() != null && !s.getPrenom().isBlank()
                        ? s.getPrenom() + " " : "") + s.getNom())
                .orElse(null);
    }
}
