package com.discipolat.common.infrastructure.config;

import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String DEFAULT_PASSWORD = "password123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        String encodedPassword = passwordEncoder.encode(DEFAULT_PASSWORD);
        int updatedCount = 0;

        for (User user : userRepository.findAll()) {
            if ("PLACEHOLDER".equals(user.getPasswordHash())) {
                user.setPasswordHash(encodedPassword);
                userRepository.save(user);
                updatedCount++;
            }
        }

        if (updatedCount > 0) {
            log.info("✅ Initialized {} user accounts with default password: {}", updatedCount, DEFAULT_PASSWORD);
        }
    }
}
