package com.discipolat.modules.authentication.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enregistre {@link SocialAuthProperties}.
 *
 * <p>{@code DiscipolatApplication} n'utilise ni
 * {@code @ConfigurationPropertiesScan} ni
 * {@code @EnableConfigurationProperties} (cf. {@code AiConfiguration}) : les
 * blocs de configuration doivent donc être enregistrés explicitement, sans
 * modifier la classe principale.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SocialAuthProperties.class)
public class SocialAuthConfiguration {
}
