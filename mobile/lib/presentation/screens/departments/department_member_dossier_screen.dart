import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/open_url.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

const _objectiveBadgeColors = <String, Color>{
  'A_FAIRE': Colors.white70,
  'EN_COURS': Colors.lightBlue,
  'ATTEINT': Colors.green,
  'ANNULE': Colors.grey,
};

const _reportTypeLabels = <String, String>{
  'COMPORTEMENT': 'Comportement',
  'ASSIDUITE': 'Assiduité',
  'CAPACITE': 'Capacité',
  'PROGRESSION': 'Progression',
  'INCIDENT': 'Incident',
  'DISCIPLINE': 'Discipline',
  'RECOMMANDATION': 'Recommandation',
};

/// Dossier du membre dans un département (parité web) : profil, objectifs de
/// progression, rapports du responsable, notes internes et journal d'activité.
class DepartmentMemberDossierScreen extends StatefulWidget {
  final String departmentId;
  final String memberId;
  final ApiService? apiService;

  const DepartmentMemberDossierScreen({
    super.key,
    required this.departmentId,
    required this.memberId,
    this.apiService,
  });

  @override
  State<DepartmentMemberDossierScreen> createState() =>
      _DepartmentMemberDossierScreenState();
}

class _DepartmentMemberDossierScreenState
    extends State<DepartmentMemberDossierScreen> {
  late final ApiService _apiService = widget.apiService ?? ApiService();
  Map<String, dynamic>? _dossier;
  bool _isLoading = true;

  String get _deptId => widget.departmentId;
  String get _memberId => widget.memberId;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final res = await _apiService.get(
          '/departments/$_deptId/members/$_memberId/dossier');
      if (mounted) {
        setState(() {
          _dossier = res.data as Map<String, dynamic>?;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _reload() async {
    await _loadData();
  }

  @override
  Widget build(BuildContext context) {
    final dossier = _dossier ?? {};
    final profil = dossier['profil'] as Map<String, dynamic>? ?? {};
    final nomComplet = (profil['nomComplet'] ?? 'Membre').toString();

    return Scaffold(
      appBar: AppBar(
        title: Text('Dossier · $nomComplet'),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _reload),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 5)
          : _dossier == null
              ? Center(
                  child: Text('Dossier introuvable',
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                )
              : DefaultTabController(
                  length: 6,
                  child: Column(
                    children: [
                      _header(context, profil, dossier),
                      TabBar(
                        isScrollable: true,
                        tabAlignment: TabAlignment.start,
                        labelColor: Colors.white,
                        indicatorColor: AppColors.accent,
                        tabs: const [
                          Tab(icon: Icon(Icons.person, size: 20), text: 'Profil'),
                          Tab(icon: Icon(Icons.flag, size: 20), text: 'Objectifs'),
                          Tab(icon: Icon(Icons.description, size: 20), text: 'Rapports'),
                          Tab(icon: Icon(Icons.sticky_note_2, size: 20), text: 'Notes'),
                          Tab(icon: Icon(Icons.folder_open, size: 20), text: 'Documents'),
                          Tab(icon: Icon(Icons.history, size: 20), text: 'Activité'),
                        ],
                      ),
                      Expanded(
                        child: TabBarView(
                          children: [
                            _ProfilTab(
                              profil: profil,
                              alertes: dossier['alertes'] as List<dynamic>? ?? [],
                              affectations: dossier['affectations'] as List<dynamic>? ?? [],
                            ),
                            _ObjectifsTab(
                              api: _apiService,
                              deptId: _deptId,
                              memberId: _memberId,
                              items: dossier['objectifs'] as List<dynamic>? ?? [],
                              onChanged: _reload,
                            ),
                            _RapportsTab(
                              api: _apiService,
                              deptId: _deptId,
                              memberId: _memberId,
                              responsable: dossier['rapportsResponsable'] as List<dynamic>? ?? [],
                              faiseur: dossier['rapports'] as Map<String, dynamic>? ?? {},
                              onChanged: _reload,
                            ),
                            _NotesTab(
                              api: _apiService,
                              deptId: _deptId,
                              memberId: _memberId,
                              items: dossier['notes'] as List<dynamic>? ?? [],
                              onChanged: _reload,
                            ),
                            _DocumentsTab(
                              documents: dossier['documents'] as List<dynamic>? ?? [],
                              notesDisciple: dossier['notesDisciple'] as List<dynamic>? ?? [],
                            ),
                            _ActiviteTab(
                              items: dossier['activite'] as List<dynamic>? ?? [],
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
    );
  }

  Widget _header(BuildContext context, Map<String, dynamic> profil, Map<String, dynamic> dossier) {
    final statut = (profil['statut'] ?? 'ACTIF').toString();
    final statutColor = switch (statut) {
      'ACTIF' => Colors.green,
      'EN_INTEGRATION' => Colors.lightBlue,
      'EN_VEILLE' => Colors.amber,
      'DECROCHE' => Colors.grey,
      _ => Colors.white70,
    };
    final origineLabels = <String, String>{
      'MANUEL': 'Ajout manuel',
      'SIGNUP': 'Inscription',
      'TRANSFERT': 'Transfert',
    };
    final origine = (profil['origine'] ?? '').toString();
    final objectifs = dossier['objectifs'] as List<dynamic>? ?? [];
    final enCours = objectifs.where((o) {
      final s = (o as Map)['statut']?.toString();
      return s == 'EN_COURS' || s == 'A_FAIRE';
    }).length;

    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 8, 12, 0),
      child: GlassCard(
        padding: const EdgeInsets.all(14),
        child: Row(
          children: [
            GradientAvatar(
              text: initialsFromName((profil['nomComplet'] ?? '').toString()),
              radius: 26,
              gradientStart: AppColors.accent,
              gradientEnd: AppColors.accentLight,
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text((profil['nomComplet'] ?? 'Membre').toString(),
                      style: const TextStyle(
                          color: Colors.white, fontWeight: FontWeight.w700, fontSize: 16)),
                  const SizedBox(height: 4),
                  Wrap(
                    spacing: 6,
                    runSpacing: 4,
                    children: [
                      StatusBadge(label: statut.replaceAll('_', ' '), color: statutColor),
                      if (origine.isNotEmpty)
                        StatusBadge(
                          label: origineLabels[origine] ?? origine,
                          color: Colors.lightBlue,
                        ),
                      StatusBadge(
                        label: '$enCours en cours',
                        color: AppColors.accent,
                      ),
                    ],
                  ),
                ],
              ),
            ),
            IconButton(
              icon: Icon(Icons.open_in_new, color: Colors.white.withValues(alpha: 0.6)),
              tooltip: 'Fiche âme',
              onPressed: () {
                final soulId = profil['id']?.toString();
                if (soulId != null && soulId.isNotEmpty) {
                  context.go('/souls/$soulId');
                }
              },
            ),
            if (dossier['profil'] != null && profil['membreActif'] == true)
              PopupMenuButton<String>(
                icon: Icon(Icons.more_vert, color: Colors.white.withValues(alpha: 0.6)),
                color: AppColors.cardDark,
                onSelected: (v) {
                  if (v == 'remove') _confirmRemove(context);
                },
                itemBuilder: (_) => [
                  PopupMenuItem(
                    value: 'remove',
                    child: const Row(
                      children: [
                        Icon(Icons.person_remove, color: Colors.red, size: 18),
                        SizedBox(width: 8),
                        Text('Retirer du département', style: TextStyle(color: Colors.red)),
                      ],
                    ),
                  ),
                ],
              ),
          ],
        ),
      ),
    );
  }

  Future<void> _confirmRemove(BuildContext context) async {
    final profil = _dossier?['profil'] as Map<String, dynamic>? ?? {};
    final nomComplet = (profil['nomComplet'] ?? 'ce membre').toString();
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.cardDark,
        title: const Text('Retirer du département', style: TextStyle(color: Colors.white)),
        content: Text('Retirer « $nomComplet » de ce département ?',
            style: TextStyle(color: Colors.white.withValues(alpha: 0.8))),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Retirer')),
        ],
      ),
    );
    if (ok == true) {
      try {
        await _apiService.delete('/departments/$_deptId/members/$_memberId');
        if (context.mounted) context.go('/departments/$_deptId');
      } catch (_) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('Échec du retrait du membre')));
        }
      }
    }
  }
}

// ============================================================
// PROFIL
// ============================================================

class _ProfilTab extends StatelessWidget {
  final Map<String, dynamic> profil;
  final List<dynamic> alertes;
  final List<dynamic> affectations;

  const _ProfilTab({
    required this.profil,
    required this.alertes,
    required this.affectations,
  });

  @override
  Widget build(BuildContext context) {
    final rows = <(String, String?)>[
      ('Email', profil['email']?.toString()),
      ('Téléphone', profil['telephone']?.toString()),
      ('Adresse', profil['adresse']?.toString()),
      ('Date de naissance', profil['dateNaissance']?.toString()),
      ('Profession', profil['profession']?.toString()),
      ('Type de disciple', profil['typeDisciple']?.toString()),
      ('État spirituel', profil['etatSpirituel']?.toString()),
      ("Date d'intégration", profil['dateIntegration']?.toString()),
      ('Ajouté par', profil['ajoutePar']?.toString()),
      ("Date d'affectation", profil['dateAffectation']?.toString()),
    ];

    return RefreshIndicator(
      onRefresh: () async {},
      child: ListView(
        padding: const EdgeInsets.all(12),
        children: [
          SectionTitle(title: 'Identité & coordonnées', icon: Icons.person),
          GlassCard(
            padding: const EdgeInsets.all(12),
            child: Column(
              children: rows.map((r) {
                return Padding(
                  padding: const EdgeInsets.symmetric(vertical: 6),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      SizedBox(
                        width: 130,
                        child: Text(r.$1,
                            style: TextStyle(
                                color: Colors.white.withValues(alpha: 0.4), fontSize: 12)),
                      ),
                      Expanded(
                        child: Text(r.$2 ?? '—',
                            style: const TextStyle(color: Colors.white, fontSize: 13)),
                      ),
                    ],
                  ),
                );
              }).toList(),
            ),
          ),
          const SizedBox(height: 12),
          SectionTitle(title: 'Affectations actives', icon: Icons.group),
          if (affectations.isEmpty)
            GlassCard(
              padding: const EdgeInsets.all(20),
              child: Text('Aucune affectation',
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
            )
          else
            ...affectations.map((a) {
              final m = a as Map;
              if (m['actif'] != true) return const SizedBox.shrink();
              return GlassCard(
                margin: const EdgeInsets.only(bottom: 8),
                padding: const EdgeInsets.all(12),
                child: Row(
                  children: [
                    Icon(Icons.group_work, color: AppColors.accent, size: 20),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        '${m['teamNom'] ?? '—'}${m['positionNom'] != null ? ' · ${m['positionNom']}' : ''}',
                        style: const TextStyle(color: Colors.white, fontSize: 13),
                      ),
                    ),
                    StatusBadge(
                      label: (m['role'] ?? 'MEMBRE').toString(),
                      color: Colors.green,
                    ),
                  ],
                ),
              );
            }),
          if (alertes.isNotEmpty) ...[
            const SizedBox(height: 12),
            SectionTitle(title: 'Alertes actives', icon: Icons.notifications_active),
            ...alertes.map((a) {
              final m = a as Map;
              return GlassCard(
                margin: const EdgeInsets.only(bottom: 8),
                padding: const EdgeInsets.all(12),
                borderColor: Colors.red.withValues(alpha: 0.4),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('${m['titre'] ?? ''}',
                        style: const TextStyle(
                            color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13)),
                    if (m['message'] != null)
                      Text('${m['message']}',
                          style: TextStyle(
                              color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                  ],
                ),
              );
            }),
          ],
        ],
      ),
    );
  }
}

// ============================================================
// OBJECTIFS DE PROGRESSION
// ============================================================

class _ObjectifsTab extends StatelessWidget {
  final ApiService api;
  final String deptId;
  final String memberId;
  final List<dynamic> items;
  final VoidCallback onChanged;

  const _ObjectifsTab({
    required this.api,
    required this.deptId,
    required this.memberId,
    required this.items,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    final list = items.map((o) => o as Map<String, dynamic>).toList();
    final enCours = list.where((o) {
      final s = o['statut']?.toString();
      return s == 'EN_COURS' || s == 'A_FAIRE';
    }).length;
    final atteints = list.where((o) => o['statut']?.toString() == 'ATTEINT').length;

    return RefreshIndicator(
      onRefresh: () async => onChanged(),
      child: ListView(
        padding: const EdgeInsets.all(12),
        children: [
          Row(
            children: [
              _statChip('En cours', '$enCours', AppColors.accent),
              const SizedBox(width: 8),
              _statChip('Atteints', '$atteints', Colors.green),
              const SizedBox(width: 8),
              _statChip('Total', '${list.length}', Colors.white70),
            ],
          ),
          const SizedBox(height: 4),
          SectionTitle(
            title: 'Objectifs de progression',
            icon: Icons.flag,
            trailing: IconButton(
              icon: const Icon(Icons.add_circle, color: Colors.amber),
              onPressed: () => _showCreate(context),
            ),
          ),
          if (list.isEmpty)
            GlassCard(
              padding: const EdgeInsets.all(24),
              child: Text('Aucun objectif fixé pour ce membre',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
            )
          else
            ...list.map((o) => _objectiveCard(context, o)),
          const SizedBox(height: 80),
        ],
      ),
    );
  }

  Widget _statChip(String label, String value, Color color) {
    return Expanded(
      child: GlassCard(
        padding: const EdgeInsets.all(10),
        child: Column(
          children: [
            Text(value,
                style: TextStyle(color: color, fontSize: 18, fontWeight: FontWeight.bold)),
            Text(label,
                style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 10)),
          ],
        ),
      ),
    );
  }

  Widget _objectiveCard(BuildContext context, Map<String, dynamic> o) {
    final enRetard = o['enRetard'] == true;
    final statut = o['statut']?.toString() ?? 'A_FAIRE';
    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      borderColor: enRetard ? Colors.red.withValues(alpha: 0.4) : null,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text('${o['titre'] ?? ''}',
                    style: const TextStyle(
                        color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
              ),
              StatusBadge(
                label: statut.replaceAll('_', ' ').toLowerCase(),
                color: _objectiveBadgeColors[statut] ?? Colors.white70,
              ),
            ],
          ),
          if (o['description'] != null) ...[
            const SizedBox(height: 4),
            Text('${o['description']}',
                style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
          ],
          const SizedBox(height: 4),
          Text(
            '${o['echeance'] != null ? 'Échéance ${o['echeance']} · ' : ''}fixé par ${o['creeParNom'] ?? '—'}',
            style: TextStyle(color: Colors.white.withValues(alpha: 0.35), fontSize: 10),
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              Expanded(
                child: SliderTheme(
                  data: SliderTheme.of(context).copyWith(
                    activeTrackColor: Colors.green,
                    inactiveTrackColor: Colors.white.withValues(alpha: 0.15),
                    thumbColor: Colors.green,
                  ),
                  child: Slider(
                    value: (o['avancement'] as num? ?? 0).toDouble().clamp(0, 100),
                    max: 100,
                    divisions: 20,
                    label: '${o['avancement'] ?? 0}%',
                    onChanged: (v) => _update(context, o, {'avancement': v.round()}),
                  ),
                ),
              ),
              Text('${o['avancement'] ?? 0}%',
                  style: const TextStyle(color: Colors.green, fontWeight: FontWeight.bold)),
              IconButton(
                icon: Icon(Icons.delete_outline,
                    color: Colors.red.withValues(alpha: 0.6), size: 20),
                tooltip: 'Supprimer',
                onPressed: () async {
                  final ok = await showDialog<bool>(
                    context: context,
                    builder: (ctx) => AlertDialog(
                      backgroundColor: AppColors.cardDark,
                      title: const Text('Supprimer l\'objectif',
                          style: TextStyle(color: Colors.white)),
                      content: Text('Supprimer « ${o['titre']} » ?',
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.8))),
                      actions: [
                        TextButton(
                            onPressed: () => Navigator.pop(ctx, false),
                            child: const Text('Annuler')),
                        FilledButton(
                            onPressed: () => Navigator.pop(ctx, true),
                            child: const Text('Supprimer')),
                      ],
                    ),
                  );
                  if (ok == true) {
                    try {
                      await api
                          .delete('/departments/$deptId/objectives/${o['id']}');
                      onChanged();
                    } catch (_) {}
                  }
                },
              ),
            ],
          ),
          DropdownButtonFormField<String>(
            initialValue: statut,
            dropdownColor: AppColors.cardDark,
            style: const TextStyle(color: Colors.white),
            isDense: true,
            items: _objectiveBadgeColors.keys.map((k) {
              return DropdownMenuItem(
                value: k,
                child: Text(k.replaceAll('_', ' ').toLowerCase(),
                    style: const TextStyle(color: Colors.white)),
              );
            }).toList(),
            onChanged: (v) {
              if (v != null) _update(context, o, {'statut': v});
            },
            decoration: const InputDecoration(labelText: 'Statut'),
          ),
        ],
      ),
    );
  }

  Future<void> _update(
      BuildContext context, Map<String, dynamic> o, Map<String, dynamic> patch) async {
    try {
      await api.put('/departments/$deptId/objectives/${o['id']}', data: {
        'titre': o['titre'],
        'description': o['description'],
        'echeance': o['echeance'],
        'avancement': patch['avancement'] ?? o['avancement'],
        'statut': patch['statut'] ?? o['statut'],
      });
      onChanged();
    } catch (_) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Échec de la mise à jour')));
      }
    }
  }

  Future<void> _showCreate(BuildContext context) async {
    final titreCtrl = TextEditingController();
    final descCtrl = TextEditingController();
    String echeance = '';

    await showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.cardDark,
        title: const Text('Nouvel objectif', style: TextStyle(color: Colors.white)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: titreCtrl,
              style: const TextStyle(color: Colors.white),
              decoration: const InputDecoration(labelText: 'Objectif *'),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: descCtrl,
              style: const TextStyle(color: Colors.white),
              decoration: const InputDecoration(labelText: 'Description'),
            ),
            const SizedBox(height: 12),
            TextField(
              style: const TextStyle(color: Colors.white),
              onChanged: (v) => echeance = v,
              decoration: const InputDecoration(
                  labelText: 'Échéance (AAAA-MM-JJ, optionnel)'),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
          FilledButton(
            onPressed: () async {
              if (titreCtrl.text.trim().isEmpty) return;
              try {
                await api.post('/departments/$deptId/members/$memberId/objectives', data: {
                  'titre': titreCtrl.text.trim(),
                  'description': descCtrl.text.trim().isEmpty ? null : descCtrl.text.trim(),
                  'echeance': echeance.isEmpty ? null : echeance,
                });
                if (ctx.mounted) Navigator.pop(ctx);
                onChanged();
              } catch (_) {
                if (ctx.mounted) {
                  ScaffoldMessenger.of(ctx).showSnackBar(
                      const SnackBar(content: Text('Échec de la création de l\'objectif')));
                }
              }
            },
            child: const Text('Créer'),
          ),
        ],
      ),
    );
  }
}

// ============================================================
// RAPPORTS
// ============================================================

class _RapportsTab extends StatelessWidget {
  final ApiService api;
  final String deptId;
  final String memberId;
  final List<dynamic> responsable;
  final Map<String, dynamic> faiseur;
  final VoidCallback onChanged;

  const _RapportsTab({
    required this.api,
    required this.deptId,
    required this.memberId,
    required this.responsable,
    required this.faiseur,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    final faiseurList = (faiseur['liste'] as List<dynamic>? ?? [])
        .map((r) => r as Map<String, dynamic>)
        .toList();

    return RefreshIndicator(
      onRefresh: () async => onChanged(),
      child: ListView(
        padding: const EdgeInsets.all(12),
        children: [
          SectionTitle(
            title: 'Rapports du responsable',
            icon: Icons.description,
            trailing: IconButton(
              icon: const Icon(Icons.add_circle, color: Colors.amber),
              onPressed: () => _showCreate(context),
            ),
          ),
          if (responsable.isEmpty)
            GlassCard(
              padding: const EdgeInsets.all(20),
              child: Text('Aucun rapport rédigé par un responsable',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
            )
          else
            ...responsable.map((r) => _responsableCard(context, r as Map<String, dynamic>)),
          const SizedBox(height: 12),
          SectionTitle(
            title:
                'Rapports du faiseur',
            icon: Icons.fact_check,
            trailing: Text(
              '${faiseur['soumis'] ?? 0} soumis / ${faiseur['total'] ?? 0}',
              style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12),
            ),
          ),
          if (faiseurList.isEmpty)
            GlassCard(
              padding: const EdgeInsets.all(20),
              child: Text('Aucun rapport',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
            )
          else
            ...faiseurList.map((r) => GlassCard(
                  margin: const EdgeInsets.only(bottom: 8),
                  padding: const EdgeInsets.all(12),
                  child: Row(
                    children: [
                      Icon(Icons.calendar_today,
                          color: Colors.white.withValues(alpha: 0.4), size: 16),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text('Semaine du ${r['semaine'] ?? ''}',
                            style: const TextStyle(
                                color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13)),
                      ),
                      StatusBadge(
                        label: r['soumis'] == true ? 'Soumis' : 'Brouillon',
                        color: r['soumis'] == true ? Colors.green : Colors.white70,
                      ),
                    ],
                  ),
                )),
          const SizedBox(height: 80),
        ],
      ),
    );
  }

  Widget _responsableCard(BuildContext context, Map<String, dynamic> r) {
    final type = r['type']?.toString() ?? 'GENERAL';
    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      borderColor: AppColors.accent.withValues(alpha: 0.3),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              StatusBadge(
                label: _reportTypeLabels[type] ?? type,
                color: AppColors.accent,
              ),
              IconButton(
                icon: Icon(Icons.delete_outline,
                    color: Colors.red.withValues(alpha: 0.6), size: 18),
                tooltip: 'Supprimer',
                onPressed: () async {
                  final ok = await showDialog<bool>(
                    context: context,
                    builder: (ctx) => AlertDialog(
                      backgroundColor: AppColors.cardDark,
                      title: const Text('Supprimer le rapport',
                          style: TextStyle(color: Colors.white)),
                      content: const Text('Supprimer ce rapport ?',
                          style: TextStyle(color: Colors.white70)),
                      actions: [
                        TextButton(
                            onPressed: () => Navigator.pop(ctx, false),
                            child: const Text('Annuler')),
                        FilledButton(
                            onPressed: () => Navigator.pop(ctx, true),
                            child: const Text('Supprimer')),
                      ],
                    ),
                  );
                  if (ok == true) {
                    try {
                      await api
                          .delete('/departments/$deptId/reports/${r['id']}');
                      onChanged();
                    } catch (_) {}
                  }
                },
              ),
            ],
          ),
          Text('${r['contenu'] ?? ''}',
              style: const TextStyle(color: Colors.white, fontSize: 13)),
          const SizedBox(height: 4),
          Text(
            '${r['auteurNom'] ?? '—'} · ${r['createdAt'] ?? ''}',
            style: TextStyle(color: Colors.white.withValues(alpha: 0.35), fontSize: 10),
          ),
        ],
      ),
    );
  }

  Future<void> _showCreate(BuildContext context) async {
    String type = 'PROGRESSION';
    final contenuCtrl = TextEditingController();

    await showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setState) => AlertDialog(
          backgroundColor: AppColors.cardDark,
          title: const Text('Nouveau rapport', style: TextStyle(color: Colors.white)),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              DropdownButtonFormField<String>(
                initialValue: type,
                dropdownColor: AppColors.cardDark,
                style: const TextStyle(color: Colors.white),
                items: _reportTypeLabels.entries.map((e) {
                  return DropdownMenuItem(
                    value: e.key,
                    child: Text(e.value, style: const TextStyle(color: Colors.white)),
                  );
                }).toList(),
                onChanged: (v) => setState(() => type = v ?? type),
                decoration: const InputDecoration(labelText: 'Type'),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: contenuCtrl,
                style: const TextStyle(color: Colors.white),
                maxLines: 3,
                decoration: const InputDecoration(labelText: 'Contenu *'),
              ),
            ],
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
            FilledButton(
              onPressed: () async {
                if (contenuCtrl.text.trim().isEmpty) return;
                try {
                  await api.post(
                      '/departments/$deptId/members/$memberId/reports',
                      data: {'type': type, 'contenu': contenuCtrl.text.trim()});
                  if (ctx.mounted) Navigator.pop(ctx);
                  onChanged();
                } catch (_) {
                  if (ctx.mounted) {
                    ScaffoldMessenger.of(ctx).showSnackBar(
                        const SnackBar(content: Text('Échec de l\'ajout du rapport')));
                  }
                }
              },
              child: const Text('Ajouter'),
            ),
          ],
        ),
      ),
    );
  }
}

// ============================================================
// NOTES
// ============================================================

class _NotesTab extends StatelessWidget {
  final ApiService api;
  final String deptId;
  final String memberId;
  final List<dynamic> items;
  final VoidCallback onChanged;

  const _NotesTab({
    required this.api,
    required this.deptId,
    required this.memberId,
    required this.items,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    final list = items.map((n) => n as Map<String, dynamic>).toList();

    return RefreshIndicator(
      onRefresh: () async => onChanged(),
      child: ListView(
        padding: const EdgeInsets.all(12),
        children: [
          SectionTitle(
            title: 'Notes du dossier (${list.length})',
            icon: Icons.sticky_note_2,
            trailing: IconButton(
              icon: const Icon(Icons.add_circle, color: Colors.amber),
              onPressed: () => _showCreate(context),
            ),
          ),
          if (list.isEmpty)
            GlassCard(
              padding: const EdgeInsets.all(24),
              child: Text('Aucune note pour ce membre',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
            )
          else
            ...list.map((n) => GlassCard(
                  margin: const EdgeInsets.only(bottom: 8),
                  padding: const EdgeInsets.all(12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('${n['contenu'] ?? ''}',
                          style: const TextStyle(color: Colors.white, fontSize: 13)),
                      const SizedBox(height: 6),
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              '${n['auteurNom'] ?? '—'} · ${n['createdAt'] ?? ''}',
                              style: TextStyle(
                                  color: Colors.white.withValues(alpha: 0.35), fontSize: 10),
                            ),
                          ),
                          IconButton(
                            icon: Icon(Icons.delete_outline,
                                color: Colors.red.withValues(alpha: 0.6), size: 18),
                            tooltip: 'Supprimer',
                            onPressed: () async {
                              final ok = await showDialog<bool>(
                                context: context,
                                builder: (ctx) => AlertDialog(
                                  backgroundColor: AppColors.cardDark,
                                  title: const Text('Supprimer la note',
                                      style: TextStyle(color: Colors.white)),
                                  content: const Text('Supprimer cette note ?',
                                      style: TextStyle(color: Colors.white70)),
                                  actions: [
                                    TextButton(
                                        onPressed: () => Navigator.pop(ctx, false),
                                        child: const Text('Annuler')),
                                    FilledButton(
                                        onPressed: () => Navigator.pop(ctx, true),
                                        child: const Text('Supprimer')),
                                  ],
                                ),
                              );
                              if (ok == true) {
                                try {
                                  await api
                                      .delete('/departments/$deptId/notes/${n['id']}');
                                  onChanged();
                                } catch (_) {}
                              }
                            },
                          ),
                        ],
                      ),
                    ],
                  ),
                )),
          const SizedBox(height: 80),
        ],
      ),
    );
  }

  Future<void> _showCreate(BuildContext context) async {
    final contenuCtrl = TextEditingController();

    await showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.cardDark,
        title: const Text('Nouvelle note', style: TextStyle(color: Colors.white)),
        content: TextField(
          controller: contenuCtrl,
          style: const TextStyle(color: Colors.white),
          maxLines: 3,
          decoration: const InputDecoration(
              labelText: 'Note interne (suivi, comportement, besoins…)'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
          FilledButton(
            onPressed: () async {
              if (contenuCtrl.text.trim().isEmpty) return;
              try {
                await api.post(
                    '/departments/$deptId/members/$memberId/notes',
                    data: {'contenu': contenuCtrl.text.trim()});
                if (ctx.mounted) Navigator.pop(ctx);
                onChanged();
              } catch (_) {
                if (ctx.mounted) {
                  ScaffoldMessenger.of(ctx).showSnackBar(
                      const SnackBar(content: Text('Échec de l\'ajout de la note')));
                }
              }
            },
            child: const Text('Ajouter'),
          ),
        ],
      ),
    );
  }
}

// ============================================================
// DOCUMENTS
// ============================================================

class _DocumentsTab extends StatelessWidget {
  final List<dynamic> documents;
  final List<dynamic> notesDisciple;

  const _DocumentsTab({required this.documents, required this.notesDisciple});

  @override
  Widget build(BuildContext context) {
    final docs = documents.map((d) => d as Map<String, dynamic>).toList();
    final notes = notesDisciple.map((n) => n as Map<String, dynamic>).toList();

    return RefreshIndicator(
      onRefresh: () async {},
      child: ListView(
        padding: const EdgeInsets.all(12),
        children: [
          SectionTitle(title: 'Documents du dossier (${docs.length})', icon: Icons.folder_open),
          if (docs.isEmpty)
            GlassCard(
              padding: const EdgeInsets.all(20),
              child: Text('Aucun document joint',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
            )
          else
            ...docs.map((d) => GlassCard(
                  margin: const EdgeInsets.only(bottom: 8),
                  padding: const EdgeInsets.all(12),
                  onTap: () => showUrlLink(context, '${d['url'] ?? ''}'),
                  child: Row(
                    children: [
                      Icon(Icons.folder_open, color: AppColors.primaryLight, size: 20),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Text('${d['nom'] ?? d['fileId'] ?? 'Document'}',
                            style: const TextStyle(
                                color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13),
                            overflow: TextOverflow.ellipsis),
                      ),
                      Icon(Icons.open_in_new,
                          color: Colors.white.withValues(alpha: 0.3), size: 16),
                    ],
                  ),
                )),
          const SizedBox(height: 12),
          SectionTitle(title: 'Notes de la fiche âme (${notes.length})', icon: Icons.sticky_note_2),
          if (notes.isEmpty)
            GlassCard(
              padding: const EdgeInsets.all(20),
              child: Text('Aucune note sur la fiche âme',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
            )
          else
            ...notes.map((n) => GlassCard(
                  margin: const EdgeInsets.only(bottom: 8),
                  padding: const EdgeInsets.all(12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('${n['contenu'] ?? ''}',
                          style: const TextStyle(color: Colors.white, fontSize: 13)),
                      const SizedBox(height: 4),
                      Text('${n['auteurNom'] ?? '—'} · ${n['createdAt'] ?? ''}',
                          style: TextStyle(
                              color: Colors.white.withValues(alpha: 0.35), fontSize: 10)),
                    ],
                  ),
                )),
          const SizedBox(height: 80),
        ],
      ),
    );
  }
}

// ============================================================
// ACTIVITÉ
// ============================================================

class _ActiviteTab extends StatelessWidget {
  final List<dynamic> items;

  const _ActiviteTab({required this.items});

  @override
  Widget build(BuildContext context) {
    if (items.isEmpty) {
      return Center(
        child: Text('Aucune activité pour ce membre',
            style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
      );
    }
    return ListView.builder(
      padding: const EdgeInsets.all(12),
      itemCount: items.length,
      itemBuilder: (context, index) {
        final a = items[index] as Map<String, dynamic>;
        final action = (a['action'] ?? '').toString().replaceAll('_', ' ').toLowerCase();
        return GlassCard(
          margin: const EdgeInsets.only(bottom: 8),
          padding: const EdgeInsets.all(12),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 20,
                height: 20,
                decoration: BoxDecoration(
                  gradient: LinearGradient(colors: [AppColors.accent, AppColors.accentLight]),
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.check, size: 12, color: Colors.white),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(action,
                        style: const TextStyle(
                            color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13)),
                    if (a['details'] != null)
                      Text('${a['details']}',
                          style: TextStyle(
                              color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                    const SizedBox(height: 2),
                    Text(
                      '${a['actorNom'] ?? ''}${(a['actorNom'] ?? '').toString().isNotEmpty ? ' · ' : ''}${a['createdAt'] ?? ''}',
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 10),
                    ),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}
