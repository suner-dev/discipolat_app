package com.discipolat.common.infrastructure.api;

import com.discipolat.common.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void featureAndQuotaRulesRemainForbidden() {
        assertThat(handler.handleBusinessRule(new BusinessRuleException("disabled", "FEATURE_DISABLED_AI")).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(handler.handleBusinessRule(new BusinessRuleException("invalid", "QUOTA_EXCEEDED_USERS")).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void ordinaryBusinessRuleIsNotForbidden() {
        assertThat(handler.handleBusinessRule(new BusinessRuleException("invalid state", "INVALID_STATE")).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }
}
