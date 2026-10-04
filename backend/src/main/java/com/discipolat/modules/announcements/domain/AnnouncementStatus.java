package com.discipolat.modules.announcements.domain;

/**
 * SPEC_ONBOARDING_FLOWS (D4) — cycle de vie d'une annonce publique.
 * Seule la valeur {@code PUBLISHED} est visible sur le landing ; le passage
 * en {@code EXPIRED} est posé par le scheduler (date d'événement ou
 * {@code expiresAt} dépassée).
 */
public enum AnnouncementStatus {
    DRAFT,
    PENDING_MODERATION,
    PUBLISHED,
    REJECTED,
    EXPIRED
}
