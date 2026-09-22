import 'package:dio/dio.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/features/health/models/health_model.dart';

part 'health_service.g.dart';

@riverpod
HealthService healthService(HealthServiceRef ref) {
  final api = ref.watch(apiServiceProvider);
  return HealthService(api);
}

class HealthService {
  final ApiService _api;

  HealthService(this._api);

  // Patients
  Future<List<Patient>> getPatients({
    int page = 0,
    int size = 20,
    PatientStatus? status,
    String? search,
  }) async {
    try {
      final queryParams = <String, dynamic>{
        'page': page,
        'size': size,
        if (status != null) 'status': status.name,
        if (search != null && search.isNotEmpty) 'search': search,
      };
      final response = await _api.get('/health/patients', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => Patient.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des patients: $e');
    }
  }

  Future<Patient> getPatient(int id) async {
    try {
      final response = await _api.get('/health/patients/$id');
      return Patient.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du chargement du patient: $e');
    }
  }

  Future<Patient> createPatient(Patient patient) async {
    try {
      final response = await _api.post('/health/patients', data: patient.toJson());
      return Patient.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création: $e');
    }
  }

  Future<Patient> updatePatient(int id, Patient patient) async {
    try {
      final response = await _api.put('/health/patients/$id', data: patient.toJson());
      return Patient.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la mise à jour: $e');
    }
  }

  // Consultations
  Future<List<Consultation>> getConsultations({
    int page = 0,
    int size = 20,
    int? patientId,
    int? doctorId,
    ConsultationStatus? status,
    DateTime? fromDate,
    DateTime? toDate,
  }) async {
    try {
      final queryParams = <String, dynamic>{
        'page': page,
        'size': size,
        if (patientId != null) 'patientId': patientId,
        if (doctorId != null) 'doctorId': doctorId,
        if (status != null) 'status': status.name,
        if (fromDate != null) 'fromDate': fromDate.toIso8601String(),
        if (toDate != null) 'toDate': toDate.toIso8601String(),
      };
      final response = await _api.get('/health/consultations', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => Consultation.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des consultations: $e');
    }
  }

  Future<Consultation> getConsultation(int id) async {
    try {
      final response = await _api.get('/health/consultations/$id');
      return Consultation.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du chargement de la consultation: $e');
    }
  }

  Future<Consultation> createConsultation(Consultation consultation) async {
    try {
      final response = await _api.post('/health/consultations', data: consultation.toJson());
      return Consultation.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création: $e');
    }
  }

  Future<Consultation> updateConsultation(int id, Consultation consultation) async {
    try {
      final response = await _api.put('/health/consultations/$id', data: consultation.toJson());
      return Consultation.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la mise à jour: $e');
    }
  }

  // Prescriptions
  Future<List<Prescription>> getPrescriptions(int consultationId) async {
    try {
      final response = await _api.get('/health/consultations/$consultationId/prescriptions');
      final data = response.data as List;
      return data.map((json) => Prescription.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des prescriptions: $e');
    }
  }

  Future<Prescription> createPrescription(Prescription prescription) async {
    try {
      final response = await _api.post('/health/prescriptions', data: prescription.toJson());
      return Prescription.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création: $e');
    }
  }

  // Medications
  Future<List<Medication>> getMedications({bool? requiresPrescription, bool? isActive}) async {
    try {
      final queryParams = <String, dynamic>{};
      if (requiresPrescription != null) queryParams['requiresPrescription'] = requiresPrescription.toString();
      if (isActive != null) queryParams['isActive'] = isActive.toString();
      final response = await _api.get('/health/medications', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => Medication.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des médicaments: $e');
    }
  }

  Future<Medication> getMedication(int id) async {
    try {
      final response = await _api.get('/health/medications/$id');
      return Medication.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du chargement du médicament: $e');
    }
  }

  // Pharmacy Stock
  Future<List<PharmacyStock>> getPharmacyStock({bool? lowStockOnly, bool? expiredOnly}) async {
    try {
      final queryParams = <String, dynamic>{};
      if (lowStockOnly == true) queryParams['lowStock'] = 'true';
      if (expiredOnly == true) queryParams['expired'] = 'true';
      final response = await _api.get('/health/pharmacy/stock', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => PharmacyStock.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement du stock: $e');
    }
  }

  Future<PharmacyStock> updatePharmacyStock(int id, PharmacyStock stock) async {
    try {
      final response = await _api.put('/health/pharmacy/stock/$id', data: stock.toJson());
      return PharmacyStock.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la mise à jour: $e');
    }
  }

  // Health Campaigns
  Future<List<HealthCampaign>> getHealthCampaigns({CampaignStatus? status}) async {
    try {
      final queryParams = <String, dynamic>{};
      if (status != null) queryParams['status'] = status.name;
      final response = await _api.get('/health/campaigns', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => HealthCampaign.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des campagnes: $e');
    }
  }

  Future<HealthCampaign> getHealthCampaign(int id) async {
    try {
      final response = await _api.get('/health/campaigns/$id');
      return HealthCampaign.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du chargement de la campagne: $e');
    }
  }

  Future<HealthCampaign> createHealthCampaign(HealthCampaign campaign) async {
    try {
      final response = await _api.post('/health/campaigns', data: campaign.toJson());
      return HealthCampaign.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création: $e');
    }
  }

  Future<List<CampaignParticipant>> getCampaignParticipants(int campaignId) async {
    try {
      final response = await _api.get('/health/campaigns/$id/participants');
      final data = response.data as List;
      return data.map((json) => CampaignParticipant.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des participants: $e');
    }
  }

  Future<CampaignParticipant> registerForCampaign(int campaignId, int patientId) async {
    try {
      final response = await _api.post('/health/campaigns/$campaignId/register', data: {'patientId': patientId});
      return CampaignParticipant.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de l\'inscription: $e');
    }
  }

  // Medical Kits
  Future<List<MedicalKit>> getMedicalKits({KitStatus? status}) async {
    try {
      final queryParams = <String, dynamic>{};
      if (status != null) queryParams['status'] = status.name;
      final response = await _api.get('/health/kits', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => MedicalKit.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des kits: $e');
    }
  }

  Future<MedicalKit> getMedicalKit(int id) async {
    try {
      final response = await _api.get('/health/kits/$id');
      return MedicalKit.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du chargement du kit: $e');
    }
  }

  // Staff Duties
  Future<List<StaffDuty>> getStaffDuties({int? staffId, DateTime? fromDate, DateTime? toDate, DutyStatus? status}) async {
    try {
      final queryParams = <String, dynamic>{};
      if (staffId != null) queryParams['staffId'] = staffId;
      if (fromDate != null) queryParams['fromDate'] = fromDate.toIso8601String();
      if (toDate != null) queryParams['toDate'] = toDate.toIso8601String();
      if (status != null) queryParams['status'] = status.name;
      final response = await _api.get('/health/duties', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => StaffDuty.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des gardes: $e');
    }
  }

  // Reports
  Future<Map<String, dynamic>> getHealthStatistics({DateTime? fromDate, DateTime? toDate}) async {
    try {
      final queryParams = <String, dynamic>{};
      if (fromDate != null) queryParams['fromDate'] = fromDate.toIso8601String();
      if (toDate != null) queryParams['toDate'] = toDate.toIso8601String();
      final response = await _api.get('/health/reports/statistics', queryParameters: queryParams);
      return response.data as Map<String, dynamic>;
    } catch (e) {
      throw Exception('Erreur lors du chargement des statistiques: $e');
    }
  }

  Future<List<Patient>> getPatientsByCondition(String condition) async {
    try {
      final response = await _api.get('/health/patients/by-condition', queryParameters: {'condition': condition});
      final data = response.data as List;
      return data.map((json) => Patient.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement: $e');
    }
  }
}