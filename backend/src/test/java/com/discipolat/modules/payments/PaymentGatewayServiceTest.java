package com.discipolat.modules.payments;

import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.finances.api.FinanceTransactionRequest;
import com.discipolat.modules.finances.domain.FinanceService;
import com.discipolat.modules.payments.domain.PaymentGatewayService;
import com.discipolat.modules.payments.domain.PaymentIntent;
import com.discipolat.modules.payments.domain.PaymentIntentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceTest {

    @Mock private PaymentIntentRepository repository;
    @Mock private FinanceService financeService;
    @Mock private EntityPropagationPublisher propagationPublisher;
    @Mock private SecurityUtils securityUtils;

    private PaymentGatewayService service;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PaymentGatewayService(repository, financeService, propagationPublisher, securityUtils);
        lenient().when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        lenient().when(repository.save(any(PaymentIntent.class))).thenAnswer(inv -> inv.getArgument(0));
        Map<String, Object> txResult = new java.util.LinkedHashMap<>();
        txResult.put("id", UUID.randomUUID());
        lenient().when(financeService.createTransaction(any(FinanceTransactionRequest.class)))
                .thenReturn(txResult);
    }

    @Test
    void initiate_genereReferenceOperateurEtStatutPending() {
        when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());

        PaymentIntent intent = service.initiate(PaymentIntent.builder()
                .operator(PaymentIntent.Operator.ORANGE_MONEY)
                .amount(BigDecimal.valueOf(5_000))
                .purpose(PaymentIntent.Purpose.DIME)
                .build());

        assertThat(intent.getProviderReference()).startsWith("OM-");
        assertThat(intent.getStatus()).isEqualTo(PaymentIntent.Status.PENDING);
        assertThat(intent.getUserId()).isNotNull();
    }

    @Test
    void webhookConfirmation_comptabiliseLaRecetteDansFinances() {
        PaymentIntent pending = PaymentIntent.builder()
                .tenantId(tenantId)
                .operator(PaymentIntent.Operator.M_PESA)
                .amount(BigDecimal.valueOf(10_000))
                .currency("XOF")
                .purpose(PaymentIntent.Purpose.OFFRANDE)
                .status(PaymentIntent.Status.PENDING)
                .providerReference("MP-240822-ABC123")
                .build();
        when(repository.findByProviderReference("MP-240822-ABC123")).thenReturn(Optional.of(pending));

        PaymentIntent confirmed = service.handleWebhook("MP-240822-ABC123", true, null);

        assertThat(confirmed.getStatus()).isEqualTo(PaymentIntent.Status.CONFIRMED);
        assertThat(confirmed.getConfirmedAt()).isNotNull();
        assertThat(confirmed.getTransactionId()).isNotNull();

        ArgumentCaptor<FinanceTransactionRequest> captor = ArgumentCaptor.forClass(FinanceTransactionRequest.class);
        verify(financeService).createTransaction(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(com.discipolat.modules.finances.domain.FinanceTransaction.TransactionType.RECETTE);
        assertThat(captor.getValue().categorie()).isEqualTo("OFFRANDE");
        assertThat(captor.getValue().montant()).isEqualByComparingTo(BigDecimal.valueOf(10_000));
    }

    @Test
    void webhookEstIdempotent_siDejaConfirme() {
        PaymentIntent confirmed = PaymentIntent.builder()
                .tenantId(tenantId)
                .operator(PaymentIntent.Operator.M_PESA)
                .amount(BigDecimal.TEN)
                .status(PaymentIntent.Status.CONFIRMED)
                .providerReference("MP-X")
                .build();
        when(repository.findByProviderReference("MP-X")).thenReturn(Optional.of(confirmed));

        service.handleWebhook("MP-X", true, null);

        verify(financeService, org.mockito.Mockito.never())
                .createTransaction(any(FinanceTransactionRequest.class));
    }

    @Test
    void webhookEchec_marqueFailedAvecRaison() {
        PaymentIntent pending = PaymentIntent.builder()
                .tenantId(tenantId)
                .operator(PaymentIntent.Operator.MTN_MOMO)
                .amount(BigDecimal.valueOf(2_000))
                .status(PaymentIntent.Status.PENDING)
                .providerReference("MTN-Y")
                .build();
        when(repository.findByProviderReference("MTN-Y")).thenReturn(Optional.of(pending));

        PaymentIntent failed = service.handleWebhook("MTN-Y", false, "Solde insuffisant");
        assertThat(failed.getStatus()).isEqualTo(PaymentIntent.Status.FAILED);
        assertThat(failed.getFailureReason()).isEqualTo("Solde insuffisant");
    }

    // ==================== Anti-IDOR : accès à UN paiement ====================

    @Test
    void accesParAuteur_autorisePourSonProprePaiement() {
        UUID auteur = UUID.randomUUID();
        PaymentIntent sien = PaymentIntent.builder()
                .tenantId(tenantId).userId(auteur)
                .operator(PaymentIntent.Operator.WAVE)
                .amount(BigDecimal.valueOf(1_000))
                .build();
        when(repository.findById(sien.getId())).thenReturn(Optional.of(sien));
        when(securityUtils.isSuperUser()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(auteur);

        assertThat(service.findByIdForCurrentUser(sien.getId())).isSameAs(sien);
    }

    @Test
    void accesParAutreUtilisateur_refuseSansFuitedExistence() {
        UUID auteur = UUID.randomUUID();
        UUID autre = UUID.randomUUID();
        PaymentIntent sien = PaymentIntent.builder()
                .tenantId(tenantId).userId(auteur)
                .operator(PaymentIntent.Operator.WAVE)
                .amount(BigDecimal.valueOf(1_000))
                .build();
        when(repository.findById(sien.getId())).thenReturn(Optional.of(sien));
        when(securityUtils.isSuperUser()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(autre);

        org.junit.jupiter.api.Assertions.assertThrows(
                com.discipolat.common.domain.EntityNotFoundException.class,
                () -> service.findByIdForCurrentUser(sien.getId()));
    }

    @Test
    void accesSuperUtilisateur_authoriseSurTousLesPaiements() {
        PaymentIntent deQuelquUn = PaymentIntent.builder()
                .tenantId(tenantId).userId(UUID.randomUUID())
                .operator(PaymentIntent.Operator.CARD)
                .amount(BigDecimal.valueOf(500))
                .build();
        when(repository.findById(deQuelquUn.getId())).thenReturn(Optional.of(deQuelquUn));
        when(securityUtils.isSuperUser()).thenReturn(true);

        assertThat(service.findByIdForCurrentUser(deQuelquUn.getId())).isSameAs(deQuelquUn);
    }

    @Test
    void annulationParAutreUtilisateur_refusee() {
        UUID autre = UUID.randomUUID();
        PaymentIntent sien = PaymentIntent.builder()
                .tenantId(tenantId).userId(UUID.randomUUID())
                .operator(PaymentIntent.Operator.ORANGE_MONEY)
                .amount(BigDecimal.valueOf(3_000))
                .status(PaymentIntent.Status.PENDING)
                .providerReference("OM-Z")
                .build();
        when(repository.findById(sien.getId())).thenReturn(Optional.of(sien));
        when(securityUtils.isSuperUser()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(autre);

        org.junit.jupiter.api.Assertions.assertThrows(
                com.discipolat.common.domain.EntityNotFoundException.class,
                () -> service.cancel(sien.getId()));
    }
}
