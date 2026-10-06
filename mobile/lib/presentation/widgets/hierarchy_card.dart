/// V231 — ARBRE de hiérarchie d'une personne (mobile).
///
/// Reflet de `frontend/src/components/relations/HierarchyTree.tsx` : une
/// racine par branche organisationnelle, la chaîne d'ascendance en dessous,
/// chaque responsable directement cliquable (sa fiche s'ouvre)..widget testable
/// isolément, sans dépendre de l'écran appelant.
library;

import 'package:flutter/material.dart';

import '../../../../l10n/app_localizations.dart';
import '../../../data/models/member_relation.dart';

/// Arbre des branches organisationnelles + ascendants unifiés + encadrement
/// pastoral. Rendu adaptatif : le repli `branches` est obligatoire, le reste
/// est conditionné par ce que le serveur a réellement pu calculer.
class HierarchyCard extends StatelessWidget {
  const HierarchyCard({
    super.key,
    required this.hierarchy,
    this.relations,
    this.onOpenProfile,
  });

  final Map<String, dynamic> hierarchy;
  final Map<String, dynamic>? relations;
  final void Function(String userId)? onOpenProfile;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final branches = RelationServiceParsing.branchesOf(hierarchy);
    final sortantes = _rows(relations?['sortantes']);
    final entrantes = _rows(relations?['entrantes']);
    // L'agrégat `ascendants` fusionne déjà organisation ∪ déclaratif, mais on
    // ne fait PAS confiance à cette fusion : si elle est absente ou dégradée,
    // les encadrants déclarés seraient invisibles alors qu'ils existent dans
    // `relations.sortantes`. On complète donc, sans doublon.
    final ascendants = _mergedAscendants(hierarchy, sortantes);
    final suivi = _map(hierarchy['suivi']);
    final resume = _map(hierarchy['resume']);

    final white70 = Colors.white.withValues(alpha: 0.7);
    final white40 = Colors.white.withValues(alpha: 0.4);
    final white35 = Colors.white.withValues(alpha: 0.35);

    final hasAnything = branches.isNotEmpty ||
        ascendants.isNotEmpty ||
        sortantes.isNotEmpty ||
        entrantes.isNotEmpty;
    if (!hasAnything && suivi.isEmpty) return const SizedBox.shrink();

    return GlassCardLike(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.account_tree,
                  color: Colors.pinkAccent, size: 20),
              const SizedBox(width: 8),
              Expanded(
                child: Text(l10n.hierarchyTitle,
                    style: const TextStyle(
                        color: Colors.white,
                        fontSize: 14,
                        fontWeight: FontWeight.w600)),
              ),
            ],
          ),
          const SizedBox(height: 10),

          // ── Ascendants unifiés (organisation ∪ déclaratif) ──────────────
          if (ascendants.isNotEmpty) ...[
            Text(l10n.hierarchyAscendants,
                style: TextStyle(
                    color: white70, fontSize: 12, fontWeight: FontWeight.w600)),
            const SizedBox(height: 4),
            Wrap(
              spacing: 6,
              runSpacing: 4,
              children: ascendants.map((a) {
                return _PersonChip(
                  label: a.nom,
                  meta: a.isDeclared
                      ? (a.typeLabel ?? l10n.hierarchyViaDeclared)
                      : (a.noeud ?? l10n.hierarchyViaOrg),
                  icon: a.isDeclared ? Icons.favorite : Icons.shield,
                  iconColor:
                      a.isDeclared ? Colors.pinkAccent : Colors.lightBlueAccent,
                  onTap: onOpenProfile == null || a.id.isEmpty
                      ? null
                      : () => onOpenProfile!(a.id),
                );
              }).toList(),
            ),
            const SizedBox(height: 10),
          ],

          // ── ARBRE des branches ─────────────────────────────────────────
          Text(l10n.hierarchyBranches,
              style: TextStyle(
                  color: white70, fontSize: 12, fontWeight: FontWeight.w600)),
          const SizedBox(height: 4),
          if (branches.isEmpty)
            Text(l10n.hierarchyBranchesEmpty,
                style: TextStyle(color: white35, fontSize: 11))
          else
            ...branches.map((b) => _BranchNode(branch: b, onOpenProfile: onOpenProfile)),

          // ── Encadrement pastoral ───────────────────────────────────────
          if (suivi.isNotEmpty) ...[
            const SizedBox(height: 10),
            Text(l10n.hierarchySuivi,
                style: TextStyle(
                    color: white70, fontSize: 12, fontWeight: FontWeight.w600)),
            const SizedBox(height: 4),
            _SuiviRow(
                label: l10n.hierarchyFaiseur,
                personId: _idOf(suivi['faiseur']),
                name: _nameOf(suivi['faiseur']),
                onOpenProfile: onOpenProfile),
            _SuiviRow(
                label: l10n.hierarchyChefDeFamille,
                personId: _idOf(suivi['chefDeFamille']),
                name: _nameOf(suivi['chefDeFamille']) ??
                    _nameOf(suivi['chefDeFamille'], key: 'chefFamilleNom'),
                onOpenProfile: onOpenProfile),
            _SuiviRow(
                label: l10n.hierarchyDepartmentsLed,
                name: _namesOf(suivi['departementsDiriges']),
                onOpenProfile: onOpenProfile),
            _SuiviRow(
                label: l10n.hierarchyNodesLed,
                name: _namesOf(suivi['noeudsDiriges']),
                onOpenProfile: onOpenProfile),
            if (suivi['amesSuiviesTotal'] is num &&
                (suivi['amesSuiviesTotal'] as num) > 0)
              Text('${l10n.hierarchySoulsFollowed}: ${suivi['amesSuiviesTotal']}',
                  style: TextStyle(color: white40, fontSize: 11)),
          ],

          // ── Membres rattachés ──────────────────────────────────────────
          if (entrantes.isNotEmpty) ...[
            const SizedBox(height: 10),
            Row(
              children: [
                Expanded(
                    child: Text(l10n.relationsMyMembers,
                        style: TextStyle(
                            color: white70,
                            fontSize: 12,
                            fontWeight: FontWeight.w600))),
                Text('${resume['membresRattaches'] ?? entrantes.length}',
                    style: TextStyle(color: white40, fontSize: 11)),
              ],
            ),
            const SizedBox(height: 4),
            Wrap(
              spacing: 6,
              runSpacing: 4,
              children: entrantes.map((r) {
                final otherId = '${r['otherUserId'] ?? ''}';
                return _PersonChip(
                  label: '${r['otherNom'] ?? '—'}',
                  meta: '${r['typeLabel'] ?? ''}',
                  icon: Icons.person_outline,
                  iconColor: Colors.greenAccent,
                  onTap: onOpenProfile == null || otherId.isEmpty
                      ? null
                      : () => onOpenProfile!(otherId),
                );
              }).toList(),
            ),
          ],
        ],
      ),
    );
  }

  static List<Map<String, dynamic>> _rows(Object? raw) =>
      raw is List ? raw.whereType<Map<String, dynamic>>().toList() : const [];

  /// Ascendants unifiés : ceux de l'agrégat, complétés par les encadrants
  /// déclarés absents de la fusion (dédupliqués sur l'identifiant personne).
  static List<Ascendant> _mergedAscendants(
      Map<String, dynamic> hierarchy, List<Map<String, dynamic>> sortantes) {
    final merged = List<Ascendant>.from(
        RelationServiceParsing.ascendantsOf(hierarchy));
    final known = merged.map((a) => a.id).toSet();
    for (final r in sortantes) {
      final id = '${r['otherUserId'] ?? ''}';
      if (id.isEmpty || known.contains(id)) continue;
      known.add(id);
      merged.add(Ascendant(
        id: id,
        nom: '${r['otherNom'] ?? '—'}',
        via: 'DECLARATIF',
        typeLabel: r['typeLabel']?.toString() ?? r['relationType']?.toString(),
      ));
    }
    return merged;
  }

  static Map<String, dynamic> _map(Object? raw) =>
      raw is Map<String, dynamic> ? raw : const {};

  static String? _idOf(Object? raw) =>
      raw is Map ? raw['id']?.toString() : null;

  static String? _nameOf(Object? raw, {String key = 'nom'}) {
    if (raw is! Map) return null;
    final value = raw[key];
    if (value == null || '$value'.isEmpty) return null;
    return '$value';
  }

  static String? _namesOf(Object? raw) {
    if (raw is! List || raw.isEmpty) return null;
    final names = raw
        .whereType<Map>()
        .map((m) => m['nom'])
        .where((n) => n != null && '$n'.isNotEmpty)
        .map((n) => '$n')
        .toList();
    return names.isEmpty ? null : names.join(', ');
  }
}

/// Une branche = racine + chaîne d'ascendance dépliable.
class _BranchNode extends StatefulWidget {
  const _BranchNode({required this.branch, this.onOpenProfile});

  final HierarchyBranch branch;
  final void Function(String userId)? onOpenProfile;

  @override
  State<_BranchNode> createState() => _BranchNodeState();
}

class _BranchNodeState extends State<_BranchNode> {
  bool _expanded = true;

  @override
  Widget build(BuildContext context) {
    final steps = widget.branch.steps;
    final meta = [widget.branch.nodeType, widget.branch.origine]
        .where((e) => e != null && e.isNotEmpty)
        .join(' · ');

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            if (steps.isNotEmpty)
              InkWell(
                onTap: () => setState(() => _expanded = !_expanded),
                child: Padding(
                  padding: const EdgeInsets.only(right: 2),
                  child: Icon(
                      _expanded
                          ? Icons.keyboard_arrow_down
                          : Icons.keyboard_arrow_right,
                      size: 16,
                      color: Colors.white.withValues(alpha: 0.5)),
                ),
              )
            else
              const SizedBox(width: 18),
            Icon(Icons.account_tree_outlined,
                size: 14, color: Colors.amberAccent.withValues(alpha: 0.9)),
            const SizedBox(width: 4),
            Expanded(
              child: Text(widget.branch.nodeNom,
                  style: const TextStyle(color: Colors.white, fontSize: 12)),
            ),
            if (meta.isNotEmpty)
              Text(meta,
                  style: TextStyle(
                      color: Colors.white.withValues(alpha: 0.4), fontSize: 10)),
          ],
        ),
        if (_expanded)
          ...steps
              .map((s) => Container(
                    margin: const EdgeInsetsDirectional.only(start: 22),
                    padding: const EdgeInsetsDirectional.only(start: 8, top: 3),
                    decoration: BoxDecoration(
                      border: BorderDirectional(
                        start: BorderSide(
                            color: Colors.white.withValues(alpha: 0.12),
                            width: 1),
                      ),
                    ),
                    child: Row(
                      children: [
                        Icon(Icons.subdirectory_arrow_right,
                            size: 12,
                            color: Colors.white.withValues(alpha: 0.35)),
                        const SizedBox(width: 4),
                        Expanded(
                          child: Text(s.nom,
                              style: TextStyle(
                                  color: Colors.white.withValues(alpha: 0.6),
                                  fontSize: 11)),
                        ),
                        if (s.responsableId != null && s.responsableId!.isNotEmpty)
                          TextButton(
                            style: TextButton.styleFrom(
                              padding: EdgeInsets.zero,
                              minimumSize: const Size(0, 24),
                              tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                            ),
                            onPressed: widget.onOpenProfile == null
                                ? null
                                : () => widget.onOpenProfile!(s.responsableId!),
                            child: Text(
                              s.responsableNom ?? '—',
                              style: const TextStyle(
                                  color: Colors.pinkAccent, fontSize: 11),
                            ),
                          ),
                      ],
                    ),
                  )),
      ],
    );
  }
}

/// Pastille personne : cliquable seulement si l'identifiant est exploitable.
class _PersonChip extends StatelessWidget {
  const _PersonChip({
    required this.label,
    required this.icon,
    required this.iconColor,
    this.meta,
    this.onTap,
  });

  final String label;
  final String? meta;
  final IconData icon;
  final Color iconColor;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final content = Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 12, color: iconColor),
        const SizedBox(width: 4),
        Flexible(
          child: Text(label,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(color: Colors.white, fontSize: 11)),
        ),
        if (meta != null && meta!.isNotEmpty) ...[
          const SizedBox(width: 4),
          Text(meta!,
              style: TextStyle(
                  color: Colors.white.withValues(alpha: 0.4), fontSize: 9)),
        ],
      ],
    );
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.06),
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: Colors.white.withValues(alpha: 0.12)),
      ),
      child: onTap == null
          ? content
          : InkWell(onTap: onTap, child: content),
    );
  }
}

/// Ligne libellé / valeur du bloc « encadrement pastoral ».
class _SuiviRow extends StatelessWidget {
  const _SuiviRow({
    required this.label,
    required this.onOpenProfile,
    this.name,
    this.personId,
  });

  final String label;
  final String? name;
  final String? personId;
  final void Function(String userId)? onOpenProfile;

  @override
  Widget build(BuildContext context) {
    if (name == null || name!.isEmpty) return const SizedBox.shrink();
    final text = Text(
      name!,
      style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 11),
    );
    final clickable = onOpenProfile != null && personId != null && personId!.isNotEmpty;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 1),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('$label : ',
              style: TextStyle(
                  color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
          Expanded(
            child: clickable
                ? InkWell(
                    onTap: () => onOpenProfile!(personId!),
                    child: text,
                  )
                : text,
          ),
        ],
      ),
    );
  }
}

/// Emballage visuel aligné sur les autres cartes de l'application.
class GlassCardLike extends StatelessWidget {
  const GlassCardLike({super.key, required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.06),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.white.withValues(alpha: 0.10)),
      ),
      child: child,
    );
  }
}

/// Parsing local des agrégats, isolé pour être testable sans service HTTP.
class RelationServiceParsing {
  const RelationServiceParsing._();

  static List<HierarchyBranch> branchesOf(Map<String, dynamic> hierarchy) {
    final raw = hierarchy['branches'];
    if (raw is! List) return const [];
    return raw
        .whereType<Map<String, dynamic>>()
        .map(HierarchyBranch.fromJson)
        .toList();
  }

  static List<Ascendant> ascendantsOf(Map<String, dynamic> hierarchy) {
    final raw = hierarchy['ascendants'];
    if (raw is! List) return const [];
    return raw
        .whereType<Map<String, dynamic>>()
        .map(Ascendant.fromJson)
        .where((a) => a.id.isNotEmpty)
        .toList();
  }
}

/// Affiche un message explicite quand le backend a dégradé la hiérarchie :
/// sans ce marqueur, l'utilisateur verrait une carte vide sans explication.
class HierarchyUnavailableCard extends StatelessWidget {
  const HierarchyUnavailableCard({super.key});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.amberAccent.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(16),
        border:
            Border.all(color: Colors.amberAccent.withValues(alpha: 0.25)),
      ),
      child: Row(
        children: [
          const Icon(Icons.warning_amber_rounded,
              color: Colors.amberAccent, size: 20),
          const SizedBox(width: 10),
          Expanded(
            child: Text(l10n.hierarchyUnavailable,
                style: TextStyle(
                    color: Colors.white.withValues(alpha: 0.75),
                    fontSize: 12)),
          ),
        ],
      ),
    );
  }
}
