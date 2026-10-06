package com.discipolat.modules.relations.api.dto;

import java.util.UUID;

/**
 * Vue publique d'un rattachement déclaratif (V231).
 *
 * <p>Record immuable sérialisé tel quel par Jackson : les noms de composants
 * <b>constituent le contrat JSON</b>. Les 9 premières clés sont celles déjà
 * livrées par l'implémentation initiale (compatibilité ascendante garantie) ;
 * {@code fromNom}/{@code toNom} complètent le contrat annoncé.
 *
 * @param id          identifiant du rattachement
 * @param fromUserId  membre qui déclare
 * @param fromNom     nom du membre qui déclare
 * @param toUserId    autorité déclarée
 * @param toNom       nom de l'autorité déclarée
 * @param otherUserId l'interlocuteur vu depuis la perspective du lecteur
 * @param otherNom    nom de l'interlocuteur
 * @param relationType code technique (PASTEUR, MENTOR…)
 * @param typeLabel    libellé paramétré par l'église
 * @param statut       ACTIVE | REVOKED
 * @param note         note libre du déclarant
 * @param createdAt    création (ISO-8601)
 * @param endedAt      fin de relation (ISO-8601), {@code null} si active
 * @param declaredBy   auteur de la déclaration (le déclarant, ou un modérateur)
 * @param revocable    {@code true} si l'utilisateur courant peut la retirer
 */
public record MemberRelationView(
        UUID id,
        UUID fromUserId,
        String fromNom,
        UUID toUserId,
        String toNom,
        UUID otherUserId,
        String otherNom,
        String relationType,
        String typeLabel,
        String statut,
        String note,
        String createdAt,
        String endedAt,
        UUID declaredBy,
        boolean revocable) {
}
