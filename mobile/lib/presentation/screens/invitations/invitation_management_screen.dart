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

  static const List<MapEntry<String, String>> _statusOptions =
      <MapEntry<String, String>>[
    MapEntry('TOUTES', 'Toutes'),
    MapEntry('PENDING', 'En attente'),
    MapEntry('ACCEPTED', 'Acceptées'),
    MapEntry('EXPIRED', 'Expirées'),
    MapEntry('CANCELED', 'Annulées'),
  ];

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final result = await _service.list(
        status: (_statusFilter == null || _statusFilter == 'TOUTES')
            ? null
            : _statusFilter,
      );
      if (!mounted) return;
      setState(() {
        _items = result.items;
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = 'Impossible de charger les invitations.';
      });
    }
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
        child: const _CreateInvitationSheet(),
      ),
    );
    if (submitted == null) return; // feuille fermée sans envoi
    await _create(
      email: submitted['email'] ?? '',
      role: submitted['role'] ?? 'MEMBRE',
      scopeType: submitted['scopeType'] ?? 'TENANT',
    );
  }

  Future<void> _create({
    required String email,
    required String role,
    required String scopeType,
  }) async {
    final trimmed = email.trim();
    if (trimmed.isEmpty) {
      _snack('Adresse email requise');
      return;
    }
    try {
      final result = await _service.create(
        email: trimmed,
        role: role,
        scopeType: scopeType,
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
    } catch (_) {
      _snack("L'invitation a échoué.");
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
    } catch (_) {
      _snack('Renvoi impossible.');
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
    } catch (_) {
      _snack('Annulation impossible.');
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
        if (_items.isEmpty)
          const Expanded(
            child: Center(
              child: Text('Aucune invitation'),
            ),
          )
        else
          Expanded(
            child: RefreshIndicator(
              onRefresh: _load,
              child: ListView.builder(
                padding: const EdgeInsets.fromLTRB(12, 4, 12, 96),
                itemCount: _items.length,
                itemBuilder: (context, index) => _buildTile(_items[index]),
              ),
            ),
          ),
      ],
    );
  }

  Widget _buildFilter() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: Row(
        children: <Widget>[
          const Text('Filtrer : '),
          const SizedBox(width: 8),
          DropdownButton<String>(
            value: _statusFilter ?? 'TOUTES',
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
      default:
        return status;
    }
  }
}

/// Feuille de création d'invitation : possède son propre contrôleur et le
/// détruit à son démontage (cycle de vie Material correct, pas de dispose
/// prématuré pendant l'animation de fermeture).
class _CreateInvitationSheet extends StatefulWidget {
  const _CreateInvitationSheet();

  @override
  State<_CreateInvitationSheet> createState() => _CreateInvitationSheetState();
}

class _CreateInvitationSheetState extends State<_CreateInvitationSheet> {
  final TextEditingController _emailController = TextEditingController();
  String _role = 'MEMBRE';
  String _scopeType = 'TENANT';

  @override
  void dispose() {
    _emailController.dispose();
    super.dispose();
  }

  void _submit() {
    Navigator.pop(context, <String, String>{
      'email': _emailController.text.trim(),
      'role': _role,
      'scopeType': _scopeType,
    });
  }

  @override
  Widget build(BuildContext context) {
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
              keyboardType: TextInputType.emailAddress,
              decoration: const InputDecoration(labelText: 'Adresse email'),
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              initialValue: _role,
              decoration: const InputDecoration(labelText: 'Rôle'),
              items: const <DropdownMenuItem<String>>[
                DropdownMenuItem(value: 'MEMBRE', child: Text('Membre')),
                DropdownMenuItem(value: 'FAISEUR', child: Text('Faiseur')),
                DropdownMenuItem(
                    value: 'CHEF_DE_FAMILLE', child: Text('Chef de famille')),
                DropdownMenuItem(
                    value: 'RESPONSABLE', child: Text('Responsable')),
              ],
              onChanged: (value) => setState(() => _role = value ?? _role),
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              initialValue: _scopeType,
              decoration: const InputDecoration(labelText: 'Portée'),
              items: const <DropdownMenuItem<String>>[
                DropdownMenuItem(value: 'TENANT', child: Text('Église')),
                DropdownMenuItem(
                    value: 'ORGANIZATION', child: Text('Organisation')),
              ],
              onChanged: (value) =>
                  setState(() => _scopeType = value ?? _scopeType),
            ),
            const SizedBox(height: 20),
            FilledButton(
              onPressed: _submit,
              child: const Text('Envoyer l’invitation'),
            ),
          ],
        ),
      ),
    );
  }
}
