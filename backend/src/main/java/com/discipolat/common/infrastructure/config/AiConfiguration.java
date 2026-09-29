package com.discipolat.common.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enregistre les blocs de configuration IA (M5, M6).
 *
 * <p>{@code DiscipolatApplication} n'utilise ni {@code @ConfigurationPropertiesScan}
 * ni {@code @EnableConfigurationProperties}, et le reste du projet configure
 * ses dépendances externes par {@code @Component + @Value}. Les deux blocs
 * ci-dessous utilisent en revanche {@code @ConfigurationProperties} (validation
 * Jakarta, valeurs par défaut déclaratives) : ils doivent donc être enregistrés
 * explicitement. Ce {@code @Configuration} est le point d'entrée unique, à la
 * place d'une modification de la classe principale.</p>
 *
 * <p>Note d'intégration : si un {@code @ConfigurationPropertiesScan} est ajouté
 * plus tard à {@code DiscipolatApplication}, ce {@code @EnableConfigurationProperties}
 * devient redondant et doit être retiré pour éviter un doublon de définition.</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({OllamaProperties.class, SpeechToTextProperties.class})
public class AiConfiguration {
}
