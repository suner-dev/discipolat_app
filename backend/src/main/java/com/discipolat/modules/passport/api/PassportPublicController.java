package com.discipolat.modules.passport.api;

import com.discipolat.modules.passport.domain.PassportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Vérification PUBLIQUE d'un passeport spirituel par code (scan du QR).
 *
 * Chemin sous `/api/v1/public/**` : autorisé en lecture publique par
 * SecurityConfig (GET). N'expose que : verdict, titulaire, émetteur, dates,
 * nombre d'entrées. Chaque tentative est journalisée (traçabilité).
 */
@RestController
@RequestMapping("/api/v1/public/passports")
public class PassportPublicController {

    private final PassportService service;

    public PassportPublicController(PassportService service) {
        this.service = service;
    }

    /** Vérifie un passeport par son code public. */
    @GetMapping("/{code}")
    public ResponseEntity<PassportService.VerificationResult> verify(@PathVariable String code,
                                                                     HttpServletRequest request) {
        String remoteAddr = clientAddr(request);
        String userAgent = request.getHeader("User-Agent");
        return ResponseEntity.ok(service.verifyByCode(code, remoteAddr, userAgent));
    }

    private String clientAddr(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
