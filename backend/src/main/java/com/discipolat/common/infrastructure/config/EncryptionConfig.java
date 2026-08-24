package com.discipolat.common.infrastructure.config;

import com.discipolat.common.infrastructure.security.CryptoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fournit le service de chiffrement AES-256-GCM (données sensibles au repos).
 */
@Configuration
public class EncryptionConfig {

    @Bean
    public CryptoService cryptoService(@Value("${app.encryption.aes-key}") String aesKey) {
        return new CryptoService(aesKey);
    }
}
