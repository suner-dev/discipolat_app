package com.discipolat.modules.currency.domain;

import jakarta.persistence.*;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "currency_configs")
public class CurrencyConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String currencyCode; // XAF, EUR, USD, KES, etc.

    @Column(nullable = false)
    private String currencySymbol; // FCFA, €, $, KSh

    private String timezone; // Africa/Douala, Europe/Paris, etc.

    private String locale; // fr_FR, en_US, sw_KE

    private Double exchangeRateToUsd = 1.0;

    @Column(nullable = false)
    private Boolean isPrimary = true;

    @Column(nullable = false)
    private Boolean isActive = true;

    // Getters & setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public String getCurrencySymbol() { return currencySymbol; }
    public void setCurrencySymbol(String currencySymbol) { this.currencySymbol = currencySymbol; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public Double getExchangeRateToUsd() { return exchangeRateToUsd; }
    public void setExchangeRateToUsd(Double exchangeRateToUsd) { this.exchangeRateToUsd = exchangeRateToUsd; }
    public Boolean getIsPrimary() { return isPrimary; }
    public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
