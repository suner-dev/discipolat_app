package com.discipolat.modules.families.repository;

import com.discipolat.modules.families.domain.FamilyMeeting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface FamilyMeetingRepository extends JpaRepository<FamilyMeeting, UUID> {

    List<FamilyMeeting> findByFamilyIdAndDeletedFalse(UUID familyId);

    List<FamilyMeeting> findByFamilyIdAndMeetingDateBetween(UUID familyId, java.time.LocalDate from, java.time.LocalDate to);

    Page<FamilyMeeting> findByTenantIdAndDeletedFalseOrderByMeetingDateDesc(UUID tenantId, Pageable pageable);
}