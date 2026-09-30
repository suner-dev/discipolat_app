package com.discipolat.common.infrastructure.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Persiste un {@code LocalDateTime} dans une colonne {@code TIMESTAMP WITH TIME
 * ZONE}, en l'interpretant comme UTC.
 *
 * <p>Pourquoi ce convertisseur existe : la table vivante {@code event} (V158) a
 * ete creee avec des {@code timestamptz} ({@code start_at}, {@code created_at},
 * {@code deleted_at}…), alors que le modele manipule des {@code LocalDateTime}.
 * Sans convertisseur, Hibernate declare une colonne {@code TIMESTAMP}, voit une
 * divergence de type et, selon le profil :
 * <ul>
 *   <li>{@code ddl-auto: validate} refuse de demarrer ;</li>
 *   <li>{@code ddl-auto: update} <em>altere la colonne</em> pour la ramener en
 *       {@code timestamp} — donc perd silencieusement l'information de fuseau, a
 *       chaque deploiement.</li>
 * </ul>
 *
 * <p>Le choix d'interpreter la valeur naive comme UTC n'est pas arbitraire : il
 * est deja impose par les donnees. {@code V158} a migre les dates legacy par
 * {@code date_debut AT TIME ZONE 'UTC'}, c'est-a-dire en Considerations les
 * {@code timestamp} sans fuseau comme etant deja de l'UTC. Le convertisseur
 * prolonge cette convention au lieu d'en inventer une autre.
 *
 * <p>Il rend aussi explicite ce qui etait implicite : sans lui, la valeur
 * passerait par le fuseau de session PostgreSQL, et le meme evenement changerait
 * de date selon le serveur qui le sert.
 */
@Converter(autoApply = false)
public class LocalDateTimeToUtcConverter implements AttributeConverter<LocalDateTime, OffsetDateTime> {

    @Override
    public OffsetDateTime convertToDatabaseColumn(LocalDateTime attribute) {
        return attribute == null ? null : attribute.atOffset(ZoneOffset.UTC);
    }

    @Override
    public LocalDateTime convertToEntityAttribute(OffsetDateTime dbData) {
        return dbData == null ? null : dbData.toLocalDateTime();
    }
}
