import 'dart:typed_data';
import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'api_config.dart';
import '../models/transfer_models.dart';
import '../../tenant_config.dart';

class ApiService {
  late final Dio _dio;
  final FlutterSecureStorage _secureStorage = const FlutterSecureStorage();

  /// Expose le Dio instance pour les services WebSocket.
  Dio get dio => _dio;

  static const String _accessTokenKey = 'access_token';
  static const String _refreshTokenKey = 'refresh_token';

  // PORT Develop1 (§G5.6) — callbacks globaux : session expirée (401
  // irrattrapable après refresh) → reconnexion ; 403 → état « accès refusé »
  // de l'écran courant. Posés dans `main.dart` / écrans.
  static void Function()? onSessionExpired;
  static void Function()? onAccessDenied;

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
            try {
              final retryResponse = await _dio.fetch(error.requestOptions);
              handler.resolve(retryResponse);
              return;
            } on DioException catch (retryError) {
              // Le rejet persiste après refresh : on laisse remonter l'erreur
              // originale plutôt que de perdre le contexte du request.
              handler.next(retryError);
              return;
            }
          }
          // Session irrattrapable → écran de reconnexion (G5.6)
          onSessionExpired?.call();
        } else if (error.response?.statusCode == 403) {
          onAccessDenied?.call();
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

  Future<Response> get(String path,
          {Map<String, dynamic>? params,
          Map<String, dynamic>? queryParameters}) =>
      _dio.get(path, queryParameters: params ?? queryParameters);

  Future<Response> getBytes(String path, {Map<String, dynamic>? params}) =>
      _dio.get(path,
          queryParameters: params,
          options: Options(responseType: ResponseType.bytes));

  /// POST qui retourne des données binaires (ex: audio TTS). Utilisé pour
  /// les endpoints qui répondent en octets (audio/mpeg, pdf, etc.).
  Future<Response> postBytes(String path,
          {dynamic data, Map<String, dynamic>? params}) =>
      _dio.post(path,
          data: data,
          queryParameters: params,
          options: Options(responseType: ResponseType.bytes));

  Future<Response> post(String path, {dynamic data}) =>
      _dio.post(path, data: data);

  Future<Response> put(String path, {dynamic data}) =>
      _dio.put(path, data: data);

  Future<Response> patch(String path, {dynamic data}) =>
      _dio.patch(path, data: data);

  Future<Response> delete(String path,
          {Map<String, dynamic>? params,
          Map<String, dynamic>? queryParameters}) =>
      _dio.delete(path, queryParameters: params ?? queryParameters);

  // ==========================================================================
  // SPEC_ORGANISATION_DENOMINATION_V2 §7.3 (T-M1) — transfert de membre.
  //
  // « Un membre qui change d'église ne se réinscrit pas » : il suffit du code
  // de la nouvelle église. Ces helpers encapsulent le contrat pour que les
  // écrans n'aient pas à connaître la forme exacte de la réponse.
  // ==========================================================================

  /// Analyse la situation AVANT confirmation : même réseau ou non
  /// (l'historique pastoral est-il préservé ?), déjà membre ou non.
  Future<TransferPreview> previewTransfer(String code) async {
    final res = await get('/tenant/transfer/preview',
        params: {'code': code.trim()});
    final body = (res.data as Map<String, dynamic>?) ?? const <String, dynamic>{};
    return TransferPreview(
      sameNetwork: body['sameNetwork'] == true,
      alreadyMember: body['alreadyMember'] == true,
      activeInOther: body['activeInOther'] == true,
      fromChurch: body['fromChurch'] as String? ?? '',
      toChurch: body['toChurch'] as String? ?? '',
      toKind: body['toKind'] as String? ?? '',
      willTransfer: body['willTransfer'] == true,
    );
  }

  /// Effectue le transfert — ou l'adhésion si les réseaux diffèrent (§4.4).
  /// Idempotent : rejouer le même code répond ALREADY_MEMBER.
  Future<TransferOutcome> transfer(String code, {String? reason}) async {
    final res = await post('/tenant/transfer', data: {
      'code': code.trim(),
      if (reason != null && reason.trim().isNotEmpty) 'reason': reason.trim(),
    });
    final body = (res.data as Map<String, dynamic>?) ?? const <String, dynamic>{};
    final outcome = TransferOutcome(
      status: body['status'] as String? ?? 'ALREADY_MEMBER',
      toChurch: body['toChurch'] as String? ?? '',
      accessToken: body['accessToken'] as String?,
      refreshToken: body['refreshToken'] as String?,
    );
    // T-B0bis : le backend réémet les jetons sur l'organisation d'accueil. On
    // les persiste ICI, sinon le claim tenantId du jeton courant continuerait de
    // désigner l'ancienne église — le membre y resterait malgré le message
    // « vous avez été transféré ».
    if (outcome.accessToken != null && outcome.accessToken!.isNotEmpty) {
      await saveTokens({
        'accessToken': outcome.accessToken,
        'refreshToken': outcome.refreshToken ?? '',
      });
    }
    return outcome;
  }

  /// §G5.7 — vrai défaut réseau/serveur injoignable (pas une erreur métier).
  /// Utilisé par les écrans de terrain (QR checkin, inventaire) pour décider
  /// d'enfileter l'opération dans la file hors-ligne idempotente.
  static bool isOfflineError(Object e) =>
      e is DioException &&
      e.response == null &&
      (e.type == DioExceptionType.connectionError ||
          e.type == DioExceptionType.connectionTimeout ||
          e.type == DioExceptionType.sendTimeout ||
          e.type == DioExceptionType.receiveTimeout);

  /// Envoie un fichier (multipart/form-data) sur [path].
  /// [fieldName] est le nom du champ multipart attendu par le backend.
  /// [mimeType] optionnel : type réel du fichier (ex. image/jpeg pour les
  /// photos de terrain — scan QR, dommage d'actif). Défaut : audio/wav.
  Future<Response> postMultipart(
    String path, {
    required String fieldName,
    required Uint8List fileBytes,
    required String filename,
    Map<String, dynamic>? data,
    DioMediaType? mimeType,
  }) {
    return postFile(
      path,
      fieldName: fieldName,
      fileBytes: fileBytes,
      filename: filename,
      contentType: mimeType ?? DioMediaType('audio', 'wav'),
      data: data,
    );
  }

  Future<Response> postImage(
    String path, {
    required String fieldName,
    required Uint8List fileBytes,
    required String filename,
    Map<String, dynamic>? data,
  }) {
    final extension =
        filename.contains('.') ? filename.split('.').last.toLowerCase() : 'jpg';
    final contentType =
        DioMediaType('image', extension == 'jpg' ? 'jpeg' : extension);
    return postFile(
      path,
      fieldName: fieldName,
      fileBytes: fileBytes,
      filename: filename,
      contentType: contentType,
      data: data,
    );
  }

  Future<Response> postFile(
    String path, {
    required String fieldName,
    required Uint8List fileBytes,
    required String filename,
    required DioMediaType contentType,
    Map<String, dynamic>? data,
  }) {
    final form = FormData.fromMap({
      if (data != null) ...data,
      fieldName: MultipartFile.fromBytes(
        fileBytes,
        filename: filename,
        contentType: contentType,
      ),
    });
    return _dio.post(path, data: form);
  }

  Future<void> saveTokens(Map<String, dynamic> data) async {
    if (data.containsKey('accessToken')) {
      await _secureStorage.write(
          key: _accessTokenKey, value: data['accessToken'] as String);
    }
    if (data.containsKey('refreshToken')) {
      await _secureStorage.write(
          key: _refreshTokenKey, value: data['refreshToken'] as String);
    }
  }

  Future<void> deleteRefreshToken() async {
    await _secureStorage.delete(key: _refreshTokenKey);
  }

  Future<void> deleteAccessToken() async {
    await _secureStorage.delete(key: _accessTokenKey);
  }

  Future<void> clearTokens() async {
    await _secureStorage.deleteAll();
  }

  Future<String?> getAccessToken() async {
    return await _secureStorage.read(key: _accessTokenKey);
  }

  Future<String?> getRefreshToken() async {
    return await _secureStorage.read(key: _refreshTokenKey);
  }
}
