package com.discipolat.modules.payments;

import com.discipolat.modules.payments.domain.PaymentGatewayService;
import com.discipolat.modules.payments.domain.PaymentIntent;
import com.discipolat.modules.payments.domain.PaymentIntentRepository;
import com.discipolat.modules.payments.domain.PaymentSimulator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Vérifie le comportement du simulateur de passerelle Mobile Money :
 * - désactivé par défaut (ne confirme rien sans passerelle réelle) ;
 * - activé, il confirme automatiquement les intentions PENDING assez anciennes ;
 * - idempotent et résilient si la confirmation échoue.
 */
@ExtendWith(MockitoExtension.class)
class PaymentSimulatorTest {

    @Mock private PaymentGatewayService gatewayService;
    @Mock private PaymentIntentRepository repository;

    private PaymentSimulator simulator;

    @BeforeEach
    void setUp() throws Exception {
        simulator = new PaymentSimulator(gatewayService, repository);
    }

    private void setSimulationEnabled(boolean enabled, long delayMs) throws Exception {
        Field f1 = PaymentSimulator.class.getDeclaredField("simulateAutoConfirm");
        f1.setAccessible(true);
        f1.setBoolean(simulator, enabled);
        Field f2 = PaymentSimulator.class.getDeclaredField("confirmDelayMs");
        f2.setAccessible(true);
        f2.setLong(simulator, delayMs);
    }

    private PaymentIntent pendingIntent() {
        return PaymentIntent.builder()
                .tenantId(UUID.randomUUID())
                .operator(PaymentIntent.Operator.ORANGE_MONEY)
                .amount(BigDecimal.valueOf(5_000))
                .purpose(PaymentIntent.Purpose.DIME)
                .status(PaymentIntent.Status.PENDING)
                .providerReference("OM-240822-ABC")
                .build();
    }

    @Test
    void modeDesactive_neConfirmeRien() throws Exception {
        setSimulationEnabled(false, 6_000);

        lenient().when(repository.findPendingOlderThan(any(LocalDateTime.class))).thenReturn(List.of(pendingIntent()));

        simulator.sweepPending();

        verify(gatewayService, never()).handleWebhook(anyString(), any(Boolean.class), any());
    }

    @Test
    void modeActive_confirmeLesPendingEligibles() throws Exception {
        setSimulationEnabled(true, 6_000);

        PaymentIntent pending = pendingIntent();
        when(repository.findPendingOlderThan(any(LocalDateTime.class))).thenReturn(List.of(pending));

        simulator.sweepPending();

        verify(gatewayService).handleWebhook("OM-240822-ABC", true, null);
    }

    @Test
    void erreurDeConfirmation_neBloquePasLeBalayage() throws Exception {
        setSimulationEnabled(true, 6_000);

        PaymentIntent pending = pendingIntent();
        when(repository.findPendingOlderThan(any(LocalDateTime.class))).thenReturn(List.of(pending));
        org.mockito.Mockito.doThrow(new RuntimeException("webhook down"))
                .when(gatewayService).handleWebhook("OM-240822-ABC", true, null);

        assertThatCode(() -> simulator.sweepPending()).doesNotThrowAnyException();
    }

    @Test
    void demoErreurRepository_neBloquePasLeBalayage() throws Exception {
        setSimulationEnabled(true, 6_000);
        when(repository.findPendingOlderThan(any(LocalDateTime.class))).thenThrow(new RuntimeException("table not ready"));

        assertThatCode(() -> simulator.sweepPending()).doesNotThrowAnyException();
    }
}
