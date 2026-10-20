package com.discipolat.modules.compliance.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * V272 §B — port publié du contexte compliance pour la purge de rétention des
 * archives d'événements (RGPD art. 5-1-e / 17-3-e).
 *
 * <p><strong>Pourquoi un port et non l'accès direct au dépôt events.</strong>
 * La règle R3 de l'ADR-002 interdit à un contexte de pénétrer les internes
 * ({@code domain}, {@code repository}) d'un autre contexte : le couplage licite
 * passe par une interface publiée. Le décideur de purge reste compliance — il
 * reste la <strong>seule</strong> porte de suppression de la table
 * {@code event_archive} — mais il exprime sa décision via ce port, et
 * l'implémentation ({@code EventArchivePurgeAdapter}, dans events) applique la
 * décision sur ses propres entités. C'est le même patron que
 * {@code GdprRequestAdapter}, avec la frontière cette fois du bon côté.
 *
 * <p>Aucune purge n'est active par défaut : le port n'est piloté que si
 * l'église a créé explicitement une politique {@code EventArchive}.
 */
public interface EventArchivePurgePort {

    /** Vue minimale d'une archive pour la décision de rétention : identité + horodatage. */
    record PurgeableArchive(UUID id, OffsetDateTime archivedAt) {}

    /** Toutes les archives du tenant, avec leur date d'archivage (jamais {@code null}). */
    List<PurgeableArchive> findArchives(UUID tenantId);

    /** Suppression définitive (mode dur) — toujours précédé d'un export consigné côté compliance. */
    void hardDelete(List<UUID> ids);

    /**
     * Anonymisation (mode souple) : la trace d'existence (dates, compteur de
     * présences) reste pour l'intégrité des listes ; tout ce qui identifie ou
     * raconte le service est effacé par le contexte qui connaît sa propre
     * structure de données.
     */
    void anonymize(List<UUID> ids);
}
