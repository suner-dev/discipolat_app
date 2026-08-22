package com.discipolat.modules.tontine.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TontineMemberRepository extends JpaRepository<TontineMember, UUID> {
    List<TontineMember> findByGroupIdOrderByOrdrePassageAsc(UUID groupId);
    Optional<TontineMember> findByGroupIdAndSoulId(UUID groupId, UUID soulId);
    long countByGroupId(UUID groupId);

    @Query("SELECT COUNT(m) FROM TontineMember m WHERE m.groupId = :groupId AND m.userId IS NOT NULL")
    long countWithUserAccount(@Param("groupId") UUID groupId);
}
