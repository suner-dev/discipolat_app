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

    public DressCodeService(DressCodeRepository dressCodeRepository, DressCodeRuleRepository ruleRepository) {
        this.dressCodeRepository = dressCodeRepository;
        this.ruleRepository = ruleRepository;
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

        return saved;
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
