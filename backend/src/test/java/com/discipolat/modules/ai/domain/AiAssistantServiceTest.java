package com.discipolat.modules.ai.domain;

import com.discipolat.common.enums.StatutAlerte;
import com.discipolat.common.enums.TypeDisciple;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.alerts.domain.AlertRepository;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.members.domain.MemberPresenceRepository;
import com.discipolat.modules.reports.domain.MakerReport;
import com.discipolat.modules.reports.domain.MakerReportRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.souls.domain.WorkspaceScopeService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AiAssistantServiceTest {

    @Mock private SoulRepository soulRepository;
    @Mock private UserRepository userRepository;
    @Mock private FamilyRepository familyRepository;
    @Mock private MakerReportRepository makerReportRepository;
    @Mock private AlertRepository alertRepository;
    @Mock private MemberPresenceRepository memberPresenceRepository;
    @Mock private WorkspaceScopeService workspaceScope;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks private AiAssistantService aiService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        lenient().when(securityUtils.getCurrentUserId()).thenReturn(userId);
        lenient().when(soulRepository.findByStatut(any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));
    }

    @Test
    void chat_returnsContextualReply() {
        lenient().when(soulRepository.count()).thenReturn(150L);
        lenient().when(familyRepository.count()).thenReturn(25L);
        lenient().when(alertRepository.countByStatut(any())).thenReturn(3L);
        lenient().when(soulRepository.findByTypeDisciple(any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));
        lenient().when(makerReportRepository.findBySemaine(any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        Map<String, Object> result = aiService.chat("Quel est le taux de présence ?", userId);

        assertNotNull(result);
        assertTrue(result.containsKey("reply"));
        assertTrue(result.containsKey("sources"));
        assertFalse(((String) result.get("reply")).isEmpty());
    }

    @Test
    void chat_storesMessageInHistory() {
        lenient().when(soulRepository.count()).thenReturn(100L);
        lenient().when(familyRepository.count()).thenReturn(20L);
        lenient().when(alertRepository.countByStatut(any())).thenReturn(0L);
        lenient().when(soulRepository.findByTypeDisciple(any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));
        lenient().when(makerReportRepository.findBySemaine(any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        aiService.chat("Bonjour", userId);
        aiService.chat("Merci", userId);

        List<Map<String, Object>> history = aiService.getChatHistory(userId);
        assertEquals(4, history.size()); // 2 user + 2 assistant messages
        assertEquals("user", history.get(0).get("role"));
        assertEquals("assistant", history.get(1).get("role"));
    }

    @Test
    void clearChatHistory_removesAllMessages() {
        lenient().when(soulRepository.count()).thenReturn(0L);
        lenient().when(familyRepository.count()).thenReturn(0L);
        lenient().when(alertRepository.countByStatut(any())).thenReturn(0L);
        lenient().when(soulRepository.findByTypeDisciple(any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));
        lenient().when(makerReportRepository.findBySemaine(any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        aiService.chat("test", userId);
        assertFalse(aiService.getChatHistory(userId).isEmpty());

        aiService.clearChatHistory(userId);
        assertTrue(aiService.getChatHistory(userId).isEmpty());
    }

    @Test
    void getContextForQuery_returnsChurchData() {
        lenient().when(soulRepository.count()).thenReturn(100L);
        lenient().when(familyRepository.count()).thenReturn(15L);
        lenient().when(alertRepository.countByStatut(any())).thenReturn(2L);
        lenient().when(soulRepository.findByTypeDisciple(any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));
        lenient().when(makerReportRepository.findBySemaine(any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        Map<String, Object> ctx = aiService.getContextForQuery("familles à risque");

        assertNotNull(ctx);
        assertEquals(100L, ctx.get("totalSouls"));
        assertEquals(15L, ctx.get("totalFamilies"));
        assertTrue(ctx.containsKey("sources"));
    }
}
