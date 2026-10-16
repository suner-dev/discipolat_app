package com.discipolat.common.infrastructure.config;

import com.discipolat.modules.tenants.domain.Permission;
import com.discipolat.modules.tenants.domain.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * Coupure du cycle de sérialisation {@code Role ↔ Permission}.
 *
 * <p>Constaté en environnement de démonstration exposé : {@code GET
 * /api/v1/admin/roles/overview} rend les entités JPA telles quelles. La
 * rétroférence {@code Permission.roles} n'étant pas coupée, Jackson partait
 * en récursion ({@code Role["permissions"] → Permission["roles"] → …})
 * jusqu'à « Document nesting depth (1001) exceeds the maximum allowed » ;
 * la réponse étant déjà engagée, le client recevait un HTTP 200 au JSON
 * tronqué (~146 Ko coupés en plein vol), inutilisable par
 * {@code TenantContext}/{@code useInvitations}.
 *
 * <p>La coupure se fait ici, par mixin d'infrastructure, et non par
 * l'annotation {@code @JsonIgnore} sur l'entité : la règle d'architecture
 * R1 (gel à platfond, test {@code ArchitectureRulesTest}) interdit de faire
 * dépendre le domaine à une librairie de sérialisation. Le domaine reste
 * propre ; la vue JSON se configure dans la couche qui possède Jackson.
 *
 * <p>Contractuellement sans effet côté clients : {@code Permission} n'entre
 * dans aucun corps de requête et la rétroférence {@code roles} n'est lue
 * nulle part (front web : {@code TenantContext}, {@code useInvitations}).
 */
@Configuration
public class TenantSerializationConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer rolePermissionCycleBreaker() {
        return builder -> builder.mixIn(Permission.class, PermissionMixins.class);
    }

    /** Only the back-reference is hidden; {@code Role.permissions} stays in the payload. */
    abstract static class PermissionMixins {
        @JsonIgnore
        abstract Set<Role> getRoles();
    }
}
