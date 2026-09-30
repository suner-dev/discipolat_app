package com.discipolat.modules.events.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Le calcul de distance decide qu'un pointage est accepte ou refuse : il doit
 * etre verifie contre des valeurs CONNUES, pas contre lui-meme.
 */
class GeoDistanceTest {

    @Test
    @DisplayName("le meme point est a zero metre")
    void memePoint() {
        assertThat(GeoDistance.betweenMeters(4.0511, 9.7679, 4.0511, 9.7679))
                .isLessThan(0.001);
    }

    @Test
    @DisplayName("un degres de latitude vaut environ 111,2 km")
    void unDegresDeLatitude() {
        // 1 degre d'arc de meridien = 111,195 km (WGS84 a l'equateur).
        double d = GeoDistance.betweenMeters(0, 0, 1, 0);
        assertThat(d).isBetween(111_000.0, 111_400.0);
    }

    @Test
    @DisplayName("Douala -> Yaounde : ~193,7 km a vol d'oiseau")
    void distanceReelleConnue() {
        // Le chiffre publie (~237 km) est la distance ROUTIERE. Comparer une
        // distance geodesique a un itineraire routier est une erreur de
        // categorie : le premier test l'a attrapee. On verifie donc la
        // geodesique (193,7 km) et on retient l'ecart de reference.
        double d = GeoDistance.betweenMeters(4.0511, 9.7679, 3.8480, 11.5021);
        assertThat(d).isBetween(193_000.0, 194_500.0);
        assertThat(d).isLessThan(237_000.0); // toujours plus court que la route
    }

    @Test
    @DisplayName("symetrie : A->B et B->A donnent la meme distance")
    void symetrie() {
        double ab = GeoDistance.betweenMeters(48.8566, 2.3522, 45.7640, 4.8357);
        double ba = GeoDistance.betweenMeters(45.7640, 4.8357, 48.8566, 2.3522);
        assertThat(ab).isCloseTo(ba, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    @DisplayName("stabile sur de tres petites distances (le cas d'usage : un pointage)")
    void tresPetiteDistance() {
        // ~11 m vers le nord (0,0001 degre de latitude).
        double d = GeoDistance.betweenMeters(4.0511, 9.7679, 4.0512, 9.7679);
        assertThat(d).isBetween(11.0, 11.2);
    }

    @Test
    @DisplayName("antimeridien : on ne rebrasse pas le globe")
    void antimeridien() {
        double d = GeoDistance.betweenMeters(0, 179.99, 0, -179.99);
        assertThat(d).isLessThan(2_300.0); // ~2,2 km, pas 40 000 km
    }

    @ParameterizedTest
    @CsvSource({
            "91, 0", "0, 181", "-90.1, 0", "0, -180.1"
    })
    @DisplayName("coordonnees hors bornes : refus explicite plutot qu'un chiffre faux")
    void coordonneesHorsBornes(double lat, double lon) {
        assertThatThrownBy(() -> GeoDistance.betweenMeters(lat, lon, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("NaN refuse aussi")
    void nanRefuse() {
        assertThatThrownBy(() -> GeoDistance.betweenMeters(Double.NaN, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
