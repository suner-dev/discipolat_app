package com.discipolat.modules.notifications.domain;

import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Catalogue statique des événements de notification configurables : libellés FR,
 * modèles suggérés par défaut, canaux recommandés et variables de rendu.
 */
public final class NotificationEventCatalog {

    private NotificationEventCatalog() {}

    public record Entry(TypeNotification event, String label, String titre, String message,
                        List<CanalNotification> canaux, List<String> variables) {}

    private static final List<Entry> ENTRIES = List.of(
            new Entry(TypeNotification.INFORMATION, "Information générale",
                    "ℹ️ Information", "{{entiteType}} — information émise par la plateforme.",
                    List.of(CanalNotification.IN_APP), List.of("{{type}}", "{{entiteType}}")),
            new Entry(TypeNotification.ALERTE_ABSENCE, "Alerte d'absence",
                    "⚠️ Absence constatée", "Un membre est sans contact depuis une durée anormale.",
                    List.of(CanalNotification.IN_APP, CanalNotification.PUSH),
                    List.of("{{type}}", "{{entiteType}}")),
            new Entry(TypeNotification.PRIERE_EXAUCEE, "Prière exaucée",
                    "🙏 Prière exaucée", "Une prière a été marquée comme exaucée.",
                    List.of(CanalNotification.IN_APP), List.of("{{type}}", "{{entiteType}}")),
            new Entry(TypeNotification.TRANSFERT_DEMANDE, "Transfert — demande créée",
                    "📨 Nouvelle demande de transfert", "Une demande de transfert a été soumise.",
                    List.of(CanalNotification.IN_APP), List.of("{{type}}", "{{entiteType}}")),
            new Entry(TypeNotification.TRANSFERT_VALIDATION, "Transfert — validation requise",
                    "✅ Transfert à valider", "Une demande de transfert attend votre validation.",
                    List.of(CanalNotification.IN_APP, CanalNotification.PUSH),
                    List.of("{{type}}", "{{entiteType}}")),
            new Entry(TypeNotification.TRANSFERT_VALIDEE, "Transfert — validé",
                    "🎉 Transfert validé", "Votre demande de transfert a été validée.",
                    List.of(CanalNotification.IN_APP), List.of("{{type}}", "{{entiteType}}")),
            new Entry(TypeNotification.TRANSFERT_REFUSEE, "Transfert — refusé",
                    "🚫 Transfert refusé", "Votre demande de transfert a été refusée.",
                    List.of(CanalNotification.IN_APP), List.of("{{type}}", "{{entiteType}}")),
            new Entry(TypeNotification.TRANSFERT_EXECUTEE, "Transfert — exécuté",
                    "➕ Transfert exécuté", "Votre demande de transfert a été exécutée.",
                    List.of(CanalNotification.IN_APP), List.of("{{type}}", "{{entiteType}}")),
            new Entry(TypeNotification.MEMBRE_AJOUTE, "Membre ajouté au département",
                    "👤 Nouveau membre", "Un membre a été ajouté à un département.",
                    List.of(CanalNotification.IN_APP), List.of("{{type}}", "{{entiteType}}")),
            new Entry(TypeNotification.TACHE_ASSIGNEE, "Tâche assignée",
                    "📋 Tâche assignée", "Une tâche vous a été assignée.",
                    List.of(CanalNotification.IN_APP, CanalNotification.PUSH),
                    List.of("{{type}}", "{{entiteType}}")),
            new Entry(TypeNotification.TACHE_EN_RETARD, "Tâche en retard",
                    "⏰ Tâche en retard", "Une tâche assignée est en retard.",
                    List.of(CanalNotification.IN_APP, CanalNotification.PUSH),
                    List.of("{{type}}", "{{entiteType}}")),
            new Entry(TypeNotification.EVENEMENT_RAPPEL, "Rappel d'événement",
                    "📅 Rappel d'événement", "Un événement approche : {{entiteType}}.",
                    List.of(CanalNotification.IN_APP, CanalNotification.PUSH),
                    List.of("{{type}}", "{{entiteType}}"))
    );

    private static final Map<TypeNotification, Entry> BY_EVENT;

    static {
        Map<TypeNotification, Entry> map = new HashMap<>();
        for (Entry e : ENTRIES) map.put(e.event(), e);
        BY_EVENT = Map.copyOf(map);
    }

    public static List<Entry> entries() {
        return ENTRIES;
    }

    public static Entry find(TypeNotification event) {
        return BY_EVENT.get(event);
    }
}
