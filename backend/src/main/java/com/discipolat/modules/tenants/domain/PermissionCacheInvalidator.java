package com.discipolat.modules.tenants.domain;

import java.util.UUID;

/**
 * PARAMÉTRISATION PAR ÉGLISE (Phase 9) — port publié par le contexte {@code
 * tenants} pour demander le recalcul des permissions effectives d'un compte.
 *
 * <p><strong>Pourquoi un port plutôt que d'appeler {@code PermissionResolver}
 * directement.</strong> Le calcul des permissions servies vit dans le contexte
 * {@code families} (au plus proche des consommateurs historiques). Que le
 * domaine {@code tenants} l'appelle par son type créerait l'arête {@code tenants
 * -> families} ; comme {@code families -> tenants} existe déjà, cela fermerait
 * un couple en <em>couplage réciproque</em>, ce que la règle R6 d'ArchUnit gèle
 * <strong>par objet</strong> : le build rougirait, et surtout les deux contexts
 * deviendraient matériellement indissociables. La règle ADR-002 est explicite —
 * le couplage entre contexts passe par un port publié ou par un événement. Ce
 * interface est ce port : l'implémentation ({@code PermissionResolver}) est
 * branchée au démarrage par Spring, le domaine reste ignorant de son exécution.
 *
 * <p>Convention d'implémentation : invalider <em>après</em> la modification de
 * la matrice, dans la même transaction, de sorte que le cache {@code
 * permission_versions} lise l'état écrit et non l'état précédent.
 */
public interface PermissionCacheInvalidator {

    /**
     * Recalcule et versionne les permissions effectives de {@code userId} dans
     * {@code tenantId}, et publie le changement aux clients connectés.
     *
     * @param tenantId   organisation concernée
     * @param userId     compte dont les droits viennent de changer
     * @param changeType trace lisible de l'origine du changement (auditable du
     *                   côté client : {@code ROLE_PERMISSIONS_ASSIGNED}…)
     */
    void bumpPermissions(UUID tenantId, UUID userId, String changeType);
}
