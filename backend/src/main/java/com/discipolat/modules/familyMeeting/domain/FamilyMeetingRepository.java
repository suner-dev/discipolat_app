package com.discipolat.modules.familyMeeting.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface FamilyMeetingRepository extends JpaRepository<FamilyMeeting, UUID> {
    List<FamilyMeeting> findByTenantIdAndFamilyIdOrderByScheduledAtDesc(UUID tenantId, UUID familyId);
    List<FamilyMeeting> findByTenantIdOrderByScheduledAtDesc(UUID tenantId);
}
