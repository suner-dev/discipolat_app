package com.discipolat.modules.relations.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux rattachements déclaratifs (V231).
 *
 * <p><b>Règle stricte</b> : TOUTE méthode porte un {@code tenantId} explicite.
 * Le filtre Hibernate {@code tenantFilter} n'est actif qu'à l'intérieur d'une
 * requête HTTP ({@code TenantFilterInterceptor}) ; hors contexte requête
 * (tâches planifiées, tests unitaires, console d'administration) il est
 * inerte et laisserait fuire les rattachements d'une autre église. Le
 * {@code tenantId} en argument est donc la barrière <i>réelle</i>, le filtre
 * n'étant qu'une seconde ligne de défense.
 */
public interface MemberRelationRepository extends JpaRepository<MemberRelation, UUID> {

    List<MemberRelation> findByTenantIdAndFromUserIdAndStatut(UUID tenantId, UUID fromUserId,
                                                              MemberRelation.RelationStatus statut);

    List<MemberRelation> findByTenantIdAndToUserIdAndStatut(UUID tenantId, UUID toUserId,
                                                             MemberRelation.RelationStatus statut);

    Page<MemberRelation> findByTenantIdAndFromUserIdAndStatut(UUID tenantId, UUID fromUserId,
                                                              MemberRelation.RelationStatus statut,
                                                              Pageable pageable);

    Page<MemberRelation> findByTenantIdAndToUserIdAndStatut(UUID tenantId, UUID toUserId,
                                                             MemberRelation.RelationStatus statut,
                                                             Pageable pageable);

    List<MemberRelation> findByTenantIdAndFromUserIdOrderByCreatedAtDesc(UUID tenantId, UUID fromUserId);

    List<MemberRelation> findByTenantIdAndToUserIdOrderByCreatedAtDesc(UUID tenantId, UUID toUserId);

    Optional<MemberRelation> findByTenantIdAndId(UUID tenantId, UUID id);

    /**
     * Détection de doublon « un seul ACTIVE par (tenant, from, to, type) ».
     * Le {@code tenantId} est indispensable : sans lui la règle n'est pas
     * applicable à l'échelle d'une église.
     */
    Optional<MemberRelation> findByTenantIdAndFromUserIdAndToUserIdAndRelationTypeAndStatut(
            UUID tenantId, UUID fromUserId, UUID toUserId, String relationType,
            MemberRelation.RelationStatus statut);

    long countByTenantIdAndFromUserIdAndStatut(UUID tenantId, UUID fromUserId,
                                               MemberRelation.RelationStatus statut);

    long countByTenantIdAndToUserIdAndStatut(UUID tenantId, UUID toUserId,
                                             MemberRelation.RelationStatus statut);
}
