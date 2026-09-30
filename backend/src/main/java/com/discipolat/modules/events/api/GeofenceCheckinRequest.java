package com.discipolat.modules.events.api;

/**
 * Pointage geolocalise.
 *
 * @param latitude       position mesuree par l'appareil (obligatoire)
 * @param longitude      position mesuree par l'appareil (obligatoire)
 * @param accuracyMeters precision annoncee par l'appareil, optionnelle
 */
public record GeofenceCheckinRequest(
        Double latitude,
        Double longitude,
        Double accuracyMeters) {
}
