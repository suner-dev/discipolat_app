package com.discipolat.modules.events.api;

import com.discipolat.modules.events.domain.Event;
import com.discipolat.modules.events.service.ChurchEventService;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Contrat anglais de {@code /api/v1/church-events} (modèle vivant Church OS).
 *
 * <p>Depuis l'arbitrage D1 (V203), l'entité unique de la table « event » est
 * {@link Event}, à propriétés françaises. Ce DTO fait la traduction EN↔FR à la
 * frontière — le fil JSON (title/startAt/status/…) est FigÉ, pas plus que le
 * fil français du contrat §3 — et remplace l'ex-entité ChurchEvent comme
 * corps de requête/réponse.
 *
 * <p>Horodatages : {@code OffsetDateTime} sur le fil, converti en la
 * convention « naive = UTC » de V158 dans les deux sens
 * ({@link ChurchEventService#naiveUtc} / {@link ChurchEventService#offsetUtc}),
 * ce qui rend exactement la même valeur qu'avant (colonne TIMESTAMPTZ lue via
 * le même driver).
 */
public record ChurchEventDto(
        UUID id,
        UUID tenantId,
        String title,
        String description,
        String type,
        String status,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String timezone,
        Boolean isRecurring,
        String recurrenceRule,
        String visibility,
        UUID organizerId,
        UUID createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime deletedAt,
        boolean isDeleted
) {

    public static ChurchEventDto from(Event e) {
        return new ChurchEventDto(
                e.getId(), e.getTenantId(), e.getTitre(), e.getDescription(),
                e.getTypeEvenement(), e.getStatut(),
                ChurchEventService.offsetUtc(e.getDateDebut()),
                ChurchEventService.offsetUtc(e.getDateFin()),
                e.getTimezone(), e.getIsRecurring(), e.getRecurrenceRule(),
                e.getVisibility(), e.getOrganisateurId(), e.getCreatedBy(),
                ChurchEventService.offsetUtc(e.getCreatedAt()),
                ChurchEventService.offsetUtc(e.getUpdatedAt()),
                ChurchEventService.offsetUtc(e.getDeletedAt()),
                e.isDeleted());
    }

    /**
     * Evénement neuf à partir du corps de création. Les defauts reprennent
     * ceux de l'ex-ChurchEvent : status DRAFT, visibility CHURCH ; le type
     * est rendu obligatoire par la table vivante (NOT NULL, hérité du contrat
     * FR) et retombe sur OTHER, l'équivalent du « ELSE 'OTHER' » de V158.
     */
    public Event toEntity() {
        return Event.builder()
                .titre(title)
                .description(description)
                .typeEvenement(type != null ? type : "OTHER")
                .statut(status != null ? status : "DRAFT")
                .dateDebut(ChurchEventService.naiveUtc(startAt))
                .dateFin(ChurchEventService.naiveUtc(endAt))
                .timezone(timezone)
                .isRecurring(isRecurring != null ? isRecurring : false)
                .recurrenceRule(recurrenceRule)
                .visibility(visibility != null ? visibility : "CHURCH")
                .organisateurId(organizerId)
                .build();
    }

    /** Patch partiel : seuls les champs fournis sont portés à l'entité. */
    public Event toPatch() {
        return Event.builder()
                .titre(title)
                .description(description)
                .typeEvenement(type)
                .statut(status)
                .dateDebut(ChurchEventService.naiveUtc(startAt))
                .dateFin(ChurchEventService.naiveUtc(endAt))
                .timezone(timezone)
                .isRecurring(isRecurring)
                .recurrenceRule(recurrenceRule)
                .visibility(visibility)
                .organisateurId(organizerId)
                .build();
    }
}
