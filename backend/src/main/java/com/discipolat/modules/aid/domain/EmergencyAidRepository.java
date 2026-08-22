package com.discipolat.modules.aid.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmergencyAidRepository extends JpaRepository<EmergencyAidRequest, UUID> {
    List<EmergencyAidRequest> findTop50ByOrderByCreatedAtDesc();
    List<EmergencyAidRequest> findByStatutOrderByCreatedAtDesc(EmergencyAidRequest.Statut statut);
}
