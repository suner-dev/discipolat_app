import 'dart:async';

import 'package:flutter/material.dart';

import '../../../data/models/member_relation.dart';
import '../../../data/services/api_service.dart';
import '../../../data/services/relation_service.dart';
import '../../../../l10n/app_localizations.dart';
import '../../widgets/glass_theme.dart';
import '../users/user_detail_screen.dart';

/// V231 — « Mon encadrement » (mobile) : le membre déclare lui-même ses
/// autorités spirituelles enregistrées (pasteur, supérieur, mentor…) selon le
/// paramétrage de son église (dictionnaire MEMBER_RELATION_TYPE). Le supérieur
/// rattaché reçoit la notification et retrouve ce membre dans « ses membres ».
/// Additif — ne remplace aucune section existante du profil.
class MyRelationsCard extends StatefulWidget {
  const MyRelationsCard({super.key, this.apiService});

  final ApiService? apiService;

  @override
  State<MyRelationsCard> createState() => _MyRelationsCardState();
}

class _MyRelationsCardState extends State<MyRelationsCard> {
  late final RelationService _service =
      RelationService(widget.apiService ?? ApiService());

  /// Page « mes encadrants » (liste bornée par le plafond serveur).
  RelationsSummary _relations = const RelationsSummary();

  /// Page « mes membres » — PAGINÉE côté serveur : un encadrement peut
  /// dépasser le plafond de rattachements entrants, une liste non bornée
  /// ferait exploser l'écran et la réponse.
  MemberRelationPage _members = const MemberRelationPage();

  List<RelationType> _types = const [];
  bool _isLoading = true;
  bool _isMembersLoading = false;
  bool _hasError = false;

  static const int _membersPageSize = 25;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load({int membersPage = 0}) async {
    if (membersPage != 0) setState(() => _isMembersLoading = true);
    try {
      final results = await Future.wait([
        _service.myRelations(),
        _service.types(),
        _service.membersPage(page: membersPage, size: _membersPageSize),
      ]);
      if (!mounted) return;
      setState(() {
        _relations = results[0] as RelationsSummary;
        _types = results[1] as List<RelationType>;
        _members = results[2] as MemberRelationPage;
        _isLoading = false;
        _isMembersLoading = false;
        _hasError = false;
      });
    } catch (_) {
      if (!mounted) return;
      // Échec explicite : jamais un profil qui affiche « aucun encadrant »
      // alors que c'est le réseau qui a échoué.
      setState(() {
        _isLoading = false;
        _isMembersLoading = false;
        _hasError = true;
      });
    }
  }

  /// Ouvre la fiche complète de la personne (exigence « cliquer dans la liste
  /// des utilisateurs »).
  void _openProfile(String userId) {
    if (userId.isEmpty) return;
    Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => UserDetailScreen(userId: userId)),
    );
  }

  Future<void> _revoke(MemberRelation relation) async {
    final l10n = AppLocalizations.of(context);
    try {
      await _service.revoke(relation.id);
      await _load();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(l10n.relationsRemovedToast)));
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text(l10n.saveFailed)));
      }
    }
  }

  Future<void> _openDeclareDialog() async {
    final l10n = AppLocalizations.of(context);
    final types = _types.isEmpty
        ? const [
            RelationType(code: 'PASTEUR', label: 'Mon pasteur'),
            RelationType(code: 'SUPERIEUR', label: 'Mon supérieur'),
            RelationType(code: 'MENTOR', label: 'Mon mentor'),
          ]
        : _types;
    String typeCode = types.first.code;
    Map<String, dynamic>? selected;
    String note = '';
    List<Map<String, dynamic>> candidates = const [];
    var searching = false;
    Timer? debounce;

    await showDialog<void>(
      context: context,
      builder: (dialogContext) => StatefulBuilder(
        builder: (context, setDialogState) {
          void onQueryChanged(String value) {
            selected = null;
            debounce?.cancel();
            debounce = Timer(const Duration(milliseconds: 400), () async {
              setDialogState(() => searching = true);
              try {
                final found = await _service.searchMembers(value);
                setDialogState(() {
                  candidates = found;
                  searching = false;
                });
              } catch (_) {
                setDialogState(() => searching = false);
              }
            });
          }

          return AlertDialog(
            title: Text(l10n.relationsTitle),
            content: SizedBox(
              width: 420,
              child: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(l10n.relationsType,
                        style: const TextStyle(fontSize: 12)),
                    const SizedBox(height: 6),
                    DropdownButtonFormField<String>(
                      initialValue: typeCode,
                      items: types
                          .map((t) => DropdownMenuItem(
                              value: t.code, child: Text(t.label)))
                          .toList(),
                      onChanged: (v) =>
                          setDialogState(() => typeCode = v ?? typeCode),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      decoration: InputDecoration(
                        labelText: l10n.relationsSearchMember,
                        prefixIcon: const Icon(Icons.search),
                        suffixIcon: searching
                            ? const Padding(
                                padding: EdgeInsets.all(12),
                                child: SizedBox(
                                    width: 16,
                                    height: 16,
                                    child:
                                        CircularProgressIndicator(strokeWidth: 2)),
                              )
                            : null,
                      ),
                      onChanged: onQueryChanged,
                    ),
                    if (selected == null && candidates.isNotEmpty)
                      ConstrainedBox(
                        constraints: const BoxConstraints(maxHeight: 160),
                        child: ListView(
                          shrinkWrap: true,
                          children: candidates.take(8).map((c) {
                            final name =
                                '${c['firstName'] ?? ''} ${c['lastName'] ?? ''}'
                                    .trim();
                            return ListTile(
                              dense: true,
                              title: Text(name.isEmpty
                                  ? '${c['email'] ?? ''}'
                                  : name),
                              subtitle: Text('${c['email'] ?? ''}'),
                              onTap: () => setDialogState(
                                  () => selected = c),
                            );
                          }).toList(),
                        ),
                      ),
                    if (selected != null)
                      Padding(
                        padding: const EdgeInsets.only(top: 6),
                        child: Chip(
                          avatar: const Icon(Icons.person, size: 16),
                          label: Text(
                              _displayNameOf(selected!)),
                          onDeleted: () =>
                              setDialogState(() => selected = null),
                        ),
                      ),
                    const SizedBox(height: 12),
                    TextField(
                      decoration:
                          InputDecoration(labelText: l10n.relationsNoteOptional),
                      maxLines: 2,
                      onChanged: (v) => note = v,
                    ),
                  ],
                ),
              ),
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(dialogContext).pop(),
                child: Text(l10n.cancel),
              ),
              FilledButton(
                onPressed: selected == null
                    ? null
                    : () async {
                        final messenger =
                            ScaffoldMessenger.of(context);
                        final messengerContext = context;
                        Navigator.of(dialogContext).pop();
                        try {
                          await _service.declare(
                              toUserId: '${selected!['id']}',
                              relationType: typeCode,
                              note: note);
                          await _load();
                          if (!mounted) return;
                          messenger.showSnackBar(SnackBar(
                              content: Text(l10n.relationsDeclaredToast)));
                        } catch (_) {
                          if (!mounted) return;
                          messenger.showSnackBar(
                              SnackBar(content: Text(l10n.saveFailed)));
                        }
                        // ignore: unnecessary_statements
                        messengerContext;
                      },
                child: Text(l10n.relationsSubmit),
              ),
            ],
          );
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final sortantes = _relations.sortantes;
    final members = _members.content;
    final white70 = Colors.white.withValues(alpha: 0.7);
    final white40 = Colors.white.withValues(alpha: 0.4);
    final white35 = Colors.white.withValues(alpha: 0.35);

    return GlassCard(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.favorite, color: Colors.pinkAccent, size: 22),
              const SizedBox(width: 12),
              Expanded(
                child: Text(l10n.relationsTitle,
                    style: const TextStyle(
                        color: Colors.white,
                        fontSize: 14,
                        fontWeight: FontWeight.w600)),
              ),
              IconButton(
                icon: const Icon(Icons.add_circle_outline,
                    color: Colors.pinkAccent),
                onPressed: _openDeclareDialog,
                tooltip: l10n.relationsAdd,
              ),
            ],
          ),
          Text(l10n.relationsHint,
              style: TextStyle(color: white40, fontSize: 11)),
          const SizedBox(height: 12),
          if (_isLoading)
            const Center(
                child: Padding(
                    padding: EdgeInsets.all(8),
                    child: SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(strokeWidth: 2))))
          else if (_hasError)
            Row(
              children: [
                Expanded(
                    child: Text(l10n.relationsLoadError,
                        style: TextStyle(color: white70, fontSize: 12))),
                TextButton(
                  onPressed: _load,
                  child: Text(l10n.relationsRetry),
                ),
              ],
            )
          else ...[
            Text(l10n.relationsMyCovering,
                style: TextStyle(
                    color: white70,
                    fontSize: 12,
                    fontWeight: FontWeight.w600)),
            const SizedBox(height: 6),
            if (sortantes.isEmpty)
              Text(l10n.relationsEmpty,
                  style: TextStyle(color: white35, fontSize: 12))
            else
              ...sortantes.map((r) => _RelationTile(
                    relation: r,
                    icon: Icons.shield,
                    iconColor: Colors.pinkAccent,
                    l10n: l10n,
                    onOpenProfile: _openProfile,
                    onRevoke: r.revocable ? _revoke : null,
                  )),
            const GlassDivider(),
            Row(
              children: [
                Expanded(
                    child: Text(l10n.relationsMyMembers,
                        style: TextStyle(
                            color: white70,
                            fontSize: 12,
                            fontWeight: FontWeight.w600))),
                if (_members.totalElements > 0)
                  Text('${_members.totalElements} ${l10n.relationsCount}',
                      style: TextStyle(color: white40, fontSize: 11)),
              ],
            ),
            const SizedBox(height: 6),
            if (members.isEmpty)
              Text(l10n.relationsEmptyMembers,
                  style: TextStyle(color: white35, fontSize: 12))
            else
              ...members.map((r) => _RelationTile(
                    relation: r,
                    icon: Icons.person_outline,
                    iconColor: Colors.greenAccent,
                    l10n: l10n,
                    onOpenProfile: _openProfile,
                    // Détacher un membre n'est proposé que si le serveur
                    // l'autorise (évite un échec 403 en cours de route).
                    onRevoke: r.revocable ? _revoke : null,
                    revokeLabel: r.revocable ? l10n.relationsDetach : null,
                  )),
            if (_members.totalPages > 1) ...[
              const SizedBox(height: 6),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  TextButton.icon(
                    onPressed: _isMembersLoading ||
                            !_members.hasPrevious ||
                            _members.number == 0
                        ? null
                        : () => _load(membersPage: _members.number - 1),
                    icon: const Icon(Icons.chevron_left, size: 18),
                    label: Text(l10n.relationsPreviousPage),
                  ),
                  Text('${_members.number + 1} / ${_members.totalPages}',
                      style: TextStyle(color: white40, fontSize: 11)),
                  TextButton.icon(
                    onPressed: _isMembersLoading ||
                            !_members.hasNext ||
                            _members.number + 1 >= _members.totalPages
                        ? null
                        : () => _load(membersPage: _members.number + 1),
                    icon: const Icon(Icons.chevron_right, size: 18),
                    label: Text(l10n.relationsNextPage),
                  ),
                ],
              ),
            ],
          ],
        ],
      ),
    );
  }
}

/// Ligne de rattachement : nom cliquable (ouvre la fiche), libellé du type,
/// et action de retrait uniquement si le serveur l'autorise.
class _RelationTile extends StatelessWidget {
  const _RelationTile({
    required this.relation,
    required this.icon,
    required this.iconColor,
    required this.l10n,
    required this.onOpenProfile,
    this.onRevoke,
    this.revokeLabel,
  });

  final MemberRelation relation;
  final IconData icon;
  final Color iconColor;
  final AppLocalizations l10n;
  final void Function(String userId) onOpenProfile;
  final Future<void> Function(MemberRelation)? onRevoke;
  final String? revokeLabel;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        children: [
          Icon(icon, size: 16, color: iconColor),
          const SizedBox(width: 8),
          Expanded(
            child: InkWell(
              onTap: () => onOpenProfile(relation.otherUserId),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    relation.otherNom,
                    style: const TextStyle(color: Colors.white, fontSize: 13),
                  ),
                  Text(
                    relation.typeLabel,
                    style: TextStyle(
                        color: Colors.white.withValues(alpha: 0.4),
                        fontSize: 11),
                  ),
                ],
              ),
            ),
          ),
          if (onRevoke != null)
            IconButton(
              icon: const Icon(Icons.link_off, size: 18, color: Colors.redAccent),
              tooltip: revokeLabel ?? l10n.relationsRemove,
              onPressed: () => onRevoke!(relation),
            ),
        ],
      ),
    );
  }
}

/// Nom lisible d'un membre (prénom nom, sinon e-mail).
String _displayNameOf(Map<String, dynamic> user) {
  final full = '${user['firstName'] ?? ''} ${user['lastName'] ?? ''}'.trim();
  if (full.isNotEmpty) return full;
  final email = '${user['email'] ?? ''}'.trim();
  return email.isEmpty ? '—' : email;
}
