package com.discipolat.modules.makerTracking.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MakerTrackingRepository extends JpaRepository<MakerTracking, UUID> {
    List<MakerTracking> findByFaiseurIdOrderByDateEvenementDesc(UUID faiseurId);
    long countByFaiseurId(UUID faiseurId);
    int sumPointsGagnesByFaiseurId(UUID faiseurId);
}
