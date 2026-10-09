// B9 — Écran de gestion des invitations pour un administrateur mobile (constat MO3).
//
// Règles métier (conformes au contrat §B9 / InvitationController) :
// - liste, filtre par statut, renvoi, annulation, copie du lien, création avec scope ;
// - `emailSent == false` → avertissement + bouton « Copier le lien » (SMTP absent, D10) ;
// - `requiresTenantSwitch == true` → message d'information non bloquant ;
// - `expiresAt`, statut et rôle affichés ;
// - AUCUN `alert` natif (confirmations via dialog Material), AUCUNE donnée en dur.
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'dart:async';
import 'package:intl/intl.dart';

import '../../../data/services/invitation_admin_service.dart';

class InvitationManagementScreen extends StatefulWidget {
  const InvitationManagementScreen({super.key, this.service});

  /// Permet d'injecter un service mocké dans les tests widget.
  final InvitationAdminService? service;

  @override
  State<InvitationManagementScreen> createState() =>
      _InvitationManagementScreenState();
}

class _InvitationManagementScreenState extends State<InvitationManagementScreen> {
  late final InvitationAdminService _service =
      widget.service ?? InvitationAdminService();

  bool _loading = true;
  String? _error;
  List<InvitationItem> _items = const <InvitationItem>[];
  String? _statusFilter;
  String _search = '';
  int _page = 0;
  int _totalPages = 1;
  int _totalElements = 0;
  bool _loadingMore = false;

  static const int _pageSize = 20;

  /// Référentiels lus depuis l'API. Une liste en dur ne peut pas connaître les
  /// rôles custom d'un tenant, et — surtout — le backend REFUSE un scope
  /// ORGANIZATION sans `organizationNodeId` (`INVITATION_SCOPE_INVALID`) : sans
  /// ce sélecteur, toute invitation d'organisation échouait en 400.
  List<AssignableRole> _roles = const <AssignableRole>[];
  List<OrganizationNodeRef> _nodes = const <OrganizationNodeRef>[];
  bool _loadingRefs = true;
  String? _refsError;

  /// `REVOKED` complète `CANCELED` : l'enum backend `InvitationStatus` contient
  /// les deux, et le filtre mobile n'en proposait qu'un (le web proposait
  /// l'autre). Sans ce correctif, une invitation révoquée était invisible.
  static const List<MapEntry<String, String>> _statusOptions =
      <MapEntry<String, String>>[
    MapEntry('TOUTES', 'Toutes'),
    MapEntry('PENDING', 'En attente'),
    MapEntry('ACCEPTED', 'Acceptées'),
    MapEntry('EXPIRED', 'Expirées'),
    MapEntry('CANCELED', 'Annulées'),
    MapEntry('REVOKED', 'Révoquées'),
  ];

  @override
  void initState() {
    super.initState();
    _loadRefs();
    _load();
  }

  Future<void> _loadRefs() async {
    setState(() {
      _loadingRefs = true;
      _refsError = null;
    });
    try {
      final results = await Future.wait(<Future<Object>>[
        _service.listAssignableRoles(),
        _service.listOrganizationNodes(),
      ]);
      if (!mounted) return;
      setState(() {
        _roles = results[0] as List<AssignableRole>;
        _nodes = results[1] as List<OrganizationNodeRef>;
        _loadingRefs = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _loadingRefs = false;
        _refsError = InvitationAdminService.translateError(error).message;
      });
    }
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    await _fetch(page: 0, replace: true);
  }

  /// Charge une page. `replace=false` = chargementpagination (on concatène),
  /// ce qui évite l'effet « la liste se vide » à chaque page suivante.
  Future<void> _fetch({required int page, required bool replace}) async {
    try {
      final result = await _service.list(
        page: page,
        size: _pageSize,
        status: (_statusFilter == null || _statusFilter == 'TOUTES')
            ? null
            : _statusFilter,
        q: _search.trim().isEmpty ? null : _search.trim(),
      );
      if (!mounted) return;
      setState(() {
        _items = replace ? result.items : <InvitationItem>[..._items, ...result.items];
        _page = result.page;
        _totalPages = result.totalPages <= 0 ? 1 : result.totalPages;
        _totalElements = result.totalElements ?? _items.length;
        _loading = false;
        _loadingMore = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _loadingMore = false;
        if (replace) {
          _error = _listErrorMessage(error);
        }
      });
    }
  }

  /// Message d'erreur de LISTE.
  ///
  /// Quand le serveur a répondu (4xx/5xx), son message est plus utile que le
  /// libellé générique : « 403 — droits insuffisants » oriente l'admin, alors
  /// que « impossible de charger » ne dit rien. Quand l'échec est local (réseau,
  /// DNS), il n'y a pas de message serveur : on garde alors le libellé d'écran,
  /// qui reste vrai et actionnable (« Réessayer »).
  String _listErrorMessage(Object error) {
    final translated = InvitationAdminService.translateError(error);
    if (translated.statusCode != null) return translated.message;
    return 'Impossible de charger les invitations.';
  }

  Future<void> _loadMore() async {
    if (_loadingMore || _loading) return;
    if (_page + 1 >= _totalPages) return;
    setState(() => _loadingMore = true);
    await _fetch(page: _page + 1, replace: false);
  }

  Future<void> _searchChanged(String value) async {
    // Anti-rebond : sans ce délai, chaque frappe relançait une requête et
    // responses poucharrives. 400 ms est le compromis usuel.
    _searchDebounce?.cancel();
    _searchDebounce = Timer(const Duration(milliseconds: 400), () async {
      setState(() => _search = value);
      await _load();
    });
  }

  Timer? _searchDebounce;

  @override
  void dispose() {
    _searchDebounce?.cancel();
    super.dispose();
  }

  void _snack(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
        .showSnackBar(SnackBar(content: Text(message)));
  }

  /// Le lien n'existe qu'en clair dans la réponse (jamais dans la liste) :
  /// quand l'email n'a pas pu être envoyé, on le présente à copier (D10).
  Future<void> _showLinkSheet(String? link, {required bool emailSent}) async {
    if (link == null || link.isEmpty) return;
    if (emailSent) return;
    if (!mounted) return;
    await showModalBottomSheet<void>(
      context: context,
      builder: (sheetContext) => SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              const Text(
                'Email non envoyé',
                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
              ),
              const SizedBox(height: 4),
              const Text(
                "L'adresse de l'invitation reste valide. Transmettez le lien manuellement.",
              ),
              const SizedBox(height: 12),
              SelectableText(link, style: const TextStyle(fontFamily: 'monospace')),
              const SizedBox(height: 12),
              Row(
                children: <Widget>[
                  FilledButton.icon(
                    onPressed: () async {
                      await Clipboard.setData(ClipboardData(text: link));
                      if (sheetContext.mounted) Navigator.pop(sheetContext);
                      _snack('Lien copié');
                    },
                    icon: const Icon(Icons.copy, size: 18),
                    label: const Text('Copier le lien'),
                  ),
                ],
              ),
              const SizedBox(height: 8),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _showCreateSheet() async {
    // La feuille est un StatefulWidget autonome : son TextEditingController est
    // détruit par son propre cycle de vie (au démontage réel, après l'animation
    // de fermeture), jamais pendant que le TextField est encore monté.
    final submitted = await showModalBottomSheet<Map<String, String>>(
      context: context,
      isScrollControlled: true,
      builder: (sheetContext) => Padding(
        padding: EdgeInsets.only(
          bottom: MediaQuery.of(sheetContext).viewInsets.bottom,
        ),
        child: _CreateInvitationSheet(
          roles: _roles,
          nodes: _nodes,
          loadingRefs: _loadingRefs,
          refsError: _refsError,
          onRetryRefs: _loadRefs,
        ),
      ),
    );
    if (submitted == null) return; // feuille fermée sans envoi
    await _create(
      email: submitted['email'] ?? '',
      role: submitted['role'] ?? '',
      scopeType: submitted['scopeType'] ?? 'TENANT',
      organizationNodeId: submitted['organizationNodeId'],
    );
  }

  Future<void> _create({
    required String email,
    required String role,
    required String scopeType,
    String? organizationNodeId,
  }) async {
    final trimmed = email.trim();
    if (trimmed.isEmpty) {
      _snack('Adresse email requise');
      return;
    }
    // Garde-fou local : le serveur renverrait 400 INVITATION_SCOPE_INVALID.
    // On bloque ici avec un message explicite plutôt que d'envoyer une
    // requête vouée à l'échec.
    if (scopeType != 'TENANT' && (organizationNodeId == null || organizationNodeId.isEmpty)) {
      _snack('Sélectionnez le nœud organisationnel de rattachement.');
      return;
    }
    try {
      final result = await _service.create(
        email: trimmed,
        role: role,
        scopeType: scopeType,
        organizationNodeId: organizationNodeId,
      );
      if (!mounted) return;
      if (result.isDirectMembership) {
        _snack('Utilisateur ajouté directement (compte existant)');
      } else if (result.requiresTenantSwitch) {
        _snack('Un compte existe déjà dans une autre église : '
            "l'utilisateur choisira son organisation à la connexion.");
      } else {
        _snack(result.emailSent
            ? 'Invitation envoyée à $trimmed'
            : 'Invitation créée (email non envoyé)');
        await _showLinkSheet(result.invitationLink,
            emailSent: result.emailSent);
      }
      await _load();
    } catch (error) {
      _snack(InvitationAdminService.translateError(error).message);
    }
  }

  Future<void> _resend(InvitationItem item) async {
    try {
      final result = await _service.resend(item.id);
      if (!mounted) return;
      _snack(result.emailSent
          ? 'Invitation renvoyée à ${item.email}'
          : 'Invitation renvoyée (email non envoyé)');
      await _showLinkSheet(result.invitationLink, emailSent: result.emailSent);
      await _load();
    } catch (error) {
      _snack(InvitationAdminService.translateError(error).message);
    }
  }

  Future<void> _cancel(InvitationItem item) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Annuler l’invitation'),
        content: Text('Annuler l’invitation de ${item.email} ?'),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            child: const Text('Non'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(dialogContext, true),
            child: const Text('Annuler l’invitation'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    try {
      await _service.cancel(item.id);
      _snack('Invitation annulée');
      await _load();
    } catch (error) {
      _snack(InvitationAdminService.translateError(error).message);
    }
  }

  String _formatExpires(DateTime? value) {
    if (value == null) return '—';
    return DateFormat('dd/MM/yyyy').format(value.toLocal());
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Invitations'),
        actions: <Widget>[
          IconButton(
            onPressed: _load,
            icon: const Icon(Icons.refresh),
            tooltip: 'Actualiser',
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _showCreateSheet,
        icon: const Icon(Icons.person_add),
        label: const Text('Inviter'),
      ),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_error != null) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            const Icon(Icons.error_outline, size: 40, color: Colors.redAccent),
            const SizedBox(height: 8),
            Text(_error!),
            const SizedBox(height: 8),
            TextButton(onPressed: _load, child: const Text('Réessayer')),
          ],
        ),
      );
    }

    return Column(
      children: <Widget>[
        _buildFilter(),
        Expanded(
          child: _items.isEmpty
              ? const Center(
                  child: Text('Aucune invitation'),
                )
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView.builder(
                    padding: const EdgeInsets.fromLTRB(12, 4, 12, 96),
                    // +1 pour la ligne de pagination en fin de liste.
                    itemCount: _items.length + 1,
                    itemBuilder: (context, index) {
                      if (index == _items.length) {
                        return _buildPaginationRow();
                      }
                      return _buildTile(_items[index]);
                    },
                  ),
                ),
        ),
      ],
    );
  }

  /// Charge utile de pagination. Le total est affiché pour que l'administrateur
  /// sache qu'il ne voit pas « tout » quand la liste est paginée.
  Widget _buildPaginationRow() {
    final hasMore = _page + 1 < _totalPages;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 16),
      child: Column(
        children: <Widget>[
          Text(
            '$_totalElements invitation(s) — page ${_page + 1}/$_totalPages',
            style: Theme.of(context).textTheme.bodySmall,
          ),
          const SizedBox(height: 8),
          if (_loadingMore)
            const Padding(
              padding: EdgeInsets.all(8),
              child: LinearProgressIndicator(),
            )
          else if (hasMore)
            OutlinedButton.icon(
              onPressed: _loadMore,
              icon: const Icon(Icons.expand_more, size: 18),
              label: const Text('Charger plus'),
            ),
        ],
      ),
    );
  }

  Widget _buildFilter() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: Column(
        children: <Widget>[
          Row(
            children: <Widget>[
              const Text('Filtrer : '),
              const SizedBox(width: 8),
              Expanded(
                child: DropdownButton<String>(
                  value: _statusFilter ?? 'TOUTES',
                  isExpanded: true,
                  underline: const SizedBox.shrink(),
                  items: _statusOptions
                      .map((e) => DropdownMenuItem<String>(
                            value: e.key,
                            child: Text(e.value),
                          ))
                      .toList(),
                  onChanged: (value) {
                    setState(() => _statusFilter = value);
                    _load();
                  },
                ),
              ),
            ],
          ),
          const SizedBox(height: 4),
          TextField(
            onChanged: _searchChanged,
            decoration: const InputDecoration(
              isDense: true,
              prefixIcon: Icon(Icons.search, size: 20),
              hintText: 'Rechercher un email…',
              border: OutlineInputBorder(),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTile(InvitationItem item) {
    final pending = item.isPending;
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 6),
      child: ListTile(
        title: Text(item.email),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Text('Statut : ${_statusLabel(item.status)}'),
            Text('Rôle : ${item.role} · Portée : ${item.scopeType}'),
            Text('Expire le : ${_formatExpires(item.expiresAt)}'),
          ],
        ),
        isThreeLine: true,
        trailing: pending
            ? Wrap(
                spacing: 0,
                children: <Widget>[
                  IconButton(
                    icon: const Icon(Icons.send),
                    tooltip: 'Renvoyer',
                    onPressed: () => _resend(item),
                  ),
                  IconButton(
                    icon: const Icon(Icons.close),
                    tooltip: 'Annuler',
                    onPressed: () => _cancel(item),
                  ),
                ],
              )
            : null,
      ),
    );
  }

  String _statusLabel(String status) {
    switch (status) {
      case 'PENDING':
        return 'En attente';
      case 'ACCEPTED':
        return 'Acceptée';
      case 'EXPIRED':
        return 'Expirée';
      case 'CANCELED':
        return 'Annulée';
      case 'REVOKED':
        return 'Révoquée';
      default:
        return status;
    }
  }
}

/// Feuille de création d'invitation : possède son propre contrôleur et le
/// détruit à son démontage (cycle de vie Material correct, pas de dispose
/// prématuré pendant l'animation de fermeture).
///
/// Les rôles ET les nœuds organisationnels sont fournis par l'écran parent,
/// qui les a lus depuis l'API. Aucune liste n'est écrite ici : c'était
/// exactement la cause du 400 `INVITATION_SCOPE_INVALID` sur le scope
/// ORGANIZATION.
class _CreateInvitationSheet extends StatefulWidget {
  const _CreateInvitationSheet({
    required this.roles,
    required this.nodes,
    required this.loadingRefs,
    required this.onRetryRefs,
    this.refsError,
  });

  final List<AssignableRole> roles;
  final List<OrganizationNodeRef> nodes;
  final bool loadingRefs;
  final String? refsError;
  final VoidCallback onRetryRefs;

  @override
  State<_CreateInvitationSheet> createState() => _CreateInvitationSheetState();
}

class _CreateInvitationSheetState extends State<_CreateInvitationSheet> {
  final TextEditingController _emailController = TextEditingController();
  String? _role;
  String _scopeType = 'TENANT';
  String? _organizationNodeId;

  @override
  void initState() {
    super.initState();
    // Le rôle par défaut est le PREMIER RÔLE RENVoyÉ PAR L'API, pas une
    // constante : sans cela, le sélecteur affichait une valeur mais `_role`
    // restait nul et le bouton d'envoi restait inactif — l'écran était mort.
    if (widget.roles.isNotEmpty) _role = widget.roles.first.key;
  }

  @override
  void dispose() {
    _emailController.dispose();
    super.dispose();
  }

  void _submit() {
    Navigator.pop(context, <String, String>{
      'email': _emailController.text.trim(),
      if (_role != null) 'role': _role!,
      'scopeType': _scopeType,
      if (_organizationNodeId != null) 'organizationNodeId': _organizationNodeId!,
    });
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final organizationScope = _scopeType != 'TENANT';
    // Le serveur exige `organizationNodeId` pour tout scope non-TENANT : sans
    // nœud choisi, on n'envoie pas (et le bouton reste inactif).
    final nodeMissing = organizationScope && _organizationNodeId == null;
    final canSubmit = !widget.loadingRefs &&
        _role != null &&
        !nodeMissing &&
        _emailController.text.trim().isNotEmpty;

    return SafeArea(
      child: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(20, 20, 20, 20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[
            const Text(
              'Inviter un membre',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _emailController,
              key: const ValueKey<String>('invitation-email-field'),
              keyboardType: TextInputType.emailAddress,
              onChanged: (_) => setState(() {}),
              decoration: const InputDecoration(labelText: 'Adresse email'),
            ),
            const SizedBox(height: 12),
            if (widget.refsError != null) ...<Widget>[
              Card(
                color: theme.colorScheme.errorContainer,
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Text(
                        widget.refsError!,
                        style: TextStyle(color: theme.colorScheme.onErrorContainer),
                      ),
                      const SizedBox(height: 8),
                      OutlinedButton.icon(
                        onPressed: widget.onRetryRefs,
                        icon: const Icon(Icons.refresh, size: 18),
                        label: const Text('Réessayer'),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 12),
            ],
            if (widget.loadingRefs)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 12),
                child: LinearProgressIndicator(),
              )
            else if (widget.roles.isEmpty)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 12),
                child: Text(
                  "Aucun rôle assignable n'a été renvoyé par le serveur. "
                  "L'invitation ne peut pas être créée.",
                  style: TextStyle(color: theme.colorScheme.error),
                ),
              )
            else
              DropdownButtonFormField<String>(
                key: const ValueKey<String>('invitation-role-dropdown'),
                initialValue: _role ?? widget.roles.first.key,
                decoration: const InputDecoration(labelText: 'Rôle'),
                items: widget.roles
                    .map(
                      (r) => DropdownMenuItem<String>(
                        value: r.key,
                        child: Text(r.displayLabel),
                      ),
                    )
                    .toList(),
                onChanged: (value) => setState(() => _role = value),
              ),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              key: const ValueKey<String>('invitation-scope-dropdown'),
              initialValue: _scopeType,
              decoration: const InputDecoration(labelText: 'Portée'),
              items: const <DropdownMenuItem<String>>[
                DropdownMenuItem(value: 'TENANT', child: Text('Église')),
                DropdownMenuItem(
                    value: 'ORGANIZATION', child: Text('Organisation')),
              ],
              onChanged: (value) => setState(() {
                _scopeType = value ?? _scopeType;
                // Changer de portée invalide le nœud choisi : on le réinitialise
                // pour ne jamais envoyer un nœud d'une autre portée.
                _organizationNodeId = null;
              }),
            ),
            if (organizationScope) ...<Widget>[
              const SizedBox(height: 12),
              if (widget.nodes.isEmpty)
                Text(
                  'Aucun nœud organisationnel disponible pour cette église.',
                  style: TextStyle(color: theme.colorScheme.error),
                )
              else
                DropdownButtonFormField<String>(
                  key: const ValueKey<String>('invitation-node-dropdown'),
                  initialValue: _organizationNodeId,
                  isExpanded: true,
                  decoration: const InputDecoration(
                    labelText: 'Nœud organisationnel',
                    helperText: 'Obligatoire : le serveur refuse une portée '
                        'organisation sans nœud',
                  ),
                  items: widget.nodes
                      .map(
                        (n) => DropdownMenuItem<String>(
                          value: n.id,
                          child: Text(
                            n.displayLabel,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      )
                      .toList(),
                  onChanged: (value) =>
                      setState(() => _organizationNodeId = value),
                ),
            ],
            const SizedBox(height: 20),
            FilledButton(
              onPressed: canSubmit ? _submit : null,
              child: const Text('Envoyer l’invitation'),
            ),
            if (nodeMissing)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Text(
                  'Choisissez un nœud organisationnel pour continuer.',
                  style: TextStyle(color: theme.colorScheme.error, fontSize: 12),
                ),
              ),
          ],
        ),
      ),
    );
  }
}
