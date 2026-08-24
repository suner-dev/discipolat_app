package com.discipolat.modules.tontine;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.tontine.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TontineServiceTest {

    @Mock private TontineGroupRepository groupRepository;
    @Mock private TontineMemberRepository memberRepository;
    @Mock private TontineContributionRepository contributionRepository;
    @Mock private EntityPropagationPublisher propagationPublisher;
    @Mock private SecurityUtils securityUtils;
    @Mock private com.discipolat.modules.notifications.domain.NotificationService notificationService;
    @Mock private jakarta.persistence.EntityManager entityManager;

    private TontineService service;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID groupId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new TontineService(groupRepository, memberRepository, contributionRepository,
                propagationPublisher, securityUtils, notificationService, entityManager);
        lenient().when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        lenient().when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());
        lenient().when(securityUtils.getCurrentUserRole()).thenReturn("ADMIN");
        lenient().when(groupRepository.save(any(TontineGroup.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(memberRepository.save(any(TontineMember.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(contributionRepository.save(any(TontineContribution.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private TontineGroup groupe(int tourActuel) {
        return TontineGroup.builder()
                .id(groupId).tenantId(tenantId).name("Solidarité Jeunes")
                .montantParTour(BigDecimal.valueOf(10_000))
                .tourActuel(tourActuel).statut(TontineGroup.Statut.ACTIVE)
                .build();
    }

    @Test
    void addMember_genereLecheancierDuTourCourant() {
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(groupe(1)));
        when(memberRepository.countByGroupId(groupId)).thenReturn(2L);

        service.addMember(groupId, TontineMember.builder().nom("Marie Lolo").build());

        org.mockito.Mockito.verify(contributionRepository).save(org.mockito.ArgumentMatchers.argThat(
                c -> c.getTour() == 1 && c.getMontant().compareTo(BigDecimal.valueOf(10_000)) == 0));
    }

    @Test
    void markPaid_enregistreDateEtNote() {
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(groupe(1)));
        UUID memberId = UUID.randomUUID();
        when(contributionRepository.findByGroupIdAndMemberIdAndTour(groupId, memberId, 1))
                .thenReturn(Optional.of(TontineContribution.builder()
                        .groupId(groupId).memberId(memberId).tour(1)
                        .montant(BigDecimal.TEN).build()));
        lenient().when(securityUtils.getCurrentUserRole()).thenReturn("RESPONSABLE");

        TontineContribution result = service.markPaid(groupId, memberId, 1, "Cash reçu");
        assertThat(result.isPaye()).isTrue();
        assertThat(result.getDatePaiement()).isNotNull();
    }

    @Test
    void markPaid_refusePourMembreSimple() {
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(groupe(1)));
        lenient().when(securityUtils.getCurrentUserRole()).thenReturn("FAISEUR");

        assertThatThrownBy(() -> service.markPaid(groupId, UUID.randomUUID(), 1, null))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void nextRound_designeBeneficiaireDansLOrdre() {
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(groupe(1)));
        TontineMember premier = TontineMember.builder().id(UUID.randomUUID()).nom("Alpha").ordrePassage(1).build();
        TontineMember deuxieme = TontineMember.builder().id(UUID.randomUUID()).nom("Beta").ordrePassage(2).build();
        when(memberRepository.findByGroupIdOrderByOrdrePassageAsc(groupId)).thenReturn(List.of(premier, deuxieme));
        when(memberRepository.save(any(TontineMember.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.nextRound(groupId);
        assertThat(result.get("beneficiary")).isEqualTo("Alpha");
        assertThat((int) result.get("newRound")).isEqualTo(2);
    }

    @Test
    void groupDetail_inconnu_leveEntityNotFound() {
        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.groupDetail(groupId))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
