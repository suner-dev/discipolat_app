package com.discipolat.modules.departments.api;

import com.discipolat.modules.departments.domain.DepartmentMemberReport;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Rapport du responsable sur un membre du département.
 */
public record DepartmentMemberReportRequest(
        @NotNull DepartmentMemberReport.ReportType type,
        @NotBlank String contenu
) {}
