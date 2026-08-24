package com.discipolat.modules.volunteers.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface VolunteerRepository extends JpaRepository<Volunteer, UUID> {
    List<Volunteer> findByTenantIdAndStatutOrderByInscritLeDesc(UUID tenantId, Volunteer.Statut statut);
    List<Volunteer> findByMembreId(UUID membreId);
    long countByTenantIdAndStatut(UUID tenantId, Volunteer.Statut statut);
}
