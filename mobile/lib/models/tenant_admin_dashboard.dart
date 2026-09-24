class TenantAdminDashboardData {
  const TenantAdminDashboardData({
    this.tenantId,
    this.tenantName,
    this.totalUsers,
    this.activeUsers,
    this.totalMemberships,
    this.churchCount,
    this.departmentCount,
    this.subChurchCount,
    this.campusCount,
    this.groupCount,
    this.membersByRole = const {},
    this.subscription,
  });

  final String? tenantId;
  final String? tenantName;
  final num? totalUsers;
  final num? activeUsers;
  final num? totalMemberships;
  final num? churchCount;
  final num? departmentCount;
  final num? subChurchCount;
  final num? campusCount;
  final num? groupCount;
  final Map<String, num> membersByRole;
  final TenantSubscriptionSummary? subscription;

  factory TenantAdminDashboardData.fromJson(dynamic json) {
    final root = TenantAdminDashboardData._mapOrThrow(json);
    final unwrapped =
        TenantAdminDashboardData._firstMap(root, const ['data', 'dashboard']) ??
            root;
    final users =
        TenantAdminDashboardData._firstMap(unwrapped, const ['users']) ??
            const {};
    final organizations = TenantAdminDashboardData._firstMap(
          unwrapped,
          const ['organizations', 'orgStats'],
        ) ??
        const {};
    final tenant =
        TenantAdminDashboardData._firstMap(unwrapped, const ['tenant']) ??
            const {};

    return TenantAdminDashboardData(
      tenantId: _string(unwrapped, 'tenantId') ?? _string(tenant, 'id'),
      tenantName: _string(unwrapped, 'tenantName') ?? _string(tenant, 'name'),
      totalUsers: _number(unwrapped, 'totalUsers') ?? _number(users, 'total'),
      activeUsers:
          _number(unwrapped, 'activeUsers') ?? _number(users, 'active'),
      totalMemberships: _number(unwrapped, 'totalMemberships') ??
          _number(users, 'totalMemberships'),
      churchCount: _number(unwrapped, 'churchCount') ??
          _number(organizations, 'churches') ??
          _number(unwrapped, 'churches'),
      departmentCount: _number(unwrapped, 'departmentCount') ??
          _number(organizations, 'departments') ??
          _number(unwrapped, 'departments'),
      subChurchCount: _number(unwrapped, 'subChurchCount') ??
          _number(organizations, 'subChurches') ??
          _number(unwrapped, 'subChurches'),
      campusCount: _number(unwrapped, 'campusCount') ??
          _number(organizations, 'campuses') ??
          _number(unwrapped, 'campuses'),
      groupCount: _number(unwrapped, 'groupCount') ??
          _number(organizations, 'groups') ??
          _number(unwrapped, 'groups'),
      membersByRole:
          _numbers(_firstMap(unwrapped, const ['membersByRole']) ?? const {}),
      subscription: _firstMap(unwrapped, const ['subscription']) == null
          ? null
          : TenantSubscriptionSummary.fromJson(
              _firstMap(unwrapped, const ['subscription']),
            ),
    );
  }

  static Map<String, dynamic> _mapOrThrow(dynamic value) {
    if (value is Map<String, dynamic>) return value;
    if (value is Map) return Map<String, dynamic>.from(value);
    throw const FormatException('Tenant dashboard response must be an object');
  }

  static Map<String, dynamic>? _firstMap(
    Map<String, dynamic> json,
    List<String> keys,
  ) {
    for (final key in keys) {
      final value = json[key];
      if (value is Map) return Map<String, dynamic>.from(value);
    }
    return null;
  }

  static String? _string(Map<String, dynamic> json, String key) {
    final value = json[key];
    return value is String && value.isNotEmpty ? value : null;
  }

  static num? _number(Map<String, dynamic> json, String key) {
    final value = json[key];
    if (value is num) return value;
    if (value is String) return num.tryParse(value);
    return null;
  }

  static Map<String, num> _numbers(Map<String, dynamic> json) {
    final result = <String, num>{};
    for (final entry in json.entries) {
      final value = _number(json, entry.key);
      if (value != null) result[entry.key] = value;
    }
    return result;
  }
}

class TenantSubscriptionSummary {
  const TenantSubscriptionSummary({
    this.planKey,
    this.status,
    this.currentPeriodEnd,
  });

  final String? planKey;
  final String? status;
  final DateTime? currentPeriodEnd;

  factory TenantSubscriptionSummary.fromJson(dynamic json) {
    if (json is! Map) {
      return const TenantSubscriptionSummary();
    }
    final map = Map<String, dynamic>.from(json);
    return TenantSubscriptionSummary(
      planKey: _string(map, 'planKey'),
      status: _string(map, 'status'),
      currentPeriodEnd:
          DateTime.tryParse(_string(map, 'currentPeriodEnd') ?? ''),
    );
  }

  static String? _string(Map<String, dynamic> json, String key) {
    final value = json[key];
    return value is String && value.isNotEmpty ? value : null;
  }
}
