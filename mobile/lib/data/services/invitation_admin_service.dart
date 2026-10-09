// B9 — Gestion complète des invitations côté administrateur mobile (constat MO3).
//
// Consomme EXACTEMENT le contrat réel de `InvitationController`
// (`/api/v1/admin/invitations`, protégé par `hasAnyRole('TENANT_OWNER','TENANT_ADMIN')`).
// Le préfixe `/api/v1` est géré par ApiService ; AUCUN endpoint inventé (gate G-B.4) :
//   GET    /admin/invitations?page&size&status&q -> PageResponse OU liste (rétro-compat)
//   POST   /admin/invitations                   { email, role, scopeType, scopeId?, organizationNodeId? }
//   POST   /admin/invitations/{id}/resend        -> { invitationLink, emailSent, expiresAt, … }
//   DELETE /admin/invitations/{id}               -> 204
//   GET    /admin/invitations/validate/{token}   (reprise du contrat d'acceptation, B11)
//   POST   /admin/invitations/accept/{token}     { password?, firstName?, lastName? }
//
// Le parsing est strictement tolérant : on ne casse jamais l'écran si un champ
// optionnel manque (jamais de `as` massif, jamais d'invention de valeur).
import 'package:dio/dio.dart';

import 'api_service.dart';

/// Une ligne d'invitation telle que renvoyée par `toMap` du contrôleur.
class InvitationItem {
  InvitationItem({
    required this.id,
    required this.email,
    required this.role,
    required this.status,
    required this.scopeType,
    this.scopeId,
    this.organizationNodeId,
    this.organizationNodeName,
    this.createdAt,
    this.expiresAt,
    this.acceptedAt,
    this.tenantName,
  });

  final String id;
  final String email;
  final String role;
  final String status;
  final String scopeType;
  final String? scopeId;
  final String? organizationNodeId;
  final String? organizationNodeName;
  final DateTime? createdAt;
  final DateTime? expiresAt;
  final DateTime? acceptedAt;
  final String? tenantName;

  bool get isPending => status.toUpperCase() == 'PENDING';

  static InvitationItem fromJson(Map<String, dynamic> json) {
    return InvitationItem(
      id: _str(json['id']) ?? '',
      email: _str(json['email']) ?? '',
      role: _str(json['role']) ?? '',
      status: (_str(json['status']) ?? '').toUpperCase(),
      scopeType: _str(json['scopeType']) ?? 'TENANT',
      scopeId: _str(json['scopeId']),
      organizationNodeId: _str(json['organizationNodeId']),
      organizationNodeName: _str(json['organizationNodeName']),
      createdAt: _date(json['createdAt']),
      expiresAt: _date(json['expiresAt']),
      acceptedAt: _date(json['acceptedAt']),
      tenantName: _str(json['tenantName']),
    );
  }
}

/// Résultat de liste, normalisé : le backend renvoie soit une PageResponse
/// `{ content, page, totalPages, totalElements }`, soit une liste simple.
class InvitationListResult {
  InvitationListResult({
    required this.items,
    this.page = 0,
    this.totalPages = 1,
    this.totalElements,
  });

  final List<InvitationItem> items;
  final int page;
  final int totalPages;
  final int? totalElements;

  bool get hasNext => page + 1 < totalPages;
}

/// Résultat d'une création : le backend renvoie soit une invitation (lien),
/// soit un ajout direct de membership si le compte existe déjà dans le tenant.
class CreateInvitationResult {
  CreateInvitationResult({
    required this.success,
    this.invitationId,
    this.invitationLink,
    this.emailSent = false,
    this.requiresTenantSwitch = false,
    this.crossTenantIdentity = false,
    this.invitedUserId,
    this.message,
  });

  final bool success;
  final String? invitationId;
  final String? invitationLink;
  final bool emailSent;
  final bool requiresTenantSwitch;
  final bool crossTenantIdentity;
  final String? invitedUserId;
  final String? message;

  /// Compte déjà connu du tenant → membership ajouté, aucune invitation créée.
  bool get isDirectMembership => invitedUserId != null && invitationLink == null;

  static CreateInvitationResult fromJson(Map<String, dynamic> json) {
    return CreateInvitationResult(
      success: json['success'] == true,
      invitationId: _str(json['invitationId']),
      invitationLink: _str(json['invitationLink']),
      emailSent: json['emailSent'] == true,
      requiresTenantSwitch: json['requiresTenantSwitch'] == true,
      crossTenantIdentity: json['crossTenantIdentity'] == true,
      invitedUserId: _str(json['invitedUserId']),
      message: _str(json['message']),
    );
  }
}

/// Résultat d'un renvoi : le lien est RENUMÉRÉ (nouveau token), à re-copier.
class ResendInvitationResult {
  ResendInvitationResult({
    required this.emailSent,
    this.invitationLink,
    this.expiresAt,
    this.message,
  });

  final bool emailSent;
  final String? invitationLink;
  final DateTime? expiresAt;
  final String? message;

  static ResendInvitationResult fromJson(Map<String, dynamic> json) {
    return ResendInvitationResult(
      emailSent: json['emailSent'] == true,
      invitationLink: _str(json['invitationLink']),
      expiresAt: _date(json['expiresAt']),
      message: _str(json['message']),
    );
  }
}
/// Rôle réellement assignable, lu depuis l'API.
///
/// Une liste figée dans l'écran ne peut pas connaître les rôles *custom* d'un
/// tenant : `RoleManagementController` → `GET /admin/roles/overview` est la
/// seule autorité (miroir du web, `useAssignableRoles`).
class AssignableRole {
  const AssignableRole(
      {required this.key, this.label, this.description, this.system = false});

  final String key;
  final String? label;
  final String? description;
  final bool system;

  /// `label` est la seule source d'affichage ; `key` reste l'identifiant
  /// technique envoyé au backend. On n'invente jamais un libellé.
  String get displayLabel => (label != null && label!.trim().isNotEmpty) ? label! : key;

  static AssignableRole fromJson(Map<String, dynamic> json) => AssignableRole(
        key: _str(json['key']) ?? '',
        label: _str(json['label']),
        description: _str(json['description']),
        system: json['system'] == true,
      );
}

/// Nœud organisationnel proposable comme portée d'invitation.
///
/// `InvitationService.createInvitation` refuse un scope non-TENANT sans
/// `organizationNodeId` (INVITATION_SCOPE_INVALID) : l'écran DOIT donc proposer
/// un nœud réel, jamais une chaîne libre.
class OrganizationNodeRef {
  const OrganizationNodeRef({required this.id, this.name, this.type, this.level});

  final String id;
  final String? name;
  final String? type;
  final int? level;

  String get displayLabel {
    final n = (name != null && name!.trim().isNotEmpty) ? name! : id;
    final t = (type != null && type!.trim().isNotEmpty) ? type! : null;
    return t == null ? n : '$n — $t';
  }

  static OrganizationNodeRef fromJson(Map<String, dynamic> json) {
    final rawLevel = json['level'];
    return OrganizationNodeRef(
      id: _str(json['id']) ?? '',
      name: _str(json['name']),
      type: _str(json['type']),
      level: rawLevel is num ? rawLevel.toInt() : null,
    );
  }
}

/// Erreur portant le message métier du serveur.
///
/// Le backend répond en `ProblemDetail` (RFC 7807) : `detail` porte le texte et
/// `type` porte le code métier (`INVITATION_SCOPE_INVALID`…). Écrasser cela par
/// « L'invitation a échoué » masquait la cause réelle à l'administrateur.
class InvitationAdminException implements Exception {
  InvitationAdminException(this.message, {this.code, this.statusCode});

  final String message;
  final String? code;
  final int? statusCode;

  @override
  String toString() => message;
}

class InvitationAdminService {

  InvitationAdminService({ApiService? apiService})
      : _apiService = apiService ?? ApiService();

  final ApiService _apiService;

  static const String _base = '/admin/invitations';

  /// Liste paginée ou simple selon la présence de [page].
  Future<InvitationListResult> list({
    int? page,
    int? size,
    String? status,
    String? q,
  }) async {
    final params = <String, dynamic>{
      if (page != null) 'page': page,
      if (size != null) 'size': size,
      if (status != null && status.trim().isNotEmpty) 'status': status.trim(),
      if (q != null && q.trim().isNotEmpty) 'q': q.trim(),
    };
    final response = await _apiService.get(
      _base,
      params: params.isEmpty ? null : params,
    );
    return _parseList(response.data);
  }

  Future<CreateInvitationResult> create({
    required String email,
    required String role,
    String scopeType = 'TENANT',
    String? scopeId,
    String? organizationNodeId,
  }) async {
    final response = await _apiService.post(_base, data: <String, dynamic>{
      'email': email.trim(),
      'role': role.trim(),
      'scopeType': scopeType,
      if (scopeId != null && scopeId.isNotEmpty) 'scopeId': scopeId,
      if (organizationNodeId != null && organizationNodeId.isNotEmpty)
        'organizationNodeId': organizationNodeId,
    });
    return CreateInvitationResult.fromJson(_asMap(response.data));
  }

  Future<ResendInvitationResult> resend(String id) async {
    final response =
        await _apiService.post('$_base/${Uri.encodeComponent(id)}/resend');
    return ResendInvitationResult.fromJson(_asMap(response.data));
  }

  Future<void> cancel(String id) async {
    await _apiService.delete('$_base/${Uri.encodeComponent(id)}');
  }

  /// Reprise du contrat d'acceptation (utilisé par l'écran d'acceptation B11).
  Future<Map<String, dynamic>> validate(String token) async {
    final response = await _apiService
        .get('$_base/validate/${Uri.encodeComponent(token)}');
    return _asMap(response.data);
  }

  Future<Map<String, dynamic>> accept(
    String token, {
    String? password,
    String? firstName,
    String? lastName,
  }) async {
    final response = await _apiService.post(
      '$_base/accept/${Uri.encodeComponent(token)}',
      data: <String, dynamic>{
        if (password != null) 'password': password,
        if (firstName != null) 'firstName': firstName,
        if (lastName != null) 'lastName': lastName,
      },
    );
    return _asMap(response.data);
  }

  // ── B9 : référentiels lus depuis l'API (aucune liste en dur) ──────────────

  /// Rôles réellement assignables — `GET /admin/roles/overview`
  /// (`RoleManagementController.getAllRoles`).
  ///
  /// Le repli n'est jamais une liste de rôles *inventée* : si l'appel échoue on
  /// renvoie une liste vide et l'appelant affiche l'erreur. Mieux vaut pas
  /// proposer de rôle que proposer un rôle qui n'existe pas chez ce tenant.
  Future<List<AssignableRole>> listAssignableRoles() async {
    final response = await _apiService.get('/admin/roles/overview');
    final data = response.data;
    if (data is! List) return const <AssignableRole>[];
    return data
        .whereType<Map>()
        .map((e) => AssignableRole.fromJson(Map<String, dynamic>.from(e)))
        .where((r) => r.key.trim().isNotEmpty)
        .toList();
  }

  /// Nœuds organisationnels — `GET /admin/org/tree`
  /// (`OrganizationManagementController.getTree`).
  ///
  /// La réponse est un `OrganizationTreeResponse { root, allNodes,
  /// childrenByParent }` : on lit `allNodes` et on aplati, parce que le
  /// sélectionneur a besoin d'une liste plate triée, pas d'un arbre récursif
  /// (la hiérarchie reste visible via `path`/`level` dans le libellé).
  Future<List<OrganizationNodeRef>> listOrganizationNodes() async {
    final response = await _apiService.get('/admin/org/tree');
    final map = _asMap(response.data);
    final rawNodes = map['allNodes'];
    final source = rawNodes is List ? rawNodes : const <dynamic>[];
    final nodes = source
        .whereType<Map>()
        .map((e) => OrganizationNodeRef.fromJson(Map<String, dynamic>.from(e)))
        .where((n) => n.id.trim().isNotEmpty)
        .toList();
    // Rendre déterministe l'ordre : un tenant avec deux nœuds de même `level`
    // doit toujours proposer le même ordre à l'administrateur.
    nodes.sort((a, b) {
      final byLevel = (a.level ?? 0).compareTo(b.level ?? 0);
      if (byLevel != 0) return byLevel;
      return a.displayLabel.compareTo(b.displayLabel);
    });
    return nodes;
  }

  // ── Aides de parsing (tolérantes, aucune invention) ──────────────────────
  InvitationListResult _parseList(dynamic data) {
    if (data is Map) {
      final map = Map<String, dynamic>.from(data);
      final content = (map['content'] is List)
          ? (map['content'] as List)
              .whereType<Map>()
              .map((e) => InvitationItem.fromJson(Map<String, dynamic>.from(e)))
              .toList()
          : <InvitationItem>[];
      return InvitationListResult(
        items: content,
        page: (map['page'] is num) ? (map['page'] as num).toInt() : 0,
        totalPages: (map['totalPages'] is num) ? (map['totalPages'] as num).toInt() : 1,
        totalElements: (map['totalElements'] is num) ? (map['totalElements'] as num).toInt() : content.length,
      );
    }
    if (data is List) {
      return InvitationListResult(
        items: data
            .whereType<Map>()
            .map((e) => InvitationItem.fromJson(Map<String, dynamic>.from(e)))
            .toList(),
      );
    }
    return InvitationListResult(
      items: <InvitationItem>[],
    );
  }

  /// Traduit une `DioException` en [InvitationAdminException] porteuse du
  /// message métier du serveur.
  ///
  /// Le backend répond en `ProblemDetail` : `detail` = texte lisible,
  /// `type` = code métier (`INVITATION_SCOPE_INVALID`, `INVITATION_ROLE_INVALID`…).
  /// Sans cette traduction, l'écran affichait « L'invitation a échoué » pour un
  /// 400 parfaitement explicite, ce qui rendait le bug ORGANIZATION
  /// impossible à diagnostiquer depuis le terrain.
  static InvitationAdminException translateError(Object error) {
    if (error is InvitationAdminException) return error;
    if (error is DioException) {
      final status = error.response?.statusCode;
      final body = error.response?.data;
      String? detail;
      String? code;
      if (body is Map) {
        detail = _str(body['detail']) ?? _str(body['message']);
        final type = _str(body['type']);
        // `…/errors/INVITATION_SCOPE_INVALID` → `INVITATION_SCOPE_INVALID`
        if (type != null && type.isNotEmpty) {
          final idx = type.lastIndexOf('/');
          code = idx >= 0 ? type.substring(idx + 1) : type;
        }
      }
      final hasBusinessDetail = detail != null && detail.trim().isNotEmpty;
      return InvitationAdminException(
        hasBusinessDetail ? detail : _fallbackMessage(status),
        code: code,
        statusCode: status,
      );
    }
    return InvitationAdminException(
      'Impossible de joindre le serveur.',
      statusCode: null,
    );
  }

  static String _fallbackMessage(int? status) {
    switch (status) {
      case 400:
        return 'Requête refusée par le serveur (données invalides).';
      case 401:
        return 'Session expirée. Reconnectez-vous.';
      case 403:
        return "Vous n'avez pas les droits nécessaires.";
      case 404:
        return 'Ressource introuvable.';
      case 409:
        return 'Conflit : cet élément existe déjà.';
      case 429:
        return 'Trop de requêtes. Réessayez dans un instant.';
      case null:
        return 'Serveur injoignable.';
      default:
        return 'Erreur serveur ($status).';
    }
  }
}

// ── Aides de parsing de niveau fichier, partagées par les modèles ci-dessus ──
Map<String, dynamic> _asMap(dynamic data) =>
    data is Map ? Map<String, dynamic>.from(data) : <String, dynamic>{};

String? _str(dynamic v) => v?.toString();

DateTime? _date(dynamic v) {
  if (v == null) return null;
  if (v is DateTime) return v;
  return DateTime.tryParse(v.toString());
}
