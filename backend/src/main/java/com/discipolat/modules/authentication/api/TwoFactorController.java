package com.discipolat.modules.authentication.api;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.authentication.domain.TwoFactorService;
import com.discipolat.modules.users.api.UserResponse;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/2fa")
public class TwoFactorController {

    private final TwoFactorService twoFactorService;
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;

    public TwoFactorController(TwoFactorService twoFactorService,
                                SecurityUtils securityUtils,
                                UserRepository userRepository) {
        this.twoFactorService = twoFactorService;
        this.securityUtils = securityUtils;
        this.userRepository = userRepository;
    }

    /** US-04: Enable 2FA */
    @PostMapping("/enable")
    public ResponseEntity<UserResponse> enable() {
        User user = twoFactorService.enableTwoFactor();
        return ResponseEntity.ok(UserResponse.from(user));
    }

    /** US-04: Disable 2FA */
    @PostMapping("/disable")
    public ResponseEntity<UserResponse> disable() {
        User user = twoFactorService.disableTwoFactor();
        return ResponseEntity.ok(UserResponse.from(user));
    }

    /** US-04: Verify 2FA code */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestBody Map<String, String> body) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("User", currentUserId));
        String code = body.get("code");
        boolean isValid = twoFactorService.verifyCode(code, user);
        return ResponseEntity.ok(Map.of("valid", isValid));
    }
}
