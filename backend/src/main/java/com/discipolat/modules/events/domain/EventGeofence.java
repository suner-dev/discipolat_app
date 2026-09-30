package com.discipolat.modules.events.domain;

import java.util.UUID;

/**
 * Moteur de geolocalisation d'un evenement.
 *
 * <p>Decide, cote serveur, si un pointage est recevable. Le client fournit sa
 * position ; le serveur possede le lieu, le rayon et le calcul. C'est la seule
 * facon dont la regle ne soit pas falsifiable par l'appelant.
 *
 * <p>Regle de tolerance : un GPS de telephone rapporte typiquement 10 a 100
 * metres de precision. Refuser une position a 190 metres d'un lieu alors que
 * l'appareil annonce 80 metres de precision serait un pointage refuse a tort,
 * et l'utilisateur n'aurait aucun recours. La regle retenue est donc
 * <em>la position reelle pourrait-elle etre dans le perimetre ?</em> :
 * {@code distance - precision <= rayon}. La distance brute et la marge sont
 * retournees : l'ecran affiche la realite, et une contestation est arbitrable.
 */
public final class EventGeofence {

    /**
     * Precision au-dela de laquelle la position est sans signification : on
     * refuse plutot que d'enregistrer une position dont l'incertitude couvre
     * eventuellement tout le perimetre.
     */
    public static final double MAX_ACCEPTABLE_ACCURACY_METERS = 500.0;

    /** Precision par defaut si l'appareil ne la communique pas. */
    public static final double DEFAULT_ACCURACY_METERS = 50.0;

    private EventGeofence() {
    }

    /** Verdict du moteur, avec les chiffres qui l'ont produit. */
    public record Verdict(
            boolean accepted,
            Reason reason,
            double distanceMeters,
            double accuracyMeters,
            double radiusMeters,
            double marginMeters) {

        public enum Reason {
            /** Position dans le perimetre. */
            DANS_LE_PERIMETRE,
            /** Trop loin, meme en tenant compte de la precision GPS. */
            HORS_PERIMETRE,
            /** Position trop imprecise pour statuer. */
            PRECISION_INSUFFISANTE,
            /** L'evenement n'a pas de lieu configure : aucun perimetre n'existe. */
            LIEU_NON_CONFIGURE,
            /** Coordonnees transmises hors bornes. */
            COORDONNEES_INVALIDES
        }
    }

    /**
     * Evalue un pointage.
     *
     * @param latitude    latitude du lieu de l'evenement, {@code null} si non configure
     * @param longitude   longitude du lieu de l'evenement
     * @param radiusM     rayon d'effet en metres
     * @param clientLat   latitude mesuree par l'appareil
     * @param clientLon   longitude mesuree par l'appareil
     * @param accuracyM   precision annoncee par l'appareil, {@code null} => valeur par defaut
     */
    public static Verdict evaluate(
            Double latitude,
            Double longitude,
            int radiusM,
            double clientLat,
            double clientLon,
            Double accuracyM) {

        // Fail-closed : sans lieu configure, personne ne peut pointer.
        if (latitude == null || longitude == null) {
            return new Verdict(false, Verdict.Reason.LIEU_NON_CONFIGURE,
                    0, 0, radiusM, 0);
        }

        double accuracy = accuracyM == null || accuracyM.isNaN()
                ? DEFAULT_ACCURACY_METERS
                : accuracyM;

        if (accuracy < 0) {
            return new Verdict(false, Verdict.Reason.COORDONNEES_INVALIDES,
                    0, accuracy, radiusM, 0);
        }

        final double distance;
        try {
            distance = GeoDistance.betweenMeters(
                    latitude, longitude, clientLat, clientLon);
        } catch (IllegalArgumentException horsBornes) {
            return new Verdict(false, Verdict.Reason.COORDONNEES_INVALIDES,
                    0, accuracy, radiusM, 0);
        }

        // Precision absurde : on ne statue pas plutot que de statuer sur du bruit.
        if (accuracy > MAX_ACCEPTABLE_ACCURACY_METERS) {
            return new Verdict(false, Verdict.Reason.PRECISION_INSUFFISANTE,
                    distance, accuracy, radiusM, 0);
        }

        // Marge : de combien la position reelle pourrait encore ameliorer.
        double margin = distance - accuracy;
        boolean within = margin <= radiusM;
        return new Verdict(
                within,
                within ? Verdict.Reason.DANS_LE_PERIMETRE : Verdict.Reason.HORS_PERIMETRE,
                distance,
                accuracy,
                radiusM,
                margin);
    }
}
