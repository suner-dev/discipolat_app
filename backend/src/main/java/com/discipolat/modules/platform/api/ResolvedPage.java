package com.discipolat.modules.platform.api;

import com.discipolat.modules.platform.domain.CustomPage;

import java.util.List;

/**
 * Page personnalisée prête à l'affichage : la page (métadonnées, rôles,
 * version) et ses blocs résolus avec leurs données réelles.
 */
public record ResolvedPage(CustomPage page, List<ResolvedBlock> blocks) {
}
