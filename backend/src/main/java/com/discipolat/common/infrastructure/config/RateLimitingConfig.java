package com.discipolat.common.infrastructure.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitingConfig {

    // ======================== LOGIN ========================
    // Default: 10 requests per minute

    @Value("${app.rate-limiting.login-capacity:10}")
    private int loginCapacity;

    @Value("${app.rate-limiting.login-refill:10}")
    private int loginRefill;

    @Value("${app.rate-limiting.login-period-minutes:1}")
    private int loginPeriodMinutes;

    // ======================== REFRESH ========================
    // Default: 20 requests per minute

    @Value("${app.rate-limiting.refresh-capacity:20}")
    private int refreshCapacity;

    @Value("${app.rate-limiting.refresh-refill:20}")
    private int refreshRefill;

    @Value("${app.rate-limiting.refresh-period-minutes:1}")
    private int refreshPeriodMinutes;

    // ======================== FORGOT PASSWORD ========================
    // Default: 3 requests per minute

    @Value("${app.rate-limiting.forgot-password-capacity:3}")
    private int forgotPasswordCapacity;

    @Value("${app.rate-limiting.forgot-password-refill:3}")
    private int forgotPasswordRefill;

    @Value("${app.rate-limiting.forgot-password-period-minutes:1}")
    private int forgotPasswordPeriodMinutes;

    // ======================== RESET PASSWORD ========================
    // Default: 5 requests per minute

    @Value("${app.rate-limiting.reset-password-capacity:5}")
    private int resetPasswordCapacity;

    @Value("${app.rate-limiting.reset-password-refill:5}")
    private int resetPasswordRefill;

    @Value("${app.rate-limiting.reset-password-period-minutes:1}")
    private int resetPasswordPeriodMinutes;

    // ======================== ACTIVATE ACCOUNT ========================
    // Default: 5 requests per minute

    @Value("${app.rate-limiting.activate-capacity:5}")
    private int activateCapacity;

    @Value("${app.rate-limiting.activate-refill:5}")
    private int activateRefill;

    @Value("${app.rate-limiting.activate-period-minutes:1}")
    private int activatePeriodMinutes;

    // ======================== CHANGE PASSWORD ========================
    // Default: 5 requests per minute

    @Value("${app.rate-limiting.change-password-capacity:5}")
    private int changePasswordCapacity;

    @Value("${app.rate-limiting.change-password-refill:5}")
    private int changePasswordRefill;

    @Value("${app.rate-limiting.change-password-period-minutes:1}")
    private int changePasswordPeriodMinutes;

    // ======================== SWITCH ROLE ========================
    // Default: 30 requests per minute

    @Value("${app.rate-limiting.switch-role-capacity:30}")
    private int switchRoleCapacity;

    @Value("${app.rate-limiting.switch-role-refill:30}")
    private int switchRoleRefill;

    @Value("${app.rate-limiting.switch-role-period-minutes:1}")
    private int switchRolePeriodMinutes;

    // ======================== BEANS ========================

    @Bean
    public Bucket loginBucket() {
        return buildBucket(loginCapacity, loginRefill, loginPeriodMinutes);
    }

    @Bean
    public Bucket refreshBucket() {
        return buildBucket(refreshCapacity, refreshRefill, refreshPeriodMinutes);
    }

    @Bean
    public Bucket forgotPasswordBucket() {
        return buildBucket(forgotPasswordCapacity, forgotPasswordRefill, forgotPasswordPeriodMinutes);
    }

    @Bean
    public Bucket resetPasswordBucket() {
        return buildBucket(resetPasswordCapacity, resetPasswordRefill, resetPasswordPeriodMinutes);
    }

    @Bean
    public Bucket activateBucket() {
        return buildBucket(activateCapacity, activateRefill, activatePeriodMinutes);
    }

    @Bean
    public Bucket changePasswordBucket() {
        return buildBucket(changePasswordCapacity, changePasswordRefill, changePasswordPeriodMinutes);
    }

    @Bean
    public Bucket switchRoleBucket() {
        return buildBucket(switchRoleCapacity, switchRoleRefill, switchRolePeriodMinutes);
    }

    // ======================== HELPER ========================

    private static Bucket buildBucket(int capacity, int refillTokens, int periodMinutes) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(refillTokens, Duration.ofMinutes(periodMinutes)));
        return Bucket.builder().addLimit(limit).build();
    }
}
