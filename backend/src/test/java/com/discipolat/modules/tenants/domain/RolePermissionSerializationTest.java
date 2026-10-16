package com.discipolat.modules.tenants.domain;

import com.discipolat.common.infrastructure.config.TenantSerializationConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verrou de sérialisation — cycle {@code Role ↔ Permission}.
 *
 * <p>Constaté en environnement de démonstration exposé : {@code GET
 * /api/v1/admin/roles/overview} renvoyait les entités JPA telles quelles.
 * La rétroférence {@code Permission.roles} n'étant pas coupée, Jackson
 * partait en récursion ({@code Role["permissions"] -> Permission["roles"]
 * -> Role["permissions"] -> …}) jusqu'à « Document nesting depth (1001)
 * exceeds the maximum allowed » — et comme la réponse était déjà engagée,
 * le client recevait un <b>HTTP 200 au JSON tronqué</b> (~146 Ko coupés en
 * plein vol), inutilisable par {@code TenantContext}/{@code useInvitations}.
 *
 * <p>Ce test est <b>discriminant</b> : il exerce le câblage réel — le
 * {@code Jackson2ObjectMapperBuilderCustomizer} de
 * {@code TenantSerializationConfig} — et tombe en récursion (rouge vérifié
 * en désactivant le customizer) si la coupure est retirée. Les trois
 * formes imposées — entité seule, dans une Map, dans une Collection —
 * couvrent toutes les manières dont ces entités atteignent un sérialiseur.
 */
class RolePermissionSerializationTest {

    /** ObjectMapper construit comme l'est celui de l'application : builder Spring + customizers réels. */
    private ObjectMapper productionMapper() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new TenantSerializationConfig().rolePermissionCycleBreaker().customize(builder);
        return builder.build();
    }

    /** Role avec Permission, liés dans les deux sens comme les rend Hibernate. */
    private Role cyclicRole() {
        UUID roleId = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
        UUID permId = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

        Permission viewMembers = Permission.builder()
                .id(permId)
                .key("MEMBER_VIEW")
                .label("Voir les membres")
                .scope(PermissionScope.TENANT)
                .system(true)
                .build();

        Role pastor = Role.builder()
                .id(roleId)
                .key("PASTEUR")
                .label("Pasteur")
                .system(true)
                .permissions(Set.of(viewMembers))
                .build();

        // Le cycle exact rendu par Hibernate en production :
        // permission.roles → pastor → pastor.permissions → permission → …
        viewMembers.setRoles(Set.of(pastor));
        return pastor;
    }

    @Test
    @DisplayName("Un Role cyclique se sérialise sans récursion, permissions contractuelles conservées")
    void roleSeul() throws Exception {
        ObjectMapper mapper = productionMapper();
        String json = assertDoesNotThrow(() -> mapper.writeValueAsString(cyclicRole()),
                "La sérialisation d'un Role lié à ses Permissions ne doit jamais partir en récursion");

        assertTrue(json.contains("\"MEMBER_VIEW\""),
                "Le contrat lu par le front (role.permissions[].key) doit rester présent");
        assertFalse(json.contains("\"roles\""),
                "La rétroférence Permission.roles doit rester coupée du JSON");
    }

    @Test
    @DisplayName("Role dans une Map : même verrou")
    void roleDansUneMap() {
        ObjectMapper mapper = productionMapper();
        Map<String, Role> payload = Map.of("role", cyclicRole());
        String json = assertDoesNotThrow(() -> mapper.writeValueAsString(payload));
        assertFalse(json.contains("\"roles\""));
        assertNotNull(json);
    }

    @Test
    @DisplayName("Role dans une Collection (contrat de /overview) : même verrou")
    void roleDansUneCollection() {
        ObjectMapper mapper = productionMapper();
        List<Role> overview = List.of(cyclicRole(), cyclicRole());
        String json = assertDoesNotThrow(() -> mapper.writeValueAsString(overview),
                "/overview rend une liste : c'est exactement la forme qui tronquait la réponse en démo");
        assertFalse(json.contains("\"roles\""));
    }

    @Test
    @DisplayName("Permission seule : sérialisable, sans exposer ses rôles")
    void permissionSeule() throws Exception {
        ObjectMapper mapper = productionMapper();
        Permission p = cyclicRole().getPermissions().iterator().next();
        String json = assertDoesNotThrow(() -> mapper.writeValueAsString(p));
        assertTrue(json.contains("\"MEMBER_VIEW\""));
        assertFalse(json.contains("\"roles\""));
    }
}
