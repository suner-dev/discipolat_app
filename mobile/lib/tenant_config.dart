import 'package:shared_preferences/shared_preferences.dart';

/// Organisation / Tenant configuration for multi-tenant isolation
class TenantConfig {
  TenantConfig._();

  /// Current organisation ID (set from app state or role-based default)
  static String? currentOrgId;

  /// Shared preferences key for persisting org selection
  static const String _orgIdKey = 'current_org_id';

  /// Returns headers with orgId filter for all API calls
  /// When currentOrgId is null, returns empty headers (global mode for dev/testing)
  static Map<String, String> orgHeaders() => {
        'X-Org-Id': currentOrgId ?? '',
        'X-Tenant': currentOrgId ?? 'default',
      };

  /// Resolves the orgId from user context, role, or settings.
  /// Should be called on auth setup.
  ///
  /// Tolerant to missing storage (e.g. unit tests without a mock):
  /// falls back to the in-memory value, then null (global mode).
  static Future<String?> resolveOrgId() async {
    // Return the cached currentOrgId if already set
    if (currentOrgId != null && currentOrgId!.isNotEmpty) {
      return currentOrgId;
    }

    // Try to load from shared preferences
    try {
      final prefs = await SharedPreferences.getInstance();
      final storedOrgId = prefs.getString(_orgIdKey);
      if (storedOrgId != null && storedOrgId.isNotEmpty) {
        currentOrgId = storedOrgId;
        return currentOrgId;
      }
    } catch (_) {
      // Storage unavailable (tests) — in-memory value only
    }

    // Fall back to null (global mode)
    return null;
  }

  /// Sets the current organisation ID and persists it
  static Future<void> setOrgId(String orgId) async {
    currentOrgId = orgId;
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_orgIdKey, orgId);
    } catch (_) {
      // Storage unavailable (tests) — in-memory value only
    }
  }

  /// Clears the current organisation ID (logout)
  static Future<void> clearOrgId() async {
    currentOrgId = null;
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove(_orgIdKey);
    } catch (_) {
      // Storage unavailable (tests) — in-memory value only
    }
  }

  /// Checks if multi-tenant mode is active
  static bool get isMultiTenantActive => currentOrgId != null && currentOrgId!.isNotEmpty;

  /// Gets the current tenant label for display
  static String? get currentTenantLabel => currentOrgId;
}
