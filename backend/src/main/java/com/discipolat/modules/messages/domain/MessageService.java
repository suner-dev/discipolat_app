package com.discipolat.modules.messages.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.exception.BadRequestException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.messages.api.ConversationResponse;
import com.discipolat.modules.messages.api.MessageResponse;
import com.discipolat.modules.messages.api.SendMessageRequest;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Messagerie interne : conversations privées entre utilisateurs.
 * Tout utilisateur authentifié peut discuter avec un autre utilisateur.
 */
@Service
@Transactional
public class MessageService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    public MessageService(ConversationRepository conversationRepository,
                          ConversationMessageRepository messageRepository,
                          UserRepository userRepository,
                          SecurityUtils securityUtils) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
    }

    /** Ouvre (ou récupère) une conversation avec un autre utilisateur. */
    public ConversationResponse startConversation(UUID otherUserId) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        if (!Conversation.isValidPair(currentUserId, otherUserId)) {
            throw new BadRequestException("Vous ne pouvez pas ouvrir une conversation avec vous-même");
        }
        userRepository.findById(otherUserId)
                .orElseThrow(() -> new EntityNotFoundException("User", otherUserId));

        UUID a = currentUserId.compareTo(otherUserId) < 0 ? currentUserId : otherUserId;
        UUID b = currentUserId.compareTo(otherUserId) < 0 ? otherUserId : currentUserId;

        Conversation conv = conversationRepository.findByUserAIdAndUserBId(a, b)
                .orElseGet(() -> conversationRepository.save(
                        Conversation.builder().userAId(a).userBId(b).build()));

        return toResponse(conv, currentUserId);
    }

    /** Liste des conversations de l'utilisateur connecté, triées par activité récente. */
    @Transactional(readOnly = true)
    public List<ConversationResponse> listConversations() {
        UUID currentUserId = securityUtils.getCurrentUserId();
        List<Conversation> mine = new java.util.ArrayList<>();
        mine.addAll(conversationRepository.findByUserAIdOrderByLastMessageAtDesc(currentUserId));
        mine.addAll(conversationRepository.findByUserBIdOrderByLastMessageAtDesc(currentUserId));
        return mine.stream()
                .distinct()
                .sorted(Comparator.comparing(Conversation::getLastMessageAt,
                        Comparator.nullsFirst(Comparator.reverseOrder())))
                .map(c -> toResponse(c, currentUserId))
                .toList();
    }

    /** Historique des messages d'une conversation (accès limité aux participants). */
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(UUID conversationId) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation", conversationId));
        assertParticipant(conv, currentUserId);
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(m -> MessageResponse.from(m, fullName(m.getSenderId())))
                .toList();
    }

    /** Envoie un message dans une conversation. */
    public MessageResponse sendMessage(UUID conversationId, SendMessageRequest request) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation", conversationId));
        assertParticipant(conv, currentUserId);

        ConversationMessage msg = messageRepository.save(ConversationMessage.builder()
                .conversationId(conversationId)
                .senderId(currentUserId)
                .content(request.content().trim())
                .build());

        conv.setLastMessage(msg.getContent());
        conv.setLastMessageAt(LocalDateTime.now());
        conv.setLastMessageSenderId(currentUserId);
        conversationRepository.save(conv);

        return MessageResponse.from(msg, fullName(currentUserId));
    }

    /** Marque tous les messages de la conversation comme lus (hors messages de l'utilisateur). */
    public void markConversationAsRead(UUID conversationId) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation", conversationId));
        assertParticipant(conv, currentUserId);
        messageRepository.markAllAsRead(conversationId, currentUserId);
    }

    // ============================================================
    // Helpers
    // ============================================================

    private void assertParticipant(Conversation conv, UUID userId) {
        if (!conv.getUserAId().equals(userId) && !conv.getUserBId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Vous n'êtes pas participant de cette conversation");
        }
    }

    private ConversationResponse toResponse(Conversation conv, UUID currentUserId) {
        UUID otherUserId = conv.getUserAId().equals(currentUserId) ? conv.getUserBId() : conv.getUserAId();
        User other = userRepository.findById(otherUserId).orElse(null);
        long unread = messageRepository.countByConversationIdAndSenderIdNotAndReadAtIsNull(
                conv.getId(), currentUserId);
        return ConversationResponse.from(
                conv, currentUserId,
                other != null ? other.getFirstName() + " " + other.getLastName() : "Utilisateur",
                other != null && other.getRoles() != null && !other.getRoles().isEmpty()
                        ? other.getRoles().iterator().next().name()
                        : (other != null && other.getRole() != null ? other.getRole().name() : "MEMBRE"),
                conv.getLastMessage(),
                conv.getLastMessageSenderId(),
                unread
        );
    }

    private String fullName(UUID userId) {
        return userRepository.findById(userId)
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse("Utilisateur");
    }
}
