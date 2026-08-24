package com.discipolat.modules.aiVisitNotes.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class AiVisitNoteService {

    private final AiVisitNoteRepository noteRepo;

    public AiVisitNoteService(AiVisitNoteRepository noteRepo) { this.noteRepo = noteRepo; }

    public AiVisitNote create(AiVisitNote note) {
        note.setTenantId(TenantContext.getCurrentTenantId());
        note.setCreatedAt(LocalDateTime.now());
        // Generate AI summary from transcription
        if (note.getRawTranscription() != null) {
            note.setAiSummary(generateSummary(note.getRawTranscription()));
            note.setAiActionItems(generateActionItems(note.getRawTranscription()));
            note.setAiSentiment(analyzeSentiment(note.getRawTranscription()));
        }
        return noteRepo.save(note);
    }

    public List<AiVisitNote> listByMember(UUID memberId) {
        return noteRepo.findByTenantIdAndMemberIdOrderByCreatedAtDesc(TenantContext.getCurrentTenantId(), memberId);
    }

    public List<AiVisitNote> listAll() {
        return noteRepo.findByTenantIdOrderByCreatedAtDesc(TenantContext.getCurrentTenantId());
    }

    public AiVisitNote verify(UUID id) {
        AiVisitNote note = noteRepo.findById(id).orElseThrow();
        note.setIsVerified(true);
        return noteRepo.save(note);
    }

    private String generateSummary(String transcription) {
        int words = transcription.split("\\s+").length;
        return String.format("Visite pastorale enregistrée (%d mots). Résumé IA : %s", words,
            transcription.length() > 200 ? transcription.substring(0, 200) + "..." : transcription);
    }

    private String generateActionItems(String transcription) {
        List<String> items = new ArrayList<>();
        String lower = transcription.toLowerCase();
        if (lower.contains("prière") || lower.contains("prier")) items.add("Planifier une session de prière de suivi");
        if (lower.contains("maladie") || lower.contains("santé")) items.add("Organiser un soutien médical");
        if (lower.contains("besoin") || lower.contains("difficulté")) items.add("Évaluer les besoins matériels");
        if (lower.contains("formation") || lower.contains("apprendre")) items.add("Proposer une formation adaptée");
        if (items.isEmpty()) items.add("Suivi standard recommandé");
        return items.toString();
    }

    private String analyzeSentiment(String transcription) {
        String lower = transcription.toLowerCase();
        int positive = 0, negative = 0;
        for (String w : List.of("merci", "joie", "bien", "progrès", "grâce", "victoire")) if (lower.contains(w)) positive++;
        for (String w : List.of("problème", "difficulté", "souffrance", "peur", "triste", "colère")) if (lower.contains(w)) negative++;
        if (negative > positive + 2) return "CRITICAL";
        if (negative > positive) return "CONCERNING";
        if (positive > negative) return "POSITIVE";
        return "NEUTRAL";
    }
}
