package com.discipolat.modules.families.service;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.families.domain.PastorateAppointment;
import com.discipolat.modules.families.domain.PastorateTransfer;
import com.discipolat.modules.families.repository.PastorateAppointmentRepository;
import com.discipolat.modules.families.repository.PastorateTransferRepository;
import com.discipolat.modules.tenants.domain.OrganizationNode;
import com.discipolat.modules.tenants.domain.OrganizationNodeRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class PastorateService {

    private final PastorateAppointmentRepository appointmentRepository;
    private final PastorateTransferRepository transferRepository;
    private final OrganizationNodeRepository orgNodeRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    // ========== APPOINTMENTS ==========

    public PastorateAppointment createAppointment(UUID tenantId, UUID actorId, PastorateAppointment appointment) {
        // Validate org unit belongs to tenant
        OrganizationNode orgUnit = orgNodeRepository.findById(appointment.getOrganizationUnitId())
                .orElseThrow(() -> new EntityNotFoundException("OrganizationNode", appointment.getOrganizationUnitId()));
        if (!orgUnit.getTenantId().equals(tenantId)) {
            throw new SecurityException("Cross-tenant");
        }

        // Validate pastor exists
        User pastor = userRepository.findById(appointment.getPastorId())
                .orElseThrow(() -> new EntityNotFoundException("User", appointment.getPastorId()));
        if (!pastor.getTenantId().equals(tenantId)) {
            throw new SecurityException("Cross-tenant");
        }

        appointment.setTenantId(tenantId);
        appointment.setCreatedBy(actorId);
        PastorateAppointment saved = appointmentRepository.save(appointment);

        // Close previous active appointment for same org unit + pastor
        closePreviousAppointments(tenantId, saved.getPastorId(), saved.getOrganizationUnitId(), saved.getId());

        return saved;
    }

    public PastorateAppointment updateAppointment(UUID tenantId, UUID actorId, UUID appointmentId, Map<String, Object> updates) {
        PastorateAppointment appointment = getAppointment(tenantId, appointmentId);

        if (updates.containsKey("endDate")) appointment.setEndDate((LocalDate) updates.get("endDate"));
        if (updates.containsKey("status")) appointment.setStatus((String) updates.get("status"));
        if (updates.containsKey("reason")) appointment.setReason((String) updates.get("reason"));

        return appointmentRepository.save(appointment);
    }

    public void endAppointment(UUID tenantId, UUID actorId, UUID appointmentId, String reason) {
        PastorateAppointment appointment = getAppointment(tenantId, appointmentId);
        appointment.setStatus("ENDED");
        appointment.setEndDate(LocalDate.now());
        appointment.setReason(reason);
        appointmentRepository.save(appointment);
    }

    @Transactional(readOnly = true)
    public PastorateAppointment getAppointment(UUID tenantId, UUID appointmentId) {
        return appointmentRepository.findByIdAndTenantIdAndDeletedAtIsNull(appointmentId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("PastorateAppointment", appointmentId));
    }

    @Transactional(readOnly = true)
    public List<PastorateAppointment> getAppointments(UUID tenantId, UUID pastorId, UUID orgUnitId) {
        if (pastorId != null) {
            return appointmentRepository.findByTenantIdAndPastorIdAndDeletedAtIsNull(tenantId, pastorId);
        }
        if (orgUnitId != null) {
            return appointmentRepository.findByTenantIdAndOrganizationUnitIdAndDeletedAtIsNull(tenantId, orgUnitId);
        }
        return appointmentRepository.findByTenantIdAndDeletedAtIsNullOrderByStartDateDesc(tenantId);
    }

    // ========== TRANSFERS ==========

    public PastorateTransfer createTransfer(UUID tenantId, UUID actorId, UUID pastorId, UUID toOrgUnitId, String reason) {
        // Validate pastor
        User pastor = userRepository.findById(pastorId)
                .orElseThrow(() -> new EntityNotFoundException("User", pastorId));
        if (!pastor.getTenantId().equals(tenantId)) {
            throw new SecurityException("Cross-tenant");
        }

        // Validate target org unit
        orgNodeRepository.findById(toOrgUnitId)
                .filter(u -> u.getTenantId().equals(tenantId))
                .orElseThrow(() -> new EntityNotFoundException("OrganizationNode", toOrgUnitId));

        // Get current appointment to get from_org_unit
        List<PastorateAppointment> current = appointmentRepository.findByTenantIdAndPastorIdAndDeletedAtIsNull(tenantId, pastorId);
        UUID fromOrgUnitId = current.isEmpty() ? null : current.get(0).getOrganizationUnitId();

        PastorateTransfer transfer = PastorateTransfer.builder()
                .tenantId(tenantId)
                .pastorId(pastorId)
                .fromOrgUnitId(fromOrgUnitId)
                .toOrgUnitId(toOrgUnitId)
                .transferDate(LocalDate.now())
                .reason(reason)
                .status("PENDING")
                .createdBy(securityUtils.getCurrentUserId())
                .build();

        return transferRepository.save(transfer);
    }

    public PastorateTransfer approveTransfer(UUID tenantId, UUID actorId, UUID transferId) {
        PastorateTransfer transfer = transferRepository.findByIdAndTenantId(transferId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("PastorateTransfer", transferId));

        if (!"PENDING".equals(transfer.getStatus())) {
            throw new IllegalStateException("Transfer already processed");
        }

        // Create appointment for new org unit
        PastorateAppointment appointment = PastorateAppointment.builder()
                .tenantId(tenantId)
                .pastorId(transfer.getPastorId())
                .organizationUnitId(transfer.getToOrgUnitId())
                .roleCode("PASTOR_CAMPUS")
                .appointmentType("TRANSFER")
                .startDate(transfer.getTransferDate())
                .reason("Transfert approuvé")
                .previousOrgUnitId(transfer.getFromOrgUnitId())
                .status("ACTIVE")
                .createdBy(actorId)
                .build();
        appointmentRepository.save(appointment);

        // End previous appointment
        closePreviousAppointments(tenantId, transfer.getPastorId(), transfer.getFromOrgUnitId(), null);

        transfer.setStatus("COMPLETED");
        transfer.setApprovedBy(securityUtils.getCurrentUserId());
        transfer.setApprovedAt(LocalDateTime.now());
        return transferRepository.save(transfer);
    }

    public void rejectTransfer(UUID tenantId, UUID actorId, UUID transferId, String reason) {
        PastorateTransfer transfer = transferRepository.findByIdAndTenantId(transferId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("PastorateTransfer", transferId));
        transfer.setStatus("REJECTED");
        transfer.setReason(reason);
        transferRepository.save(transfer);
    }

    @Transactional(readOnly = true)
    public List<PastorateTransfer> getTransfers(UUID tenantId, String status) {
        if (status != null && !status.isBlank()) {
            return transferRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status);
        }
        return transferRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, org.springframework.data.domain.PageRequest.of(0, 50)).getContent();
    }

    // ========== HELPERS ==========

    private void closePreviousAppointments(UUID tenantId, UUID pastorId, UUID orgUnitId, UUID excludeId) {
        List<PastorateAppointment> active = appointmentRepository.findByTenantIdAndPastorIdAndDeletedAtIsNull(tenantId, pastorId);
        for (PastorateAppointment appt : active) {
            if (appt.getOrganizationUnitId().equals(orgUnitId) && !appt.getId().equals(excludeId)) {
                appt.setStatus("ENDED");
                appt.setEndDate(LocalDate.now());
                appointmentRepository.save(appt);
            }
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPastorHistory(UUID tenantId, UUID pastorId) {
        Map<String, Object> history = new LinkedHashMap<>();
        List<PastorateAppointment> appointments = appointmentRepository.findByTenantIdAndPastorIdAndDeletedAtIsNull(tenantId, pastorId);
        List<PastorateTransfer> transfers = transferRepository.findByPastorIdAndStatus(pastorId, "COMPLETED");

        history.put("appointments", appointments);
        history.put("transfers", transfers);
        return history;
    }
}