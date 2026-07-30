package com.discipolat.modules.authentication.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.authentication.api.TwoFactorSetupResponse;
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

    private static final char[] BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();

    private String bytesToBase32(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0, bitsLeft = 0;
        for (byte b : bytes) {
            buffer = (buffer << 8) | (b & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                sb.append(BASE32[(buffer >> (bitsLeft - 5)) & 0x1f]);
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            sb.append(BASE32[(buffer << (5 - bitsLeft)) & 0x1f]);
        }
        return sb.toString();
    }

    /**
     * Generate a TOTP secret for 2FA (Base64-encoded)
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
     * US-04: Enable 2FA for current user — returns setup data
     */
    public TwoFactorSetupResponse enableTwoFactor(String email) {
        UUID userId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        String secret = generateSecret();
        user.setTwoFactorSecret(secret);
        user.setTwoFactorEnabled(true);

        // Generate backup codes
        SecureRandom random = new SecureRandom();
        List<String> backupCodes = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            backupCodes.add(String.format("%08d", random.nextInt(100_000_000)));
        }
        user.setTwoFactorBackupCodes(String.join(",", backupCodes));

        userRepository.save(user);

        // Build Base32 secret for authenticator app
        byte[] secretBytes = Base64.getDecoder().decode(secret);
        String base32Secret = bytesToBase32(secretBytes);
        String otpauthUri = "otpauth://totp/Discipolat:" + email
                + "?secret=" + base32Secret
                + "&issuer=Discipolat"
                + "&algorithm=SHA1&digits=6&period=30";

        return new TwoFactorSetupResponse(true, base32Secret, otpauthUri, backupCodes);
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
