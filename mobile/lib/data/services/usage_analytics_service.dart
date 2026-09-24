import '../../models/usage_analytics_summary.dart';
import 'api_service.dart';

class UsageAnalyticsService {
  UsageAnalyticsService({ApiService? apiService})
      : _apiService = apiService ?? ApiService();

  final ApiService _apiService;

  Future<UsageAnalyticsSummary> fetchSummary({required int days}) async {
    final response = await _apiService.get(
      '/usage-analytics/summary',
      params: {'days': days},
    );
    return UsageAnalyticsSummary.fromJson(response.data);
  }

  Future<bool?> fetchAnalyticsEnabled() async {
    final response = await _apiService.get('/admin/settings');
    return _analyticsEnabled(response.data);
  }

  static bool? _analyticsEnabled(dynamic json) {
    if (json is! Map) return null;
    final map = Map<String, dynamic>.from(json);
    final settings = map['settings'];
    final values = settings is Map ? Map<String, dynamic>.from(settings) : map;
    final value = values['analyticsEnabled'] ?? values['analytics_enabled'];
    if (value is bool) return value;
    if (value is String) {
      if (value.toLowerCase() == 'true') return true;
      if (value.toLowerCase() == 'false') return false;
    }
    return null;
  }
}
