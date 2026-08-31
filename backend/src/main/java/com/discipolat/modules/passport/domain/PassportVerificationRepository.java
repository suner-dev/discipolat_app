package com.discipolat.modules.passport.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PassportVerificationRepository extends JpaRepository<PassportVerification, UUID> {

    Page<PassportVerification> findByPassportCodeOrderByVerifiedAtDesc(String passportCode, Pageable pageable);

    long countByPassportCode(String passportCode);
}
