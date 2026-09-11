package com.discipolat.modules.tenants.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * QuotaService — Vérifie les quotas SaaS par tenant (Section 31 du prompt maître).
 * 
 * Les quotas sont vérifiés côté backend à partir du plan actif du tenant.
 * Chaque plan définit des limites (max_users, max_churches, etc.) stockées en JSON.
 */
@Service
public class QuotaService {

    private final SaasPlanService saasPlanService;
    private final TenantMembershipRepository membershipRepository;
    private final OrganizationNodeRepository orgNodeRepository;

    public QuotaService(SaasPlanService saasPlanService,
                        TenantMembershipRepository membershipRepository,
                        OrganizationNodeRepository orgNodeRepository) {
        this.saasPlanService = saasPlanService;
        this.membershipRepository = membershipRepository;
        this.orgNodeRepository = orgNodeRepository;
    }

    /**
     * Vérifie si le quota d'utilisateurs est atteint pour le tenant courant.
     */
    public boolean canAddUser(UUID tenantId) {
        long currentUsers = membershipRepository.countByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE);
        SaasPlanService.QuotaCheck check = saasPlanService.checkQuota(tenantId, "max_users", currentUsers);
        return check.allowed();
    }

    /**
     * Vérifie si le quota d'églises est atteint.
     */
    public boolean canAddChurch(UUID tenantId) {
        long currentChurches = orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.ROOT_CHURCH);
        SaasPlanService.QuotaCheck check = saasPlanService.checkQuota(tenantId, "max_churches", currentChurches);
        return check.allowed();
    }

    /**
     * Vérifie si le quota de sous-églises est atteint.
     */
    public boolean canAddSubChurch(UUID tenantId) {
        long currentSubChurches = orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.SUB_CHURCH);
        SaasPlanService.QuotaCheck check = saasPlanService.checkQuota(tenantId, "max_sub_churches", currentSubChurches);
        return check.allowed();
    }

    /**
     * Vérifie si le quota de campus est atteint.
     */
    public boolean canAddCampus(UUID tenantId) {
        long currentCampuses = orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.CAMPUS);
        SaasPlanService.QuotaCheck check = saasPlanService.checkQuota(tenantId, "max_campuses", currentCampuses);
        return check.allowed();
    }

    /**
     * Vérifie si le quota de stockage est atteint (en MB).
     */
    public boolean canAddStorage(UUID tenantId, long additionalMb) {
        SaasPlanService.QuotaCheck check = saasPlanService.checkQuota(tenantId, "max_storage_mb", additionalMb);
        return check.allowed();
    }

    /**
     * Vérifie si le quota de requêtes IA est atteint.
     */
    public boolean canUseAi(UUID tenantId) {
        SaasPlanService.QuotaCheck check = saasPlanService.checkQuota(tenantId, "max_ai_requests_month", 0);
        return check.allowed() && check.limit() > 0;
    }

    /**
     * Vérifie si le quota de cours est atteint.
     */
    public boolean canAddCourse(UUID tenantId) {
        SaasPlanService.QuotaCheck check = saasPlanService.checkQuota(tenantId, "max_courses", 0);
        return check.allowed();
    }

    /**
     * Vérifie si le quota d'admins est atteint.
     */
    public boolean canAddAdmin(UUID tenantId) {
        SaasPlanService.QuotaCheck check = saasPlanService.checkQuota(tenantId, "max_admin_users", 0);
        return check.allowed();
    }

    /**
     * Vérifie un quota arbitraire avec la valeur courante donnée.
     */
    public SaasPlanService.QuotaCheck checkQuota(UUID tenantId, String quotaKey, long currentUsage) {
        return saasPlanService.checkQuota(tenantId, quotaKey, currentUsage);
    }

    /**
     * Vérifie si le tenant courant peut ajouter un utilisateur.
     */
    public boolean canCurrentTenantAddUser() {
        UUID tenantId = TenantContext.requireTenantId();
        return canAddUser(tenantId);
    }

    /**
     * Vérifie si le tenant courant peut ajouter une église.
     */
    public boolean canCurrentTenantAddChurch() {
        UUID tenantId = TenantContext.requireTenantId();
        return canAddChurch(tenantId);
    }
}
