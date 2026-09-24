package com.discipolat.modules.platform.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PlatformFeatureFlagService {

    private final PlatformFeatureFlagRepository repository;

    public List<PlatformFeatureFlag> getAll() {
        return repository.findAllByOrderByCategoryAscKeyAsc();
    }

    public Map<String, Boolean> getAllAsMap() {
        return repository.findAllByOrderByCategoryAscKeyAsc().stream()
                .collect(Collectors.toMap(PlatformFeatureFlag::getKey, PlatformFeatureFlag::isEnabled));
    }

    public Optional<PlatformFeatureFlag> getByKey(String key) {
        return repository.findByKey(key);
    }

    public PlatformFeatureFlag createOrUpdate(String key, String name, String description, boolean enabled, String category) {
        PlatformFeatureFlag flag = repository.findByKey(key).orElse(PlatformFeatureFlag.builder().key(key).build());
        flag.setName(name);
        flag.setDescription(description);
        flag.setEnabled(enabled);
        flag.setCategory(category);
        return repository.save(flag);
    }

    public PlatformFeatureFlag toggle(String key, boolean enabled) {
        PlatformFeatureFlag flag = repository.findByKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Feature flag not found: " + key));
        flag.setEnabled(enabled);
        return repository.save(flag);
    }

    public void delete(String key) {
        repository.findByKey(key).ifPresent(repository::delete);
    }

    public void seedDefaults() {
        var defaults = List.of(
                new DefaultFlag("aiEnabled", "Intelligence Artificielle", "Fonctionnalités IA (sermons, conseils, etc.)", true, "AI"),
                new DefaultFlag("mobileMoneyEnabled", "Mobile Money", "Paiements Mobile Money (Orange Money, MTN MoMo, etc.)", true, "PAYMENTS"),
                new DefaultFlag("whatsappEnabled", "WhatsApp Business", "Notifications et communication via WhatsApp", true, "COMMUNICATION"),
                new DefaultFlag("analyticsEnabled", "Analytics Avancés", "Tableaux de bord et rapports avancés", true, "ANALYTICS"),
                new DefaultFlag("docsEnabled", "Documentation", "Accès à la documentation intégrée", true, "CORE")
        );

        for (var d : defaults) {
            if (repository.findByKey(d.key).isEmpty()) {
                createOrUpdate(d.key, d.name, d.description, d.enabled, d.category);
            }
        }
    }

    private record DefaultFlag(String key, String name, String description, boolean enabled, String category) {}
}