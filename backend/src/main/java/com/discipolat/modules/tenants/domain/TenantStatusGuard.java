package com.discipolat.modules.tenants.domain;

import com.discipolat.common.exception.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Constat B1 — garde de statut d'un tenant (constat {@code §4 A1}).
 *
 * <p>Un tenant {@code SUSPENDED} ou {@code CANCELLED} ne doit plus pouvoir se
 * connecter, rafraîchir son jeton, changer d'organisation, ni appeler aucune API.
 * Avant ce correctif, seule la désactivation cosmétique existait : le service
 * restait pleinement accessible.
 *
 * <p><b>Fail-closed par construction</b> :
 * <ul>
 *   <li>tout statut autre que {@code ACTIVE} / {@code PENDING_SETUP} est refusé ;</li>
 *   <li>toute erreur de lecture du statut (indisponibilité base, tenant
 *       introuvable, panne) est traduite en {@code 403 TENANT_STATUS_UNAVAILABLE}
 *       et <b>jamais</b> en 500 ni en accès accordé.</li>
 * </ul>
 *
 * <p><b>Coût</b> : le statut est mis en cache 30 s (décision D1) afin de ne pas
 * ajouter une lecture base par requête en régime normal. La cache est invalidée
 * immédiatement par {@link TenantStatusChangedEvent} : une suspension ou une
 * réactivation est donc visible <b>immédiatement</b>, sans attendre le TTL.
 *
 * <p><b>La table {@code tenants} est globale</b> (elle définit le tenant, elle
 * n'est pas filtrée par {@code tenantFilter}) : cette lecture n'est donc jamais
 * restreinte au tenant courant — c'est indispensable pour contrôler un tenant
 * <i>cible</i> lors d'un changement d'organisation.
 */
@Service
public class TenantStatusGuard {

    private static final Logger log = LoggerFactory.getLogger(TenantStatusGuard.class);

    /** TTL du cache de statut (décision D1 du plan). */
    public static final long CACHE_TTL_MILLIS = 30_000L;

    static final String CODE_SUSPENDED = "TENANT_SUSPENDED";
    static final String CODE_CANCELLED = "TENANT_CANCELLED";
    static final String CODE_UNAVAILABLE = "TENANT_STATUS_UNAVAILABLE";

    static final String DETAIL_SUSPENDED =
            "Le service de cette église est suspendu. Contactez le support Discipolat.";
    static final String DETAIL_CANCELLED =
            "Le service de cette église a été résilié. Contactez le support Discipolat.";
    static final String DETAIL_UNAVAILABLE =
            "Le statut de cette église est momentanément indisponible. "
                    + "Aucun accès n'est accordé par défaut. Réessayez plus tard ou contactez le support Discipolat.";

    private final TenantRepository tenantRepository;
    private final ConcurrentHashMap<UUID, CacheEntry> cache = new ConcurrentHashMap<>();

    /** Horloge injectable pour les tests (TTL, fail-closed). */
    private Clock clock = Clock.systemUTC();

    public TenantStatusGuard(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    /** @visibleForTesting — permet de contrôler le temps sans attendre 30 s. */
    void setClock(Clock clock) {
        this.clock = clock;
    }

    /**
     * Vérifie qu'un tenant est utilisable. Ne fait rien si {@code tenantId} est
     * null (super admin plateforme agissant hors contexte tenant).
     *
     * @throws DomainException 403 avec {@code TENANT_SUSPENDED},
     *                         {@code TENANT_CANCELLED} ou {@code TENANT_STATUS_UNAVAILABLE}
     */
    public void assertAccessible(UUID tenantId) {
        if (tenantId == null) {
            return;
        }
        TenantStatus status = resolveStatusOrFailClosed(tenantId);
        switch (status) {
            case ACTIVE, PENDING_SETUP -> {
                // Tenant utilisable.
            }
            case SUSPENDED -> throw new DomainException(
                    DETAIL_SUSPENDED, HttpStatus.FORBIDDEN, CODE_SUSPENDED);
            case CANCELLED -> throw new DomainException(
                    DETAIL_CANCELLED, HttpStatus.FORBIDDEN, CODE_CANCELLED);
        }
    }

    /**
     * Statut courant d'un tenant, servi par la cache quand elle est encore
     * valide, sinon relu en base.
     */
    @Transactional(readOnly = true)
    public TenantStatus resolveStatusOrFailClosed(UUID tenantId) {
        CacheEntry cached = cache.get(tenantId);
        if (cached != null && cached.expiresAt().isAfter(Instant.now(clock))) {
            return cached.status();
        }

        TenantStatus status;
        try {
            Optional<Tenant> tenant = tenantRepository.findById(tenantId);
            if (tenant.isEmpty()) {
                // Fail-closed : sans tenant lisible, aucun accès n'est accordé et
                // on ne renvoie surtout pas un 500.
                log.error("TenantStatusGuard: tenant {} introuvable — accès refusé (fail-closed)", tenantId);
                evict(tenantId);
                throw new DomainException(
                        DETAIL_UNAVAILABLE, HttpStatus.FORBIDDEN, CODE_UNAVAILABLE,
                        Map.of("tenantId", tenantId.toString(), "reason", "TENANT_NOT_FOUND"));
            }
            status = tenant.get().getStatus();
        } catch (DomainException alreadyThrown) {
            throw alreadyThrown;
        } catch (RuntimeException failure) {
            log.error("TenantStatusGuard: lecture du statut du tenant {} impossible — accès refusé (fail-closed)",
                    tenantId, failure);
            evict(tenantId);
            throw new DomainException(
                    DETAIL_UNAVAILABLE, HttpStatus.FORBIDDEN, CODE_UNAVAILABLE,
                    Map.of("tenantId", tenantId.toString(), "reason", "STATUS_READ_FAILED"));
        }

        if (status == null) {
            log.error("TenantStatusGuard: statut NULL pour le tenant {} — accès refusé (fail-closed)", tenantId);
            evict(tenantId);
            throw new DomainException(
                    DETAIL_UNAVAILABLE, HttpStatus.FORBIDDEN, CODE_UNAVAILABLE,
                    Map.of("tenantId", tenantId.toString(), "reason", "STATUS_NULL"));
        }

        cache.put(tenantId, new CacheEntry(status, Instant.now(clock).plusMillis(CACHE_TTL_MILLIS)));
        return status;
    }

    /** Invalidation immédiate sur changement de statut (Décision D1). */
    @EventListener
    public void onTenantStatusChanged(TenantStatusChangedEvent event) {
        evict(event.tenantId());
        log.info("TenantStatusGuard: cache invalidé pour le tenant {} ({} -> {})",
                event.tenantId(), event.previousStatus(), event.newStatus());
    }

    private void evict(UUID tenantId) {
        cache.remove(tenantId);
    }

    /** Visible pour les tests : nombre d'entrées actuellement en cache. */
    int cachedTenantCount() {
        return cache.size();
    }

    private record CacheEntry(TenantStatus status, Instant expiresAt) {
    }
}
