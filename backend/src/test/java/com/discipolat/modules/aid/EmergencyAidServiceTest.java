package com.discipolat.modules.aid;

import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.aid.domain.EmergencyAidRequest;
import com.discipolat.modules.aid.domain.EmergencyAidRepository;
import com.discipolat.modules.aid.domain.EmergencyAidService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class EmergencyAidServiceTest {

    @Mock private EmergencyAidRepository repository;
    @Mock private EntityPropagationPublisher propagationPublisher;
    @Mock private SecurityUtils securityUtils;

    private EmergencyAidService service;

    @BeforeEach
    void setUp() {
        service = new EmergencyAidService(repository, propagationPublisher, securityUtils);
        lenient().when(securityUtils.getCurrentTenantId()).thenReturn(UUID.randomUUID());
        lenient().when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());
        lenient().when(repository.save(any(EmergencyAidRequest.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void open_genereUnPlanDeSecoursAutomatise() {
        EmergencyAidRequest saved = service.open(EmergencyAidRequest.builder()
                .urgency(EmergencyAidRequest.Urgence.CRITIQUE)
                .category("MEDICAL")
                .description("Hospitalisation d'urgence d'un disciple")
                .build());

        assertThat(saved.getPlanJson()).isNotBlank();
        assertThat(saved.getPlanJson()).contains("\"steps\":[");
        assertThat(saved.getPlanJson()).contains("chaîne de prière");
        assertThat(saved.getPlanJson()).contains("collecte d'urgence");
        // 6 étapes du plan
        assertThat(saved.getPlanJson()).contains("\"order\":6");
    }

    @Test
    void convert_usdVersXof() {
        var result = service.convert(BigDecimal.valueOf(100), "USD", "XOF");
        assertThat((long) result.get("converted")).isEqualTo(60_500L);
        assertThat(result.get("from")).isEqualTo("USD");
        assertThat(result.get("to")).isEqualTo("XOF");
    }

    @Test
    void convert_xofVersEur_arrondiCorrect() {
        var result = service.convert(BigDecimal.valueOf(65_596), "XOF", "EUR");
        double converted = (double) result.get("converted");
        assertThat(converted).isCloseTo(100.0, org.assertj.core.data.Offset.offset(0.05));
    }

    @Test
    void collect_cumuleLesMontants() {
        EmergencyAidRequest req = EmergencyAidRequest.builder()
                .tenantId(UUID.randomUUID()).requestedBy(UUID.randomUUID())
                .urgency(EmergencyAidRequest.Urgence.HAUTE)
                .amountCollected(BigDecimal.valueOf(25_000))
                .statut(EmergencyAidRequest.Statut.OUVERT)
                .build();
        org.mockito.Mockito.when(repository.findById(req.getId())).thenReturn(java.util.Optional.of(req));

        EmergencyAidRequest updated = service.addCollected(req.getId(), BigDecimal.valueOf(15_000));
        assertThat(updated.getAmountCollected()).isEqualByComparingTo(BigDecimal.valueOf(40_000));
        assertThat(updated.getStatut()).isEqualTo(EmergencyAidRequest.Statut.EN_COURS);
    }
}
