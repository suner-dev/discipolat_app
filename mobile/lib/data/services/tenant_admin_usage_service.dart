import '../../models/quota_usage.dart';
import '../../models/tenant_admin_dashboard.dart';
import 'api_service.dart';

class TenantAdminUsageSnapshot {
  const TenantAdminUsageSnapshot({
    this.dashboard,
    this.quotaUsage,
    this.analyticsEnabled,
    this.dashboardFailed = false,
    this.quotaFailed = false,
    this.settingsFailed = false,
  });

  final TenantAdminDashboardData? dashboard;
  final QuotaUsage? quotaUsage;
  final bool? analyticsEnabled;
  final bool dashboardFailed;
  final bool quotaFailed;
  final bool settingsFailed;
}

class TenantAdminUsageService {
  TenantAdminUsageService({ApiService? apiService})
      : _apiService = apiService ?? ApiService();

  final ApiService _apiService;

  Future<QuotaUsage> fetchQuotaUsage() async {
    final response = await _apiService.get('/admin/quotas/usage');
    return QuotaUsage.fromJson(response.data);
  }

  Future<TenantAdminDashboardData> fetchDashboard() async {
    final response = await _apiService.get('/admin/dashboard/overview');
    return TenantAdminDashboardData.fromJson(response.data);
  }

  Future<bool?> fetchAnalyticsEnabled() async {
    final response = await _apiService.get('/admin/settings');
    return _analyticsEnabled(response.data);
  }

  Future<TenantAdminUsageSnapshot> load() async {
    final dashboard = _load(
      () => fetchDashboard(),
      (value) => value,
    );
    final quotaUsage = _load(
      () => fetchQuotaUsage(),
      (value) => value,
    );
    final settings = _load(
      () => fetchAnalyticsEnabled(),
      (value) => value,
    );
    final results = await Future.wait([
      dashboard,
      quotaUsage,
      settings,
    ]);

    return TenantAdminUsageSnapshot(
      dashboard: results[0].value as TenantAdminDashboardData?,
      quotaUsage: results[1].value as QuotaUsage?,
      analyticsEnabled: results[2].value as bool?,
      dashboardFailed: results[0].failed,
      quotaFailed: results[1].failed,
      settingsFailed: results[2].failed,
    );
  }

  static bool? _analyticsEnabled(dynamic json) {
    if (json is! Map) return null;
    final map = Map<String, dynamic>.from(json);
    final settingsValue = map['settings'];
    final settings =
        settingsValue is Map ? Map<String, dynamic>.from(settingsValue) : map;
    final value = settings['analyticsEnabled'] ?? settings['analytics_enabled'];
    if (value is bool) return value;
    if (value is String) {
      if (value.toLowerCase() == 'true') return true;
      if (value.toLowerCase() == 'false') return false;
    }
    return null;
  }

  Future<_EndpointResult<T>> _load<T>(
    Future<T> Function() request,
    T Function(T value) identity,
  ) async {
    try {
      final value = await request();
      return _EndpointResult<T>(value: identity(value));
    } catch (_) {
      return _EndpointResult<T>.failed();
    }
  }
}

class _EndpointResult<T> {
  const _EndpointResult({this.value, this.failed = false});

  const _EndpointResult.failed() : this(failed: true);

  final T? value;
  final bool failed;
}
