import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/open_url.dart';
import '../../../l10n/app_localizations.dart';

/// Fiche utilisateur complète (mobile) : identité, âme liée, âmes suivies si
/// faiseur, départements + membres si responsable, famille gérée si chef de
/// famille, et évaluation (donner si absente / modifier si présente).
class UserDetailScreen extends StatefulWidget {
  const UserDetailScreen({super.key, required this.userId, this.apiService});

  final String userId;
  final ApiService? apiService;

  @override
  State<UserDetailScreen> createState() => _UserDetailScreenState();
}

class _UserDetailScreenState extends State<UserDetailScreen> {
  late final ApiService _apiService = widget.apiService ?? ApiService();
  Map<String, dynamic>? _detail;
  bool _isLoading = true;
  bool _isSaving = false;

  // Formulaire d'évaluation
  int _note = 0;
  int _hoverNote = 0;
  final TextEditingController _commentCtrl = TextEditingController();

  AppLocalizations get l10n => AppLocalizations.of(context)!;

  static const _roleLabels = {
    'ADMIN': 'Administrateur',
    'PASTEUR': 'Pasteur',
    'RESPONSABLE': 'Responsable',
    'CHEF_DE_FAMILLE': 'Chef de famille',
    'FAISEUR': 'Faiseur de disciples',
    'MEMBRE': 'Membre',
  };

  static const _roleColors = {
    'ADMIN': Color(0xFF3B82F6),
    'PASTEUR': Color(0xFF8B5CF6),
    'RESPONSABLE': Color(0xFFF59E0B),
    'CHEF_DE_FAMILLE': Color(0xFFF59E0B),
    'FAISEUR': Color(0xFF22C55E),
    'MEMBRE': Color(0xFF6B7280),
  };

  static const _statutColors = {
    'ACTIF': Color(0xFF22C55E),
    'EN_INTEGRATION': Color(0xFFF59E0B),
    'EN_VEILLE': Color(0xFF3B82F6),
    'DECROCHE': Color(0xFFEF4444),
  };

  static const _catLabels = {
    'RESPONSABLE': 'Responsable',
    'CHEF_FAMILLE': 'Chef de famille',
    'FAISEUR': 'Faiseur',
    'MEMBRE': 'Membre',
  };

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _commentCtrl.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() => _isLoading = true);
    try {
      final res = await _apiService.get('/users/${widget.userId}/detail');
      if (mounted) {
        final detail = res.data as Map<String, dynamic>;
        // Pré-remplir avec MA dernière évaluation
        final mine = (detail['monEvaluation'] as List?) ?? [];
        if (mine.isNotEmpty) {
          final first = mine.first as Map<String, dynamic>;
          _note = (first['note'] as num?)?.toInt() ?? 0;
          _commentCtrl.text = (first['commentaire'] as String?) ?? '';
        }
        setState(() {
          _detail = detail;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _saveEvaluation() async {
    if (_note < 1 || _note > 5) return;
    setState(() => _isSaving = true);
    try {
      await _apiService.put('/evaluations/${widget.userId}', data: {
        'note': _note,
        'commentaire': _commentCtrl.text.trim().isEmpty ? null : _commentCtrl.text.trim(),
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(_hasMyEval ? l10n.userDetailEvalModified : l10n.userDetailEvalSaved)),
        );
        await _load();
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(l10n.userDetailEvalError)));
      }
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  bool get _hasMyEval => ((_detail?['monEvaluation'] as List?) ?? []).isNotEmpty;

  Color _roleColor(String? role) => _roleColors[role] ?? Colors.grey;
  Color _statutColor(String? statut) => _statutColors[statut] ?? Colors.grey;
  String _statutLabel(String? statut) {
    switch (statut) {
      case 'ACTIF': return 'Actif';
      case 'EN_INTEGRATION': return 'Intégration';
      case 'EN_VEILLE': return 'Veille';
      case 'DECROCHE': return 'Décroché';
      default: return statut ?? '—';
    }
  }

  String _formatDate(dynamic date) {
    if (date == null) return '—';
    final s = date.toString();
    return s.length >= 10 ? '${s.substring(8, 10)}/${s.substring(5, 7)}/${s.substring(0, 4)}' : s;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.userDetailAppTitle),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _load),
        ],
      ),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 5)
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                physics: const AlwaysScrollableScrollPhysics(),
                padding: const EdgeInsets.all(16),
                children: _buildSections(),
              ),
            ),
    );
  }

  List<Widget> _buildSections() {
    final d = _detail;
    if (d == null) {
      return [
        GlassCard(
          padding: const EdgeInsets.all(32),
          child: Column(children: [
            Icon(Icons.person_off, size: 48, color: Colors.white.withValues(alpha: 0.15)),
            const SizedBox(height: 12),
            Text(l10n.userDetailUnavailable, style: TextStyle(color: Colors.white.withValues(alpha: 0.4))),
          ]),
        ),
      ];
    }

    final sections = <Widget>[
      _buildIdentityCard(d),
      const SizedBox(height: 12),
      if (d['ame'] != null) ...[_buildAmeCard(d['ame'] as Map<String, dynamic>), const SizedBox(height: 12)],
      _buildEvaluationCard(d),
      const SizedBox(height: 12),
      if (d['amesSuivies'] is List) ...[_buildAmesSuivies(d), const SizedBox(height: 12)],
      if (d['sorties'] is List && (d['sorties'] as List).isNotEmpty) ...[_buildSorties(d), const SizedBox(height: 12)],
      if (d['departements'] is List) ...[_buildDepartements(d), const SizedBox(height: 12)],
      if (d['familleGeree'] != null) ...[_buildFamilleGeree(d['familleGeree'] as Map<String, dynamic>), const SizedBox(height: 12)],
      if ((d['dossier'] is List && (d['dossier'] as List).isNotEmpty) ||
          (d['dossierDocuments'] is List && (d['dossierDocuments'] as List).isNotEmpty))
        ...[_buildDossier(d), const SizedBox(height: 12)],
    ];
    return sections;
  }

  Widget _buildIdentityCard(Map<String, dynamic> d) {
    final role = (d['role'] as String?) ?? 'MEMBRE';
    final color = _roleColor(role);
    final name = '${d['firstName'] ?? ''} ${d['lastName'] ?? ''}';
    return GlassCard(
      padding: const EdgeInsets.all(16),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          Container(
            width: 56, height: 56,
            decoration: BoxDecoration(
              gradient: LinearGradient(colors: [color, color.withValues(alpha: 0.7)]),
              borderRadius: BorderRadius.circular(16),
            ),
            child: Center(child: Text(initialsFromName(name), style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 18))),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(name.trim().isEmpty ? '—' : name, style: const TextStyle(color: Colors.white, fontSize: 17, fontWeight: FontWeight.bold)),
              const SizedBox(height: 4),
              Text('${d['email'] ?? ''}', style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 12)),
            ]),
          ),
          if (d['statut'] == 'ACTIVE')
            Container(width: 10, height: 10, decoration: BoxDecoration(shape: BoxShape.circle, color: Colors.green, boxShadow: [BoxShadow(color: Colors.green.withValues(alpha: 0.5), blurRadius: 6)]))
          else
            Container(width: 10, height: 10, decoration: const BoxDecoration(shape: BoxShape.circle, color: Colors.grey)),
        ]),
        const SizedBox(height: 12),
        Wrap(spacing: 6, runSpacing: 6, children: [
          StatusBadge(label: _roleLabels[role] ?? role, color: color),
          StatusBadge(label: d['statut'] == 'ACTIVE' ? 'Actif' : 'Inactif', color: d['statut'] == 'ACTIVE' ? Colors.green : Colors.grey),
          if (d['estChefDeFamille'] == true) StatusBadge(label: 'Chef de famille', color: const Color(0xFFF59E0B)),
          if (d['dateCreation'] != null)
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
              decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.05), borderRadius: BorderRadius.circular(20)),
              child: Row(mainAxisSize: MainAxisSize.min, children: [
                const Icon(Icons.calendar_today, size: 11, color: Colors.white38),
                const SizedBox(width: 4),
                Text('Depuis ${_formatDate(d['dateCreation'])}', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
              ]),
            ),
        ]),
      ]),
    );
  }

  Widget _buildAmeCard(Map<String, dynamic> ame) {
    return GlassCard(
      padding: const EdgeInsets.all(14),
      borderColor: AppColors.primary.withValues(alpha: 0.25),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          Icon(Icons.person_outline, color: AppColors.primary, size: 16),
          const SizedBox(width: 6),
          Text(l10n.userDetailLinkedAme, style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 12, fontWeight: FontWeight.w600)),
        ]),
        const SizedBox(height: 10),
        _infoRow(l10n.userDetailName, ame['nomComplet']?.toString() ?? '—'),
        _infoRow(l10n.userDetailStatus, _statutLabel(ame['statut']?.toString()), color: _statutColor(ame['statut']?.toString())),
        _infoRow(l10n.userDetailType, (ame['typeDisciple']?.toString() ?? '—').replaceAll('_', ' ')),
        _infoRow(l10n.userDetailFamily, ame['familleNom']?.toString() ?? 'Sans famille'),
        if (ame['faiseurNom'] != null) _infoRow(l10n.userDetailFaiseur, ame['faiseurNom'].toString()),
      ]),
    );
  }

  Widget _infoRow(String label, String value, {Color? color}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(children: [
        SizedBox(width: 72, child: Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.35), fontSize: 12))),
        Expanded(
          child: Text(value, maxLines: 1, overflow: TextOverflow.ellipsis,
              style: TextStyle(color: color ?? Colors.white.withValues(alpha: 0.8), fontSize: 13, fontWeight: FontWeight.w500)),
        ),
      ]),
    );
  }

  Widget _buildEvaluationCard(Map<String, dynamic> d) {
    final evaluations = (d['evaluations'] as Map<String, dynamic>?) ?? {};
    return GlassCard(
      padding: const EdgeInsets.all(14),
      borderColor: Colors.amber.withValues(alpha: 0.25),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          const Icon(Icons.star, color: Colors.amber, size: 16),
          const SizedBox(width: 6),
          Expanded(
            child: Text(l10n.userDetailEvaluation, style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 12, fontWeight: FontWeight.w600)),
          ),
          if (_hasMyEval)
            StatusBadge(label: l10n.userDetailEvaluated, color: Colors.green)
          else
            StatusBadge(label: l10n.userDetailNotEvaluated, color: Colors.white38),
        ]),
        const SizedBox(height: 12),
        // Étoiles interactives
        Row(
          children: List.generate(5, (i) {
            final star = i + 1;
            final filled = star <= (_hoverNote > 0 ? _hoverNote : _note);
            return GestureDetector(
              onTap: () => setState(() => _note = star),
              onTapDown: (_) => setState(() => _hoverNote = star),
              onTapUp: (_) => setState(() => _hoverNote = 0),
              onTapCancel: () => setState(() => _hoverNote = 0),
              child: Icon(
                filled ? Icons.star_rounded : Icons.star_border_rounded,
                color: filled ? Colors.amber : Colors.white24,
                size: 34,
              ),
            );
          }),
        ),
        const SizedBox(height: 8),
        TextField(
          controller: _commentCtrl,
          maxLines: 2,
          minLines: 1,
          decoration: InputDecoration(
            hintText: l10n.userDetailEvalHint,
          ),
        ),
        const SizedBox(height: 10),
        Row(mainAxisAlignment: MainAxisAlignment.end, children: [
          FilledButton.icon(
            onPressed: (_note < 1 || _isSaving) ? null : _saveEvaluation,
            icon: _isSaving
                ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                : const Icon(Icons.send, size: 16),
            label: Text(_hasMyEval ? l10n.userDetailEvalEdit : l10n.userDetailEvalCreate),
          ),
        ]),
        // Statistiques reçues
        if (evaluations.isNotEmpty) ...[
          const SizedBox(height: 12),
          Divider(color: Colors.white.withValues(alpha: 0.06)),
          const SizedBox(height: 8),
          Text(l10n.userDetailEvalReceived, style: TextStyle(color: Colors.white.withValues(alpha: 0.35), fontSize: 11)),
          const SizedBox(height: 6),
          Wrap(spacing: 6, runSpacing: 6, children: evaluations.entries.map((e) {
            final cat = e.key;
            final s = e.value as Map<String, dynamic>;
            final moyenne = s['moyenne'];
            final total = s['total'] ?? 0;
            return Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
              decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.04), borderRadius: BorderRadius.circular(10)),
              child: Text(
                '${_catLabels[cat] ?? cat}: ${moyenne != null ? moyenne.toString() : '—'}/5 ($total)',
                style: const TextStyle(color: Colors.white70, fontSize: 11),
              ),
            );
          }).toList()),
        ],
      ]),
    );
  }

  Widget _buildAmesSuivies(Map<String, dynamic> d) {
    final ames = (d['amesSuivies'] as List? ?? []);
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Row(children: [
        const Icon(Icons.favorite, color: Colors.greenAccent, size: 16),
        const SizedBox(width: 6),
        Expanded(child: Text('Âmes suivies (${d['nombreAmesSuivies'] ?? ames.length})', style: const TextStyle(color: Colors.white, fontSize: 15, fontWeight: FontWeight.bold))),
      ]),
      const SizedBox(height: 8),
      if (ames.isEmpty)
        GlassCard(
          padding: const EdgeInsets.all(20),
          child: Center(child: Text(l10n.userDetailNoFollowedSouls, style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 13))),
        )
      else
        ...ames.map((raw) {
          final a = raw as Map<String, dynamic>;
          final statut = (a['statut'] as String?) ?? '';
          return Container(
            margin: const EdgeInsets.only(bottom: 6),
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.04), borderRadius: BorderRadius.circular(12)),
            child: Row(children: [
              Container(
                width: 34, height: 34,
                decoration: BoxDecoration(color: _statutColor(statut).withValues(alpha: 0.15), borderRadius: BorderRadius.circular(10)),
                child: Center(child: Text(initialsFromName(a['nom']?.toString()), style: TextStyle(color: _statutColor(statut), fontSize: 11, fontWeight: FontWeight.bold))),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text(a['nom']?.toString() ?? '—', style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w500)),
                  if (a['familleNom'] != null)
                    Text('Famille ${a['familleNom']}', style: TextStyle(color: Colors.white.withValues(alpha: 0.35), fontSize: 10)),
                ]),
              ),
              StatusBadge(label: _statutLabel(statut), color: _statutColor(statut)),
            ]),
          );
        }),
    ]);
  }

  Widget _buildSorties(Map<String, dynamic> d) {
    final sorties = (d['sorties'] as List? ?? []);
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Row(children: [
        const Icon(Icons.person_remove, color: Colors.redAccent, size: 16),
        const SizedBox(width: 6),
        Expanded(child: Text('${l10n.userDetailExits} (${sorties.length})', style: const TextStyle(color: Colors.white, fontSize: 15, fontWeight: FontWeight.bold))),
      ]),
      const SizedBox(height: 8),
      ...sorties.map((raw) {
        final ex = raw as Map<String, dynamic>;
        return Container(
          margin: const EdgeInsets.only(bottom: 6),
          padding: const EdgeInsets.all(10),
          decoration: BoxDecoration(
            color: Colors.red.withValues(alpha: 0.06),
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: Colors.red.withValues(alpha: 0.2)),
          ),
          child: Row(children: [
            const Icon(Icons.person_remove, color: Colors.redAccent, size: 14),
            const SizedBox(width: 8),
            Expanded(child: Text(ex['motif']?.toString() ?? l10n.userDetailExitReason, style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 12))),
            Text(_formatDate(ex['dateSortie']), style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 10)),
          ]),
        );
      }),
    ]);
  }

  Widget _buildDepartements(Map<String, dynamic> d) {
    final depts = (d['departements'] as List? ?? []);
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Row(children: [
        const Icon(Icons.apartment, color: Colors.amber, size: 16),
        const SizedBox(width: 6),
        Expanded(child: Text('${l10n.userDetailDeptsManaged} (${depts.length})', style: const TextStyle(color: Colors.white, fontSize: 15, fontWeight: FontWeight.bold))),
      ]),
      const SizedBox(height: 8),
      ...depts.map((raw) {
        final dept = raw as Map<String, dynamic>;
        final membres = (dept['membres'] as List? ?? []);
        return GlassCard(
          margin: const EdgeInsets.only(bottom: 8),
          padding: const EdgeInsets.all(12),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Row(children: [
              const Icon(Icons.apartment, color: Colors.amber, size: 15),
              const SizedBox(width: 6),
              Expanded(
                child: Text(dept['nom']?.toString() ?? '—', style: const TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w600)),
              ),
              Text('${membres.length} membres', style: TextStyle(color: Colors.white.withValues(alpha: 0.35), fontSize: 11)),
            ]),
            const SizedBox(height: 8),
            if (membres.isEmpty)
              Text(l10n.userDetailNoMembers, style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 12))
            else
              ...membres.map((raw) {
                final m = raw as Map<String, dynamic>;
                final statut = (m['statut'] as String?) ?? '';
                return Container(
                  margin: const EdgeInsets.only(bottom: 4),
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.03), borderRadius: BorderRadius.circular(10)),
                  child: Row(children: [
                    Container(
                      width: 30, height: 30,
                      decoration: BoxDecoration(color: _statutColor(statut).withValues(alpha: 0.15), borderRadius: BorderRadius.circular(8)),
                      child: Center(child: Text(initialsFromName(m['nomComplet']?.toString()), style: TextStyle(color: _statutColor(statut), fontSize: 10, fontWeight: FontWeight.bold))),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        Text(m['nomComplet']?.toString() ?? '—', style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w500)),
                        if (m['faiseurNom'] != null)
                          Text(l10n.userDetailFollowedBy.replaceAll('{name}', m['faiseurNom']?.toString() ?? ''), style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 9)),
                      ]),
                    ),
                    StatusBadge(label: _statutLabel(statut), color: _statutColor(statut)),
                  ]),
                );
              }),
          ]),
        );
      }),
    ]);
  }

  static const _objectifStatutColors = {
    'A_FAIRE': Color(0xFFF59E0B),
    'EN_COURS': Color(0xFF3B82F6),
    'ATTEINT': Color(0xFF22C55E),
    'ANNULE': Color(0xFF6B7280),
  };

  static const _rapportTypeColors = {
    'COMPORTEMENT': Color(0xFF8B5CF6),
    'ASSIDUITE': Color(0xFF3B82F6),
    'CAPACITE': Color(0xFF14B8A6),
    'PROGRESSION': Color(0xFF22C55E),
    'INCIDENT': Color(0xFFEF4444),
    'DISCIPLINE': Color(0xFFF59E0B),
    'RECOMMANDATION': Color(0xFF06B6D4),
  };

  String _statutLabelShort(String? statut) {
    final raw = (statut ?? '').replaceAll('_', ' ').toLowerCase().trim();
    if (raw.isEmpty) return raw;
    return raw.split(' ').map((w) => w.isEmpty ? w : w[0].toUpperCase() + w.substring(1)).join(' ');
  }

  Widget _buildDossier(Map<String, dynamic> d) {
    final dossier = (d['dossier'] as List? ?? []);
    final docs = (d['dossierDocuments'] as List? ?? []);
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Row(children: [
        Icon(Icons.folder_shared, color: AppColors.primary, size: 16),
        const SizedBox(width: 6),
        Expanded(
          child: Text(l10n.userDetailDossier.replaceAll('{count}', dossier.length.toString()),
              style: const TextStyle(color: Colors.white, fontSize: 15, fontWeight: FontWeight.bold)),
        ),
      ]),
      const SizedBox(height: 8),

      // Objectifs + rapports + notes par département
      ...dossier.map((raw) {
        final dept = raw as Map<String, dynamic>;
        final objectifs = (dept['objectifs'] as List? ?? []);
        final rapports = (dept['rapportsResponsable'] as List? ?? []);
        final notes = (dept['notes'] as List? ?? []);
        return GlassCard(
          margin: const EdgeInsets.only(bottom: 8),
          padding: const EdgeInsets.all(12),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Row(children: [
              const Icon(Icons.apartment, color: Colors.amber, size: 15),
              const SizedBox(width: 6),
              Expanded(
                child: Text(dept['departmentNom']?.toString() ?? 'Département',
                    style: const TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w600)),
              ),
            ]),

            // Objectifs
            const SizedBox(height: 10),
            Row(children: [
              const Icon(Icons.flag, color: Colors.greenAccent, size: 14),
              const SizedBox(width: 5),
              Expanded(
                child: Text('${l10n.userDetailObjectives} (${objectifs.length})',
                    style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 11, fontWeight: FontWeight.w600)),
              ),
            ]),
            const SizedBox(height: 6),
            if (objectifs.isEmpty)
              Text(l10n.userDetailNoObjectives,
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 12))
            else
              ...objectifs.map((rawO) {
                final o = rawO as Map<String, dynamic>;
                final statut = (o['statut'] as String?) ?? '';
                final statutColor = _objectifStatutColors[statut] ?? Colors.grey;
                final avancement = ((o['avancement'] as num?) ?? 0).clamp(0, 100);
                return Container(
                  margin: const EdgeInsets.only(bottom: 6),
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.03), borderRadius: BorderRadius.circular(10)),
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Row(children: [
                      Expanded(
                        child: Text(o['titre']?.toString() ?? '—',
                            style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w600)),
                      ),
                      if (o['enRetard'] == true)
                        const Padding(
                          padding: EdgeInsets.only(right: 4),
                          child: Icon(Icons.schedule, color: Colors.redAccent, size: 13),
                        ),
                      StatusBadge(label: _statutLabelShort(statut), color: statutColor),
                    ]),
                    if (o['description'] != null) ...[
                      const SizedBox(height: 3),
                      Text(o['description'].toString(),
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 10)),
                    ],
                    const SizedBox(height: 6),
                    Row(children: [
                      Expanded(
                        child: ClipRRect(
                          borderRadius: BorderRadius.circular(4),
                          child: LinearProgressIndicator(
                            value: avancement.toDouble() / 100,
                            minHeight: 5,
                            backgroundColor: Colors.white.withValues(alpha: 0.08),
                            valueColor: const AlwaysStoppedAnimation(Colors.greenAccent),
                          ),
                        ),
                      ),
                      const SizedBox(width: 6),
                      Text('$avancement%',
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 10)),
                      if (o['echeance'] != null) ...[
                        const SizedBox(width: 6),
                        Text(_formatDate(o['echeance']),
                            style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 9)),
                      ],
                    ]),
                  ]),
                );
              }),

            // Rapports du responsable
            const SizedBox(height: 8),
            Row(children: [
              const Icon(Icons.description, color: Colors.lightBlueAccent, size: 14),
              const SizedBox(width: 5),
              Expanded(
                child: Text('${l10n.userDetailReports} (${rapports.length})',
                    style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 11, fontWeight: FontWeight.w600)),
              ),
            ]),
            const SizedBox(height: 6),
            if (rapports.isEmpty)
              Text(l10n.userDetailNoReports,
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 12))
            else
              ...rapports.map((rawR) {
                final r = rawR as Map<String, dynamic>;
                final type = (r['type'] as String?) ?? '';
                return Container(
                  margin: const EdgeInsets.only(bottom: 6),
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    color: (_rapportTypeColors[type] ?? Colors.blue).withValues(alpha: 0.06),
                    borderRadius: BorderRadius.circular(10),
                    border: Border.all(color: (_rapportTypeColors[type] ?? Colors.blue).withValues(alpha: 0.2)),
                  ),
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Row(children: [
                      StatusBadge(label: _statutLabelShort(type), color: _rapportTypeColors[type] ?? Colors.blue),
                      const Spacer(),
                      Text('${r['auteurNom'] ?? '—'} · ${_formatDate(r['createdAt'])}',
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 9)),
                    ]),
                    const SizedBox(height: 4),
                    Text(r['contenu']?.toString() ?? '',
                        style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 11, height: 1.35)),
                  ]),
                );
              }),

            // Notes
            if (notes.isNotEmpty) ...[
              const SizedBox(height: 8),
              Row(children: [
                const Icon(Icons.sticky_note_2, color: Colors.amber, size: 14),
                const SizedBox(width: 5),
                Expanded(
                  child: Text('${l10n.userDetailNotes} (${notes.length})',
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 11, fontWeight: FontWeight.w600)),
                ),
              ]),
              const SizedBox(height: 6),
              ...notes.map((rawN) {
                final n = rawN as Map<String, dynamic>;
                return Container(
                  margin: const EdgeInsets.only(bottom: 5),
                  padding: const EdgeInsets.all(9),
                  decoration: BoxDecoration(
                    color: Colors.amber.withValues(alpha: 0.06),
                    borderRadius: BorderRadius.circular(10),
                    border: Border.all(color: Colors.amber.withValues(alpha: 0.15)),
                  ),
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Text(n['contenu']?.toString() ?? '',
                        style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 11)),
                    const SizedBox(height: 3),
                    Text('${n['auteurNom'] ?? '—'} · ${_formatDate(n['createdAt'])}',
                        style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 9)),
                  ]),
                );
              }),
            ],
          ]),
        );
      }),

      // Documents du dossier
      if (docs.isNotEmpty) ...[
        Row(children: [
          const Icon(Icons.attach_file, color: Color(0xFF8B5CF6), size: 14),
          const SizedBox(width: 5),
          Expanded(
            child: Text('Documents du dossier (${docs.length})',
                style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 11, fontWeight: FontWeight.w600)),
          ),
        ]),
        const SizedBox(height: 6),
        ...docs.map((rawDoc) {
          final doc = rawDoc as Map<String, dynamic>;
          return GestureDetector(
            onTap: () => showUrlLink(context, doc['url']?.toString() ?? ''),
            child: Container(
              margin: const EdgeInsets.only(bottom: 5),
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 9),
              decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.03), borderRadius: BorderRadius.circular(10)),
              child: Row(children: [
                const Icon(Icons.attach_file, color: Color(0xFF8B5CF6), size: 14),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(doc['nom']?.toString() ?? 'Document', maxLines: 1, overflow: TextOverflow.ellipsis,
                      style: const TextStyle(color: Colors.white, fontSize: 12)),
                ),
                Icon(Icons.open_in_new, size: 12, color: Colors.white.withValues(alpha: 0.3)),
              ]),
            ),
          );
        }),
      ],
    ]);
  }

  Widget _buildFamilleGeree(Map<String, dynamic> famille) {
    final membres = (famille['membres'] as List? ?? []);
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Row(children: [
        const Icon(Icons.home, color: Color(0xFFF59E0B), size: 16),
        const SizedBox(width: 6),
        Expanded(
          child: Text('${l10n.userDetailManagedFamily} : ${famille['nom'] ?? '—'} (${membres.length})',
              style: const TextStyle(color: Colors.white, fontSize: 15, fontWeight: FontWeight.bold)),
        ),
      ]),
      const SizedBox(height: 8),
      GlassCard(
        padding: const EdgeInsets.all(12),
        child: membres.isEmpty
            ? Text(l10n.userDetailNoMembers, style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 12))
            : Column(children: membres.map((raw) {
                final m = raw as Map<String, dynamic>;
                final statut = (m['statut'] as String?) ?? '';
                return Padding(
                  padding: const EdgeInsets.symmetric(vertical: 4),
                  child: Row(children: [
                    Container(
                      width: 30, height: 30,
                      decoration: BoxDecoration(color: _statutColor(statut).withValues(alpha: 0.15), borderRadius: BorderRadius.circular(8)),
                      child: Center(child: Text(initialsFromName(m['nomComplet']?.toString()), style: TextStyle(color: _statutColor(statut), fontSize: 10, fontWeight: FontWeight.bold))),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(m['nomComplet']?.toString() ?? '—', style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w500)),
                    ),
                    if (m['faiseurNom'] != null)
                      Text('par ${m['faiseurNom']}', style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 9)),
                    const SizedBox(width: 6),
                    StatusBadge(label: _statutLabel(statut), color: _statutColor(statut)),
                  ]),
                );
              }).toList()),
      ),
    ]);
  }
}
