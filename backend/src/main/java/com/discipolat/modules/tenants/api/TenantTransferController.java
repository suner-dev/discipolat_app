package com.discipolat.modules.tenants.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.ActiveTenantService;
import com.discipolat.modules.tenants.domain.TenantTransferService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * <b>Transfert de membre</b> — SPEC_ORGANISATION_DENOMINATION_V2 §6 / §4.4.
 *
 * <p>Accessible à tout membre authentifié : ce n'est pas une action
 * d'administration. La décision transfert / adhésion est prise par le backend
 * d'après la racine des deux organisations — l'IHM ne fait que la montrer
 * avant confirmation.
 *
 * <p><b>T-B0bis.</b> Comme {@code POST /tenant/join}, la réponse porte les
 * jetons réémis : sans eux, le membre resterait dans l'organisation précédente
 * alors que l'écran annonce « vous êtes transféré ».
 */
@RestController
@RequestMapping("/api/v1/tenant/transfer")
public class TenantTransferController {

    private final TenantTransferService transferService;

    public TenantTransferController(TenantTransferService transferService) {
        this.transferService = transferService;
    }

    public record TransferRequest(String code, String reason) {
    }

    /**
     * Ce qu'il faut savoir avant de confirmer : même réseau ou non
     * (l'historique pastoral est-il préservé ?), déjà membre ou non.
     */
    @GetMapping("/preview")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> preview(@RequestParam String code) {
        UUID userId = TenantContext.getCurrentUserId();
        TenantTransferService.TransferPreview preview = transferService.preview(userId, code);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sameNetwork", preview.sameNetwork());
        body.put("alreadyMember", preview.alreadyMember());
        body.put("activeInOther", preview.activeInOther());
        body.put("fromChurch", preview.fromChurch() == null ? "" : preview.fromChurch());
        body.put("toChurch", preview.toChurch() == null ? "" : preview.toChurch());
        body.put("toKind", preview.toKind() == null ? "" : preview.toKind());
        body.put("willTransfer", preview.sameNetwork() && !preview.alreadyMember());
        return ResponseEntity.ok(body);
    }

    /**
     * Effectue le transfert — ou l'adhésion si les racines diffèrent (§4.4).
     *
     * <p>Idempotent : rejouer le même code répond {@code ALREADY_MEMBER} au
     * lieu de créer une seconde appartenance.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> transfer(@RequestBody TransferRequest request) {
        UUID userId = TenantContext.getCurrentUserId();
        TenantTransferService.TransferOutcome outcome =
                transferService.transfer(userId, request.code(), request.reason());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", outcome.status());
        body.put("fromTenantId", outcome.fromTenantId());
        body.put("toTenantId", outcome.toTenantId());
        body.put("toChurch", outcome.toChurch() == null ? "" : outcome.toChurch());
        if (outcome.tokenSwitched() && outcome.accessToken() != null) {
            body.put("accessToken", outcome.accessToken());
            body.put("refreshToken", outcome.refreshToken() == null ? "" : outcome.refreshToken());
        }
        return ResponseEntity.ok(body);
    }
}
