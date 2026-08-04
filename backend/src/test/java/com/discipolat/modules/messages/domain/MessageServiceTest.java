package com.discipolat.modules.messages.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.exception.BadRequestException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.messages.api.SendMessageRequest;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ConversationMessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SecurityUtils securityUtils;

    private MessageService messageService;

    private UUID userA;
    private UUID userB;

    @BeforeEach
    void setUp() {
        messageService = new MessageService(conversationRepository, messageRepository,
                userRepository, securityUtils);
        userA = UUID.fromString("10000000-0000-0000-0000-000000000001");
        userB = UUID.fromString("10000000-0000-0000-0000-000000000002");
    }

    @Test
    void startConversation_WithOtherUser_CreatesOrReturnsConversation() {
        when(securityUtils.getCurrentUserId()).thenReturn(userA);
        when(userRepository.findById(userB)).thenReturn(Optional.of(userWith(userB)));
        when(conversationRepository.findByUserAIdAndUserBId(userA, userB)).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(messageRepository.countByConversationIdAndSenderIdNotAndReadAtIsNull(any(), any())).thenReturn(0L);

        var response = messageService.startConversation(userB);

        assertNotNull(response);
        assertEquals(userB, response.otherUserId());
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    void startConversation_WithSelf_ThrowsBadRequest() {
        when(securityUtils.getCurrentUserId()).thenReturn(userA);

        assertThrows(BadRequestException.class, () -> messageService.startConversation(userA));
    }

    @Test
    void startConversation_WithUnknownUser_ThrowsEntityNotFound() {
        when(securityUtils.getCurrentUserId()).thenReturn(userA);
        when(userRepository.findById(userB)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> messageService.startConversation(userB));
    }

    @Test
    void listConversations_ReturnsOnlyMine() {
        when(securityUtils.getCurrentUserId()).thenReturn(userA);
        Conversation conv = Conversation.builder()
                .id(UUID.randomUUID())
                .userAId(userA).userBId(userB)
                .lastMessageAt(LocalDateTime.now())
                .lastMessage("Bonjour")
                .lastMessageSenderId(userA)
                .build();
        when(conversationRepository.findByUserAIdOrderByLastMessageAtDesc(userA)).thenReturn(List.of(conv));
        when(conversationRepository.findByUserBIdOrderByLastMessageAtDesc(userA)).thenReturn(List.of());
        when(userRepository.findById(userB)).thenReturn(Optional.of(userWith(userB)));
        when(messageRepository.countByConversationIdAndSenderIdNotAndReadAtIsNull(any(), any())).thenReturn(2L);

        var list = messageService.listConversations();

        assertEquals(1, list.size());
        assertEquals(userB, list.get(0).otherUserId());
        assertEquals(2, list.get(0).unreadCount());
    }

    @Test
    void getMessages_WhenNotParticipant_ThrowsAccessDenied() {
        when(securityUtils.getCurrentUserId()).thenReturn(userA);
        UUID otherConv = UUID.randomUUID();
        Conversation conv = Conversation.builder()
                .id(otherConv)
                .userAId(UUID.randomUUID()).userBId(UUID.randomUUID())
                .build();
        when(conversationRepository.findById(otherConv)).thenReturn(Optional.of(conv));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> messageService.getMessages(otherConv));
    }

    @Test
    void sendMessage_UpdatesLastMessage() {
        when(securityUtils.getCurrentUserId()).thenReturn(userA);
        Conversation conv = Conversation.builder()
                .id(UUID.randomUUID())
                .userAId(userA).userBId(userB)
                .build();
        when(conversationRepository.findById(conv.getId())).thenReturn(Optional.of(conv));
        when(messageRepository.save(any(ConversationMessage.class))).thenAnswer(inv -> {
            ConversationMessage m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            m.setCreatedAt(LocalDateTime.now());
            return m;
        });
        when(userRepository.findById(userA)).thenReturn(Optional.of(userWith(userA)));

        var response = messageService.sendMessage(conv.getId(), new SendMessageRequest("Salut !"));

        assertNotNull(response);
        assertEquals("Salut !", response.content());
        assertEquals("Salut !", conv.getLastMessage());
        assertEquals(userA, conv.getLastMessageSenderId());
        verify(conversationRepository).save(conv);
    }

    private User userWith(UUID id) {
        return User.builder().id(id).firstName("Prénom").lastName("Nom").build();
    }
}
