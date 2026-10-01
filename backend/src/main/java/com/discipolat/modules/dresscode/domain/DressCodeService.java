package com.discipolat.modules.dresscode.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DressCodeService {

    private final DressCodeRepository dressCodeRepository;
    private final DressCodeRuleRepository ruleRepository;
    /** §G3.4/§G6.4 — publication réelle : outbox (temps réel + historique) + notifications membres. */
    private final com.discipolat.modules.core.service.OutboxPublisher outboxPublisher;
    private final com.discipolat.modules.people.repository.SpaceMembershipRepository spaceMembershipRepository;
    private final com.discipolat.modules.people.repository.PersonRepository personRepository;
    private final com.discipolat.modules.users.domain.UserRepository userRepository;
    private final com.discipolat.modules.notifications.domain.NotificationService notificationService;

    public DressCodeService(DressCodeRepository dressCodeRepository, DressCodeRuleRepository ruleRepository,
                            com.discipolat.modules.core.service.OutboxPublisher outboxPublisher,
                            com.discipolat.modules.people.repository.SpaceMembershipRepository spaceMembershipRepository,
                            com.discipolat.modules.people.repository.PersonRepository personRepository,
                            com.discipolat.modules.users.domain.UserRepository userRepository,
                            com.discipolat.modules.notifications.domain.NotificationService notificationService) {
        this.dressCodeRepository = dressCodeRepository;
        this.ruleRepository = ruleRepository;
        this.outboxPublisher = outboxPublisher;
        this.spaceMembershipRepository = spaceMembershipRepository;
        this.personRepository = personRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<DressCode> findAll(UUID tenantId, UUID spaceId, UUID eventId) {
        return dressCodeRepository.findFiltered(tenantId, spaceId, eventId);
    }

    @Transactional(readOnly = true)
    public DressCode findById(UUID tenantId, UUID id) {
        return dressCodeRepository.findById(id)
                .filter(dc -> dc.getTenantId().equals(tenantId))
                .orElseThrow(() -> new EntityNotFoundException("DressCode", id));
    }

    public DressCode create(UUID tenantId, UUID creatorId, DressCode dressCode, List<DressCodeRule> rules) {
        dressCode.setTenantId(tenantId);
        dressCode.setCreatedBy(creatorId);
        dressCode.setArchived(false);
        DressCode saved = dressCodeRepository.save(dressCode);

        if (rules != null) {
            for (DressCodeRule rule : rules) {
                rule.setDressCodeId(saved.getId());
                ruleRepository.save(rule);
            }
        }

        publishIfNeeded(tenantId, saved);
        return saved;
    }

    public DressCode update(UUID tenantId, UUID id, DressCode updated, List<DressCodeRule> rules) {
        DressCode existing = findById(tenantId, id);
        existing.setTitle(updated.getTitle());
        existing.setServiceName(updated.getServiceName());
        existing.setSpaceId(updated.getSpaceId());
        existing.setEventId(updated.getEventId());
        existing.setBeginsAt(updated.getBeginsAt());
        existing.setEndsAt(updated.getEndsAt());
        existing.setStatus(updated.getStatus());

        DressCode saved = dressCodeRepository.save(existing);

        // Replace rules if provided
        if (rules != null) {
            ruleRepository.deleteByDressCodeId(saved.getId());
            for (DressCodeRule rule : rules) {
                rule.setDressCodeId(saved.getId());
                ruleRepository.save(rule);
            }
        }

        publishIfNeeded(tenantId, saved);
        return saved;
    }

    /**
     * §G3.4 — un dress code PUBLIED émet réellement l'événement
     * DressCodePublished (relay temps réel espace + historique métier via
     * l'outbox) ET notifie les membres de l'espace concerné (web + mobile).
     */
    private void publishIfNeeded(UUID tenantId, DressCode saved) {
        if (!"PUBLISHED".equalsIgnoreCase(saved.getStatus())) {
            return;
        }
        try {
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("title", saved.getTitle());
            payload.put("dressCodeId", saved.getId().toString());
            if (saved.getSpaceId() != null) payload.put("spaceId", saved.getSpaceId().toString());
            if (saved.getEventId() != null) payload.put("eventId", saved.getEventId().toString());
            outboxPublisher.publish(tenantId, "DRESS_CODE", saved.getId(), "DressCodePublished", payload);
        } catch (RuntimeException e) {
            // la notification ne bloque jamais l'enregistrement métier
        }
        notifySpaceMembers(tenantId, saved);
    }

    private void notifySpaceMembers(UUID tenantId, DressCode saved) {
        if (saved.getSpaceId() == null) {
            return;
        }
        try {
            String message = "Nouvelle tenue « " + saved.getTitle() + " »"
                    + (saved.getServiceName() != null ? " pour " + saved.getServiceName() : "")
                    + (saved.getBeginsAt() != null ? " à partir de " + saved.getBeginsAt() : "") + ".";
            for (var sm : spaceMembershipRepository.findByTenantIdAndSpaceIdAndStatus(tenantId, saved.getSpaceId(), "ACTIVE")) {
                personRepository.findById(sm.getPersonId()).ifPresent(person -> {
                    if (person.getEmailNormalized() == null) return;
                    userRepository.findByTenantIdAndEmailIgnoreCase(tenantId, person.getEmailNormalized()).ifPresent(user ->
                            notificationService.create(tenantId, user.getId(),
                                    com.discipolat.common.enums.TypeNotification.INFORMATION,
                                    com.discipolat.common.enums.CanalNotification.IN_APP,
                                    "Tenue de service publiée", message,
                                    saved.getId(), "DRESS_CODE"));
                });
            }
        } catch (RuntimeException e) {
            // notification défensive : jamais bloquante
        }
    }

    public void archive(UUID tenantId, UUID id) {
        DressCode dressCode = findById(tenantId, id);
        dressCode.setArchived(true);
        dressCodeRepository.save(dressCode);
    }

    public void delete(UUID tenantId, UUID id) {
        DressCode dressCode = findById(tenantId, id);
        ruleRepository.deleteByDressCodeId(id);
        dressCodeRepository.delete(dressCode);
    }

    @Transactional(readOnly = true)
    public List<DressCodeRule> getRules(UUID tenantId, UUID dressCodeId) {
        findById(tenantId, dressCodeId); // validate access
        return ruleRepository.findByDressCodeId(dressCodeId);
    }

    @Transactional(readOnly = true)
    public long count(UUID tenantId) {
        return dressCodeRepository.countByTenantIdAndArchivedFalse(tenantId);
    }
}
