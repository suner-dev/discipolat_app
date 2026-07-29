package com.discipolat.modules.users.domain;

import com.discipolat.common.domain.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    List<User> findByRole(UserRole role);

    Page<User> findByRole(UserRole role, Pageable pageable);

    List<User> findByFamilleGereeId(UUID familleId);

    boolean existsByEmail(String email);

    List<User> findByEstChefDeFamilleTrue();

    long countByRole(UserRole role);

    Optional<User> findByFamilleGereeIdAndEstChefDeFamilleTrue(UUID familleId);
}
