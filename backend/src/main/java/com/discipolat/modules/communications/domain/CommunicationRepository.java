package com.discipolat.modules.communications.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommunicationRepository extends JpaRepository<Communication, UUID> {

    List<Communication> findByDeletedFalseOrderByCreatedAtDesc();

    List<Communication> findByDeletedFalseAndStatutOrderByDatePublicationDesc(Communication.Statut statut);
}
