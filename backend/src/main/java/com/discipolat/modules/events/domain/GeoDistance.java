package com.discipolat.modules.events.domain;

/**
 * Distance geodesique (formule de Haversine) sur la sphere de WGS84.
 *
 * <p>Utilitaire SANS etat, donc testable sans Spring ni base : c'est la brique la
 * plus critique du moteur de geolocalisation, celle dont l'erreur se verrait
 * immediatement sur le terrain.
 *
 * <p>Choix de la formule : Haversine et non la loi des cosinus, parce
 * qu'elle reste stable pour de tres petites distances (un pointage a 50 metres
 * d'un lieu), ou la formule trigonometrique est numeriquement instable.
 */
public final class GeoDistance {

    /** Rayon moyen de la Terre en metres (WGS84). */
    public static final double EARTH_RADIUS_METERS = 6_371_008.8;

    private GeoDistance() {
    }

    /**
     * Distance en metres entre deux points.
     *
     * @throws IllegalArgumentException si une latitude sort de [-90, 90] ou une
     *         longitude de [-180, 180] : on refuse de calculer plutot que de
     *         renvoyer un nombre faux.
     */
    public static double betweenMeters(double lat1, double lon1, double lat2, double lon2) {
        requireValid(lat1, lon1);
        requireValid(lat2, lon2);

        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2)
                * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
        // min(1) : une protect arithmetique (a peut tres legerement depasser 1
        // a cause des arrondis), pas une correction de donnees.
        double c = 2 * Math.atan2(Math.sqrt(Math.min(1.0, a)), Math.sqrt(1 - Math.min(1.0, a)));
        return EARTH_RADIUS_METERS * c;
    }

    private static void requireValid(double latitude, double longitude) {
        if (Double.isNaN(latitude) || Double.isNaN(longitude)) {
            throw new IllegalArgumentException("Coordonnees invalides");
        }
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude hors bornes : " + latitude);
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude hors bornes : " + longitude);
        }
    }
}
