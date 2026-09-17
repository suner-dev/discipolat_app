package com.discipolat.modules.core.config;

import com.discipolat.modules.core.service.OutboxConsumers;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxAutoConfiguration {

    private final OutboxConsumers outboxConsumers;

    @PostConstruct
    public void init() {
        outboxConsumers.registerAllConsumers();
    }
}