package com.discipolat.modules.payments.domain;

import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * P12 — Service de dons récurrents.
 *
 * Permet aux membres de configurer des dons automatiques (dîme mensuelle,
 * offrande hebdomadaire) qui créent automatiquement des PaymentIntent
 * à chaque échéance.
 */
@Service
@Transactional
public class RecurringDonationService {

    private static final Logger log = LoggerFactory.getLogger(RecurringDonationService.class);

    private final RecurringDonationRepository repository;
    private final PaymentGatewayService paymentGatewayService;
    private final EntityPropagationPublisher propagationPublisher;
    private final SecurityUtils securityUtils;

    public RecurringDonationService(RecurringDonationRepository repository,
                                     PaymentGatewayService paymentGatewayService,
                                     EntityPropagationPublisher propagationPublisher,
                                     SecurityUtils securityUtils) {
        this.repository = repository;
        this.paymentGatewayService = paymentGatewayService;
        this.propagationPublisher = propagationPublisher;
        this.securityUtils = securityUtils;
    }

    /** Crée un don récurrent. */
    public RecurringDonation create(RecurringDonation donation) {
        donation.setTenantId(securityUtils.getCurrentTenantId());
        if (donation.getUserId() == null) {
            donation.setUserId(securityUtils.getCurrentUserId());
        }
        if (donation.getNextDonationDate() == null) {
            donation.setNextDonationDate(LocalDate.now());
        }
        RecurringDonation saved = repository.save(donation);
        propagationPublisher.publishCreated("RECURRING_DONATION", saved.getId(),
                Map.of("amount", saved.getAmount(), "frequency", saved.getFrequency().name()),
                "Don récurrent créé : " + saved.getAmount() + " " + saved.getCurrency()
                        + " / " + saved.getFrequency().getLabel());
        return saved;
    }

    /** Annule un don récurrent. */
    public RecurringDonation cancel(UUID id) {
        RecurringDonation donation = repository.findById(id)
                .orElseThrow(() -> new com.discipolat.common.domain.EntityNotFoundException("RecurringDonation", id));
        donation.setActive(false);
        repository.save(donation);
        return donation;
    }

    /** Dons récurrents de l'utilisateur courant. */
    @Transactional(readOnly = true)
    public List<RecurringDonation> mine() {
        return repository.findByUserIdOrderByCreatedAtDesc(securityUtils.getCurrentUserId());
    }

    /** Stats globales (admin). */
    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        UUID tenantId = securityUtils.getCurrentTenantId();
        return Map.of(
                "activeRecurring", repository.countByTenantIdAndActiveTrue(tenantId),
                "totalRecurring", repository.count()
        );
    }

    /**
     * Traite les dons récurrents dont la date d'échéance est passée.
     * Tourne tous les jours à 6h30.
     */
    @Scheduled(cron = "0 30 6 * * *")
    @Transactional
    public void processDueDonations() {
        List<RecurringDonation> due = repository
                .findByActiveTrueAndNextDonationDateLessThanOrEqual(LocalDate.now());
        log.info("[RecurringDonation] Processing {} due donations", due.size());

        for (RecurringDonation donation : due) {
            try {
                // Create payment intent
                PaymentIntent intent = PaymentIntent.builder()
                        .operator(donation.getOperator())
                        .phoneNumber(donation.getPhoneNumber())
                        .amount(donation.getAmount())
                        .currency(donation.getCurrency())
                        .purpose(donation.getPurpose())
                        .build();
                intent.setUserId(donation.getUserId());
                paymentGatewayService.initiate(intent);

                // Update donation stats
                donation.setTotalDonated(donation.getTotalDonated().add(donation.getAmount()));
                donation.setDonationCount(donation.getDonationCount() + 1);
                donation.setNextDonationDate(calculateNextDate(donation));
                repository.save(donation);

                log.info("[RecurringDonation] Processed donation {} — next: {}",
                        donation.getId(), donation.getNextDonationDate());
            } catch (Exception e) {
                log.error("[RecurringDonation] Failed to process donation {}: {}",
                        donation.getId(), e.getMessage());
            }
        }
    }

    private LocalDate calculateNextDate(RecurringDonation donation) {
        return switch (donation.getFrequency()) {
            case WEEKLY -> donation.getNextDonationDate().plusWeeks(1);
            case BIWEEKLY -> donation.getNextDonationDate().plusWeeks(2);
            case MONTHLY -> donation.getNextDonationDate().plusMonths(1);
            case QUARTERLY -> donation.getNextDonationDate().plusMonths(3);
            case YEARLY -> donation.getNextDonationDate().plusYears(1);
        };
    }
}
