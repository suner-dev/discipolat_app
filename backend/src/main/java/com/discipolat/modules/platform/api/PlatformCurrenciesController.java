package com.discipolat.modules.platform.api;

import com.discipolat.modules.currency.domain.Iso4217CurrencyValidator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * A3 (M9) — Référentiel public des devises ISO-4217.
 *
 * <p>Le client (web/mobile) ne doit RIEN connaître de figé en matière de
 * monnaie : ce endpoint livre le catalogue complet (code, nom, symbole,
 * décimales) pour que le formatage passe par {@code Intl.NumberFormat} et la
 * validation par les règles livrées, plus par des listes nationales triées sur
 * le volet. Lecture seule, aucune donnée tenant : accessible authentifié,
 * identique pour tous.</p>
 */
@RestController
@RequestMapping("/api/v1/platform/currencies")
public class PlatformCurrenciesController {

    private final Iso4217CurrencyValidator validator;

    public PlatformCurrenciesController(Iso4217CurrencyValidator validator) {
        this.validator = validator;
    }

    @GetMapping
    public Map<String, Object> list() {
        List<Map<String, Object>> currencies = validator.catalog().values().stream()
                .map(entry -> Map.of(
                        "code", entry.get("code"),
                        "name", entry.get("name"),
                        "symbol", entry.get("symbol"),
                        "decimals", entry.get("decimals")))
                .toList();
        return Map.of(
                "standard", "ISO-4217",
                "count", currencies.size(),
                "currencies", currencies);
    }
}
