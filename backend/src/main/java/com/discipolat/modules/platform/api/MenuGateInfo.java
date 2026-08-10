package com.discipolat.modules.platform.api;

/**
 * Information de gating de route pour le frontend : l'URL et le module
 * associé, avec son état d'activation. Permet de bloquer proprement l'accès
 * direct à une route dont le module est désactivé (aucune donnée exposée).
 */
public record MenuGateInfo(String href, String moduleKey, boolean moduleEnabled) {}
