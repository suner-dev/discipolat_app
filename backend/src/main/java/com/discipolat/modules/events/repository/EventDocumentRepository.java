package com.discipolat.modules.events.repository;

import com.discipolat.modules.events.domain.EventDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventDocumentRepository extends JpaRepository<EventDocument, UUID> {

    List<EventDocument> findByChurchEventId(UUID churchEventId);
}