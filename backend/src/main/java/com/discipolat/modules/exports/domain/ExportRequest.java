package com.discipolat.modules.exports.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportRequest {
    private ExportType type;
    private String format; // csv, excel, pdf, json
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private List<String> columns;
    private Map<String, Object> filters;
    private UUID departmentId;
    private UUID familyId;
    private UUID userId;
    private Boolean includeHeaders;
    private String locale;
}