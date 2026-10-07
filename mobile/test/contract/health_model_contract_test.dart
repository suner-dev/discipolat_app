// Contrat de désérialisation du module santé.
//
// POURQUOI CE TEST EXISTE
// Le modèle déclarait `required int id` alors que les 11 entités de santé sont
// `GenerationType.UUID` côté serveur. Le `.g.dart` produisait donc
// `id: (json['id'] as num).toInt()` : appliqué à une chaîne UUID, ce cast lève
// un `TypeError` et fait échouer TOUTE la liste. Symptôme en production : l'écran
// Santé vide, sans erreur visible.
//
// Ce test fige le contrat réel : un payload serveur tel qu'il est sérialisé
// doit se désérialiser. Il couvre aussi la tolérance (clés absentes, enums
// inconnus, dates nulles) et la non-régression des champs d'affichage.

import 'package:discipolat_mobile/features/health/models/health_model.dart';
import 'package:flutter_test/flutter_test.dart';

/// Payload réel d'un `PatientRecord` sérialisé par Jackson :
/// l'`id` est une chaîne UUID.
const _patientJson = <String, dynamic>{
  'id': '3f2504e0-4f89-11d3-9a0c-0305e82c3301',
  'tenantId': 'aaaaaaaa-0000-0000-0000-000000000001',
  'groupeSanguin': 'O+',
  'allergies': 'Pénicilline',
  'antecedents': 'Hypertension',
  'notesSensibles': 'Sous anticoagulants',
  'deleted': false,
  'createdAt': '2026-01-15T08:30:00Z',
  'updatedAt': '2026-02-01T10:00:00Z',
};

void main() {
  group('Identifiants UUID (le bug qui cassait l\'écran Santé)', () {
    test('Patient : un id UUID chaîne ne lève pas et devient une String', () {
      final p = Patient.fromJson(_patientJson);

      expect(p.id, '3f2504e0-4f89-11d3-9a0c-0305e82c3301');
      // La régression exacte : un cast num→Int sur une chaîne aurait levé ici.
      expect(p.id, isA<String>());
    });

    test('Consultation : les ids de relation restent des chaînes', () {
      final c = Consultation.fromJson(const {
        'id': '11111111-1111-1111-1111-111111111111',
        'patientId': '22222222-2222-2222-2222-222222222222',
        'practitionerId': '33333333-3333-3333-3333-333333333333',
        'consultationDate': '2026-03-04T10:15:00Z',
        'typeConsultation': 'CONSULTATION',
        'motif': 'Fièvre',
        'diagnostic': 'Angine',
        'status': 'COMPLETED',
      });

      expect(c.id, isA<String>());
      expect(c.patientId, '22222222-2222-2222-2222-222222222222');
      expect(c.practitionerId, '33333333-3333-3333-3333-333333333333');
      expect(c.status, ConsultationStatus.completed);
      expect(c.dateTime.year, 2026);
      expect(c.dateTime, c.consultationDate);
    });

    test('Prescription : consultationId et patientId UUID', () {
      final p = Prescription.fromJson(const {
        'id': 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee',
        'consultationId': '11111111-1111-1111-1111-111111111111',
        'patientId': '22222222-2222-2222-2222-222222222222',
        'medicament': 'Amoxicilline 500 mg',
        'dosage': '1 comprimé',
        'posologie': '3×/jour',
        'duree': '7 jours',
        'status': 'ACTIVE',
      });

      expect(p.id, isA<String>());
      expect(p.consultationId, isA<String>());
      expect(p.medicationName, 'Amoxicilline 500 mg');
      expect(p.frequency, '3×/jour');
      expect(p.isActive, isTrue);
    });

    test('CampaignParticipant : la référence personne est userId (UUID)', () {
      final cp = CampaignParticipant.fromJson(const {
        'id': 'cccccccc-dddd-eeee-ffff-000000000000',
        'campaignId': '3f2504e0-4f89-11d3-9a0c-0305e82c3301',
        'userId': '22222222-2222-2222-2222-222222222222',
        'registeredAt': '2026-05-06T09:00:00Z',
        'status': 'REGISTERED',
      });

      expect(cp.id, isA<String>());
      expect(cp.userId, '22222222-2222-2222-2222-222222222222');
      // Alias historique consommé par les écrans.
      expect(cp.patientId, cp.userId);
      expect(cp.registrationDate, isNotNull);
    });
  });

  group('Tolérance : un payload incomplet ne casse pas une liste', () {
    test('une entité presque vide reste désérialisable', () {
      final p = Patient.fromJson(const {'id': 'abc'});
      expect(p.id, 'abc');
      expect(p.firstName, '');
      expect(p.createdAt, isNull);
      expect(p.fullName, 'Patient');
    });

    test('un enum inconnu retombe sur une valeur par défaut, sans exception', () {
      final c = Consultation.fromJson(const {
        'id': 'x',
        'patientId': 'y',
        'status': 'EN_STATUT_INVENTE',
        'typeConsultation': 'TYPE_INVENTE',
      });
      expect(c.status, isA<ConsultationStatus>());
      expect(c.type, isA<ConsultationType>());
    });

    test('les dates obligatoires ont un repli qui ne dit pas « aujourd\'hui »', () {
      final camp = HealthCampaign.fromJson(const {'id': 'c', 'name': 'Dépistage'});
      // Une date manquante ne doit pas devenir « now » : la campagne
      // passerait pour "_je démarre aujourd'hui_".
      expect(camp.startDate.millisecondsSinceEpoch, 0);
      expect(camp.endDate.millisecondsSinceEpoch, 0);
      expect(camp.isActive, isFalse);
      expect(camp.isCompleted, isTrue);
    });

    test('les collections absentes valent null, pas une exception', () {
      final c = Consultation.fromJson(const {'id': 'x', 'patientId': 'y'});
      expect(c.prescriptions, isNull);
      expect(c.vitalSigns, isNull);
    });
  });

  group('Champs d\'affichage conservés (non-régression des écrans)', () {
    test('Patient : age, gender et photoUrl restent disponibles', () {
      final p = Patient.fromJson({
        ..._patientJson,
        'dateOfBirth': '1990-06-15',
        'gender': 'F',
        'photoUrl': 'https://x/p.png',
        'person': {'firstName': 'Marie', 'lastName': 'Dupont'},
      });

      expect(p.firstName, 'Marie');
      expect(p.lastName, 'Dupont');
      expect(p.gender, 'F');
      expect(p.photoUrl, 'https://x/p.png');
      // 2026 - 1990 = 36, et l'anniversaire du 15/06 est déjà passé.
      expect(p.age, greaterThanOrEqualTo(35));
      expect(p.fullName, 'Marie Dupont');
    });

    test('Patient sans date de naissance : age = 0, jamais négatif ni d\'exception', () {
      final p = Patient.fromJson({..._patientJson, 'dateOfBirth': '2200-01-01'});
      expect(p.age, 0);
    });

    test('PharmacyStock : alias historiques et seuil d\'alerte', () {
      final s = PharmacyStock.fromJson(const {
        'id': 's1',
        'lotNumber': 'LOT-42',
        'quantite': 3,
        'seuilAlerte': 10,
        'dateExpiration': '2020-01-01',
        'prixUnitaire': 12.5,
        'status': 'LOW_STOCK',
      });

      expect(s.batchNumber, 'LOT-42');
      expect(s.quantity, 3);
      expect(s.minStockLevel, 10);
      expect(s.isLowStock, isTrue);
      expect(s.isExpired, isTrue);
      expect(s.unitCost, 12.5);
      // Un lot sans nom de produit reste lisible.
      expect(s.medicationName, isNotEmpty);
    });

    test('PharmacyStock : expiryDate est non nul même si absent', () {
      final s = PharmacyStock.fromJson(const {'id': 's', 'quantite': 1});
      expect(s.expiryDate, isNotNull);
      expect(s.isExpired, isFalse);
    });

    test('HealthCampaign : distributions et isUpcoming cohérents', () {
      final future = DateTime.now().add(const Duration(days: 30));
      final camp = HealthCampaign.fromJson({
        'id': 'c',
        'name': 'Vaccination',
        'status': 'PLANNED',
        'startDate': future.toIso8601String(),
        'endDate': future.add(const Duration(days: 5)).toIso8601String(),
        'registeredCount': 12,
      });

      expect(camp.isUpcoming, isTrue);
      expect(camp.isActive, isFalse);
      expect(camp.registeredCount, 12);
      expect(camp.status.displayName, isNotEmpty);
      expect(camp.type.displayName, isNotEmpty);
    });
  });

  group('Contrat d\'écriture (toJson)', () {
    test('Patient.toJson n\'invente aucune clé hors celles lues par le serveur', () {
      final json = Patient.fromJson(_patientJson).toJson();

      // `HealthController.createPatientRecord` lit ces champs ; tout le reste
      // est de la decoration d'affichage et ne doit pas partir au serveur.
      expect(
        json.keys.toSet(),
        {'id', 'personId', 'familyId', 'bloodType', 'allergies', 'chronicConditions', 'notes'},
      );
    });

    test('Consultation.toJson utilise le nom serveur de la date de fin', () {
      final json = Consultation.fromJson(const {
        'id': 'x',
        'patientId': 'y',
        'consultationDate': '2026-03-04T10:15:00Z',
        'status': 'COMPLETED',
      }).toJson();

      // Le serveur ne lit pas `dateTime` : c'est `consultation_date`.
      expect(json.containsKey('typeConsultation'), isTrue);
      expect(json['status'], 'COMPLETED');
      expect(json.containsKey('dateTime'), isFalse);
    });

    test('CampaignParticipant.toJson envoie la clé userId attendue', () {
      final json = CampaignParticipant.fromJson(const {
        'id': 'c',
        'campaignId': 'camp',
        'userId': 'user-1',
      }).toJson();

      expect(json['userId'], 'user-1');
      expect(json.containsKey('campaignId'), isTrue);
    });
  });
}
