package com.discipolat.modules.events.api;

import java.util.List;

/**
 * §G3.4 — facette de l'écran « Archives » (V272) : les années et mois
 * réellement archivés, pour alimenter les filtres sans lister en dur.
 *
 * <p>Le web servait auparavant les filtres depuis le jeu de données complet
 * téléchargé ; il les dérive désormais de cet agrégat (une seule requête
 * {@code GROUP BY}, sans instantané). Aucun libellé de mois n'est codé en dur
 * : le client formate le numéro de mois dans la locale active
 * ({@code Intl.DateTimeFormat}).
 *
 * @param eventYear   année archivée
 * @param months      mois (1-12) contenant au moins une archive, triés
 * @param archiveCount nombre total de versions archivées cette année-là
 */
public record ChurchEventArchiveFacet(
        Integer eventYear,
        List<Integer> months,
        long archiveCount) {
}
