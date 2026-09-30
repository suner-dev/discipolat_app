package com.discipolat.modules.events.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regles de decision du moteur de geolocalisation, y compris les cas qui
 * degradent l'experience : un GPS de telephone vaut rarement mieux que 20 m.
 */
class EventGeofenceTest {

    private static final double LIEU_LAT = 4.0511;
    private static final double LIEU_LON = 9.7679;
    private static final int RAYON = 200;

    @Test
    @DisplayName("au point exact du lieu : accepte")
    void surLeLieu() {
        EventGeofence.Verdict v = EventGeofence.evaluate(
                LIEU_LAT, LIEU_LON, RAYON, LIEU_LAT, LIEU_LON, 10.0);
        assertThat(v.accepted()).isTrue();
        assertThat(v.reason()).isEqualTo(EventGeofence.Verdict.Reason.DANS_LE_PERIMETRE);
    }

    @Test
    @DisplayName("100 m du lieu : accepte")
    void dansLePerimetre() {
        // ~111 m au nord
        EventGeofence.Verdict v = EventGeofence.evaluate(
                LIEU_LAT, LIEU_LON, RAYON, LIEU_LAT + 0.001, LIEU_LON, 10.0);
        assertThat(v.accepted()).isTrue();
        assertThat(v.distanceMeters()).isBetween(110.0, 112.0);
    }

    @Test
    @DisplayName("2 km du lieu, GPS fiable : refuse")
    void loinEtPrecise() {
        EventGeofence.Verdict v = EventGeofence.evaluate(
                LIEU_LAT, LIEU_LON, RAYON, LIEU_LAT + 0.018, LIEU_LON, 10.0);
        assertThat(v.accepted()).isFalse();
        assertThat(v.reason()).isEqualTo(EventGeofence.Verdict.Reason.HORS_PERIMETRE);
        assertThat(v.distanceMeters()).isGreaterThan(1900.0);
    }

    @Test
    @DisplayName("190 m du lieu avec un GPS a 80 m de precision : accepte")
    void tolerancePrecisionGps() {
        // Un pointage refuse a tort, sans recours pour l'utilisateur, est un bug :
        // la position reelle pourrait bien etre dans le perimetre.
        EventGeofence.Verdict v = EventGeofence.evaluate(
                LIEU_LAT, LIEU_LON, RAYON, LIEU_LAT + 0.0017, LIEU_LON, 80.0);
        assertThat(v.accepted()).isTrue();
        // marge = distance - precision = 189 - 80 = 109 m, soit 109 m de marge
        // reelle AVANT que la position ne sorte du perimetre de 200 m.
        assertThat(v.marginMeters()).isBetween(108.0, 110.0);
        assertThat(v.marginMeters()).isLessThanOrEqualTo(RAYON);
    }

    @Test
    @DisplayName("300 m avec un GPS a 80 m : refuse malgre la precision")
    void toleranceInsuffisante() {
        EventGeofence.Verdict v = EventGeofence.evaluate(
                LIEU_LAT, LIEU_LON, RAYON, LIEU_LAT + 0.0027, LIEU_LON, 80.0);
        assertThat(v.accepted()).isFalse();
        assertThat(v.reason()).isEqualTo(EventGeofence.Verdict.Reason.HORS_PERIMETRE);
    }

    @Test
    @DisplayName("GPS a plus d'un km : on ne statue pas")
    void precisionAbsurde() {
        EventGeofence.Verdict v = EventGeofence.evaluate(
                LIEU_LAT, LIEU_LON, RAYON, LIEU_LAT, LIEU_LON, 1_200.0);
        assertThat(v.accepted()).isFalse();
        assertThat(v.reason()).isEqualTo(EventGeofence.Verdict.Reason.PRECISION_INSUFFISANTE);
    }

    @Test
    @DisplayName("evenement sans lieu configure : refus, jamais d'acceptation par defaut")
    void lieuNonConfigure() {
        EventGeofence.Verdict v = EventGeofence.evaluate(
                null, null, RAYON, LIEU_LAT, LIEU_LON, 10.0);
        assertThat(v.accepted()).isFalse();
        assertThat(v.reason()).isEqualTo(EventGeofence.Verdict.Reason.LIEU_NON_CONFIGURE);
    }

    @Test
    @DisplayName("position transmise hors bornes : refus, pas de NaN")
    void positionInvalide() {
        EventGeofence.Verdict v = EventGeofence.evaluate(
                LIEU_LAT, LIEU_LON, RAYON, 999.0, LIEU_LON, 10.0);
        assertThat(v.accepted()).isFalse();
        assertThat(v.reason()).isEqualTo(EventGeofence.Verdict.Reason.COORDONNEES_INVALIDES);
    }

    @Test
    @DisplayName("precision non fournie : une valeur par defaut, pas un crash")
    void precisionParDefaut() {
        EventGeofence.Verdict v = EventGeofence.evaluate(
                LIEU_LAT, LIEU_LON, RAYON, LIEU_LAT, LIEU_LON, null);
        assertThat(v.accepted()).isTrue();
        assertThat(v.accuracyMeters()).isEqualTo(EventGeofence.DEFAULT_ACCURACY_METERS);
    }
}
