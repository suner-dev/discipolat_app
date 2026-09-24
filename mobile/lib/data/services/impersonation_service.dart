import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../../app.dart';
import '../../tenant_config.dart';
import 'api_service.dart';

class ImpersonationService extends ChangeNotifier {
  ImpersonationService({ApiService? apiService})
      : _api = apiService ?? ApiService();

  static final ImpersonationService instance = ImpersonationService();

  static const _metadataKey = 'impersonation_metadata';
  static const _adminAccessTokenKey = 'impersonation_admin_access_token';
  static const _adminRefreshTokenKey = 'impersonation_admin_refresh_token';

  final ApiService _api;
  final FlutterSecureStorage _storage = const FlutterSecureStorage();
  bool _isImpersonating = false;
  String? targetEmail;
  String? targetUserId;
  String? targetRole;
  String? targetTenantId;
  String? targetTenantName;
  String? expiresAt;
  Map<String, dynamic>? _adminUser;
  String? _originalOrgId;

  bool get isImpersonating => _isImpersonating;

  Future<void> restoreIfNeeded() async {
    final raw = await _storage.read(key: _metadataKey);
    if (raw == null) return;
    try {
      final metadata = Map<String, dynamic>.from(jsonDecode(raw) as Map);
      _adminUser = Map<String, dynamic>.from(metadata['adminUser'] as Map);
      _originalOrgId = metadata['originalOrgId'] as String?;
      final activeToken = await _api.getAccessToken();
      final persistedToken = metadata['impersonationToken'] as String?;
      if (activeToken == null ||
          persistedToken == null ||
          activeToken != persistedToken) {
        await _restoreAdminAfterInterruptedStart();
        return;
      }
      targetEmail = metadata['targetEmail'] as String?;
      targetUserId = metadata['targetUserId'] as String?;
      targetRole = metadata['targetRole'] as String?;
      targetTenantId = metadata['targetTenantId'] as String?;
      targetTenantName = metadata['targetTenantName'] as String?;
      expiresAt = metadata['expiresAt'] as String?;
      _isImpersonating = true;
      await _applyTargetState();
      notifyListeners();
    } catch (_) {
      await clearBackup();
    }
  }

  Future<void> start({
    required String targetEmail,
    required String tenantId,
    required String reason,
  }) async {
    if (_isImpersonating || await _storage.read(key: _metadataKey) != null) {
      throw StateError('Une impersonation est déjà active');
    }
    final normalizedTenantId = tenantId.trim();
    if (normalizedTenantId.isEmpty) {
      throw StateError('Tenant cible requis');
    }
    final adminAccessToken = await _api.getAccessToken();
    final adminRefreshToken = await _api.getRefreshToken();
    final auth = AuthState();
    if (adminAccessToken == null) {
      throw StateError('Session administrateur introuvable');
    }

    final response = await _api.post(
      '/platform/admin/impersonation',
      data: {
        'tenantId': normalizedTenantId,
        'targetUserEmail': targetEmail,
        'reason': reason,
      },
    );
    final data = Map<String, dynamic>.from(response.data as Map);
    final impersonationToken = data['impersonationToken'] as String;
    final returnedUserId = data['targetUserId'] as String;
    final returnedTenantId = data['tenantId'] as String;
    final returnedRole = data['targetRole'] as String;
    final returnedTenantName = data['tenantName'] as String?;
    final returnedExpiresAt = data['expiresAt'] as String;
    if (returnedTenantId != normalizedTenantId) {
      throw StateError('Le tenant retourné ne correspond pas à la demande');
    }

    final adminUser = <String, dynamic>{
      'userId': auth.userId,
      'email': auth.email,
      'role': auth.activeRole,
      'roles': auth.roles,
      'platformRoles': auth.platformRoles,
      'platformSuperAdmin': auth.isPlatformSuperAdmin,
      'activeRole': auth.activeRole,
      'firstName': auth.firstName,
      'lastName': auth.lastName,
      'estChefDeFamille': auth.estChefDeFamille,
      'familleGereeId': auth.familleGereeId,
      'orgId': auth.orgId,
    };
    final originalOrgId = auth.orgId;

    await _storage.write(key: _adminAccessTokenKey, value: adminAccessToken);
    if (adminRefreshToken != null) {
      await _storage.write(
          key: _adminRefreshTokenKey, value: adminRefreshToken);
    } else {
      await _storage.delete(key: _adminRefreshTokenKey);
    }
    await _storage.write(
      key: _metadataKey,
      value: jsonEncode({
        'impersonationToken': impersonationToken,
        'targetEmail': targetEmail,
        'targetRole': returnedRole,
        'targetUserId': returnedUserId,
        'targetTenantId': returnedTenantId,
        'targetTenantName': returnedTenantName,
        'expiresAt': returnedExpiresAt,
        'originalOrgId': originalOrgId,
        'adminUser': adminUser,
      }),
    );

    await _api.deleteRefreshToken();
    await _api.saveTokens({'accessToken': impersonationToken});
    _adminUser = adminUser;
    _originalOrgId = originalOrgId;
    targetEmail = targetEmail;
    targetUserId = returnedUserId;
    targetRole = returnedRole;
    targetTenantId = returnedTenantId;
    targetTenantName = returnedTenantName;
    expiresAt = returnedExpiresAt;
    _isImpersonating = true;
    await _applyTargetState();
    notifyListeners();
  }

  Future<void> stop() async {
    if (!_isImpersonating) return;
    final currentToken = await _api.getAccessToken();
    try {
      if (currentToken != null) {
        await _api.post(
          '/platform/admin/impersonation/stop',
          data: {'impersonationToken': currentToken},
        );
      }
    } catch (_) {
    } finally {
      final adminAccessToken = await _storage.read(key: _adminAccessTokenKey);
      final adminRefreshToken = await _storage.read(key: _adminRefreshTokenKey);
      if (adminAccessToken != null) {
        await _api.saveTokens({
          'accessToken': adminAccessToken,
          if (adminRefreshToken != null) 'refreshToken': adminRefreshToken,
        });
      } else {
        await _api.deleteAccessToken();
      }
      final adminUser = _adminUser;
      if (adminUser != null) {
        AuthState().setAuthenticated(true, userData: adminUser);
      }
      if (_originalOrgId != null && _originalOrgId!.isNotEmpty) {
        await TenantConfig.setOrgId(_originalOrgId!);
      } else {
        await TenantConfig.clearOrgId();
      }
      _isImpersonating = false;
      targetEmail = null;
      targetUserId = null;
      targetRole = null;
      targetTenantId = null;
      targetTenantName = null;
      expiresAt = null;
      _adminUser = null;
      _originalOrgId = null;
      await clearBackup();
      notifyListeners();
    }
  }

  Future<void> _restoreAdminAfterInterruptedStart() async {
    final adminAccessToken = await _storage.read(key: _adminAccessTokenKey);
    final adminRefreshToken = await _storage.read(key: _adminRefreshTokenKey);
    if (adminAccessToken != null) {
      await _api.saveTokens({
        'accessToken': adminAccessToken,
        if (adminRefreshToken != null) 'refreshToken': adminRefreshToken,
      });
      final adminUser = _adminUser;
      if (adminUser != null) {
        AuthState().setAuthenticated(true, userData: adminUser);
      }
      if (_originalOrgId != null && _originalOrgId!.isNotEmpty) {
        await TenantConfig.setOrgId(_originalOrgId!);
      } else {
        await TenantConfig.clearOrgId();
      }
    } else {
      await _api.clearTokens();
      AuthState().logout();
    }
    await clearBackup();
  }

  Future<void> clearBackup() async {
    await _storage.delete(key: _metadataKey);
    await _storage.delete(key: _adminAccessTokenKey);
    await _storage.delete(key: _adminRefreshTokenKey);
    _isImpersonating = false;
    targetEmail = null;
    targetUserId = null;
    targetRole = null;
    targetTenantId = null;
    targetTenantName = null;
    expiresAt = null;
    _adminUser = null;
    _originalOrgId = null;
  }

  Future<void> _applyTargetState() async {
    AuthState().setAuthenticated(true, userData: {
      'userId': targetUserId,
      'email': targetEmail,
      'role': targetRole,
      'roles': [if (targetRole != null) targetRole!],
      'platformRoles': <String>[],
      'platformSuperAdmin': false,
      'activeRole': targetRole,
      'orgId': targetTenantId,
    });
    if (targetTenantId != null && targetTenantId!.isNotEmpty) {
      await TenantConfig.setOrgId(targetTenantId!);
    }
  }
}
