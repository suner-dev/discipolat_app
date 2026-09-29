// Consommation mobile des endpoints RGPD livrés par l'Agent A :
//   POST /api/v1/compliance/consents        — enregistrer un consentement
//   GET  /api/v1/compliance/consents/mine   — mes consentements
//   GET  /api/v1/public/legal               — documents légaux publiés
//   GET  /api/v1/public/legal/{code}        — une version publiée
//
// Contrat figé (R2) : le backend n'accepte que 6 types de consentement
// (ALLOWED_TYPES dans ConsentController). On n'invente aucun type.

import 'api_service.dart';

/// Types acceptés par le backend. Toute autre valeur serait rejetée en 400.
enum ConsentType {
  cgu,
  privacy,
  consentArt9,
  whatsapp,
  marketing,
  photo;

  /// Identifiant exact attendu par le backend.
  String get wire => switch (this) {
        ConsentType.cgu => 'CGU',
        ConsentType.privacy => 'PRIVACY',
        ConsentType.consentArt9 => 'CONSENT_ART9',
        ConsentType.whatsapp => 'WHATSAPP',
        ConsentType.marketing => 'MARKETING',
        ConsentType.photo => 'PHOTO',
      };

  static ConsentType? tryParse(String raw) {
    final v = raw.trim().toUpperCase();
    for (final t in ConsentType.values) {
      if (t.wire == v) return t;
    }
    return null;
  }
}

class MyConsent {
  const MyConsent({required this.type, required this.granted, this.policyVersion, this.details, this.grantedAt});

  final ConsentType type;
  final bool granted;
  final String? policyVersion;
  final String? details;
  final DateTime? grantedAt;

  static MyConsent fromJson(Map<String, dynamic> json) {
    final rawType = (json['type'] ?? '').toString();
    final type = ConsentType.tryParse(rawType);
    if (type == null) {
      throw FormatException('Type de consentement inconnu : $rawType');
    }
    final at = json['grantedAt'] ?? json['dateConsent'];
    return MyConsent(
      type: type,
      granted: json['granted'] == true,
      policyVersion: json['policyVersion'] as String?,
      details: json['details'] as String?,
      grantedAt: at == null ? null : DateTime.tryParse(at.toString()),
    );
  }
}

class LegalDocument {
  const LegalDocument({required this.code, required this.title, this.version, this.content});

  final String code;
  final String title;
  final String? version;
  final String? content;

  static LegalDocument fromJson(Map<String, dynamic> json) {
    final code = (json['code'] ?? '').toString();
    if (code.isEmpty) throw const FormatException('Champ manquant : code');
    return LegalDocument(
      code: code,
      title: (json['title'] ?? code).toString(),
      version: (json['version'] ?? json['policyVersion'])?.toString(),
      content: (json['content'] ?? json['body'])?.toString(),
    );
  }
}

class ComplianceService {
  ComplianceService({ApiService? apiService}) : _api = apiService ?? ApiService();

  final ApiService _api;

  /// Enregistre un consentement. `granted: false` = retrait (RGPD art. 7.3).
  Future<void> recordConsent(
    ConsentType type, {
    required bool granted,
    String? policyVersion,
    String? details,
  }) async {
    await _api.post('/compliance/consents', data: <String, dynamic>{
      'type': type.wire,
      'granted': granted,
      if (policyVersion != null) 'policyVersion': policyVersion,
      if (details != null) 'details': details,
    });
  }

  Future<List<MyConsent>> myConsents() async {
    final res = await _api.get('/compliance/consents/mine');
    final data = res.data;
    if (data is! List) {
      throw FormatException('Réponse inattendue : ${data.runtimeType}');
    }
    return data
        .whereType<Map>()
        .map((e) => MyConsent.fromJson(e.cast<String, dynamic>()))
        .toList();
  }

  Future<List<LegalDocument>> legalDocuments() async {
    final res = await _api.get('/public/legal');
    final data = res.data;
    if (data is! List) {
      throw FormatException('Réponse inattendue : ${data.runtimeType}');
    }
    return data
        .whereType<Map>()
        .map((e) => LegalDocument.fromJson(e.cast<String, dynamic>()))
        .toList();
  }

  Future<LegalDocument> legalDocument(String code) async {
    final res = await _api.get('/public/legal/$code');
    final data = res.data;
    if (data is! Map) {
      throw FormatException('Réponse inattendue : ${data.runtimeType}');
    }
    return LegalDocument.fromJson(data.cast<String, dynamic>());
  }
}
