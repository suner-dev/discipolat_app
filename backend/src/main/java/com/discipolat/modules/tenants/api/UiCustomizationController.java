package com.discipolat.modules.tenants.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.UiCustomizationService;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * LOT 2 §LB — API de paramétrage fin de l'interface (« rien en dur »).
 *
 * <p><b>Lecture ouverte</b> ({@code isAuthenticated}) : chaque utilisateur doit
 * recevoir ses surcharges de libellés et ses réglages de fonctionnalités, sinon
 * la personnalisation de son église ne s'affiche pas. La réponse ne contient
 * qu'un texte d'affichage et des interrupteurs — <b>aucune donnée métier</b>,
 * et rien quiaption d'une autre église.
 *
 * <p><b>Écriture</b> réservée à l'admin de l'église
 * ({@code @authz.isTenantAdmin()} — qui lit {@code tenant_memberships}, jamais
 * le claim du JWT, cf. F10). Les réglages <b>globaux</b> restent en lecture
 * seule ici : ils se modifient depuis la console plateforme.
 */
@RestController
@RequestMapping("/api/v1/tenant/customization")
public class UiCustomizationController {

    private final UiCustomizationService customizationService;
    private final SecurityUtils securityUtils;

    public UiCustomizationController(UiCustomizationService customizationService, SecurityUtils securityUtils) {
        this.customizationService = customizationService;
        this.securityUtils = securityUtils;
    }

    public record LabelRequest(String labelKey, String locale, String value,
                               String description, Boolean enabled, UUID nodeId) {
    }

    public record FeatureRequest(String pageKey, String featureKey, String labelOverride,
                                 Boolean enabled, Integer displayOrder, String moduleKey,
                                 String description, UUID nodeId) {
    }

    /**
     * Réglages effectifs de l'utilisateur : surcharges de libellés (par langue)
     * et fonctionnalités activables par écran.
     *
     * @param locale langue courante ; les surcharges « toutes langues » s'y appliquent aussi
     * @param nodeId nœud du réseau courant, pour un réglage propre à une église
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> mine(
            @RequestParam(required = false) String locale,
            @RequestParam(required = false) UUID nodeId) {
        // Sans locale explicite, on suit la locale de la requête (Accept-Language
        // via le LocaleResolver de Spring) — le client envoie de toute façon sa
        // langue dans ce paramètre, ce paramètre reste donc prioritaire.
        String effectiveLocale = (locale == null || locale.isBlank())
                ? LocaleContextHolder.getLocale().getLanguage()
                : locale;
        return ResponseEntity.ok(customizationService
                .customizationFor(TenantContext.getCurrentTenantId(), effectiveLocale, nodeId));
    }

    /** Réglages bruts de l'église, pour l'écran d'administration. */
    @GetMapping("/admin")
    @PreAuthorize("@authz.isTenantAdmin()")
    public ResponseEntity<Map<String, Object>> admin() {
        return ResponseEntity.ok(customizationService.adminView(TenantContext.getCurrentTenantId()));
    }

    @PostMapping("/labels")
    @PreAuthorize("@authz.isTenantAdmin()")
    public ResponseEntity<Void> upsertLabel(@RequestBody LabelRequest req) {
        customizationService.upsertLabel(TenantContext.getCurrentTenantId(), req.nodeId(),
                req.labelKey(), req.locale(), req.value(), req.description(), req.enabled());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/labels/{id}")
    @PreAuthorize("@authz.isTenantAdmin()")
    public ResponseEntity<Void> deleteLabel(@PathVariable UUID id) {
        customizationService.deleteLabel(TenantContext.getCurrentTenantId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/features")
    @PreAuthorize("@authz.isTenantAdmin()")
    public ResponseEntity<Void> upsertFeature(@RequestBody FeatureRequest req) {
        customizationService.upsertFeature(TenantContext.getCurrentTenantId(), req.nodeId(),
                req.pageKey(), req.featureKey(), req.labelOverride(), req.enabled(),
                req.displayOrder(), req.moduleKey(), req.description());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/features/{id}")
    @PreAuthorize("@authz.isTenantAdmin()")
    public ResponseEntity<Void> deleteFeature(@PathVariable UUID id) {
        customizationService.deleteFeature(TenantContext.getCurrentTenantId(), id);
        return ResponseEntity.noContent().build();
    }
}