package com.discipolat.common.multitenancy;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Wires the tenant-aware repository base class into Spring Data JPA so that
 * {@code findById}/{@code getReferenceById} enforce tenant isolation on every
 * repository of the application (Hibernate {@code @Filter} does not cover
 * {@code EntityManager.find()}-based access).
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "com.discipolat",
        repositoryBaseClass = TenantAwareSimpleJpaRepository.class
)
public class TenantJpaConfig {
}
