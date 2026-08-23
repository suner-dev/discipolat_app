package com.discipolat.modules.tickets.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tickets.api.CreateTicketRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMessageRepository messageRepository;

    public TicketService(TicketRepository ticketRepository, TicketMessageRepository messageRepository) {
        this.ticketRepository = ticketRepository;
        this.messageRepository = messageRepository;
    }

    public Page<Ticket> list(Pageable pageable, String statut, String categorie) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        if (statut != null && categorie != null) {
            return ticketRepository.findByTenantIdAndStatutAndCategorie(tenantId,
                    Ticket.Statut.valueOf(statut), Ticket.Categorie.valueOf(categorie), pageable);
        } else if (statut != null) {
            return ticketRepository.findByTenantIdAndStatut(tenantId,
                    Ticket.Statut.valueOf(statut), pageable);
        } else if (categorie != null) {
            return ticketRepository.findByTenantIdAndCategorie(tenantId,
                    Ticket.Categorie.valueOf(categorie), pageable);
        }
        return ticketRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    public Ticket getById(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ticket", id));
    }

    public Ticket create(CreateTicketRequest request, UUID userId) {
        Ticket ticket = new Ticket();
        ticket.setTenantId(TenantContext.getCurrentTenantId());
        ticket.setTitre(request.titre());
        ticket.setDescription(request.description());
        ticket.setCategorie(Ticket.Categorie.valueOf(request.categorie()));
        ticket.setPriorite(Ticket.Priorite.valueOf(request.priorite()));
        ticket.setCreePar(userId);
        return ticketRepository.save(ticket);
    }

    public Ticket updateStatus(UUID id, String statut) {
        Ticket ticket = getById(id);
        ticket.setStatut(Ticket.Statut.valueOf(statut));
        ticket.setUpdatedAt(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }

    public Ticket addMessage(UUID ticketId, String contenu, UUID userId) {
        Ticket ticket = getById(ticketId);
        TicketMessage message = new TicketMessage();
        message.setTicket(ticket);
        message.setContenu(contenu);
        message.setAuteurId(userId);
        messageRepository.save(message);
        ticket.getMessages().add(message);
        ticket.setUpdatedAt(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }
}
