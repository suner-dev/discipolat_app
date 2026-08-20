import 'package:dio/dio.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';

/// Reusable fake ApiService for widget tests.
/// Returns minimal mock data for all common endpoints.
class FakeApiService extends ApiService {
  FakeApiService() : super(baseUrl: 'http://fake');

  final List<String> getPaths = [];
  final List<String> postPaths = [];

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    getPaths.add(path);

    // Smart Alerts
    if (path.contains('/smart-alerts/summary')) {
      return _json(path, {'totalActive': 0, 'criticalActive': 0, 'lastScan': DateTime.now().toIso8601String()});
    }
    if (path.contains('/alerts')) {
      return _json(path, {'content': []});
    }

    // BI Dashboard
    if (path.contains('/admin/stats/overview')) {
      return _json(path, {
        'totalMembers': 100, 'activeMembers': 80, 'growthRate': 2.5, 'attendanceRate': 75.0,
        'weeklyTrend': [], 'departmentPerformance': [], 'newConverts': 5,
        'activeDisciples': 20, 'reportsSubmitted': 50, 'reportsPending': 5,
      });
    }

    // Benchmark
    if (path.contains('/benchmark/trends')) {
      return _json(path, {'attendanceTrend': [], 'growthTrend': []});
    }
    if (path.contains('/benchmark')) {
      return _json(path, {
        'currentChurch': {'totalMembers': 100, 'attendanceRate': 75.0, 'growthRate': 2.5},
        'averagePeers': {'totalMembers': 80, 'attendanceRate': 68.0, 'growthRate': 1.8},
        'percentile': {'attendanceRate': 60.0, 'growthRate': 70.0, 'reportsSubmitted': 55.0, 'volunteerRate': 50.0},
      });
    }

    // Sermons
    if (path.contains('/sermons')) {
      return _json(path, {'content': []});
    }

    // Users/me
    if (path.contains('/users/me')) {
      return _json(path, {'firstName': 'Test', 'lastName': 'User', 'email': 'test@test.com'});
    }

    // Events
    if (path.contains('/events')) {
      return _json(path, {'content': []});
    }

    // Souls
    if (path.contains('/souls')) {
      return _json(path, {'content': []});
    }

    // Geofencing
    if (path.contains('/geofencing/config')) {
      return _json(path, {'enabled': true, 'latitude': 48.8566, 'longitude': 2.3522, 'radiusMeters': 200, 'churchName': 'Église'});
    }

    // Default
    return _json(path, {});
  }

  @override
  Future<Response> post(String path, {dynamic data}) async {
    postPaths.add(path);
    return _json(path, {'status': 'ok'});
  }

  @override
  Future<Response> put(String path, {dynamic data}) async {
    return _json(path, {'status': 'ok'});
  }

  @override
  Future<Response> delete(String path) async {
    return _json(path, {'status': 'ok'});
  }

  Response _json(String path, dynamic data) {
    return Response(
      requestOptions: RequestOptions(path: path),
      data: data,
      statusCode: 200,
    );
  }
}
