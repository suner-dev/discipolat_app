package com.discipolat.modules.platform.domain;

import com.discipolat.common.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformFeatureFlagServiceTest {

    @Mock
    private PlatformFeatureFlagRepository repository;

    private PlatformFeatureFlagService service;

    @BeforeEach
    void setUp() {
        service = new PlatformFeatureFlagService(repository);
    }

    @Test
    void persistedTrueEnablesFeature() {
        when(repository.findByKey(PlatformFeatureFlagService.AI_ENABLED))
                .thenReturn(Optional.of(flag(PlatformFeatureFlagService.AI_ENABLED, true)));

        assertThat(service.isEnabled(PlatformFeatureFlagService.AI_ENABLED)).isTrue();
        service.requireEnabled(PlatformFeatureFlagService.AI_ENABLED);
    }

    @Test
    void persistedFalseBlocksFeatureWithFeatureSpecificRule() {
        when(repository.findByKey(PlatformFeatureFlagService.WHATSAPP_ENABLED))
                .thenReturn(Optional.of(flag(PlatformFeatureFlagService.WHATSAPP_ENABLED, false)));

        assertThatThrownBy(() -> service.requireEnabled(PlatformFeatureFlagService.WHATSAPP_ENABLED))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("WhatsApp")
                .extracting("code")
                .isEqualTo("FEATURE_DISABLED_WHATSAPP");
    }

    @Test
    void missingV168FlagsDefaultToEnabled() {
        when(repository.findByKey(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.empty());

        assertThat(List.of(
                PlatformFeatureFlagService.AI_ENABLED,
                PlatformFeatureFlagService.MOBILE_MONEY_ENABLED,
                PlatformFeatureFlagService.WHATSAPP_ENABLED,
                PlatformFeatureFlagService.ANALYTICS_ENABLED,
                PlatformFeatureFlagService.DOCS_ENABLED))
                .allMatch(service::isEnabled);
    }

    @Test
    void updateInvalidatesCachedValue() {
        PlatformFeatureFlag enabled = flag(PlatformFeatureFlagService.AI_ENABLED, true);
        PlatformFeatureFlag disabled = flag(PlatformFeatureFlagService.AI_ENABLED, false);
        when(repository.findByKey(PlatformFeatureFlagService.AI_ENABLED))
                .thenReturn(Optional.of(enabled), Optional.of(disabled));
        when(repository.save(disabled)).thenReturn(disabled);

        assertThat(service.isEnabled(PlatformFeatureFlagService.AI_ENABLED)).isTrue();
        service.toggle(PlatformFeatureFlagService.AI_ENABLED, false);
        assertThat(service.isEnabled(PlatformFeatureFlagService.AI_ENABLED)).isFalse();
        verify(repository).save(disabled);
    }

    private PlatformFeatureFlag flag(String key, boolean enabled) {
        PlatformFeatureFlag flag = new PlatformFeatureFlag();
        flag.setKey(key);
        flag.setName(key);
        flag.setEnabled(enabled);
        return flag;
    }
}
