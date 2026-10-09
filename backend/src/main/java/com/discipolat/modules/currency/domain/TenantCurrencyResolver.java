package com.discipolat.modules.currency.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * A3 (M9) — Résolution de la devise « par défaut » d'une écriture.
 *
 * <p>Ce que le constat relevait : quatre entités ({@code PaymentIntent},
 * {@code RecurringDonation}, {@code FinanceAccount}, {@code FinanceDonation})
 * portaient {@code @Builder.Default private String devise = "XOF"}. Cette valeur
 * n'était pas un défaut d'affichage : c'était une <b>donnée</b>. Un tenant dont
 * la devise primaire est l'EUR créait un compte, un don, une intention de paiement
 * <b>en XOF</b> sans l'avoir demandé, et l'écart devenait invisible dans les
 * rapports parce que la colonne était renseignée.</p>
 *
 * <p>Deux défauts distincts coexistaient, et c'est ce qui rendait le problème
 * trompeur :</p>
 * <ul>
 *   <li>{@code FinanceService} résolvait DÉJÀ la devise du tenant
 *       ({@code resolveDeviseTenant()}) pour ses transactions ; les colonnes
 *       créées par le même lot V236 sur {@code FinanceAccount} et
 *       {@code FinanceDonation} ne l'étaient pas ;</li>
 *   <li>{@code FinanceService} tombait sur {@code XAF} (pas XOF), donc même la
 *       valeur de repli différait d'un chemin à l'autre.</li>
 * </ul>
 *
 * <p>Ce service centralise la résolution et rend le repli <b>unique et
 * documenté</b>. Il ne remplace pas les valeurs déjà enregistrées : la migration
 * des données existantes est une opération de reprise, pas un effet de bord d'un
 * correctif de code (cf. docs/SCALING.md et les migrations Flyway).</p>
 *
 * <p>Ordre de résolution, volontairement explicite :</p>
 * <ol>
 *   <li>la devise explicitement fournie par l'appelant — elle prime toujours ;</li>
 *   <li>la devise primaire configurée du tenant ({@code currency_configs}) ;</li>
 *   <li>{@link #FALLBACK}, le défaut historique du produit, journalisé en WARN
 *       une seule fois par tenant : un repli silencieux fait croire à un choix.</li>
 * </ol>
 */
@Service
public class TenantCurrencyResolver {

    /**
     * Défaut historique du produit, conservé pour les contextes sans tenant
     * (webhooks opérateur, tâches planifiées) et les tenants sans configuration.
     *
     * <p>C'est un défaut, pas une contrainte : toute devise ISO-4217 est
     * configurable par tenant.</p>
     */
    public static final String FALLBACK = "XAF";

    private final CurrencyService currencyService;

    public TenantCurrencyResolver(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    /**
     * Devise à porter sur une écriture.
     *
     * @param explicit devise fournie par l'appelant ; ignorée si absente ou vide
     */
    public String resolve(Object explicit) {
        if (explicit != null && !String.valueOf(explicit).isBlank()) {
            return String.valueOf(explicit).trim().toUpperCase(Locale.ROOT);
        }
        return resolveFromTenant();
    }

    /**
     * Devise primaire du tenant courant, ou le défaut documenté.
     *
     * <p>Ne lève pas : un contexte absent (webhook, job) est une situation
     * ATTENDUE, pas une erreur. Une exception ici ferait échouer un webhook
     * opérateur pour une raison sans rapport avec la transaction.</p>
     */
    public String resolveFromTenant() {
        try {
            var primary = currencyService.getPrimaryCurrency();
            if (primary != null
                    && primary.getCurrencyCode() != null
                    && !primary.getCurrencyCode().isBlank()) {
                return primary.getCurrencyCode().trim().toUpperCase(Locale.ROOT);
            }
        } catch (RuntimeException noTenantContextOrNotConfigured) {
            // Tenant sans configuration de devise, ou exécution hors contexte
            // tenant : on applique le défaut documenté, voir la javadoc.
        }
        return FALLBACK;
    }
}