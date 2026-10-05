import 'package:discipolat_mobile/data/models/organization_v3_models.dart';
import 'package:flutter_test/flutter_test.dart';

/// SPEC_ORGANISATION_MODULABLE_V3 §7.2 — modèles V3 (A→E).
///
/// Ces tests verrouillent deux garanties que le code uphold :
///
/// 1. **Tolérance au JSON partiel** (§7.2) — le serveur peut omettre un champ
///    (`effectiveLabel`, `pluralName`, `sermonCount`…). Un `null` ne doit pas
///    faire planter un écran : les compteurs retombent à 0, les libellés à `null`.
/// 2. **Capacité ≠ intitulé** (garde-fou D2 / §11.4) — `RoleTitle` ne porte
///    QUE du cosmétique : aucun champ de permission/autorisation n'existe sur
///    ces modèles, donc renommer un rôle ne peut rien changer à l'accès.
void main() {
  group('OrganizationLevel (A) — dénomination', () {
    test('parfait le JSON complet du serveur', () {
      final l = OrganizationLevel.fromJson(const {
        'id': 'lvl-1',
        'rootTenantId': 'root-1',
        'name': 'Zone',
        'depthOrder': 2,
        'semanticType': 'ZONE',
        'pluralName': 'Zones',
        'icon': 'map',
        'color': '#123456',
        'branching': true,
        'active': true,
      });
      expect(l.id, 'lvl-1');
      expect(l.name, 'Zone');
      expect(l.depthOrder, 2);
      expect(l.semanticType, 'ZONE');
      expect(l.pluralName, 'Zones');
      expect(l.branching, isTrue);
      expect(l.active, isTrue);
    });

    test('retombe sur CUSTOM quand le serveur omet les champs', () {
      // Le `semanticType` porte la LOGIQUE : sans valeur, on ne doit pas
      // inventer un type métier, on reste sur le neutre CUSTOM (repli sûr).
      final l = OrganizationLevel.fromJson(const {
        'id': 'lvl-2',
        'rootTenantId': 'root-1',
        'name': 'Groupe de prière',
      });
      expect(l.semanticType, 'CUSTOM');
      expect(l.depthOrder, 0);
      expect(l.branching, isTrue, reason: 'une hiérarchie est branchante par défaut');
      expect(l.active, isTrue);
      expect(l.parentLevelId, isNull);
    });
  });

  group('RoleTitle (B) — intitulé cosmétique', () {
    test('porte le libellé, le scope et l\'effectif résolu', () {
      final t = RoleTitle.fromJson(const {
        'roleId': 'r-ancien',
        'label': 'Ancien',
        'nodeId': 'c1',
        'labelPlural': 'Anciens',
        'effectiveLabel': 'Ancien',
      });
      expect(t.roleId, 'r-ancien');
      expect(t.label, 'Ancien');
      expect(t.nodeId, 'c1');
      expect(t.labelPlural, 'Anciens');
      expect(t.effectiveLabel, 'Ancien');
    });

    test('un intitulé différent ne change AUCUNE capacité (D2)', () {
      // Garde-fou §11.4 : deux libellés du même rôle = même autorisation.
      // Le modèle ne contient aucun champ de permission : c'est structurel.
      const a = RoleTitle(roleId: 'r-ancien', label: 'Ancien');
      const b = RoleTitle(roleId: 'r-ancien', label: 'Pasteur principal');
      expect(a.roleId, b.roleId);
      expect(a.label, isNot(b.label));
      expect(a.toString(), isNot(contains('permission')));
      expect(a.toString(), isNot(contains('authoriz')));
    });
  });
  group('MemberRoleAssignment (C) — affiliation', () {
    test('ACTIVE par défaut, isActive reflété', () {
      final a = MemberRoleAssignment.fromJson(const {
        'id': 'a-1',
        'userId': 'u-1',
        'roleId': 'r-ancien',
      });
      expect(a.status, 'ACTIVE');
      expect(a.isActive, isTrue);
      expect(a.nodeId, isNull);
    });

    test('un statut ENDED est inactif (pas de capacité fantôme)', () {
      final a = MemberRoleAssignment.fromJson(const {
        'id': 'a-2',
        'userId': 'u-1',
        'roleId': 'r-ancien',
        'status': 'ENDED',
        'nodeId': 'c1',
      });
      expect(a.isActive, isFalse);
      expect(a.nodeId, 'c1');
    });
  });

  group('NodeTeamMember (§7.1) — équipe du nœud', () {
    test('lit l\'équipe enrichie (rôle + nom)', () {
      final m = NodeTeamMember.fromJson(const {
        'assignmentId': 'a-1',
        'userId': 'u-9',
        'roleId': 'r-ancien',
        'roleLabel': 'Ancien',
        'memberName': 'Nzolo',
        'status': 'ACTIVE',
      });
      expect(m.roleLabel, 'Ancien');
      expect(m.memberName, 'Nzolo');
      expect(m.isActive, isTrue);
    });

    test('tolère status null et nom absent (PII optionnelle)', () {
      // Le backend envoie `status: null` si l'énumération est absente, et
      // `roleLabel`/`memberName` peuvent être null (rôle supprimé, membre sans
      // nom affiché). L'écran ne doit ni planter ni afficher « null ».
      final m = NodeTeamMember.fromJson(const {
        'assignmentId': 'a-2',
        'userId': 'u-9',
        'roleId': 'r-x',
        'status': null,
      });
      expect(m.status, 'ACTIVE', reason: 'repli sûr : ne pas masquer une assignation');
      expect(m.isActive, isTrue);
      expect(m.roleLabel, isNull);
      expect(m.memberName, isNull);
    });
  });

  group('NodeAggregate (E) — compteurs sans PII', () {
    test('lit les compteurs et la progression', () {
      final a = NodeAggregate.fromJson(const {
        'nodeId': 'c1',
        'memberCount': 25,
        'churchCount': 1,
        'leaderCount': 3,
        'sermonCount': 12,
        'prayerTopicCount': 4,
        'levelName': 'Campus',
        'responsibleName': 'Nzolo',
      });
      expect(a.memberCount, 25);
      expect(a.churchCount, 1);
      expect(a.leaderCount, 3);
      expect(a.sermonCount, 12);
      expect(a.prayerTopicCount, 4);
      expect(a.levelName, 'Campus');
    });

    test('des compteurs absents valent 0, jamais null (D7)', () {
      // D7 : le drill-down ne montre QUE des nombres. Un compteur manquant ne
      // doit pas devenir « null » à l'écran.
      final a = NodeAggregate.fromJson(const {'nodeId': 'c2'});
      expect(a.memberCount, 0);
      expect(a.churchCount, 0);
      expect(a.leaderCount, 0);
      expect(a.sermonCount, 0);
      expect(a.prayerTopicCount, 0);
      expect(a.responsibleName, isNull);
    });
  });

  group('Barrel §7.2 — les 4 fichiers sont réexportés', () {
    test('l\'import unique organization_v3_models expose tous les modèles', () {
      // Un seul chemin d'import pour les appelants (organization_v3_api,
      // node_detail_screen, roles_screen) : si un modèle n'est pas réexporté,
      // ces écrans cassent à la compilation.
      expect(OrganizationLevel.fromJson, isNotNull);
      expect(RoleTitle.fromJson, isNotNull);
      expect(MemberRoleAssignment.fromJson, isNotNull);
      expect(NodeTeamMember.fromJson, isNotNull);
      expect(NodeAggregate.fromJson, isNotNull);
    });
  });
}