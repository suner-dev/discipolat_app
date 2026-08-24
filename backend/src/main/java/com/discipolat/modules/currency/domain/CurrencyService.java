package com.discipolat.modules.currency.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class CurrencyService {

    private final CurrencyConfigRepository currencyRepo;

    public CurrencyService(CurrencyConfigRepository currencyRepo) {
        this.currencyRepo = currencyRepo;
    }

    public List<CurrencyConfig> listCurrencies() {
        return currencyRepo.findByTenantIdAndIsActiveTrueOrderByIsPrimaryDesc(TenantContext.getCurrentTenantId());
    }

    public CurrencyConfig getPrimaryCurrency() {
        return currencyRepo.findByTenantIdAndIsPrimaryTrue(TenantContext.getCurrentTenantId())
                .orElseGet(() -> createDefaultCurrency());
    }

    public CurrencyConfig create(CurrencyConfig config) {
        config.setTenantId(TenantContext.getCurrentTenantId());
        if (config.getIsPrimary() != null && config.getIsPrimary()) {
            clearPrimaryFlag();
        }
        return currencyRepo.save(config);
    }

    public CurrencyConfig update(UUID id, CurrencyConfig updates) {
        CurrencyConfig existing = currencyRepo.findById(id).orElseThrow();
        if (updates.getCurrencyCode() != null) existing.setCurrencyCode(updates.getCurrencyCode());
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

    public List<Map<String, String>> getSupportedCurrencies() {
        return List.of(
            Map.of("code", "XAF", "symbol", "FCFA", "name", "Franc CFA"),
            Map.of("code", "EUR", "symbol", "€", "name", "Euro"),
            Map.of("code", "USD", "symbol", "$", "name", "Dollar US"),
            Map.of("code", "KES", "symbol", "KSh", "name", "Shilling Kényan"),
            Map.of("code", "NGN", "symbol", "₦", "name", "Naira Nigérian"),
            Map.of("code", "CDF", "symbol", "FC", "name", "Franc Congolais"),
            Map.of("code", "GBP", "symbol", "£", "name", "Livre Sterling"),
            Map.of("code", "BIF", "symbol", "FBu", "name", "Franc Burundais"),
            Map.of("code", "RWF", "symbol", "FRw", "name", "Franc Rwandais"),
            Map.of("code", "ZAR", "symbol", "R", "name", "Rand Sud-Africain")
        );
    }

    public List<Map<String, String>> getSupportedTimezones() {
        return List.of(
            Map.of("id", "Africa/Douala", "name", "Douala (GMT+1)"),
            Map.of("id", "Africa/Lagos", "name", "Lagos (GMT+1)"),
            Map.of("id", "Africa/Nairobi", "name", "Nairobi (GMT+3)"),
            Map.of("id", "Africa/Kinshasa", "name", "Kinshasa (GMT+1)"),
            Map.of("id", "Africa/Kigali", "name", "Kigali (GMT+2)"),
            Map.of("id", "Africa/Bujumbura", "name", "Bujumbura (GMT+2)"),
            Map.of("id", "Europe/Paris", "name", "Paris (GMT+1/+2)"),
            Map.of("id", "Europe/London", "name", "Londres (GMT+0/+1)"),
            Map.of("id", "America/New_York", "name", "New York (GMT-5/-4)"),
            Map.of("id", "Asia/Dubai", "name", "Dubaï (GMT+4)")
        );
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
