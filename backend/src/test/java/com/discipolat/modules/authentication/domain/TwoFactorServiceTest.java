package com.discipolat.modules.authentication.domain;

import com.discipolat.common.domain.UserRole;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.discipolat.common.infrastructure.security.SecurityTestHelper;

@ExtendWith(MockitoExtension.class)
class TwoFactorServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private SecurityUtils securityUtils;

    private TwoFactorService twoFactorService;
    private UUID userId;
    private User testUser;

    @BeforeEach
    void setUp() {
        SecurityTestHelper.loginAs(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        twoFactorService = new TwoFactorService(userRepository, securityUtils);
        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .email("test@discipolat.com")
                .role(UserRole.PASTEUR)
                .statut(UserStatus.ACTIVE)
                .twoFactorEnabled(false)
                .build();
    }

    @Test
    void generateSecret_ShouldReturnBase64EncodedString() {
        String secret = twoFactorService.generateSecret();
        assertNotNull(secret);
        assertFalse(secret.isEmpty());
    }

    @Test
    void generateTOTP_ShouldReturn6DigitCode() {
        String secret = twoFactorService.generateSecret();
        String code = twoFactorService.generateTOTP(secret);
        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"));
    }

    @Test
    void enableTwoFactor_ShouldReturnSetupResponse() {
        SecurityTestHelper.loginAs(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        var result = twoFactorService.enableTwoFactor("test@discipolat.com");

        assertTrue(result.twoFactorEnabled());
        assertNotNull(result.secret());
        assertNotNull(result.backupCodes());
        assertEquals(8, result.backupCodes().size());
        assertNotNull(result.otpauthUri());
        assertTrue(result.otpauthUri().startsWith("otpauth://totp/"));
    }

    @Test
    void disableTwoFactor_ShouldClearFields() {
        testUser.setTwoFactorEnabled(true);
        testUser.setTwoFactorSecret("secret");
        testUser.setTwoFactorBackupCodes("1234,5678");

        SecurityTestHelper.loginAs(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = twoFactorService.disableTwoFactor();

        assertFalse(result.isTwoFactorEnabled());
        assertNull(result.getTwoFactorSecret());
        assertNull(result.getTwoFactorBackupCodes());
    }

    @Test
    void verifyCode_Without2FA_ShouldReturnTrue() {
        assertTrue(twoFactorService.verifyCode("000000", testUser));
    }

    @Test
    void verifyCode_WithBackupCode_ShouldReturnTrue() {
        testUser.setTwoFactorEnabled(true);
        testUser.setTwoFactorSecret(twoFactorService.generateSecret());
        testUser.setTwoFactorBackupCodes("12345678,87654321");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        assertTrue(twoFactorService.verifyCode("12345678", testUser));
        // Backup code should have been removed after use
        assertFalse(testUser.getTwoFactorBackupCodes().contains("12345678"));
    }
}
