import 'dart:typed_data';
import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'api_config.dart';
import '../../tenant_config.dart';

class ApiService {
  late final Dio _dio;
  final FlutterSecureStorage _secureStorage = const FlutterSecureStorage();

  /// Expose le Dio instance pour les services WebSocket.
  Dio get dio => _dio;

  static const String _accessTokenKey = 'access_token';
  static const String _refreshTokenKey = 'refresh_token';

  /// Crée une instance d'ApiService.
  ///
  /// [baseUrl] peut être omis pour utiliser la configuration automatique
  /// (production Render par défaut, ou `--dart-define=API_URL=...`).
  ApiService({String? baseUrl}) {
    final resolvedUrl = baseUrl ?? ApiConfig.baseUrl;
    _dio = Dio(BaseOptions(
      baseUrl: resolvedUrl,
      connectTimeout: const Duration(seconds: 15),
      receiveTimeout: const Duration(seconds: 15),
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
    ));

    // Add orgId filtering header for multi-tenant isolation
    _dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) async {
        // Add authentication token
        final token = await _secureStorage.read(key: _accessTokenKey);
        if (token != null) {
          options.headers['Authorization'] = 'Bearer $token';
        }

        // Add organisation/tenant filter for multi-tenant isolation
        final orgId = await TenantConfig.resolveOrgId();
        if (orgId != null && orgId.isNotEmpty) {
          options.headers['X-Org-Id'] = orgId;
          options.headers['X-Tenant'] = 'active';
        }

        handler.next(options);
      },
      onError: (error, handler) async {
        if (error.response?.statusCode == 401) {
          final refreshed = await _refreshToken();
          if (refreshed) {
            final retryResponse = await _dio.fetch(error.requestOptions);
            handler.resolve(retryResponse);
            return;
          }
        }
        handler.next(error);
      },
    ));
  }

  Future<bool> _refreshToken() async {
    try {
      final refreshToken = await _secureStorage.read(key: _refreshTokenKey);
      if (refreshToken == null) return false;

      final response = await Dio().post(
        '${_dio.options.baseUrl}/auth/refresh',
        data: {'refreshToken': refreshToken},
      );

      await _secureStorage.write(
        key: _accessTokenKey,
        value: response.data['accessToken'],
      );
      await _secureStorage.write(
        key: _refreshTokenKey,
        value: response.data['refreshToken'],
      );
      return true;
    } catch (_) {
      await _secureStorage.deleteAll();
      return false;
    }
  }

  Future<Response> get(String path, {Map<String, dynamic>? params}) =>
    _dio.get(path, queryParameters: params);

  Future<Response> getBytes(String path, {Map<String, dynamic>? params}) =>
    _dio.get(path, queryParameters: params, options: Options(responseType: ResponseType.bytes));

  Future<Response> post(String path, {dynamic data}) =>
    _dio.post(path, data: data);

  Future<Response> put(String path, {dynamic data}) =>
    _dio.put(path, data: data);

  Future<Response> patch(String path, {dynamic data}) =>
    _dio.patch(path, data: data);

  Future<Response> delete(String path) => _dio.delete(path);

  /// Envoie un fichier (multipart/form-data) sur [path].
  /// [fieldName] est le nom du champ multipart attendu par le backend.
  Future<Response> postMultipart(
    String path, {
    required String fieldName,
    required Uint8List fileBytes,
    required String filename,
    Map<String, dynamic>? data,
  }) async {
    final form = FormData.fromMap({
      if (data != null) ...data,
      fieldName: MultipartFile.fromBytes(
        fileBytes,
        filename: filename,
        contentType: DioMediaType('audio', 'wav'),
      ),
    });
    return _dio.post(path, data: form);
  }

  Future<void> saveTokens(Map<String, dynamic> data) async {
    if (data.containsKey('accessToken')) {
      await _secureStorage.write(key: _accessTokenKey, value: data['accessToken'] as String);
    }
    if (data.containsKey('refreshToken')) {
      await _secureStorage.write(key: _refreshTokenKey, value: data['refreshToken'] as String);
    }
  }

  Future<void> clearTokens() async {
    await _secureStorage.deleteAll();
  }

  Future<String?> getAccessToken() async {
    return await _secureStorage.read(key: _accessTokenKey);
  }
}
