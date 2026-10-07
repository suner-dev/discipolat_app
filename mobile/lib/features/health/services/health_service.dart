import 'package:riverpod_annotation/riverpod_annotation.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/data/services/providers.dart';
import 'package:discipolat_mobile/features/health/models/health_model.dart';

part 'health_service.g.dart';

@riverpod
HealthService healthService(HealthServiceRef ref) {
  final api = ref.watch(apiServiceProvider);
  return HealthService(api);
}

/// Service santé mobile — contrat exact V240 du `HealthController`
/// (`/api/v1/health`).
///
/// Règles vérifiées dans le code serveur (pas supposées) :
/// 1. Les endpoints paginés renvoient `PageResponse` = `{content, page, size,
///    totalElements, totalPages}` — jamais une liste brute ; le parsing passe
///    obligatoirement par [_contentOf].
/// 2. Les endpoints à relation renvoient des vues Map aplaties (patientName,
///    itemName, responsibleName…) ; les ids sont des UUID **String**.
/// 3. Les écritures envoient les associations imbriquées (`person: {id: …}`)
///    — les corps sont construits par les `createBody`/`updateBody` du
///    modèle, le service ne fait que les transmettre.
/// 4. `POST /campaigns/{id}/register` attend `{"userId": "<uuid>"}`
///    (`Map<String, UUID>` côté serveur), et `POST /campaigns` force le
///    responsable à l'acteur courant (ne pas l'envoyer).
/// 5. Les erreurs Dio remontent typées (jamais enveloppées dans une
///    `Exception` generic qui écrase le statut HTTP) — l'appelant (UI)
///    décide du message affiché.
class HealthService {
  final ApiService _api;

  HealthService(this._api);

  static const String _base = '/health';

  // --------------------------------------------------------------------------
  // Parsing PageResponse — point unique, tolérant aux éléments non-Map.
  // --------------------------------------------------------------------------

  static List<Map<String, dynamic>> _contentOf(dynamic data) {
    if (data is Map && data['content'] is List) {
      return (data['content'] as List)
          .whereType<Map<dynamic, dynamic>>()
          .map((e) => e.cast<String, dynamic>())
          .toList();
    }
    // Liste brute acceptée pour les rares endpoints non paginés typés List.
    if (data is List) {
      return data
          .whereType<Map<dynamic, dynamic>>()
          .map((e) => e.cast<String, dynamic>())
          .toList();
    }
    return const [];
  }

  static Map<String, dynamic> _mapOf(dynamic data) =>
      data is Map ? data.cast<String, dynamic>() : const {};

  // --------------------------------------------------------------------------
  // Patients (vues aplaties {personId, personName, familyId, familyName})
  // --------------------------------------------------------------------------

  Future<List<PatientRecord>> getPatients({
    int page = 0,
    int size = 20,
    String? search,
  }) async {
    final response = await _api.get('$_base/patients', queryParameters: {
      'page': page,
      'size': size,
      if (search != null && search.isNotEmpty) 'search': search,
    });
    return _contentOf(response.data)
        .map(PatientRecord.fromJson)
        .toList(growable: false);
  }

  Future<PatientRecord> getPatient(String id) async {
    final response = await _api.get('$_base/patients/$id');
    return PatientRecord.fromJson(_mapOf(response.data));
  }

  /// Corps construit par [PatientRecord.createBody] (association `person`
  /// imbriquée exigée par la désérialisation serveur).
  Future<PatientRecord> createPatient(Map<String, dynamic> body) async {
    final response = await _api.post('$_base/patients', data: body);
    return PatientRecord.fromJson(_mapOf(response.data));
  }

  /// Corps construit par [PatientRecord.updateBody] — seuls les champs
  /// présents sont appliqués par le serveur.
  Future<PatientRecord> updatePatient(String id, Map<String, dynamic> body) async {
    final response = await _api.put('$_base/patients/$id', data: body);
    return PatientRecord.fromJson(_mapOf(response.data));
  }

  Future<List<PatientRecord>> getPatientsByCondition(String condition) async {
    final response = await _api.get('$_base/patients/by-condition',
        queryParameters: {'condition': condition});
    return _contentOf(response.data)
        .map(PatientRecord.fromJson)
        .toList(growable: false);
  }

  // --------------------------------------------------------------------------
  // Consultations (vues aplaties {patientName, practitionerName})
  // --------------------------------------------------------------------------

  /// `patientId` = UUID String ; `from`/`to` = `yyyy-MM-dd` (LocalDate).
  Future<List<MedicalConsultation>> getConsultations({
    String? patientId,
    String? from,
    String? to,
    int page = 0,
    int size = 20,
  }) async {
    final response = await _api.get('$_base/consultations', queryParameters: {
      'page': page,
      'size': size,
      if (patientId != null) 'patientId': patientId,
      if (from != null) 'from': from,
      if (to != null) 'to': to,
    });
    return _contentOf(response.data)
        .map(MedicalConsultation.fromJson)
        .toList(growable: false);
  }

  Future<MedicalConsultation> getConsultation(String id) async {
    final response = await _api.get('$_base/consultations/$id');
    return MedicalConsultation.fromJson(_mapOf(response.data));
  }

  Future<MedicalConsultation> createConsultation(Map<String, dynamic> body) async {
    final response = await _api.post('$_base/consultations', data: body);
    return MedicalConsultation.fromJson(_mapOf(response.data));
  }

  Future<MedicalConsultation> updateConsultation(String id, Map<String, dynamic> body) async {
    final response = await _api.put('$_base/consultations/$id', data: body);
    return MedicalConsultation.fromJson(_mapOf(response.data));
  }

  // --------------------------------------------------------------------------
  // Prescriptions (vue aplatie {patientName}, fallback consultation→patient)
  // --------------------------------------------------------------------------

  Future<List<Prescription>> getPrescriptions({
    String? patientId,
    String? consultationId,
    int page = 0,
    int size = 20,
  }) async {
    final response = await _api.get('$_base/prescriptions', queryParameters: {
      'page': page,
      'size': size,
      if (patientId != null) 'patientId': patientId,
      if (consultationId != null) 'consultationId': consultationId,
    });
    return _contentOf(response.data)
        .map(Prescription.fromJson)
        .toList(growable: false);
  }

  Future<Prescription> createPrescription(Map<String, dynamic> body) async {
    final response = await _api.post('$_base/prescriptions', data: body);
    return Prescription.fromJson(_mapOf(response.data));
  }

  Future<List<Prescription>> getConsultationPrescriptions(String consultationId) async {
    final response =
        await _api.get('$_base/consultations/$consultationId/prescriptions');
    return _contentOf(response.data)
        .map(Prescription.fromJson)
        .toList(growable: false);
  }

  // --------------------------------------------------------------------------
  // Pharmacie — articles bruts (pas de LAZY), stocks en vues aplaties
  // --------------------------------------------------------------------------

  Future<List<PharmacyItem>> getPharmacyItems({
    String? search,
    String? categorie,
    int page = 0,
    int size = 50,
  }) async {
    final response = await _api.get('$_base/pharmacy/items', queryParameters: {
      'page': page,
      'size': size,
      if (search != null && search.isNotEmpty) 'search': search,
      if (categorie != null && categorie.isNotEmpty) 'categorie': categorie,
    });
    return _contentOf(response.data)
        .map(PharmacyItem.fromJson)
        .toList(growable: false);
  }

  Future<PharmacyItem> createPharmacyItem(Map<String, dynamic> body) async {
    final response = await _api.post('$_base/pharmacy/items', data: body);
    return PharmacyItem.fromJson(_mapOf(response.data));
  }

  Future<PharmacyItem> updatePharmacyItem(String id, Map<String, dynamic> body) async {
    final response = await _api.put('$_base/pharmacy/items/$id', data: body);
    return PharmacyItem.fromJson(_mapOf(response.data));
  }

  /// `status` : valeur wire exacte de [StockStatus] (accents inclus).
  Future<List<PharmacyStock>> getPharmacyStock({
    String? itemId,
    StockStatus? status,
    int page = 0,
    int size = 50,
  }) async {
    final response = await _api.get('$_base/pharmacy/stock', queryParameters: {
      'page': page,
      'size': size,
      if (itemId != null) 'itemId': itemId,
      if (status != null) 'status': status.wire,
    });
    return _contentOf(response.data)
        .map(PharmacyStock.fromJson)
        .toList(growable: false);
  }

  Future<PharmacyStock> updatePharmacyStock(String id, Map<String, dynamic> body) async {
    final response = await _api.put('$_base/pharmacy/stock/$id', data: body);
    return PharmacyStock.fromJson(_mapOf(response.data));
  }

  Future<List<PharmacyStock>> getLowStockAlerts() async {
    final response = await _api.get('$_base/pharmacy/stock/alerts/low');
    return _contentOf(response.data)
        .map(PharmacyStock.fromJson)
        .toList(growable: false);
  }

  Future<List<PharmacyStock>> getExpiringSoonAlerts({int days = 30}) async {
    final response = await _api.get('$_base/pharmacy/stock/alerts/expiring',
        queryParameters: {'days': days});
    return _contentOf(response.data)
        .map(PharmacyStock.fromJson)
        .toList(growable: false);
  }

  /// Mouvement de stock — corps avec association `stock: {id}` imbriquée ;
  /// réponse = vue aplatie (le mobile n'a pas de modèle fort pour les
  /// mouvements, seule la création est exposée à l'UI).
  Future<Map<String, dynamic>> createMovement(Map<String, dynamic> body) async {
    final response = await _api.post('$_base/pharmacy/movements', data: body);
    return _mapOf(response.data);
  }

  // --------------------------------------------------------------------------
  // Campagnes (vue aplatie {responsibleName, participantsCount})
  // --------------------------------------------------------------------------

  Future<List<HealthCampaign>> getCampaigns({
    CampaignStatus? status,
    int page = 0,
    int size = 20,
  }) async {
    final response = await _api.get('$_base/campaigns', queryParameters: {
      'page': page,
      'size': size,
      if (status != null) 'status': status.wire,
    });
    return _contentOf(response.data)
        .map(HealthCampaign.fromJson)
        .toList(growable: false);
  }

  Future<HealthCampaign> getCampaign(String id) async {
    final response = await _api.get('$_base/campaigns/$id');
    return HealthCampaign.fromJson(_mapOf(response.data));
  }

  /// Le serveur force `responsible` à l'acteur courant : ne pas l'envoyer.
  Future<HealthCampaign> createCampaign(Map<String, dynamic> body) async {
    final response = await _api.post('$_base/campaigns', data: body);
    return HealthCampaign.fromJson(_mapOf(response.data));
  }

  Future<List<CampaignParticipant>> getCampaignParticipants(String campaignId) async {
    final response =
        await _api.get('$_base/campaigns/$campaignId/participants');
    return _contentOf(response.data)
        .map(CampaignParticipant.fromJson)
        .toList(growable: false);
  }

  /// `POST /campaigns/{id}/register` — le serveur attend
  /// `Map<String, UUID>` = `{"userId": "<uuid>"}` (PAS patientId).
  Future<CampaignParticipant> registerForCampaign(
      String campaignId, String userId) async {
    final response = await _api
        .post('$_base/campaigns/$campaignId/register', data: {'userId': userId});
    return CampaignParticipant.fromJson(_mapOf(response.data));
  }

  // --------------------------------------------------------------------------
  // Médicaments / kits / gardes — entités plates V235 (liste brute)
  // --------------------------------------------------------------------------

  Future<List<HealthMedication>> getMedications() async {
    final response = await _api.get('$_base/medications');
    return _contentOf(response.data)
        .map(HealthMedication.fromJson)
        .toList(growable: false);
  }

  Future<HealthMedication> getMedication(String id) async {
    final response = await _api.get('$_base/medications/$id');
    return HealthMedication.fromJson(_mapOf(response.data));
  }

  Future<HealthMedication> createMedication(Map<String, dynamic> body) async {
    final response = await _api.post('$_base/medications', data: body);
    return HealthMedication.fromJson(_mapOf(response.data));
  }

  Future<List<HealthKit>> getKits() async {
    final response = await _api.get('$_base/kits');
    return _contentOf(response.data)
        .map(HealthKit.fromJson)
        .toList(growable: false);
  }

  Future<HealthKit> getKit(String id) async {
    final response = await _api.get('$_base/kits/$id');
    return HealthKit.fromJson(_mapOf(response.data));
  }

  Future<List<HealthDuty>> getDuties() async {
    final response = await _api.get('$_base/duties');
    return _contentOf(response.data)
        .map(HealthDuty.fromJson)
        .toList(growable: false);
  }

  // --------------------------------------------------------------------------
  // Dashboard / statistiques
  // --------------------------------------------------------------------------

  Future<Map<String, dynamic>> getDashboardStats() async {
    final response = await _api.get('$_base/dashboard/stats');
    return _mapOf(response.data);
  }

  Future<Map<String, dynamic>> getHealthReportsStatistics() async {
    final response = await _api.get('$_base/reports/statistics');
    return _mapOf(response.data);
  }
}
