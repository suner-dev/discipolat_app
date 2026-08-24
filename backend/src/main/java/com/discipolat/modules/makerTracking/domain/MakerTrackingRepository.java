package com.discipolat.modules.makerTracking.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface MakerTrackingRepository extends JpaRepository<MakerTracking, UUID> {
    List<MakerTracking> findByFaiseurIdOrderByDateEvenementDesc(UUID faiseurId);
    long countByFaiseurId(UUID faiseurId);

    @Query("SELECT COALESCE(SUM(m.pointsGagnes), 0) FROM MakerTracking m WHERE m.faiseurId = :faiseurId")
    int sumPointsGagnesByFaiseurId(@Param("faiseurId") UUID faiseurId);
}
