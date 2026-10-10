package com.discipolat.modules.audit.domain;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Port de lecture des adresses électroniques — première rupture de couple V0.15
 * ({@code audit <-> users}, règle R6 d'ArchUnit).
 *
 * <p><b>Pourquoi ce port existe</b> : l'export CSV d'audit et le fil d'activité du tableau de
 * bord affichent l'email de l'acteur, pas son UUID. Jusqu'ici {@code AuditService} allait chercher
 * cette information en pénétrant les <b>internes</b> du contexte {@code users}
 * ({@code users.domain.User} et {@code users.domain.UserRepository}), pendant que {@code users}
 * appelait {@code AuditService} pour journaliser ses propres opérations. Les deux contexts se
 * connaissaient mutuellement : un cycle, donc une extraction matériellement impossible
 * (ADR-002, {@code docs/architecture/backend-target-architecture.md} §4.1 — Maven refuse les
 * cycles entre modules).
 *
 * <p><b>Ce qui est inversé</b> : c'est le consommateur ({@code audit}) qui publie l'abstraction
 * dont il a besoin, et le détenteur de la donnée ({@code users}) qui fournit l'adaptateur
 * ({@code modules/users/service/UserEmailAdapter}). Le graphe ne garde plus qu'un sens, licite :
 * {@code users -> audit}. Aucun contrat de transport n'est touché : mêmes colonnes de CSV, mêmes
 * clés du fil d'activité.
 *
 * <p><b>Précédent de style dans le dépôt</b> :
 * {@code modules/onboarding/domain/TenantOnboardingStatusPort}, même forme (port publié par le
 * consommateur dans son {@code domain}, adaptateur fourni par l'autre contexte).
 */
public interface UserEmailPort {

    /**
     * Adresse électronique par identifiant utilisateur, pour les identifiants demandés.
     *
     * <p>Le contrat est une lecture <b>en lot</b> — un export de 50 000 lignes ne doit pas
     * déclencher 50 000 requêtes — et {@code Map.of()} quand il n'y a rien à résoudre.
     *
     * <p>Comportement repris à l'identique de l'appel direct que faisait {@code AuditService} :
     * un utilisateur dont l'email est {@code null} fait échouer la résolution (le collecteur
     * refuse les valeurs nulles). Ce n'est pas une régression introduite ici mais un constat
     * préexistant ; le changer relève d'une PR métier, pas d'une PR de frontière (règle A4 :
     * aucun comportement observable modifié par V0).
     */
    Map<UUID, String> emailsOf(Collection<UUID> userIds);
}
