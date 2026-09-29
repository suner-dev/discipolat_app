package com.discipolat.modules.currency.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class CurrencyService {

    private final CurrencyConfigRepository currencyRepo;
    private final Iso4217CurrencyValidator validator;

    public CurrencyService(CurrencyConfigRepository currencyRepo, Iso4217CurrencyValidator validator) {
        this.currencyRepo = currencyRepo;
        this.validator = validator;
    }

    public List<CurrencyConfig> listCurrencies() {
        return currencyRepo.findByTenantIdAndIsActiveTrueOrderByIsPrimaryDesc(TenantContext.getCurrentTenantId());
    }

    public CurrencyConfig getPrimaryCurrency() {
        return currencyRepo.findByTenantIdAndIsPrimaryTrue(TenantContext.getCurrentTenantId())
                .orElseGet(() -> createDefaultCurrency());
    }

    public CurrencyConfig create(CurrencyConfig config) {
        // A3 (M9) — fail-closed : seule une devise ISO-4217 réelle entre en base.
        validator.require(config.getCurrencyCode());
        config.setTenantId(TenantContext.getCurrentTenantId());
        if (config.getIsPrimary() != null && config.getIsPrimary()) {
            clearPrimaryFlag();
        }
        return currencyRepo.save(config);
    }

    public CurrencyConfig update(UUID id, CurrencyConfig updates) {
        CurrencyConfig existing = currencyRepo.findById(id).orElseThrow();
        if (updates.getCurrencyCode() != null) {
            validator.require(updates.getCurrencyCode());
            existing.setCurrencyCode(updates.getCurrencyCode());
        }
        if (updates.getCurrencySymbol() != null) existing.setCurrencySymbol(updates.getCurrencySymbol());
        if (updates.getTimezone() != null) existing.setTimezone(updates.getTimezone());
        if (updates.getLocale() != null) existing.setLocale(updates.getLocale());
        if (updates.getExchangeRateToUsd() != null) existing.setExchangeRateToUsd(updates.getExchangeRateToUsd());
        if (updates.getIsPrimary() != null && updates.getIsPrimary()) {
            clearPrimaryFlag();
            existing.setIsPrimary(true);
        }
        return currencyRepo.save(existing);
    }

    public void delete(UUID id) {
        CurrencyConfig config = currencyRepo.findById(id).orElseThrow();
        config.setIsActive(false);
        currencyRepo.save(config);
    }

    public Double convertAmount(Double amount, String fromCurrency, String toCurrency) {
        var fromOpt = currencyRepo.findByTenantIdAndCurrencyCode(TenantContext.getCurrentTenantId(), fromCurrency);
        var toOpt = currencyRepo.findByTenantIdAndCurrencyCode(TenantContext.getCurrentTenantId(), toCurrency);
        if (fromOpt.isEmpty() || toOpt.isEmpty()) return amount;
        double fromRate = fromOpt.get().getExchangeRateToUsd();
        double toRate = toOpt.get().getExchangeRateToUsd();
        return amount * (fromRate / toRate);
    }

    public Map<String, Object> getStats() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        var currencies = currencyRepo.findByTenantIdAndIsActiveTrueOrderByIsPrimaryDesc(tenantId);
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCurrencies", currencies.size());
        stats.put("primaryCurrency", getPrimaryCurrency().getCurrencyCode());
        stats.put("timezones", currencies.stream().map(CurrencyConfig::getTimezone).distinct().toList());
        return stats;
    }

    /**
     * A3 (M9) — Le catalogue servi est le référentiel ISO-4217 complet
     * (java.util.Currency), plus une liste nationale triée sur le volet : une
     * devise absente de la liste était structurellement injouable, c'était le
     * bug. Format de réponse conservé (code, symbol, name) + décimales ajoutées
     * (additif) pour que le client formate avec Intl.NumberFormat sans rien figer.
     */
    public List<Map<String, Object>> getSupportedCurrencies() {
        return validator.catalog().values().stream()
                .<Map<String, Object>>map(entry -> Map.of(
                        "code", entry.get("code"),
                        "symbol", entry.get("symbol"),
                        "name", entry.get("name"),
                        "decimals", entry.get("decimals")))
                .toList();
    }

    /**
     * Fuseaux horaires : la planète n'est pas l'Afrique francophone. Liste
     * complète des identifiants IANA canoniques (les liens type « Africa/Accra »
     * inclus dans la JVM sont des alias, tous valides) ; le tenant choisit le
     * sien — le serveur valide l'identifiant, pas le continent.
     */
    public List<Map<String, String>> getSupportedTimezones() {
        return java.time.zone.ZoneRulesProvider.getAvailableZoneIds().stream()
                .sorted()
                .map(id -> Map.of("id", id, "name", id.replace('_', ' ')))
                .toList();
    }

    private void clearPrimaryFlag() {
        currencyRepo.findByTenantIdAndIsPrimaryTrue(TenantContext.getCurrentTenantId())
                .ifPresent(c -> { c.setIsPrimary(false); currencyRepo.save(c); });
    }

    private CurrencyConfig createDefaultCurrency() {
        CurrencyConfig config = new CurrencyConfig();
        config.setTenantId(TenantContext.getCurrentTenantId());
        config.setCurrencyCode("XAF");
        config.setCurrencySymbol("FCFA");
        config.setTimezone("Africa/Douala");
        config.setLocale("fr_FR");
        config.setExchangeRateToUsd(0.0016);
        config.setIsPrimary(true);
        return currencyRepo.save(config);
    }
}
