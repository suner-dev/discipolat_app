// Contrat de désérialisation du module santé (modèle V240 restauré).
//
// POURQUOI CE TEST EXISTE
// Le modèle déclarait `required int id` alors que les 11 entités de santé sont
// `GenerationType.UUID` côté serveur. Le `.g.dart` produisait donc
// `id: (json['id'] as num).toInt()` : appliqué à une chaîne UUID, ce cast lève
// un `TypeError` et fait échouer TOUTE la liste. Symptôme en production : l'écran
// Santé vide, sans erreur visible.
//
// Ce test fige le contrat réel vérifié dans le code serveur (vues aplaties
// HealthService : {personId, personName}, {patientId, patientName},
// {itemId, itemName}, {responsibleId, responsibleName, participantsCount}) :
// un payload serveur tel qu'il est sérialisé doit se désérialiser. Il couvre
// aussi la tolérance (clés absentes, enums inconnus, dates nulles) et la
// non-régression des champs d'affichage consommés par les écrans.

import 'package:discipolat_mobile/features/health/models/health_model.dart';
import 'package:flutter_test/flutter_test.dart';

/// Payload réel d'un `PatientRecord` sérialisé par Jackson (vue aplatie) :
/// l'`id` est une chaîne UUID, la personne est aplatie en {personId,
/// personName} — jamais embarquée.
const _patientJson = <String, dynamic>{
  'id': '3f2504e0-4f89-11d3-9a0c-0305e82c3301',
  'personId': '22222222-2222-2222-2222-222222222222',
  'personName': 'Marie Dupont',
  'familyId': 'aaaaaaaa-0000-0000-0000-000000000001',
  'familyName': 'Famille Dupont',
  'groupeSanguin': 'O+',
  'allergies': 'Pénicilline',
  'antecedents': 'Hypertension',
  'notesSensibles': 'Sous anticoagulants',
  'createdAt': '2026-01-15T08:30:00Z',
  'updatedAt': '2026-02-01T10:00:00Z',
};

void main() {
  group('Identifiants UUID (le bug qui cassait l\'écran Santé)', () {
    test('PatientRecord : un id UUID chaîne ne lève pas et reste une String', () {
      final p = PatientRecord.fromJson(_patientJson);

      expect(p.id, '3f2504e0-4f89-11d3-9a0c-0305e82c3301');
      // La régression exacte : un cast num→int sur une chaîne aurait levé ici.
      expect(p.id, isA<String>());
      expect(p.personId, isA<String>());
    });

    test('MedicalConsultation : les ids de relation restent des chaînes', () {
      final c = MedicalConsultation.fromJson(const {
        'id': '11111111-1111-1111-1111-111111111111',
        'patientId': '22222222-2222-2222-2222-222222222222',
        'patientName': 'Marie Dupont',
        'practitionerId': '33333333-3333-3333-3333-333333333333',
        'practitionerName': 'Dr Obam',
        'consultationDate': '2026-03-04',
        'typeConsultation': 'CONSULTATION',
        'motif': 'Fièvre',
        'diagnostic': 'Angine',
        'status': 'COMPLETED',
      });

      expect(c.id, isA<String>());
      expect(c.patientId, '22222222-2222-2222-2222-222222222222');
      expect(c.practitionerId, '33333333-3333-3333-3333-333333333333');
      expect(c.status, ConsultationStatus.completed);
      // `consultationDate` est un LocalDate serveur : String « yyyy-MM-dd »,
      // lisible telle quelle par les écrans.
      expect(c.consultationDate, '2026-03-04');
      expect(DateTime.parse(c.consultationDate!).year, 2026);
    });

    test('Prescription : consultationId et patientId UUID', () {
      final p = Prescription.fromJson(const {
        'id': 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee',
        'consultationId': '11111111-1111-1111-1111-111111111111',
        'patientId': '22222222-2222-2222-2222-222222222222',
        'patientName': 'Marie Dupont',
        'medicament': 'Amoxicilline 500 mg',
        'dosage': '1 comprimé',
        'posologie': '3×/jour',
        'duree': '7 jours',
        'status': 'ACTIVE',
      });

      expect(p.id, isA<String>());
      expect(p.consultationId, isA<String>());
      expect(p.medicament, 'Amoxicilline 500 mg');
      expect(p.posologie, '3×/jour');
      expect(p.status, PrescriptionStatus.active);
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
      expect(cp.registeredAt, isNotNull);
      expect(cp.status, ParticipantStatus.registered);
    });
  });

  group('Tolérance : un payload incomplet ne casse pas une liste', () {
    test('une entité presque vide reste désérialisable', () {
      final p = PatientRecord.fromJson(const {'id': 'abc'});
      expect(p.id, 'abc');
      expect(p.personName, isNull);
      expect(p.groupeSanguin, isNull);
      expect(p.createdAt, isNull);
    });

    test('un enum inconnu retombe sur une valeur par défaut, sans exception', () {
      final c = MedicalConsultation.fromJson(const {
        'id': 'x',
        'patientId': 'y',
        'status': 'EN_STATUT_INVENTE',
      });
      expect(c.status, ConsultationStatus.scheduled);
      final camp = HealthCampaign.fromJson(const {
        'id': 'c',
        'campaignType': 'TYPE_INVENTE',
      });
      expect(camp.campaignType, CampaignType.autre);
    });

    test('une date LocalDate absente vaut null, jamais un faux « aujourd\'hui »', () {
      final camp = HealthCampaign.fromJson(const {'id': 'c', 'title': 'Dépistage'});
      // Une date manquante ne doit pas devenir « now » : la campagne
      // passerait pour « je démarre aujourd'hui ».
      expect(camp.startDate, isNull);
      expect(camp.endDate, isNull);
      expect(camp.participantsCount, 0);
    });

    test('les valeurs nulles ou vides côté serveur deviennent null ici', () {
      final c = MedicalConsultation.fromJson(const {
        'id': 'x',
        'patientId': 'y',
        'motif': '',
        'diagnostic': null,
      });
      expect(c.motif, isNull);
      expect(c.diagnostic, isNull);
    });
  });

  group('Champs d\'affichage conservés (non-régression des écrans)', () {
    test('PatientRecord : vue aplatie lisible sans barre de requête', () {
      final p = PatientRecord.fromJson(_patientJson);

      expect(p.personName, 'Marie Dupont');
      expect(p.familyName, 'Famille Dupont');
      expect(p.groupeSanguin, 'O+');
      expect(p.allergies, 'Pénicilline');
      expect(p.confidentialityLevel, ConfidentialityLevel.strict);
      expect(p.createdAt!.year, 2026);
    });

    test('PharmacyStock : seuil d\'alerte et alias serveur', () {
      final s = PharmacyStock.fromJson(const {
        'id': 's1',
        'itemId': 'item-1',
        'itemName': 'Paracétamol 500 mg',
        'lotNumber': 'LOT-42',
        'quantite': 3,
        'seuilAlerte': 10,
        'dateExpiration': '2020-01-01',
        'prixUnitaire': 12.5,
        'status': 'EXPIRÉ',
        'isExpired': true,
      });

      expect(s.lotNumber, 'LOT-42');
      expect(s.quantite, 3);
      expect(s.seuilAlerte, 10);
      expect(s.itemName, 'Paracétamol 500 mg');
      expect(s.isLowStock, isTrue);
      expect(s.isExpired, isTrue);
      expect(s.prixUnitaire, 12.5);
      expect(s.status, StockStatus.expire);
    });

    test('PharmacyStock : isLowStock prudent sans seuil', () {
      final s = PharmacyStock.fromJson(const {'id': 's', 'quantite': 1});
      expect(s.isLowStock, isFalse);
    });

    test('HealthCampaign : compteurs et champs français du serveur', () {
      final camp = HealthCampaign.fromJson(const {
        'id': 'c',
        'title': 'Vaccination',
        'status': 'PLANNED',
        'startDate': '2026-11-01',
        'endDate': '2026-11-06',
        'lieu': 'Plateau technique',
        'responsibleId': 'r-1',
        'responsibleName': 'Diacre Essomba',
        'participantsCount': 12,
        'campaignType': 'VACCINATION',
      });

      expect(camp.title, 'Vaccination');
      expect(camp.lieu, 'Plateau technique');
      expect(camp.responsibleName, 'Diacre Essomba');
      expect(camp.participantsCount, 12);
      expect(camp.status.displayName, isNotEmpty);
      expect(camp.campaignType.displayName, isNotEmpty);
    });
  });

  group('Contrat d\'écriture (corps exacts lus par le serveur)', () {
    test('PatientRecord.createBody imbrique la personne comme le serveur l\'attend', () {
      final body = PatientRecord.createBody(
        personId: 'p-1',
        groupeSanguin: 'O+',
      );

      // `HealthController.createPatient` désérialise l'entité : la personne
      // est une association {"person": {"id": …}}, PAS une clé plate.
      expect(body['person'], {'id': 'p-1'});
      expect(body.containsKey('personId'), isFalse);
      expect(body['groupeSanguin'], 'O+');
      // Aucune clé inventée : les champs optionnels absents ne sont pas envoyés.
      expect(body.containsKey('allergies'), isFalse);
    });

    test('MedicalConsultation.createBody envoie les associations obligatoires', () {
      final body = MedicalConsultation.createBody(
        patientId: 'pat-1',
        practitionerId: 'pra-1',
        consultationDate: '2026-03-04',
        motif: 'Fièvre',
      );

      expect(body['patient'], {'id': 'pat-1'});
      expect(body['practitioner'], {'id': 'pra-1'});
      expect(body['consultationDate'], '2026-03-04');
      expect(body['typeConsultation'], 'CONSULTATION');
      expect(body['status'], isNull); // porté par la défaut serveur
    });

    test('HealthCampaign.createBody porte les nullable=false obligatoires', () {
      final body = HealthCampaign.createBody(
        title: 'Dépistage',
        startDate: '2026-12-01',
        lieu: 'Parvis',
        campaignType: CampaignType.depistage,
      );

      expect(body['title'], 'Dépistage');
      expect(body['startDate'], '2026-12-01');
      expect(body['lieu'], 'Parvis');
      expect(body['campaignType'], 'DEPISTAGE');
      // createCampaign force le responsible à l'acteur courant côté serveur :
      // le corps ne doit PAS envoyer de responsible.
      expect(body.containsKey('responsible'), isFalse);
    });

    test('Prescription.createBody imbrique consultation et patient', () {
      final body = Prescription.createBody(
        consultationId: 'c-1',
        patientId: 'p-1',
        medicament: 'Amoxicilline',
      );

      expect(body['consultation'], {'id': 'c-1'});
      expect(body['patient'], {'id': 'p-1'});
      expect(body['medicament'], 'Amoxicilline');
    });
  });
}
