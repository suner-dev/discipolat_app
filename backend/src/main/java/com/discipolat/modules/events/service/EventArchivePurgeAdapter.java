package com.discipolat.modules.events.service;

import com.discipolat.modules.compliance.api.EventArchivePurgePort;
import com.discipolat.modules.events.domain.ChurchEventArchive;
import com.discipolat.modules.events.repository.ChurchEventArchiveRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * V272 §B — implementation, côté events, du port de purge publié par compliance.
 *
 * <p>Le contexte events ne pénètre pas compliance et compliance ne pénètre pas
 * events (règle R3, ADR-002) : la décision de rétention appartient à compliance
 * (unique porte de purge de {@code event_archive}, opt-in strict), l'exécution
 * sur les entités appartient à events, qui seul connaît leur structure. Le
 * câblage Spring relie les deux par le port — même patron que
 * {@code GdprRequestAdapterFactory} côté gdpr, avec la frontière publiée du
 * bon côté.
 *
 * <p>Placé dans {@code events.service} et non {@code events.domain} : le
 * composant Spring est une décision de câblage (règle R4), pas du métier.
 */
@Component
public class EventArchivePurgeAdapter implements EventArchivePurgePort {

    private final ChurchEventArchiveRepository archiveRepository;

    public EventArchivePurgeAdapter(ChurchEventArchiveRepository archiveRepository) {
        this.archiveRepository = archiveRepository;
    }

    @Override
    public List<PurgeableArchive> findArchives(UUID tenantId) {
        return archiveRepository.findByTenantId(tenantId).stream()
                .filter(a -> a.getArchivedAt() != null)
                .map(a -> new PurgeableArchive(a.getId(), a.getArchivedAt()))
                .toList();
    }

    @Override
    public void hardDelete(List<UUID> ids) {
        archiveRepository.deleteAllById(ids);
    }

    @Override
    public void anonymize(List<UUID> ids) {
        List<ChurchEventArchive> cibles = archiveRepository.findAllById(ids);
        cibles.forEach(a -> {
            // La trace d'existence (dates, année/mois, compteur de présences)
            // reste pour l'intégrité des listes ; tout ce qui identifie ou
            // raconte le service est effacé.
            a.setEventTitle("Archive anonymisée (rétention)");
            a.setSnapshotJson(Map.of("anonymized", true,
                    "anonymizedAt", LocalDateTime.now().toString()));
            a.setArchivedBy(null);
        });
        archiveRepository.saveAll(cibles);
    }
}
