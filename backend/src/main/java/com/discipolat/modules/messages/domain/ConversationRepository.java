package com.discipolat.modules.messages.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByUserAIdAndUserBId(UUID userAId, UUID userBId);
    Optional<Conversation> findByUserBIdAndUserAId(UUID userBId, UUID userAId);

    List<Conversation> findByUserAIdOrderByLastMessageAtDesc(UUID userAId);
    List<Conversation> findByUserBIdOrderByLastMessageAtDesc(UUID userBId);
}
