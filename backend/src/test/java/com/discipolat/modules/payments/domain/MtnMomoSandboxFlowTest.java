package com.discipolat.modules.payments.domain;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.common.infrastructure.security.SecurityTestHelper;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.finances.api.FinanceTransactionRequest;
import com.discipolat.modules.finances.domain.FinanceService;
import com.discipolat.modules.payments.domain.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test d'intégration bout-en-bout du flux MTN MoMo.
 *
 * <p>Simule le parcours complet :</p>
 * <ol>
 *   <li>Initiation du paiement → génération de la référence locale</li>
 *   <li>Appel du provider MTN réel (via mock HTTP) → obtention de la référence opérateur</li>
 *   <li>Vérification du statut auprès de MTN (SUCCESSFUL)</li>
 *   <li>Webhook de confirmation → création du reçu financier</li>
 *   <li>Idempotence du webhook (double appel)</li>
 *   <li>Échec du webhook → marquage FAILED</li>
 * </ol>
 *
 * <p>Ce test valide la cohérence entre tous les composants du module paiement
 * en utilisant les mocks pour isoler les appels réseau réels.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MtnMomoSandboxFlowTest {

    @Mock private PaymentIntentRepository repository;
    @Mock private RecurringDonationRepository recurringDonationRepository;
    @Mock private FinanceService financeService;
    @Mock private EntityPropagationPublisher propagationPublisher;
    @Mock private SecurityUtils securityUtils;
    @Mock private MobileMoneyProviderRegistry providerRegistry;

    private MobileMoneyProvider mtnProvider;
    @Mock private WebhookLogService webhookLogService;

    private PaymentGatewayService gatewayService;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        gatewayService = new PaymentGatewayService(
                repository, recurringDonationRepository, financeService,
                propagationPublisher, securityUtils, providerRegistry);
        TenantContext.setTenantId(tenantId);

        // Default: MTN provider is active
        mtnProvider = mock(MobileMoneyProvider.class);
        when(mtnProvider.operator()).thenReturn(PaymentIntent.Operator.MTN_MOMO);
        when(providerRegistry.find(PaymentIntent.Operator.MTN_MOMO)).thenReturn(mtnProvider);
        when(providerRegistry.find(PaymentIntent.Operator.ORANGE_MONEY)).thenReturn(null);
        when(providerRegistry.find(PaymentIntent.Operator.M_PESA)).thenReturn(null);

        // Default: repository saves entities
        when(repository.save(any(PaymentIntent.class))).thenAnswer(inv -> {
            PaymentIntent p = inv.getArgument(0);
            if (p.getId() == null) p.setId(UUID.randomUUID());
            return p;
        });

        // Default: finance service returns transaction
        Map<String, Object> txResult = new LinkedHashMap<>();
        txResult.put("id", UUID.randomUUID());
        when(financeService.createTransaction(any(FinanceTransactionRequest.class))).thenReturn(txResult);

        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(securityUtils.isSuperUser()).thenReturn(false);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FLOW 1: Initiation → Provider MTN → Référence opérateur
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Initiation MTN MoMo → appel provider → référence opérateur stockée")
    void flow1_initiation_appeleProviderMTN() {
        // Arrange: le provider MTN retourne une succès
        String operatorRef = UUID.randomUUID().toString();
        when(mtnProvider.initiate(any(PaymentIntent.class))).thenReturn(
                new MobileMoneyProvider.Result(operatorRef, null,
                        "Validez sur votre téléphone.", false));

        // Act
        SecurityTestHelper.loginAs(userId);
        PaymentIntent result = gatewayService.initiate(PaymentIntent.builder()
                .operator(PaymentIntent.Operator.MTN_MOMO)
                .amount(BigDecimal.valueOf(5_000))
                .currency("XOF")
                .purpose(PaymentIntent.Purpose.DIME)
                .phoneNumber("+2250701234567")
                .build());

        // Assert
        assertThat(result.getStatus()).isEqualTo(PaymentIntent.Status.PENDING);
        assertThat(result.getProviderReference()).isEqualTo(operatorRef);
        assertThat(result.getProviderName()).isEqualTo("MTN Mobile Money");
        assertThat(result.getInstructions()).contains("Validez");
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getTenantId()).isEqualTo(tenantId);

        verify(mtnProvider).initiate(any(PaymentIntent.class));
        verify(repository).save(any(PaymentIntent.class));
    }

    @Test
    @DisplayName("Provider MTN échoue → fallback référence locale")
    void flow1_providerEchoue_fallbackReference() {
        // Arrange: le provider MTN lève une exception
        when(mtnProvider.initiate(any(PaymentIntent.class)))
                .thenThrow(new RuntimeException("Connection timeout"));

        // Act
        SecurityTestHelper.loginAs(userId);
        PaymentIntent result = gatewayService.initiate(PaymentIntent.builder()
                .operator(PaymentIntent.Operator.MTN_MOMO)
                .amount(BigDecimal.valueOf(2_000))
                .purpose(PaymentIntent.Purpose.OFFRANDE)
                .build());

        // Assert: référence locale générée malgré l'échec
        assertThat(result.getProviderReference()).startsWith("MTN-");
        assertThat(result.getStatus()).isEqualTo(PaymentIntent.Status.PENDING);
        verify(mtnProvider).initiate(any(PaymentIntent.class));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FLOW 2: Vérification statut MTN → SUCCESSFUL
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Vérification MTN SUCCESSFUL → webhook confirme le paiement")
    void flow2_verificationMTNSuccessful() {
        String ref = UUID.randomUUID().toString();
        PaymentIntent pending = PaymentIntent.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId).userId(userId)
                .operator(PaymentIntent.Operator.MTN_MOMO)
                .amount(BigDecimal.valueOf(3_000))
                .currency("XOF")
                .purpose(PaymentIntent.Purpose.DIME)
                .status(PaymentIntent.Status.PENDING)
                .providerReference(ref)
                .build();
        when(repository.findByProviderReference(ref)).thenReturn(Optional.of(pending));

        // Simulate MTN verify returns SUCCESSFUL
        when(mtnProvider.verify(ref)).thenReturn(
                new MobileMoneyProvider.Verification(true, "SUCCESSFUL", null));

        // Act: verify then webhook
        MobileMoneyProvider.Verification verification = mtnProvider.verify(ref);
        PaymentIntent result = gatewayService.handleWebhook(ref, verification.paid(), verification.failureReason());

        // Assert
        assertThat(result.getStatus()).isEqualTo(PaymentIntent.Status.CONFIRMED);
        assertThat(result.getConfirmedAt()).isNotNull();
        assertThat(result.getTransactionId()).isNotNull();

        // Finance receipt created
        ArgumentCaptor<FinanceTransactionRequest> captor = ArgumentCaptor.forClass(FinanceTransactionRequest.class);
        verify(financeService).createTransaction(captor.capture());
        assertThat(captor.getValue().categorie()).isEqualTo("DIME");
        assertThat(captor.getValue().montant()).isEqualByComparingTo(BigDecimal.valueOf(3_000));
    }

    @Test
    @DisplayName("Vérification MTN FAILED → webhook marque échoué")
    void flow2_verificationMTNFailed() {
        String ref = UUID.randomUUID().toString();
        PaymentIntent pending = PaymentIntent.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .operator(PaymentIntent.Operator.MTN_MOMO)
                .amount(BigDecimal.valueOf(1_000))
                .status(PaymentIntent.Status.PENDING)
                .providerReference(ref)
                .build();
        when(repository.findByProviderReference(ref)).thenReturn(Optional.of(pending));

        // Simulate MTN verify returns FAILED
        when(mtnProvider.verify(ref)).thenReturn(
                new MobileMoneyProvider.Verification(false, "FAILED", "Insufficient balance"));

        // Act
        MobileMoneyProvider.Verification verification = mtnProvider.verify(ref);
        PaymentIntent result = gatewayService.handleWebhook(ref, verification.paid(), verification.failureReason());

        // Assert
        assertThat(result.getStatus()).isEqualTo(PaymentIntent.Status.FAILED);
        assertThat(result.getFailureReason()).isEqualTo("Insufficient balance");
        verify(financeService, never()).createTransaction(any());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FLOW 3: Webhook direct (sans provider) → confirmation
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Webhook direct confirmation → reçu financier créé")
    void flow3_webhookDirect_confirmation() {
        String ref = "MTN-260901-ABCDEF";
        PaymentIntent pending = PaymentIntent.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .operator(PaymentIntent.Operator.MTN_MOMO)
                .amount(BigDecimal.valueOf(7_500))
                .currency("XOF")
                .purpose(PaymentIntent.Purpose.OFFRANDE)
                .status(PaymentIntent.Status.PENDING)
                .providerReference(ref)
                .build();
        when(repository.findByProviderReference(ref)).thenReturn(Optional.of(pending));

        // Act
        PaymentIntent result = gatewayService.handleWebhook(ref, true, null);

        // Assert
        assertThat(result.getStatus()).isEqualTo(PaymentIntent.Status.CONFIRMED);
        assertThat(result.getConfirmedAt()).isNotNull();

        ArgumentCaptor<FinanceTransactionRequest> captor = ArgumentCaptor.forClass(FinanceTransactionRequest.class);
        verify(financeService).createTransaction(captor.capture());
        assertThat(captor.getValue().categorie()).isEqualTo("OFFRANDE");
    }

    @Test
    @DisplayName("Webhook idempotent — double appel ne recrée pas le reçu")
    void flow3_webhookIdempotent() {
        String ref = "MTN-260901-DOUBLE";
        PaymentIntent alreadyConfirmed = PaymentIntent.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .operator(PaymentIntent.Operator.MTN_MOMO)
                .amount(BigDecimal.valueOf(2_000))
                .status(PaymentIntent.Status.CONFIRMED)
                .providerReference(ref)
                .confirmedAt(LocalDateTime.now())
                .build();
        when(repository.findByProviderReference(ref)).thenReturn(Optional.of(alreadyConfirmed));

        // Act: double webhook
        PaymentIntent first = gatewayService.handleWebhook(ref, true, null);
        PaymentIntent second = gatewayService.handleWebhook(ref, true, null);

        // Assert: même résultat, pas de double comptabilisation
        assertThat(first.getStatus()).isEqualTo(PaymentIntent.Status.CONFIRMED);
        assertThat(second.getStatus()).isEqualTo(PaymentIntent.Status.CONFIRMED);
        verify(financeService, never()).createTransaction(any());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FLOW 4: Tous les opérateurs avec le même schéma
    // ═══════════════════════════════════════════════════════════════════════════

    @ParameterizedTest
    @EnumSource(value = PaymentIntent.Operator.class, names = {"MTN_MOMO", "M_PESA", "ORANGE_MONEY"})
    @DisplayName("Initiation + confirmation pour chaque opérateur Mobile Money")
    void flow4_tousOperateurs(PaymentIntent.Operator operator) {
        // Arrange
        MobileMoneyProvider mockProvider = mock(MobileMoneyProvider.class);
        when(providerRegistry.find(operator)).thenReturn(mockProvider);
        String opRef = UUID.randomUUID().toString();
        when(mockProvider.initiate(any(PaymentIntent.class))).thenReturn(
                new MobileMoneyProvider.Result(opRef, null, "Instructions", false));

        // Act: initiate
        SecurityTestHelper.loginAs(userId);
        PaymentIntent initiated = gatewayService.initiate(PaymentIntent.builder()
                .operator(operator)
                .amount(BigDecimal.valueOf(5_000))
                .currency("XOF")
                .purpose(PaymentIntent.Purpose.DIME)
                .build());

        // Assert: initiated
        assertThat(initiated.getProviderReference()).isEqualTo(opRef);
        assertThat(initiated.getStatus()).isEqualTo(PaymentIntent.Status.PENDING);

        // Act: confirm via webhook
        PaymentIntent pending = PaymentIntent.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .operator(operator)
                .amount(BigDecimal.valueOf(5_000))
                .currency("XOF")
                .status(PaymentIntent.Status.PENDING)
                .providerReference(opRef)
                .build();
        when(repository.findByProviderReference(opRef)).thenReturn(Optional.of(pending));

        PaymentIntent confirmed = gatewayService.handleWebhook(opRef, true, null);

        // Assert: confirmed
        assertThat(confirmed.getStatus()).isEqualTo(PaymentIntent.Status.CONFIRMED);
        verify(financeService).createTransaction(any());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FLOW 5: Annulation avant confirmation
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Annulation avant confirmation → status CANCELLED")
    void flow5_annulationAvantConfirmation() {
        UUID paymentId = UUID.randomUUID();
        PaymentIntent pending = PaymentIntent.builder()
                .id(paymentId)
                .tenantId(tenantId).userId(userId)
                .operator(PaymentIntent.Operator.MTN_MOMO)
                .amount(BigDecimal.valueOf(1_000))
                .status(PaymentIntent.Status.PENDING)
                .providerReference("MTN-CANCEL")
                .build();
        when(repository.findById(paymentId)).thenReturn(Optional.of(pending));

        // Act
        SecurityTestHelper.loginAs(userId);
        PaymentIntent cancelled = gatewayService.cancel(paymentId);

        // Assert
        assertThat(cancelled.getStatus()).isEqualTo(PaymentIntent.Status.CANCELLED);
    }

    @Test
    @DisplayName("Annulation d'un paiement déjà confirmé → reste CONFIRMED")
    void flow5_annulationApresConfirmation() {
        UUID paymentId = UUID.randomUUID();
        PaymentIntent confirmed = PaymentIntent.builder()
                .id(paymentId)
                .tenantId(tenantId).userId(userId)
                .operator(PaymentIntent.Operator.MTN_MOMO)
                .amount(BigDecimal.valueOf(1_000))
                .status(PaymentIntent.Status.CONFIRMED)
                .providerReference("MTN-NO-CANCEL")
                .build();
        when(repository.findById(paymentId)).thenReturn(Optional.of(confirmed));

        // Act
        SecurityTestHelper.loginAs(userId);
        PaymentIntent result = gatewayService.cancel(paymentId);

        // Assert: reste CONFIRMED (annulation impossible après confirmation)
        assertThat(result.getStatus()).isEqualTo(PaymentIntent.Status.CONFIRMED);
    }
}
