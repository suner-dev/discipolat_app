package com.discipolat.modules.tickets.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    Page<Ticket> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    Page<Ticket> findByTenantIdAndStatut(UUID tenantId, Ticket.Statut statut, Pageable pageable);
    Page<Ticket> findByTenantIdAndCategorie(UUID tenantId, Ticket.Categorie categorie, Pageable pageable);
    Page<Ticket> findByTenantIdAndStatutAndCategorie(UUID tenantId, Ticket.Statut statut, Ticket.Categorie categorie, Pageable pageable);
}
