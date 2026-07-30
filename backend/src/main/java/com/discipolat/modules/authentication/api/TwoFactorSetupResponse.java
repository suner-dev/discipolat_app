package com.discipolat.modules.authentication.api;

import java.util.List;

public record TwoFactorSetupResponse(
    boolean twoFactorEnabled,
    String secret,
    String otpauthUri,
    List<String> backupCodes
) {}
