package com.discipolat.modules.platform.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.modules.audit.domain.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Dictionnaires de la plateforme : référentiels configurables (types
 * d'événements, statuts, raisons d'absence, catégories…) chargés par le
 * frontend et adaptables par l'administrateur — sans code.
 */
@Service
@Transactional
public class DictionaryService {

    private final DictionaryEntryRepository repository;
    private final AuditService auditService;

    public DictionaryService(DictionaryEntryRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    /** Toutes les entrées (admin) — groupes par clé de dictionnaire. */
    @Transactional(readOnly = true)
    public Map<String, List<DictionaryEntry>> allGrouped() {
        return repository.findAllByOrderByDictKeyAscOrdreAsc().stream()
                .collect(Collectors.groupingBy(DictionaryEntry::getDictKey, LinkedHashMap::new, Collectors.toList()));
    }

    /** Entrées actives uniquement (application) — groupes par clé. */
    @Transactional(readOnly = true)
    public Map<String, List<DictionaryEntry>> activeGrouped() {
        return repository.findByActifTrueOrderByDictKeyAscOrdreAsc().stream()
                .collect(Collectors.groupingBy(DictionaryEntry::getDictKey, LinkedHashMap::new, Collectors.toList()));
    }

    /** Entrées actives d'un dictionnaire précis. */
    @Transactional(readOnly = true)
    public List<DictionaryEntry> activeByKey(String dictKey) {
        return repository.findByDictKeyOrderByOrdreAsc(dictKey).stream()
                .filter(DictionaryEntry::isActif)
                .toList();
    }

    public DictionaryEntry create(String dictKey, DictionaryEntry entry) {
        if (dictKey == null || dictKey.isBlank()) {
            throw new IllegalArgumentException("La clé du dictionnaire est obligatoire");
        }
        if (entry.getCode() == null || entry.getCode().isBlank()) {
            throw new IllegalArgumentException("Le code de l'entrée est obligatoire");
        }
        if (entry.getLabel() == null || entry.getLabel().isBlank()) {
            throw new IllegalArgumentException("Le libellé de l'entrée est obligatoire");
        }
        String key = dictKey.trim().toUpperCase();
        String code = entry.getCode().trim().toUpperCase();
        if (repository.findByDictKeyAndCode(key, code).isPresent()) {
            throw new IllegalArgumentException(
                    "Une entrée « " + code + " » existe déjà dans le dictionnaire « " + key + " »");
        }
        DictionaryEntry saved = DictionaryEntry.builder()
                .dictKey(key)
                .code(code)
                .label(entry.getLabel().trim())
                .description(entry.getDescription())
                .color(entry.getColor())
                .ordre(entry.getOrdre())
                .actif(entry.isActif())
                .isDefault(false)
                .build();
        repository.save(saved);
        auditService.logSimple("DICTIONARY_ENTRY_CREATED", "PLATFORM_DICTIONARY", null);
        return saved;
    }

    public DictionaryEntry update(UUID id, DictionaryEntry request) {
        DictionaryEntry entry = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DictionaryEntry", id));
        if (request.getLabel() != null && !request.getLabel().isBlank()) entry.setLabel(request.getLabel().trim());
        if (request.getDescription() != null) entry.setDescription(request.getDescription());
        if (request.getColor() != null) entry.setColor(request.getColor());
        entry.setOrdre(request.getOrdre());
        entry.setActif(request.isActif());
        repository.save(entry);
        auditService.logSimple("DICTIONARY_ENTRY_UPDATED", "PLATFORM_DICTIONARY", id);
        return entry;
    }

    public void delete(UUID id) {
        DictionaryEntry entry = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DictionaryEntry", id));
        repository.delete(entry);
        auditService.logSimple("DICTIONARY_ENTRY_DELETED", "PLATFORM_DICTIONARY", id);
    }

    /**
     * Restaure les entrées par défaut : supprime les entrées créées par
     * l'admin (non-défaut), puis remet CHAQUE entrée par défaut dans son
     * état initial (libellé, couleur, ordre, actif) — y compris celles
     * qui ont été modifiées par l'admin.
     */
    public void resetDefaults() {
        List<DictionaryEntry> all = repository.findAll();
        for (DictionaryEntry entry : all) {
            if (!entry.isDefault()) {
                repository.delete(entry);
            }
        }
        Map<String, List<Object[]>> defaults = defaultSeedData();
        for (Map.Entry<String, List<Object[]>> group : defaults.entrySet()) {
            int ordre = 1;
            for (Object[] row : group.getValue()) {
                String code = (String) row[0];
                String label = (String) row[1];
                String color = (String) row[2];
                int position = ordre;
                repository.findByDictKeyAndCode(group.getKey(), code)
                        .ifPresentOrElse(entry -> {
                            entry.setLabel(label);
                            entry.setColor(color);
                            entry.setOrdre(position);
                            entry.setActif(true);
                            repository.save(entry);
                        }, () -> repository.save(DictionaryEntry.builder()
                                .dictKey(group.getKey())
                                .code(code)
                                .label(label)
                                .color(color)
                                .ordre(position)
                                .actif(true)
                                .isDefault(true)
                                .build()));
                ordre++;
            }
        }
        auditService.logSimple("DICTIONARIES_RESET", "PLATFORM_DICTIONARY", null);
    }

    /** Garantit la présence des dictionnaires par défaut (nouvelle base). */
    public void seedIfEmpty() {
        if (repository.count() > 0) {
            return;
        }
        Map<String, List<Object[]>> defaults = defaultSeedData();
        for (Map.Entry<String, List<Object[]>> group : defaults.entrySet()) {
            int ordre = 1;
            for (Object[] row : group.getValue()) {
                repository.save(DictionaryEntry.builder()
                        .dictKey(group.getKey())
                        .code((String) row[0])
                        .label((String) row[1])
                        .color((String) row[2])
                        .ordre(ordre++)
                        .actif(true)
                        .isDefault(true)
                        .build());
            }
        }
    }

    /** Données par défaut (miroir de la migration V42). */
    private Map<String, List<Object[]>> defaultSeedData() {
        Map<String, List<Object[]>> map = new LinkedHashMap<>();
        map.put("EVENT_TYPE", rows(
                row("SORTIE", "Sortie", "#22c55e"),
                row("RETRAITE", "Retraite", "#a855f7"),
                row("EVANGELISATION", "Évangélisation", "#f97316"),
                row("REUNION", "Réunion", "#3b82f6"),
                row("VISITE", "Visite", "#06b6d4"),
                row("CONFERENCE", "Conférence", "#6366f1"),
                row("FORMATION", "Formation", "#f59e0b"),
                row("ANNIVERSAIRE", "Anniversaire", "#ec4899"),
                row("CULTE", "Culte", "#22c55e"),
                row("ETUDE_BIBLIQUE", "Étude biblique", "#3b82f6"),
                row("VEILLEE", "Veillée", "#a855f7"),
                row("PRIERE", "Temps de prière", "#f59e0b"),
                row("AUTRE", "Autre", "#6b7280")));
        map.put("EVENT_STATUS", rows(
                row("PLANIFIE", "Planifié", "#3b82f6"),
                row("EN_COURS", "En cours", "#f59e0b"),
                row("TERMINE", "Terminé", "#22c55e"),
                row("ANNULE", "Annulé", "#ef4444")));
        map.put("SOUL_TYPE", rows(
                row("NOUVEL_ARRIVANT", "Nouvel arrivant", "#3b82f6"),
                row("NOUVEAU_CONVERTI", "Nouveau converti", "#22c55e")));
        map.put("SOUL_STATUS", rows(
                row("NOUVEAU_CONVERTI", "Nouveau converti", "#22c55e"),
                row("NOUVEL_ARRIVANT", "Nouvel arrivant", "#3b82f6"),
                row("EN_INTEGRATION", "En intégration", "#06b6d4"),
                row("ACTIF", "Actif", "#22c55e"),
                row("EN_VEILLE", "En veille", "#f59e0b"),
                row("DECROCHE", "Décroché", "#ef4444")));
        map.put("ABSENCE_RAISON", rows(
                row("MALADIE", "Maladie", "#ef4444"),
                row("VOYAGE", "Voyage", "#3b82f6"),
                row("INDISPONIBILITE", "Indisponibilité", "#f59e0b"),
                row("INJOIGNABLE", "Injoignable", "#6b7280"),
                row("NON_RENSEIGNE", "Non renseigné", "#94a3b8"),
                row("AUTRE", "Autre", "#6b7280")));
        map.put("EXIT_MOTIF", rows(
                row("INTEGRE_AUTONOME", "Intégré / autonome", "#22c55e"),
                row("TRANSFERT", "Transfert", "#3b82f6"),
                row("ABANDON", "Abandon", "#ef4444"),
                row("INJOIGNABLE_DURABLE", "Injoignable durable", "#6b7280"),
                row("DECES", "Décès", "#374151"),
                row("AUTRE", "Autre", "#6b7280")));
        map.put("DIFFICULTE_CATEGORIE", rows(
                row("SPIRITUEL", "Spirituel", "#a855f7"),
                row("FAMILIAL", "Familial", "#ec4899"),
                row("FINANCIER", "Financier", "#f59e0b"),
                row("SANTE", "Santé", "#ef4444"),
                row("AUTRE", "Autre", "#6b7280")));
        map.put("SITUATION_FAMILIALE", rows(
                row("CELIBATAIRE", "Célibataire", "#3b82f6"),
                row("MARIE", "Marié(e)", "#22c55e"),
                row("DIVORCE", "Divorcé(e)", "#f59e0b"),
                row("VEUF", "Veuf / veuve", "#6b7280"),
                row("PARENT_CELIBATAIRE", "Parent célibataire", "#06b6d4"),
                row("AUTRE", "Autre", "#94a3b8")));
        map.put("PRAYER_CATEGORIE", rows(
                row("SANTE", "Santé", "#ef4444"),
                row("FAMILLE", "Famille", "#22c55e"),
                row("TRAVAIL", "Travail", "#3b82f6"),
                row("SPIRITUEL", "Spirituel", "#a855f7"),
                row("AUTRE", "Autre", "#6b7280")));
        map.put("PRAYER_PRIORITE", rows(
                row("BASSE", "Basse", "#94a3b8"),
                row("MOYENNE", "Moyenne", "#f59e0b"),
                row("HAUTE", "Haute", "#ef4444")));
        map.put("DOCUMENT_CATEGORIE", rows(
                row("COMPTE_RENDU", "Compte rendu", "#3b82f6"),
                row("RAPPORT", "Rapport", "#22c55e"),
                row("FORMATION", "Formation", "#a855f7"),
                row("ADMINISTRATIF", "Administratif", "#f59e0b"),
                row("AUTRE", "Autre", "#6b7280")));
        map.put("FOLLOWUP_RAISON", rows(
                row("DECROCHAGE", "Décrochage", "#ef4444"),
                row("ABSENCE_REPETEE", "Absences répétées", "#f59e0b"),
                row("DIFFICULTE_SPIRITUELLE", "Difficulté spirituelle", "#a855f7"),
                row("SITUATION_DIFFICILE", "Situation difficile", "#ec4899"),
                row("NOUVEAU_CONVERTI", "Nouveau converti", "#22c55e"),
                row("AUTRE", "Autre", "#6b7280")));
        map.put("CULTE", rows(
                row("DIMANCHE_MATIN", "Dimanche Matin", "#22c55e"),
                row("MERCREDI_SOIR", "Mercredi Soir", "#3b82f6"),
                row("VENDREDI_SOIR", "Vendredi Soir", "#a855f7")));
        map.put("INTERACTION_TYPE", rows(
                row("APPEL", "Appel", "#3b82f6"),
                row("VISITE", "Visite", "#22c55e"),
                row("MESSAGE", "Message", "#06b6d4"),
                row("RENCONTRE", "Rencontre", "#f59e0b"),
                row("AUTRE", "Autre", "#6b7280")));
        map.put("APPOINTMENT_MOTIF", rows(
                row("CONSEIL", "Conseil", "#3b82f6"),
                row("CONFESSION", "Confession", "#a855f7"),
                row("SUIVI", "Suivi", "#22c55e"),
                row("FORMATION", "Formation", "#f59e0b"),
                row("AUTRE", "Autre", "#6b7280")));
        map.put("GRATITUDE_CATEGORIE", rows(
                row("SANTE", "Santé", "#ef4444"),
                row("FAMILLE", "Famille", "#22c55e"),
                row("TRAVAIL", "Travail", "#3b82f6"),
                row("SPIRITUEL", "Spirituel", "#a855f7"),
                row("AUTRE", "Autre", "#6b7280")));
        map.put("MEMBER_REQUEST_TYPE", rows(
                row("SUGGESTION", "Suggestion", "#3b82f6"),
                row("RENDEZ_VOUS", "Rendez-vous", "#22c55e"),
                row("SIGNALEMENT", "Signalement", "#f59e0b")));
        map.put("USER_ROLE", rows(
                row("ADMIN", "Administrateur", "#3b82f6"),
                row("PASTEUR", "Pasteur", "#8b5cf6"),
                row("RESPONSABLE", "Responsable", "#f59e0b"),
                row("CHEF_DE_FAMILLE", "Chef de famille", "#f59e0b"),
                row("FAISEUR", "Faiseur de disciples", "#22c55e"),
                row("MEMBRE", "Membre", "#6b7280")));
        map.put("USER_CHARGE", rows(
                row("LEGER", "Léger", "#22c55e"),
                row("NORMAL", "Normal", "#3b82f6"),
                row("SURCHARGE", "Surchargé", "#ef4444")));
        map.put("FEEDBACK_CATEGORIE", rows(
                row("BUG", "Bug", "#ef4444"),
                row("UX", "UX", "#06b6d4"),
                row("SUGGESTION", "Suggestion", "#3b82f6"),
                row("FONCTIONNALITE_MANQUANTE", "Fonctionnalité manquante", "#a855f7"),
                row("PERFORMANCE", "Performance", "#f59e0b"),
                row("TRADUCTION", "Traduction", "#ec4899"),
                row("AFFICHAGE", "Affichage", "#6366f1"),
                row("AUTRE", "Autre", "#6b7280")));
        map.put("FEEDBACK_STATUS", rows(
                row("NOUVEAU", "Nouveau", "#3b82f6"),
                row("EN_COURS", "En cours", "#f59e0b"),
                row("RESOLU", "Résolu", "#22c55e"),
                row("REJETE", "Rejeté", "#6b7280")));
        map.put("TRANSFER_TYPE", rows(
                row("MEMBRE_DEPARTEMENT_TRANSFERT", "Transfert de membre entre départements", "#3b82f6"),
                row("MEMBRE_DEPARTEMENT_AJOUT", "Ajout de membre dans un département", "#22c55e"),
                row("MEMBRE_DEPARTEMENT_RETRAIT", "Retrait de membre d'un département", "#ef4444"),
                row("DISCIPLE_FAMILLE_TRANSFERT", "Transfert de disciple entre familles", "#3b82f6"),
                row("FAISEUR_FAMILLE_TRANSFERT", "Transfert de faiseur entre familles", "#06b6d4"),
                row("CHEF_FAMILLE_TRANSFERT", "Transfert de chef de famille", "#f59e0b"),
                row("FAISEUR_DISCIPLE_CHANGEMENT", "Changement du faiseur d'un disciple", "#a855f7"),
                row("RESPONSABLE_DEPARTEMENT_CHANGEMENT", "Changement du responsable d'un département", "#ec4899"),
                row("CHEF_ADJOINT_CHANGEMENT", "Changement du chef adjoint d'une famille", "#6366f1")));
        map.put("TRANSFER_STATUS", rows(
                row("BROUILLON", "Brouillon", "#6b7280"),
                row("SOUMIS", "Soumis", "#3b82f6"),
                row("EN_ATTENTE_VALIDATION", "En attente de validation", "#f59e0b"),
                row("VALIDATION_PARTIELLE", "Validation partielle", "#06b6d4"),
                row("VALIDE", "Validé", "#8b5cf6"),
                row("REFUSE", "Refusé", "#ef4444"),
                row("ANNULE", "Annulé", "#6b7280"),
                row("EXECUTE", "Exécuté", "#22c55e"),
                row("ARCHIVE", "Archivé", "#94a3b8")));
        map.put("TRANSFER_DECISION", rows(
                row("APPROBATION", "Approbation", "#22c55e"),
                row("REFUS", "Refus", "#ef4444"),
                row("DEMANDE_INFORMATIONS", "Demande d'informations", "#f59e0b"),
                row("RENVOI_CORRECTION", "Renvoi pour correction", "#f59e0b")));
        map.put("TRANSFER_PRIORITE", rows(
                row("BASSE", "Basse", "#94a3b8"),
                row("MOYENNE", "Moyenne", "#3b82f6"),
                row("HAUTE", "Haute", "#f59e0b"),
                row("URGENTE", "Urgente", "#ef4444")));
        map.put("MEMBER_REQUEST_TARGET", rows(
                row("PASTEUR", "Pasteur", "#8b5cf6"),
                row("RESPONSABLE", "Responsable", "#f59e0b"),
                row("CHEF_DE_FAMILLE", "Chef de famille", "#22c55e")));
        map.put("MEMBER_REQUEST_STATUS", rows(
                row("OUVERT", "Ouvert", "#f59e0b"),
                row("EN_COURS", "En cours", "#3b82f6"),
                row("RESOLU", "Résolu", "#22c55e"),
                row("REJETE", "Rejeté", "#ef4444")));
        map.put("PRAYER_VISIBILITE", rows(
                row("GENERALE", "Général", "#06b6d4"),
                row("PASTEUR_RESPONSABLE", "Pasteur + Resp.", "#a855f7"),
                row("FAISEUR", "Chefs + Faiseurs", "#f59e0b"),
                row("PARTAGEE", "Famille", "#3b82f6"),
                row("PRIVEE", "Privé", "#6b7280")));
        map.put("EVALUATION_CATEGORIE", rows(
                row("RESPONSABLE", "Responsable", "#a855f7"),
                row("CHEF_FAMILLE", "Chef de famille", "#f59e0b"),
                row("FAISEUR", "Faiseur", "#22c55e")));
        map.put("SPIRITUAL_LEVEL", rows(
                row("NOUVEAU_CONVERTI", "Nouveau converti", "#22c55e"),
                row("EN_CROISSANCE", "En croissance", "#06b6d4"),
                row("MATURE", "Mature", "#3b82f6"),
                row("EN_DIFFICULTE", "En difficulté", "#ef4444")));
        map.put("DISCIPLINE_CATEGORIE", rows(
                row("COMPORTEMENT", "Comportement", "#ef4444"),
                row("CONDUITE", "Conduite", "#ef4444"),
                row("HABILLEMENT", "Habillement", "#f59e0b"),
                row("VIE_SPIRITUELLE", "Vie spirituelle", "#a855f7"),
                row("PONCTUALITE", "Ponctualité", "#06b6d4"),
                row("PARTICIPATION", "Participation", "#3b82f6"),
                row("FIDELITE", "Fidélité", "#22c55e"),
                row("ENGAGEMENT", "Engagement", "#22c55e"),
                row("REPROCHE", "Reproche", "#f59e0b"),
                row("SANCTION", "Sanction", "#ef4444"),
                row("LITIGE", "Litige", "#f59e0b"),
                row("CONFLIT", "Conflit", "#ef4444"),
                row("SCANDALE", "Scandale", "#ef4444"),
                row("RELATION_PROBLEMATIQUE", "Relation problématique", "#ec4899"),
                row("FLIRT_INAPPROPRIE", "Flirt inapproprié", "#ec4899"),
                row("DEGAT_MATERIEL", "Dégât matériel", "#6b7280"),
                row("DEGAT_RELATIONNEL", "Dégât relationnel", "#6b7280")));
        map.put("AUDIT_ENTITY", rows(
                row("USER", "Utilisateur", "#3b82f6"),
                row("FAMILY", "Famille", "#22c55e"),
                row("SOUL", "Âme", "#06b6d4"),
                row("REPORT", "Rapport", "#f59e0b"),
                row("DEPARTMENT", "Département", "#a855f7"),
                row("TRANSFER", "Transfert", "#8b5cf6"),
                row("EVENT", "Événement", "#ec4899"),
                row("MEMBER_REQUEST", "Demande membre", "#6366f1")));
        map.put("REPORT_STATUS", rows(
                row("BROUILLON", "Brouillon", "#f59e0b"),
                row("SOUMIS", "Soumis", "#22c55e"),
                row("VU_PAR_RESPONSABLE", "Vu responsable", "#3b82f6"),
                row("VU_PAR_PASTEUR", "Vu pasteur", "#8b5cf6")));
        map.put("ALERT_TYPE", rows(
                row("ABSENCE_48H", "Absence 48h", "#ef4444"),
                row("ABSENCE_3_SEMAINES", "Décrochage 3 semaines", "#ef4444"),
                row("RAPPORT_NON_SOUMIS", "Rapport non soumis", "#f59e0b"),
                row("RAPPORT_FAMILLE_NON_SOUMIS", "Rapport famille non soumis", "#f59e0b"),
                row("ALERTE_ABSENCE", "Alerte absence", "#ef4444"),
                row("MANUEL", "Manuelle", "#8b5cf6")));
        map.put("ALERT_CIBLE", rows(
                row("PERSONNE", "Personne", "#3b82f6"),
                row("DEPARTEMENT", "Département", "#a855f7"),
                row("FAMILLE", "Famille", "#22c55e"),
                row("GROUPE", "Groupe", "#f59e0b"),
                row("EGLISE", "Église", "#06b6d4")));
        map.put("ALERT_PRIORITE", rows(
                row("BASSE", "Basse", "#94a3b8"),
                row("MOYENNE", "Moyenne", "#3b82f6"),
                row("HAUTE", "Haute", "#f59e0b"),
                row("URGENTE", "Urgente", "#ef4444")));
        map.put("ALERT_STATUS", rows(
                row("ACTIVE", "Active", "#ef4444"),
                row("TRAITEE", "Traitée", "#f59e0b"),
                row("RESOLUE", "Résolue", "#22c55e")));
        map.put("NOTIFICATION_TYPE", rows(
                row("RAPPORT_NON_SOUMIS", "Rapport non soumis", "#f59e0b"),
                row("ABSENCE_48H", "Absence 48h", "#ef4444"),
                row("RAPPORT_FAMILLE_NON_SOUMIS", "Rapport famille non soumis", "#f59e0b"),
                row("ALERTE_ABSENCE", "Alerte absence", "#ef4444"),
                row("INFORMATION", "Information", "#3b82f6"),
                row("PRIERE_EXAUCEE", "Prières exaucées", "#22c55e"),
                row("TRANSFERT_DEMANDE", "Demande de transfert", "#3b82f6"),
                row("TRANSFERT_VALIDATION", "Validation de transfert", "#8b5cf6"),
                row("TRANSFERT_VALIDEE", "Transfert validé", "#22c55e"),
                row("TRANSFERT_REFUSEE", "Transfert refusé", "#ef4444"),
                row("TRANSFERT_INFOS_DEMANDEES", "Informations demandées", "#f59e0b"),
                row("TRANSFERT_CORRECTION", "Correction demandée", "#f59e0b"),
                row("TRANSFERT_EXECUTEE", "Transfert exécuté", "#22c55e"),
                row("TRANSFERT_ANNULEE", "Transfert annulé", "#6b7280"),
                row("TRANSFERT_DELAI_DEPASSE", "Délai de traitement dépassé", "#ef4444")));
        map.put("NOTIFICATION_CANAL", rows(
                row("IN_APP", "Dans l'application", "#3b82f6"),
                row("EMAIL", "E-mail", "#8b5cf6"),
                row("PUSH", "Notification push", "#06b6d4")));
        map.put("DISCIPLINE_TYPE", rows(
                row("REPROCHE", "Reproche", "#f59e0b"),
                row("SANCTION", "Sanction", "#ef4444"),
                row("LITIGE", "Litige", "#f59e0b"),
                row("CONFLIT", "Conflit", "#ef4444"),
                row("SCANDALE", "Scandale", "#ef4444"),
                row("OBSERVATION", "Observation", "#3b82f6"),
                row("TEMOIGNAGE", "Témoignage", "#22c55e"),
                row("ENTRETIEN", "Entretien pastoral", "#06b6d4"),
                row("RESOLUTION", "Résolution", "#22c55e"),
                row("AUTRE", "Autre", "#6b7280")));
        map.put("DISCIPLINE_GRAVITE", rows(
                row("FAIBLE", "Faible", "#22c55e"),
                row("MOYENNE", "Moyenne", "#f59e0b"),
                row("GRAVE", "Grave", "#f97316"),
                row("CRITIQUE", "Critique", "#ef4444")));
        map.put("FEEDBACK_PRIORITE", rows(
                row("BASSE", "Basse", "#94a3b8"),
                row("MOYENNE", "Moyenne", "#3b82f6"),
                row("HAUTE", "Haute", "#f59e0b"),
                row("CRITIQUE", "Critique", "#ef4444")));
        map.put("INTERACTION_CANAL", rows(
                row("TELEPHONE", "Téléphone", "#3b82f6"),
                row("WHATSAPP", "WhatsApp", "#22c55e"),
                row("SMS", "SMS", "#06b6d4"),
                row("EMAIL", "E-mail", "#a855f7"),
                row("VIDEO", "Visioconférence", "#f59e0b"),
                row("PRESENTIEL", "En présentiel", "#22c55e")));
        return map;
    }

    private static Object[] row(String code, String label, String color) {
        return new Object[]{code, label, color};
    }

    @SafeVarargs
    private static List<Object[]> rows(Object[]... rows) {
        return new ArrayList<>(Arrays.asList(rows));
    }
}
