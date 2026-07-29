package com.discipolat.modules.reports.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportCorrectionRepository extends JpaRepository<ReportCorrection, UUID> {
    List<ReportCorrection> findByReportIdOrderByCreatedAtDesc(UUID reportId);
}
