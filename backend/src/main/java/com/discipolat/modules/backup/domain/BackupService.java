package com.discipolat.modules.backup.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Contrat du service de sauvegarde / restauration.
 *
 * <p><b>Isolation tenant.</b> Aucune méthode ne prend de tenant id dans un
 * corps de requête : le tenant est toujours un paramètre explicite fourni par
 * l'appelant, lui-même alimenté par {@code TenantContext.requireTenantId()}.
 * Le service filtre systématiquement sur ce tenant, y compris en défense
 * profonde quand le dépôt renvoie déjà des lignes d'un autre tenant : une fuite
 * inter-tenant ne doit pas dépendre d'une seule couche.
 *
 * <p><b>Implémentation.</b> L'archivage est un export logique fait en JDBC
 * depuis la {@code DataSource} existante (voir
 * {@code scripts/backup.sh} pour l'équivalent shell). Aucun processus externe
 * n'est lancé : ni {@code pg_dump}, ni shell, donc aucune chaîne de commande
 * construite à partir d'une entrée utilisateur.
 */
public interface BackupService {

    /**
     * Crée une sauvegarde du tenant et renvoie son descripteur.
     *
     * <p>Un tenant sans aucune donnée produit une archive valide et vide :
     * l'opération réussit.
     *
     * @param tenantId tenant à sauvegarder (obligatoire)
     * @param actorId  auteur de la demande
     * @return descripteur complet de l'archive créée (statut {@link BackupStatus#COMPLETED})
     * @throws IllegalArgumentException si {@code tenantId} est nul
     * @throws IllegalStateException    si l'archive n'a pas pu être écrite
     */
    BackupDescriptor create(UUID tenantId, UUID actorId);

    /**
     * Idem {@link #create(UUID, UUID)} mais renvoie aussi les compteurs de
     * tables et de lignes exportées.
     *
     * @param tenantId tenant à sauvegarder (obligatoire)
     * @param actorId  auteur de la demande
     * @return rapport détaillé de l'archivage
     */
    BackupResult createWithReport(UUID tenantId, UUID actorId);

    /**
     * Recherche une sauvegarde par son identifiant, dans le périmètre d'un tenant.
     *
     * @param id       identifiant de la sauvegarde
     * @param tenantId tenant courant
     * @return le descripteur si la sauvegarde existe ET appartient à ce tenant,
     *         sinon {@link Optional#empty()} (une sauvegarde d'un autre tenant
     *         est traitée comme inexistante : on ne confirme pas son existence)
     */
    Optional<BackupDescriptor> find(UUID id, UUID tenantId);

    /**
     * Liste les sauvegardes d'un tenant, de la plus récente à la plus ancienne.
     *
     * @param tenantId tenant courant
     * @return sauvegardes appartenant à ce tenant uniquement
     */
    List<BackupDescriptor> list(UUID tenantId);

    /**
     * Vérifie l'intégrité d'une archive en RELISANT le fichier et en recalculant
     * son SHA-256.
     *
     * <p>Met à jour le statut du descripteur : {@link BackupStatus#VERIFIED} si
     * l'archive est conforme, {@link BackupStatus#FAILED} si elle est corrompue
     * ou absente.
     *
     * @param id       identifiant de la sauvegarde
     * @param tenantId tenant courant
     * @return résultat de la vérification, jamais nul
     * @throws com.discipolat.common.exception.ResourceNotFoundException si la
     *         sauvegarde n'existe pas pour ce tenant
     */
    VerificationResult verify(UUID id, UUID tenantId);

    /**
     * Supprime une sauvegarde : l'archive sur disque puis son descripteur.
     *
     * @param id       identifiant de la sauvegarde
     * @param tenantId tenant courant
     * @throws com.discipolat.common.exception.ResourceNotFoundException si la
     *         sauvegarde n'existe pas pour ce tenant
     */
    void delete(UUID id, UUID tenantId);

    /**
     * Résout l'archive sur le disque en vue d'un téléchargement.
     *
     * @param id       identifiant de la sauvegarde
     * @param tenantId tenant courant
     * @return l'archive si elle existe pour ce tenant et est présente sur le disque
     */
    Optional<BackupArchive> openArchive(UUID id, UUID tenantId);

    /**
     * Applique la politique de rétention d'un tenant : supprime les archives
     * (fichier + descripteur) plus anciennes que la rétention configurée.
     *
     * <p>Reflète le nettoyage à 30 jours de {@code scripts/backup.sh}.
     * Une rétention à 0 ou négative désactive le nettoyage.
     *
     * @param tenantId tenant courant
     * @return nombre d'archives supprimées
     */
    int purgeExpired(UUID tenantId);
}
