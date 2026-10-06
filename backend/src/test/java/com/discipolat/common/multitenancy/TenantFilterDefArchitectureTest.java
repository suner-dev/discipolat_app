package com.discipolat.common.multitenancy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garde-fou d'architecture multi-tenant.
 *
 * <p>Le filtre Hibernate {@code tenantFilter} doit être <b>déclaré une seule
 * fois</b> dans l'application (sur l'entité {@code User}). Hibernate lève
 * {@code AnnotationException: Multiple '@FilterDef' annotations define a
 * filter named 'tenantFilter'} dès qu'un second {@code @FilterDef} porte le
 * même nom — et cela <b>empêche l'EntityManagerFactory de démarrer</b>, donc
 * l'application entière de démarrer.
 *
 * <p>Ce défaut n'est détectable que par un test à contexte Spring complet :
 * les tests unitaires à mocks et les {@code @WebMvcTest} ne construisent pas
 * d'EntityManagerFactory et le laisse passer. D'où ce test, en plus du
 * démarrage réel de l'application.
 */
class TenantFilterDefArchitectureTest {

    private static final Path ENTITY_ROOT = Paths.get("src/main/java/com/discipolat");

    @Test
    @DisplayName("tenantFilter n'est DÉFINI (@FilterDef) qu'une seule fois dans toute la base")
    void tenantFilterDefIsDeclaredExactlyOnce() throws IOException {
        List<Path> declaring = new ArrayList<>();
        try (Stream<Path> files = Files.walk(ENTITY_ROOT)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                if (content.contains("@FilterDef(name = \"tenantFilter\"")) {
                    declaring.add(file);
                }
            }
        }
        assertThat(declaring)
                .as("un seul @FilterDef(name = \"tenantFilter\") est autorisé : Hibernate refuse "
                        + "les définitions dupliquées au démarrage de l'EntityManagerFactory")
                .hasSize(1);
        assertThat(declaring.get(0).toString())
                .as("la définition canonique vit sur l'entité User")
                .endsWith("users/domain/User.java");
    }

    @Test
    @DisplayName("toute entité filtrée par tenant déclare bien un champ tenantId")
    void filteredEntitiesExposeTenantId() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(ENTITY_ROOT)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                if (!content.contains("@Filter(name = \"tenantFilter\"")) {
                    continue;
                }
                // Un @Filter sans colonne tenant_id lèverait une erreur SQL à
                // l'exécution ; on l'attrape ici plutôt qu'en production.
                if (!content.contains("tenantId") || !content.contains("tenant_id")) {
                    offenders.add(file.toString());
                }
            }
        }
        assertThat(offenders)
                .as("toute entité portant @Filter(tenantFilter) doit exposer tenantId / tenant_id")
                .isEmpty();
    }
}
