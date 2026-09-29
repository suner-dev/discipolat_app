package com.discipolat.modules.currency.domain;

import com.discipolat.common.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A3 (M9) — Autorité unique sur la devise ISO-4217 côté serveur.
 *
 * <p>Avant cette classe, la « validation » de devise était une liste codée en
 * dur dans {@code CurrencyService.getSupportedCurrencies()} : toute devise
 * absente de cette liste était structurellement injouable, et le serveur
 * ignorait le nombre de décimales réel d'une monnaie. Le produit doit
 * fonctionner sur toute la planète : la source de vérité est donc
 * {@link java.util.Currency} (jeu ISO-4217 complet livré avec la JVM), et la
 * règle des unités mineures en découle nativement.</p>
 *
 * <p>Règles appliquées :</p>
 * <ul>
 *   <li>un code inconnu d'ISO-4217 est refusé ({@code CURRENCY_UNSUPPORTED}) ;</li>
 *   <li>une devise à 0 unité mineure (JPY, KRW, XOF…) refuse tout montant à
 *       décimales ({@code CURRENCY_DECIMALS_INVALID}) — c'est l'exactitude
 *       comptable, pas une préférence ;</li>
 *   <li>les conversions montant ↔ unités mineures sont centralisées ici pour
 *       qu'aucun module ne réinvente virgule × 100.</li>
 * </ul>
 */
@Service
public class Iso4217CurrencyValidator {

    /** Valide et retourne le code normalisé (majuscules). */
    public java.util.Currency require(String code) {
        if (code == null || code.isBlank()) {
            throw invalid("CURRENCY_REQUIRED", "Aucune devise fournie");
        }
        try {
            return java.util.Currency.getInstance(code.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            throw invalid("CURRENCY_UNSUPPORTED", "Devise inconnue du référentiel ISO-4217 : " + code);
        }
    }

    public boolean isSupported(String code) {
        try {
            require(code);
            return true;
        } catch (DomainException e) {
            return false;
        }
    }

    /** Nombre de décimales autorisées (0 pour XOF, JPY, KRW…). */
    public int minorUnits(String code) {
        return Math.max(0, require(code).getDefaultFractionDigits());
    }

    /**
     * Vérifie qu'un montant respecte la devise : décimales bornées, montant
     * strictement positif. À appeler avant toute persistance monétaire.
     */
    public void validateAmount(String code, BigDecimal amount) {
        java.util.Currency currency = require(code);
        if (amount == null || amount.signum() <= 0) {
            throw invalid("AMOUNT_INVALID", "Le montant doit être strictement positif");
        }
        int allowed = Math.max(0, currency.getDefaultFractionDigits());
        if (amount.stripTrailingZeros().scale() > allowed) {
            throw invalid("CURRENCY_DECIMALS_INVALID",
                    "La devise " + currency.getCurrencyCode() + " n'admet pas de décimales"
                            + (allowed > 0 ? " au-delà de " + allowed : ""));
        }
    }

    /** Convertit un montant majeur en unités mineures exactes. */
    public long toMinorUnits(String code, BigDecimal amount) {
        validateAmount(code, amount);
        return amount.movePointRight(minorUnits(code)).longValueExact();
    }

    /** Convertit des unités mineures en montant majeur exact. */
    public BigDecimal fromMinorUnits(String code, long minor) {
        return BigDecimal.valueOf(minor, minorUnits(code));
    }

    /**
     * Catalogue ISO-4217 complet pour le client : code, nom anglais, symbole
     * par défaut, nombre de décimales. Le frontend formatage avec
     * {@code Intl.NumberFormat} — ce catalogue sert à connaître les règles,
     * jamais à les réécrire.
     */
    public Map<String, Map<String, Object>> catalog() {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        Set<java.util.Currency> currencies = java.util.Currency.getAvailableCurrencies();
        for (java.util.Currency currency : currencies.stream()
                .sorted(java.util.Comparator.comparing(java.util.Currency::getCurrencyCode)).toList()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("code", currency.getCurrencyCode());
            entry.put("name", currency.getDisplayName(Locale.ENGLISH));
            entry.put("symbol", currency.getSymbol(Locale.US));
            entry.put("decimals", Math.max(0, currency.getDefaultFractionDigits()));
            result.put(currency.getCurrencyCode(), entry);
        }
        return result;
    }

    /** Codes du catalogue (pour contrôles d'ensemble côté tests). */
    public Set<String> supportedCodes() {
        return catalog().keySet();
    }

    private static DomainException invalid(String code, String message) {
        return new DomainException(message, HttpStatus.BAD_REQUEST, code);
    }
}
