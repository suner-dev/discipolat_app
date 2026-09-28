package com.discipolat.common.domain;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Construction de charges utiles tolérantes aux valeurs nulles.
 *
 * <h2>Pourquoi ce composant existe</h2>
 *
 * <p>{@code Map.of(...)} <b>lève une {@link NullPointerException}</b> dès qu'une
 * seule de ses valeurs est {@code null} : l'implémentation appelle
 * {@code Objects.requireNonNull} sur chaque entrée. Or, dans ce code base, les
 * charges utiles d'audit et d'évènement transportent très souvent des valeurs
 * legitimately absentes — un nœud sans code, un nœud racine sans parent, un
 * abonnement sans date de fin, un template non personnalisé.
 *
 * <p>Conséquence mesurée : chaque occurrence est un HTTP 500, et — parce que la
 * plupart de ces appels sont dans un {@code @Transactional} — souvent un
 * <b>rollback</b>, donc une écriture perdue. C'est-à-dire un défaut
 * <em>fonctionnel</em>, pas une simple gêne d'affichage.
 *
 * <p>Cette classe rend le même service que {@code Map.of} mais tolère le
 * {@code null} : les clés sont conservées, avec leur valeur {@code null}, et
 * l'ordre d'insertion est stable.
 *
 * <h2>Emploi</h2>
 *
 * <pre>{@code
 * // AVANT — lève une NPE si oldCode ou newCode est null
 * auditService.log(..., Map.of("oldName", oldName, "newName", name,
 *                               "oldCode", oldCode, "newCode", code), ...);
 *
 * // APRÈS
 * auditService.log(..., Payloads.of("oldName", oldName, "newName", name,
 *                                   "oldCode", oldCode, "newCode", code), ...);
 * }</pre>
 *
 * <p>Ne pas utiliser {@link #of(Object...)} avec un nombre impair d'arguments :
 * c'est une erreur de programmation, et une {@link IllegalArgumentException} est
 * préférable à une carte silencieusement tronquée.
 *
 * <h2>Garde-fou</h2>
 *
 * <p>Le test {@code NoNullUnsafeMapLiteralTest} interdit la réintroduction de
 * {@code Map.of} dans une charge utile d'audit ou d'évènement. Ce composant et ce
 * test forment donc un correctif <b>systémique</b> : ils empêchent la récurrence
 * au lieu de corriger thirty sites un par un puis de les oublier.
 */
public final class Payloads {

    private Payloads() {
    }

    /**
     * Construit une carte à partir de paires clé/valeur, tolérante au
     * {@code null}.
     *
     * @throws IllegalArgumentException si le nombre d'arguments est impair
     */
    public static Map<String, Object> of(Object... keyValuePairs) {
        if (keyValuePairs == null) {
            return new LinkedHashMap<>();
        }
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "Payloads.of attend des paires clé/valeur, reçu " + keyValuePairs.length
                            + " arguments (dernière clé sans valeur ?)");
        }
        Map<String, Object> payload = new LinkedHashMap<>(keyValuePairs.length);
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            payload.put((String) keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return payload;
    }

    /** Carte à une seule entrée, tolérante au {@code null}. */
    public static Map<String, Object> of(String key, Object value) {
        Map<String, Object> payload = new LinkedHashMap<>(2);
        payload.put(key, value);
        return payload;
    }
}
