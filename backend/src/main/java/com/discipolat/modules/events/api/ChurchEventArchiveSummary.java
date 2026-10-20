package com.discipolat.modules.events.api;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * §G3.4 — ligne de liste de l'écran « Archives » (V272).
 *
 * <p>Résumé SANS {@code snapshot_json} : la liste paginée
 * ({@code GET /church-events/archives/search}) ne charge jamais les instantanés
 * complets — seule la ligne {@code GET /archives/{id}} les déserialise pour le
 * détail. C'est le contrat qui rend la consultation bornée : ni mémoire, ni
 * bande passante, ni temps de rendu ne croissent avec la taille des
 * instantanés.
 *
 * <p>Le compteur de présences vient de la colonne dénormalisée
 * {@code attendance_count} (V272 §3), pas du snapshot.
 *
 * @param id            identifiant de la version archivée
 * @param eventId       événement d'origine (plusieurs versions possibles)
 * @param eventTitle    titre figé au clôture
 * @param eventStartAt  début de l'événement archivé (borne de navigation)
 * @param eventYear     année dérivée, pour le filtre sans désérialisation
 * @param eventMonth    mois dérivé (1-12)
 * @param spaceId       espace principal rattaché, le cas échéant
 * @param version       numéro de version, incrémenté à chaque re-clôture
 * @param attendanceCount présences figées au clôture
 * @param archivedAt    horodatage de la clôture
 * @param archivedBy    acteur qui a clôturé
 */
public record ChurchEventArchiveSummary(
        UUID id,
        UUID eventId,
        String eventTitle,
        OffsetDateTime eventStartAt,
        Integer eventYear,
        Integer eventMonth,
        UUID spaceId,
        Integer version,
        Integer attendanceCount,
        OffsetDateTime archivedAt,
        UUID archivedBy) {
}
