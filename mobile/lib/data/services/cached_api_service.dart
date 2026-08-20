import 'package:dio/dio.dart';
import '../utils/performance_utils.dart';
import 'api_service.dart';

/// Cached wrapper around ApiService.
/// Stores GET responses in memory for [cacheDuration] to avoid redundant calls.
/// POST/PUT/DELETE automatically invalidate related cache entries.
class CachedApiService {
  final ApiService _api;
  final ApiCache _cache;

  CachedApiService({Duration cacheDuration = const Duration(minutes: 3)})
      : _api = ApiService(),
        _cache = ApiCache(ttl: cacheDuration);

  /// GET with caching. Returns cached response if available and fresh.
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    final cacheKey = '$path${params != null ? '?${params.entries.map((e) => '${e.key}=${e.value}').join('&')}' : ''}';

    // Return cached if available
    final cached = _cache.get<Response>(cacheKey);
    if (cached != null) return cached;

    // Fetch from network
    final response = await _api.get(path, params: params);
    _cache.put(cacheKey, response);
    return response;
  }

  /// POST — invalidates cache entries matching the path prefix.
  Future<Response> post(String path, {dynamic data}) async {
    _cache.invalidatePrefix(path);
    return _api.post(path, data: data);
  }

  /// PUT — invalidates cache entries matching the path prefix.
  Future<Response> put(String path, {dynamic data}) async {
    _cache.invalidatePrefix(path);
    return _api.put(path, data: data);
  }

  /// DELETE — invalidates cache entries matching the path prefix.
  Future<Response> delete(String path) async {
    _cache.invalidatePrefix(path);
    return _api.delete(path);
  }

  /// Explicitly invalidate cache for a specific path.
  void invalidate(String path) => _cache.invalidatePrefix(path);

  /// Clear all cached responses.
  void clearCache() => _cache.invalidateAll();

  /// Get the underlying Dio instance (for WebSocket setup etc.)
  Dio get dio => _api.dio;
}
