package com.discipolat.modules.payments.payout;

import com.discipolat.common.exception.DomainException;
import com.discipolat.modules.currency.domain.Iso4217CurrencyValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A3 (M8) — Service de décaissement : le seul point d'entrée vers le SPI
 * {@link PayoutProvider}.
 *
 * <p>Ce que le constat relevait : {@link PayoutProviderRegistry} existait, ses
 * fournisseurs appelaient réellement les API opérateur (Stripe
 * {@code POST /v1/transfers}, PayPal {@code /v1/payments/payouts}, SEPA avec
 * validation IBAN mod-97), mais <b>aucun service métier ni contrôleur
 * n'invoquait le registre</b>. {@code initiate()} n'était atteignable que
 * depuis un test : le SPI était écrit, testé, documenté — et mort en
 * production.</p>
 *
 * <p>Ce service ferme l'écart, avec trois garde-fous non négociables :</p>
 * <ol>
 *   <li><b>Fournisseur inconnu → 400</b> et non « repli sur un défaut ». Envoyer
 *       vers un autre opérateur que celui demandé serait un transfert d'argent
 *       vers le mauvais destinataire.</li>
 *   <li><b>Fournisseur connu mais inactif → 503</b> avec la raison honnête
 *       rapportée par le fournisseur. Jamais un succès simulé : c'est la règle
 *       fail-closed de l'SPI.</li>
 *   <li><b>Idempotence</b> : la clé métier est dérivée de la référence
 *       décaissée, donc un rejeu de la même demande ne déplace pas deux fois de
 *       l'argent. C'est porté par le champ {@code idempotencyKey} du
 *       {@link PayoutRequest}, que chaque fournisseur transmet.</li>
 * </ol>
 *
 * <p>Aucune persistance n'est introduite ici : le SPI ne définit pas de contrat
 * de stockage, et inventer une table de décaissements dans ce lot serait créer
 * une source de vérité que le webhook de réconciliation devra réconcilier. Le
 * service est donc volontairement sans état, et le contrôleur expose le
 * diagnostic du registre — c'est ce qui manquait réellement.</p>
 */
@Service
public class PayoutService {

    private static final Logger log = LoggerFactory.getLogger(PayoutService.class);

    private final PayoutProviderRegistry registry;
    private final Iso4217CurrencyValidator currencyValidator;

    public PayoutService(PayoutProviderRegistry registry,
                         Iso4217CurrencyValidator currencyValidator) {
        this.registry = registry;
        this.currencyValidator = currencyValidator;
    }

    /**
     * Déclenche un décaissement via le fournisseur demandé.
     *
     * @throws DomainException 400 si la clé de fournisseur est inconnue ou la
     *         devise invalide ; 503 si le fournisseur n'est pas exploitable.
     */
    @Transactional(readOnly = true)
    public PayoutResult initiate(String providerKey, PayoutRequest request) {
        PayoutProvider provider = requireActive(providerKey);
        String currency = requireKnownCurrency(request.currency());

        PayoutRequest validated = new PayoutRequest(
                idempotencyKeyFor(request),
                request.recipient(),
                request.amountMinor(),
                currency,
                request.reference(),
                request.metadata());

        log.info("Décaissement {} : fournisseur={}, devise={}, unité mineure={}, référence={}",
                validated.reference(), provider.key(), currency,
                validated.amountMinor(), provider.key());

        // Aucun try/catch : une DomainException (503/502) doit remonter telle
        // quelle, avec son code métier. L'encapsuler en RuntimeException
        // générique transformait un « Stripe non configuré » en 500 inexpliqué.
        return provider.initiate(validated);
    }

    /**
     * Statut d'une référence fournisseur.
     *
     * <p>Ne lève pas : un fournisseur injoignable donne
     * {@link PayoutStatus#UNKNOWN}, jamais une exception. Un webhook de
     * réconciliation qui reçoit une 500 parce que l'opérateur est lent
     * rejouerait indéfiniment sans rien apprendre.</p>
     */
    @Transactional(readOnly = true)
    public PayoutStatus queryStatus(String providerKey, String providerReference) {
        if (providerKey == null || providerKey.isBlank()
                || providerReference == null || providerReference.isBlank()) {
            return PayoutStatus.UNKNOWN;
        }
        Optional<PayoutProvider> provider = registry.findActive(providerKey);
        if (provider.isEmpty()) {
            return PayoutStatus.UNKNOWN;
        }
        return provider.get().queryStatus(providerReference);
    }

    /**
     * Vérifie la signature d'un webhook fournisseur.
     *
     * <p>Fail-closed : toute anomalie (secret absent, signature absente, corps
     * vide) donne {@code false}. Le SPI l'implémente déjà ainsi ; ce service se
     * contente de ne pas l'assouplir.</p>
     */
    @Transactional(readOnly = true)
    public boolean verifyWebhookSignature(String providerKey,
                                          Map<String, String> headers,
                                          String rawBody) {
        Optional<PayoutProvider> provider = registry.findActive(providerKey);
        if (provider.isEmpty()) {
            return false;
        }
        return provider.get().verifyWebhookSignature(
                headers == null ? Map.of() : headers,
                rawBody == null ? "" : rawBody);
    }

    /**
     * Diagnostic des canaux de décaissement : ce qui est actif, ce qui ne l'est
     * pas, et POURQUOI.
     *
     * <p>Expose uniquement {@code key}/{@code configured}/{@code reason} : aucun
     * identifiant d'API, aucune URL signée (voir la règle analogue de
     * {@code SystemConfigSummaryController}).</p>
     */
    @Transactional(readOnly = true)
    public Map<String, Object> channelStatus() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("activeCount", registry.activeCount());
        body.put("channels", registry.status());
        return body;
    }

    private PayoutProvider requireActive(String providerKey) {
        if (providerKey == null || providerKey.isBlank()) {
            throw new DomainException("Le canal de décaissement est requis",
                    HttpStatus.BAD_REQUEST, "PAYOUT_PROVIDER_REQUIRED");
        }
        String normalized = providerKey.trim().toLowerCase();

        // Distinguer « inconnu » de « connu mais inactif » : les deux sont des
        // erreurs, mais pas la même. « Inactif » se répare en configuration ;
        // « inconnu » se répare dans le code appelant.
        boolean known = registry.status().containsKey(normalized);
        if (!known) {
            throw new DomainException(
                    "Canal de décaissement inconnu : " + normalized,
                    HttpStatus.BAD_REQUEST, "PAYOUT_PROVIDER_UNKNOWN");
        }
        return registry.findActive(normalized).orElseThrow(() -> new DomainException(
                "Canal de décaissement non configuré : " + normalized
                        + " (" + registry.status().get(normalized).get("reason") + ")",
                HttpStatus.SERVICE_UNAVAILABLE, "PAYOUT_PROVIDER_NOT_CONFIGURED"));
    }

    private String requireKnownCurrency(String currency) {
        if (currency == null || !currency.matches("^[A-Z]{3}$")) {
            throw new DomainException("Devise invalide : " + currency,
                    HttpStatus.BAD_REQUEST, "PAYOUT_CURRENCY_INVALID");
        }
        if (!currencyValidator.isSupported(currency)) {
            throw new DomainException(
                    "Devise inconnue du catalogue ISO-4217 : " + currency,
                    HttpStatus.BAD_REQUEST, "PAYOUT_CURRENCY_UNKNOWN");
        }
        return currency;
    }

    /**
     * Clé d'idempotence dérivée de la référence métier.
     *
     * <p>Un UUID pur serait unique à chaque essai et casserait l'idempotence en
     * cas de rejeu ; un hachage de la seule référence est stable mais risquerait
     * une collision entre deux tenants décaissant la même référence. On combine
     * les deux : stable pour une même référence, distincte entre tenants.</p>
     */
    private String idempotencyKeyFor(PayoutRequest request) {
        // Une clé fournie par l'appelant est respectée telle quelle : elle vient
        // d'un flux amont (webhook opérateur, job de lot) qui a déjà calculé son
        // propre jeton d'idempotence. Le écraser ici casserait la
        // déduplication en amont. La clé dérivée ne sert que lorsqu'aucune n'est
        // fournie — le SPI l'exigeant pour ne jamais double-voter.
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            return request.idempotencyKey();
        }
        java.util.UUID current = com.discipolat.common.multitenancy.TenantContext.getCurrentTenantId();
        String tenantId = current == null ? null : current.toString();
        String seed = (tenantId == null ? "no-tenant" : tenantId) + "|" + request.reference();
        return "payout-" + UUID.nameUUIDFromBytes(seed.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}