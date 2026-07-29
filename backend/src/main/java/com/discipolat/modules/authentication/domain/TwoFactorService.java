package com.discipolat.modules.authentication.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class TwoFactorService {

    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    public TwoFactorService(UserRepository userRepository, SecurityUtils securityUtils) {
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
    }

    /**
     * Generate a TOTP secret for 2FA
     */
    public String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Generate a simple 6-digit TOTP code from secret
     */
    public String generateTOTP(String secret) {
        try {
            byte[] secretBytes = Base64.getDecoder().decode(secret);
            long counter = Instant.now().getEpochSecond() / 30;

            byte[] counterBytes = new byte[8];
            for (int i = 7; i >= 0; i--) {
                counterBytes[i] = (byte) (counter & 0xff);
                counter >>= 8;
            }

            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec keySpec = new SecretKeySpec(secretBytes, "HmacSHA1");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(counterBytes);

            int offset = hash[hash.length - 1] & 0xf;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);

            int otp = binary % 1000000;
            return String.format("%06d", otp);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate TOTP", e);
        }
    }

    /**
     * US-04: Enable 2FA for current user
     */
    public User enableTwoFactor() {
        UUID userId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        String secret = generateSecret();
        user.setTwoFactorSecret(secret);
        user.setTwoFactorEnabled(true);

        // Generate backup codes
        List<String> backupCodes = new ArrayList<>();
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 8; i++) {
            backupCodes.add(String.format("%08d", random.nextInt(100_000_000)));
        }
        user.setTwoFactorBackupCodes(String.join(",", backupCodes));

        return userRepository.save(user);
    }

    /**
     * US-04: Disable 2FA
     */
    public User disableTwoFactor() {
        UUID userId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        user.setTwoFactorBackupCodes(null);
        return userRepository.save(user);
    }

    /**
     * Verify a TOTP code or backup code
     */
    public boolean verifyCode(String code, User user) {
        if (!user.isTwoFactorEnabled() || user.getTwoFactorSecret() == null) {
            return true; // 2FA not enabled
        }

        // Check if it's a backup code
        if (user.getTwoFactorBackupCodes() != null) {
            Set<String> codes = new HashSet<>(Arrays.asList(user.getTwoFactorBackupCodes().split(",")));
            if (codes.contains(code)) {
                // Remove used backup code
                codes.remove(code);
                user.setTwoFactorBackupCodes(String.join(",", codes));
                userRepository.save(user);
                return true;
            }
        }

        // Check TOTP (allow ±1 time step for clock skew)
        String expectedCode = generateTOTP(user.getTwoFactorSecret());
        return expectedCode.equals(code);
    }

    /**
     * Force 2FA setup for Admin role
     */
    public void enforceAdmin2FA(UUID adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new EntityNotFoundException("User", adminId));
        if (admin.getRole() == UserRole.ADMIN && !admin.isTwoFactorEnabled()) {
            enableTwoFactorForUser(admin);
        }
    }

    private void enableTwoFactorForUser(User user) {
        String secret = generateSecret();
        user.setTwoFactorSecret(secret);
        user.setTwoFactorEnabled(true);
        userRepository.save(user);
    }
}
