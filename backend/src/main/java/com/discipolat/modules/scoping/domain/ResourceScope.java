package com.discipolat.modules.scoping.domain;

/**
 * G1.8 §54 — Portee d'une ressource.
 *
 * TENANT_GLOBAL      : partagee a tout le tenant (organization_unit_id NULL).
 * ORGANIZATION_LOCAL : rattachee a une unite d'organisation (visible dans l'unite et ses descendants).
 * UNIT_LOCAL         : rattachee a une unite stricte (meme regle de visibilite que ORGANIZATION_LOCAL,
 *                      mais l'edition est reservee aux responsables de l'unite elle-meme).
 */
public enum ResourceScope {
    TENANT_GLOBAL,
    ORGANIZATION_LOCAL,
    UNIT_LOCAL;

    public boolean isGlobal() {
        return this == TENANT_GLOBAL;
    }

    public boolean isLocal() {
        return this != TENANT_GLOBAL;
    }
}
