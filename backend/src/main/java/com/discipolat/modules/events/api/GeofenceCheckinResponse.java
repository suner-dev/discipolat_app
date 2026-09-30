package com.discipolat.modules.events.api;

/**
 * Verdict du serveur sur un pointage.
 *
 * <p>Expose les chiffres plutot qu'un simple booleen : l'utilisateur doit
 * pouvoir comprendre un refus, et une contestation doit etre arbitrable.
 */
public record GeofenceCheckinResponse(
        boolean success,
        String reason,
        double distanceMeters,
        double accuracyMeters,
        double radiusMeters,
        double marginMeters,
        String message) {

    public static GeofenceCheckinResponse of(boolean success, String reason,
                                              double distance, double accuracy,
                                              double radius, double margin,
                                              String message) {
        return new GeofenceCheckinResponse(success, reason, round(distance),
                round(accuracy), radius, round(margin), message);
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
