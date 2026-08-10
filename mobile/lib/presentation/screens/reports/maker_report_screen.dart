import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../data/services/api_service.dart';
import '../../../data/models/soul.dart';
import '../../../data/local/database.dart';
import '../../../data/local/sync_service.dart';
import '../../widgets/app_drawer.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/attachment_picker_field.dart';

class MakerReportScreen extends ConsumerStatefulWidget {
  const MakerReportScreen({super.key});

  @override
  ConsumerState<MakerReportScreen> createState() => _MakerReportScreenState();
}

class _MakerReportScreenState extends ConsumerState<MakerReportScreen> {
  final _apiService = ApiService();
  List<Soul> _souls = [];
  bool _isLoading = true;
  bool _isOffline = false;
  String _semaine = DateTime.now().toIso8601String().split('T')[0];

  // Report data per soul
  final Map<String, Map<String, bool>> _presences = {};
  final Map<String, String?> _absenceRaisons = {};
  final Map<String, TextEditingController> _absenceCommentaires = {};
  final Map<String, TextEditingController> _difficultes = {};
  final Map<String, TextEditingController> _notes = {};
  final Map<String, int> _nbSorties = {};
  final Map<String, Set<String>> _fichierIds = {};
  final Map<String, bool> _submitted = {};
  final Map<String, bool> _syncing = {};
  final Map<String, bool> _expanded = {};

  // Track drafts loaded from local DB
  final Set<String> _offlineDrafts = {};

  @override
  void initState() {
    super.initState();
    _loadData();
    ref.listen(isOnlineProvider, (prev, next) {
      if (prev == false && next == true) {
        _autoSync();
      }
      setState(() => _isOffline = !next);
    });
  }

  @override
  void dispose() {
    for (var c in _absenceCommentaires.values) c.dispose();
    for (var c in _difficultes.values) c.dispose();
    for (var c in _notes.values) c.dispose();
    super.dispose();
  }

  Future<void> _loadData() async {
    await Future.wait([_loadSoulsFromApi(), _loadOfflineDrafts()]);
  }

  Future<void> _loadSoulsFromApi() async {
    try {
      final response = await _apiService.get('/souls', params: {'size': '50'});
      final data = response.data as Map<String, dynamic>;
      final syncService = ref.read(syncServiceProvider);
      await syncService.cacheSouls(data);
      setState(() {
        _souls = (data['content'] as List)
            .map((e) => Soul.fromJson(e as Map<String, dynamic>))
            .toList();
        _isOffline = false;
        _initDataStructures();
      });
    } catch (_) {
      await _loadSoulsFromCache();
    }
  }

  Future<void> _loadSoulsFromCache() async {
    final syncService = ref.read(syncServiceProvider);
    final cachedSouls = await syncService.getCachedSouls();
    if (cachedSouls.isNotEmpty) {
      setState(() {
        _souls = cachedSouls
            .map((s) => Soul(
                  id: s.id,
                  nom: s.nom,
                  prenom: s.prenom,
                  email: s.email,
                  telephone: s.telephone,
                  typeDisciple: s.typeDisciple,
                  statut: s.statut,
                  dateIntegration: s.dateIntegration,
                  faiseurId: s.faiseurId,
                  familleId: s.familleId,
                  dateDernierContact: s.dateDernierContact,
                ))
            .toList();
        _isOffline = true;
        _initDataStructures();
      });
    } else {
      setState(() => _isLoading = false);
    }
  }

  Future<void> _loadOfflineDrafts() async {
    _offlineDrafts.clear();
    final syncService = ref.read(syncServiceProvider);
    final drafts = await syncService.getUnsyncedDrafts();
    for (final draft in drafts) {
      if (_presences.containsKey(draft.ameId)) continue;
      _offlineDrafts.add(draft.ameId);
      _presences[draft.ameId] =
          Map<String, bool>.from(jsonDecode(draft.presencesParCulte));
      _absenceRaisons[draft.ameId] = draft.absenceRaison;
      _absenceCommentaires[draft.ameId] =
          TextEditingController(text: draft.absenceCommentaire ?? '');
      _difficultes[draft.ameId] =
          TextEditingController(text: draft.difficultes ?? '');
      _notes[draft.ameId] =
          TextEditingController(text: draft.notesComplementaires ?? '');
      _nbSorties[draft.ameId] = draft.nbSorties;
      _submitted[draft.ameId] = draft.synced;
    }
  }

  void _initDataStructures() {
    for (var soul in _souls) {
      _presences.putIfAbsent(soul.id, () => {
            'Dimanche Matin': false,
            'Mercredi Soir': false,
            'Vendredi Soir': false,
          });
      _absenceRaisons.putIfAbsent(soul.id, () => null);
      _absenceCommentaires.putIfAbsent(soul.id, () => TextEditingController());
      _difficultes.putIfAbsent(soul.id, () => TextEditingController());
      _notes.putIfAbsent(soul.id, () => TextEditingController());
      _nbSorties.putIfAbsent(soul.id, () => 0);
      _fichierIds.putIfAbsent(soul.id, () => {});
      _submitted.putIfAbsent(soul.id, () => false);
      _syncing.putIfAbsent(soul.id, () => false);
      _expanded.putIfAbsent(soul.id, () => false);
    }
    _isLoading = false;
  }

  Future<void> _saveOffline(String ameId) async {
    final syncService = ref.read(syncServiceProvider);
    setState(() => _syncing[ameId] = true);
    await syncService.saveReportLocally(
      ameId: ameId,
      semaine: _semaine,
      presencesParCulte: _presences[ameId] ?? {},
      absenceRaison: _absenceRaisons[ameId],
      absenceCommentaire: _absenceCommentaires[ameId]?.text,
      difficultes: _difficultes[ameId]?.text,
      notesComplementaires: _notes[ameId]?.text,
      nbSorties: _nbSorties[ameId] ?? 0,
      fichierIds: _fichierIds[ameId]?.toList() ?? [],
    );
    setState(() {
      _submitted[ameId] = true;
      _syncing[ameId] = false;
      _offlineDrafts.add(ameId);
    });
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Row(
          children: [
            Icon(_isOffline ? Icons.save : Icons.check_circle,
                color: Colors.white, size: 20),
            const SizedBox(width: 8),
            Text(_isOffline
                ? 'Rapport sauvegardé localement'
                : 'Rapport soumis avec succès'),
          ],
        ),
        backgroundColor: _isOffline ? Colors.orange.shade700 : AppColors.primary,
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ));
    }
  }

  Future<void> _autoSync() async {
    final syncService = ref.read(syncServiceProvider);
    final result = await syncService.syncPending();
    if (result.synced > 0 && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Row(
          children: [
            const Icon(Icons.cloud_done, color: Colors.white, size: 20),
            const SizedBox(width: 8),
            Text('${result.synced} rapport(s) synchronisé(s)'),
          ],
        ),
        backgroundColor: AppColors.primary,
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ));
      _loadOfflineDrafts();
    }
    if (result.failed > 0 && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Row(
          children: [
            const Icon(Icons.error, color: Colors.white, size: 20),
            const SizedBox(width: 8),
            Text('${result.failed} rapport(s) en échec'),
          ],
        ),
        backgroundColor: Colors.red.shade600,
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ));
    }
  }

  int get submittedCount => _submitted.values.where((v) => v).length;
  int get pendingSyncCount => _offlineDrafts.length;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      extendBodyBehindAppBar: true,
      appBar: AppBar(
        title: Row(
          children: [
            Container(
              width: 8, height: 8,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: _isOffline ? Colors.orange : AppColors.primary,
                boxShadow: [BoxShadow(
                  color: (_isOffline ? Colors.orange : AppColors.primary).withValues(alpha: 0.5),
                  blurRadius: 8,
                )],
              ),
            ),
            const SizedBox(width: 8),
            const Text('Rapport hebdo', style: TextStyle(color: Colors.white, fontSize: 18)),
          ],
        ),
        actions: [
          if (_isOffline)
            Padding(
              padding: const EdgeInsets.only(right: 4),
              child: StatusBadge(
                label: 'Hors-ligne',
                color: Colors.orange,
                glowing: true,
              ),
            ),
          if (pendingSyncCount > 0)
            Padding(
              padding: const EdgeInsets.only(right: 4),
              child: Stack(
                children: [
                  IconButton(
                    icon: const Icon(Icons.sync, color: Colors.orange),
                    onPressed: _autoSync,
                  ),
                  Positioned(right: 4, top: 4, child: Container(
                    width: 18, height: 18,
                    decoration: const BoxDecoration(
                      color: Colors.orange, shape: BoxShape.circle,
                    ),
                    child: Center(child: Text(
                      '$pendingSyncCount',
                      style: const TextStyle(color: Colors.white, fontSize: 10, fontWeight: FontWeight.bold),
                    )),
                  )),
                ],
              ),
            ),
          IconButton(
            icon: const Icon(Icons.refresh, color: Colors.white60),
            onPressed: _loadData,
          ),
        ],
      ),
      drawer: const AppDrawer(),
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [
              const Color(0xFF030712),
              const Color(0xFF111827).withValues(alpha: 0.9),
              const Color(0xFF030712),
            ],
          ),
        ),
        child: _isLoading
            ? const ShimmerLoading(itemCount: 4)
            : _souls.isEmpty
                ? Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Container(
                          padding: const EdgeInsets.all(24),
                          decoration: BoxDecoration(
                            color: Colors.white.withValues(alpha: 0.05),
                            shape: BoxShape.circle,
                          ),
                          child: Icon(
                            _isOffline ? Icons.cloud_off : Icons.favorite_border,
                            size: 48, color: Colors.white24,
                          ),
                        ),
                        const SizedBox(height: 16),
                        Text(
                          _isOffline
                              ? 'Aucune donnée en cache'
                              : 'Aucune âme assignée',
                          style: TextStyle(
                            color: Colors.white.withValues(alpha: 0.4),
                            fontSize: 16,
                          ),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          _isOffline
                              ? 'Connectez-vous à internet pour\ncharger vos âmes'
                              : 'Votre faiseur vous attribuera\nbientôt des âmes à suivre',
                          textAlign: TextAlign.center,
                          style: TextStyle(
                            color: Colors.white.withValues(alpha: 0.2),
                            fontSize: 13,
                          ),
                        ),
                      ],
                    ),
                  )
                : RefreshIndicator(
                    onRefresh: _loadData,
                    color: AppColors.primary,
                    child: ListView.builder(
                      padding: const EdgeInsets.fromLTRB(16, 16, 16, 100),
                      itemCount: _souls.length + 1,
                      itemBuilder: (context, index) {
                        if (index == 0) {
                          return _buildHeader();
                        }
                        final soul = _souls[index - 1];
                        return _buildSoulCard(soul, index - 1);
                      },
                    ),
                  ),
      ),
    );
  }

  Widget _buildHeader() {
    final total = _souls.length;
    final submitted = submittedCount;
    final progress = total > 0 ? submitted / total : 0.0;

    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: GlassCard(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Semaine du ${_semaine.substring(8, 10)}/${_semaine.substring(5, 7)}/${_semaine.substring(0, 4)}',
                        style: TextStyle(
                          color: Colors.white.withValues(alpha: 0.6),
                          fontSize: 12,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        '$submitted / $total rapports',
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 20,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                ),
                SizedBox(
                  width: 48, height: 48,
                  child: Stack(
                    alignment: Alignment.center,
                    children: [
                      SizedBox(
                        width: 48, height: 48,
                        child: CircularProgressIndicator(
                          value: progress,
                          strokeWidth: 4,
                          backgroundColor: Colors.white.withValues(alpha: 0.08),
                          valueColor: AlwaysStoppedAnimation(AppColors.primary),
                        ),
                      ),
                      Text(
                        '${(progress * 100).toInt()}%',
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 11,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            if (progress < 1.0) ...[
              const SizedBox(height: 12),
              ClipRRect(
                borderRadius: BorderRadius.circular(4),
                child: LinearProgressIndicator(
                  value: progress,
                  backgroundColor: Colors.white.withValues(alpha: 0.08),
                  valueColor: AlwaysStoppedAnimation(AppColors.primary),
                  minHeight: 3,
                ),
              ),
            ],
            if (_isOffline) ...[
              const SizedBox(height: 12),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                decoration: BoxDecoration(
                  color: Colors.orange.withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.orange.withValues(alpha: 0.2)),
                ),
                child: Row(
                  children: [
                    Icon(Icons.cloud_off, color: Colors.orange.shade300, size: 16),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        'Mode hors-ligne — les rapports seront synchronisés automatiquement',
                        style: TextStyle(color: Colors.orange.shade200, fontSize: 11),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildSoulCard(Soul soul, int index) {
    final ameId = soul.id;
    final soumis = _submitted[ameId] ?? false;
    final isSyncingNow = _syncing[ameId] ?? false;
    final isOfflineDraft = _offlineDrafts.contains(ameId);
    final isExpanded = _expanded[ameId] ?? false;
    final typeLabel = soul.typeDisciple == 'NOUVEAU_CONVERTI'
        ? 'Nouveau converti'
        : 'Nouvel arrivant';
    final presenceCount =
        (_presences[ameId]?.values.where((v) => v).length ?? 0);

    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: GlassCard(
        padding: EdgeInsets.zero,
        borderColor: soumis
            ? (isOfflineDraft
                ? Colors.orange.withValues(alpha: 0.3)
                : AppColors.primary.withValues(alpha: 0.3))
            : null,
        child: Column(
          children: [
            // Card header
            InkWell(
              borderRadius: const BorderRadius.vertical(top: Radius.circular(16)),
              onTap: () => setState(() => _expanded[ameId] = !isExpanded),
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Row(
                  children: [
                    GradientAvatar(
                      text: soul.nom[0],
                      radius: 22,
                      showGlow: soumis,
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Flexible(
                                child: Text(
                                  soul.nomComplet,
                                  style: const TextStyle(
                                    color: Colors.white,
                                    fontSize: 15,
                                    fontWeight: FontWeight.w600,
                                  ),
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                              const SizedBox(width: 6),
                              StatusBadge(
                                label: typeLabel == 'Nouveau converti' ? 'NC' : 'NA',
                                color: typeLabel == 'Nouveau converti'
                                    ? Colors.blue
                                    : Colors.purple,
                              ),
                            ],
                          ),
                          const SizedBox(height: 4),
                          Row(
                            children: [
                              Icon(Icons.check_circle_outline,
                                  size: 12,
                                  color: presenceCount > 0
                                      ? AppColors.primary
                                      : Colors.white24),
                              const SizedBox(width: 4),
                              Text(
                                '$presenceCount/3 présences',
                                style: TextStyle(
                                  color: presenceCount > 0
                                      ? AppColors.primary
                                      : Colors.white.withValues(alpha: 0.3),
                                  fontSize: 11,
                                ),
                              ),
                              const SizedBox(width: 12),
                              if (soumis)
                                StatusBadge(
                                  label: isOfflineDraft ? 'En attente' : 'Soumis',
                                  color: isOfflineDraft ? Colors.orange : Colors.green,
                                  glowing: isOfflineDraft,
                                ),
                            ],
                          ),
                        ],
                      ),
                    ),
                    if (isSyncingNow)
                      const SizedBox(
                        width: 20, height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    else
                      AnimatedRotation(
                        turns: isExpanded ? 0.5 : 0,
                        duration: const Duration(milliseconds: 200),
                        child: Icon(
                          Icons.chevron_left,
                          color: Colors.white.withValues(alpha: 0.3),
                          size: 20,
                        ),
                      ),
                  ],
                ),
              ),
            ),

            // Expanded form
            AnimatedCrossFade(
              firstChild: const SizedBox.shrink(),
              secondChild: _buildReportForm(soul, isOfflineDraft, soumis),
              crossFadeState: isExpanded
                  ? CrossFadeState.showSecond
                  : CrossFadeState.showFirst,
              duration: const Duration(milliseconds: 250),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildReportForm(Soul soul, bool isOfflineDraft, bool soumis) {
    final ameId = soul.id;

    return Container(
      decoration: BoxDecoration(
        border: Border(
          top: BorderSide(color: Colors.white.withValues(alpha: 0.06)),
        ),
      ),
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Offline banner
          if (isOfflineDraft && soumis)
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
              margin: const EdgeInsets.only(bottom: 16),
              decoration: BoxDecoration(
                color: Colors.orange.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: Colors.orange.withValues(alpha: 0.2)),
              ),
              child: Row(
                children: [
                  Icon(Icons.cloud_upload, color: Colors.orange.shade300, size: 18),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      'En attente de synchronisation',
                      style: TextStyle(color: Colors.orange.shade200, fontSize: 13),
                    ),
                  ),
                  TextButton(
                    onPressed: _autoSync,
                    child: const Text('Sync', style: TextStyle(fontSize: 12)),
                  ),
                ],
              ),
            ),

          // Presence section
          _buildSectionTitle('Présence par culte', Icons.event_available),
          const SizedBox(height: 8),
          ...(_presences[ameId] ?? {}).entries.map((entry) => Padding(
                padding: const EdgeInsets.only(bottom: 2),
                child: InkWell(
                  onTap: soumis
                      ? null
                      : () => setState(() {
                            _presences[ameId]![entry.key] = !entry.value;
                          }),
                  child: Padding(
                    padding: const EdgeInsets.symmetric(vertical: 4),
                    child: Row(
                      children: [
                        Container(
                          width: 22, height: 22,
                          decoration: BoxDecoration(
                            color: entry.value
                                ? AppColors.primary
                                : Colors.transparent,
                            borderRadius: BorderRadius.circular(6),
                            border: Border.all(
                              color: entry.value
                                  ? AppColors.primary
                                  : Colors.white.withValues(alpha: 0.15),
                              width: 1.5,
                            ),
                          ),
                          child: entry.value
                              ? const Icon(Icons.check,
                                  color: Colors.white, size: 14)
                              : null,
                        ),
                        const SizedBox(width: 10),
                        Text(
                          entry.key,
                          style: TextStyle(
                            color: entry.value
                                ? Colors.white
                                : Colors.white.withValues(alpha: 0.6),
                            fontSize: 14,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              )),
          const SizedBox(height: 20),

          // Absence section
          _buildSectionTitle("Raison d'absence", Icons.help_outline),
          const SizedBox(height: 8),
          Container(
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: 0.05),
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: Colors.white.withValues(alpha: 0.1)),
            ),
            padding: const EdgeInsets.symmetric(horizontal: 12),
            child: DropdownButtonHideUnderline(
              child: DropdownButton<String>(
                value: _absenceRaisons[ameId],
                isExpanded: true,
                hint: Text(
                  'Sélectionner un motif...',
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.3)),
                ),
                dropdownColor: const Color(0xFF1F2937),
                style: const TextStyle(color: Colors.white, fontSize: 14),
                items: const [
                  DropdownMenuItem(value: 'MALADIE', child: Text('Maladie')),
                  DropdownMenuItem(value: 'VOYAGE', child: Text('Voyage')),
                  DropdownMenuItem(value: 'INDISPONIBILITE', child: Text('Indisponibilité')),
                  DropdownMenuItem(value: 'INJOIGNABLE', child: Text('Injoignable')),
                  DropdownMenuItem(value: 'NON_RENSEIGNE', child: Text('Non renseigné')),
                  DropdownMenuItem(value: 'AUTRE', child: Text('Autre')),
                ],
                onChanged: soumis ? null : (v) => setState(() => _absenceRaisons[ameId] = v),
              ),
            ),
          ),
          const SizedBox(height: 8),
          TextField(
            controller: _absenceCommentaires[ameId],
            enabled: !soumis,
            decoration: const InputDecoration(
              hintText: "Commentaire sur l'absence...",
              isDense: true,
            ),
            maxLines: 2,
          ),
          const SizedBox(height: 20),

          // Difficulties
          _buildSectionTitle('Difficultés / Challenges', Icons.warning_amber),
          const SizedBox(height: 8),
          TextField(
            controller: _difficultes[ameId],
            enabled: !soumis,
            decoration: const InputDecoration(
              hintText: 'Description des difficultés rencontrées...',
            ),
            maxLines: 3,
          ),
          const SizedBox(height: 20),

          // Exit count
          _buildSectionTitle('Sorties du suivi', Icons.exit_to_app),
          const SizedBox(height: 8),
          GlassCard(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  'Nombre de sorties',
                  style: TextStyle(
                    color: Colors.white.withValues(alpha: 0.6),
                    fontSize: 13,
                  ),
                ),
                Row(
                  children: [
                    _buildCircleButton(
                      Icons.remove,
                      soumis || (_nbSorties[ameId] ?? 0) <= 0,
                      () => setState(
                          () => _nbSorties[ameId] = (_nbSorties[ameId] ?? 1) - 1),
                    ),
                    const SizedBox(width: 16),
                    Text(
                      '${_nbSorties[ameId] ?? 0}',
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 22,
                        fontWeight: FontWeight.bold,
                        fontFamily: 'monospace',
                      ),
                    ),
                    const SizedBox(width: 16),
                    _buildCircleButton(
                      Icons.add,
                      soumis,
                      () => setState(
                          () => _nbSorties[ameId] = (_nbSorties[ameId] ?? 0) + 1),
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),

          // Notes
          _buildSectionTitle('Notes complémentaires', Icons.notes),
          const SizedBox(height: 8),
          TextField(
            controller: _notes[ameId],
            enabled: !soumis,
            decoration: const InputDecoration(
              hintText: 'Notes additionnelles sur la semaine...',
            ),
            maxLines: 2,
          ),
          const SizedBox(height: 20),

          // Pièces jointes (sélecteur partagé du module Fichiers) — masqué une fois soumis
          if (!soumis) ...[
            _buildSectionTitle('Pièces jointes', Icons.attach_file),
            const SizedBox(height: 8),
            AttachmentPickerField(
              apiService: _apiService,
              value: _fichierIds[ameId] ?? {},
              onChanged: (ids) => setState(() => _fichierIds[ameId] = Set.of(ids)),
            ),
            const SizedBox(height: 20),
          ],

          // Submit button
          if (!soumis)
            SizedBox(
              width: double.infinity,
              child: FilledButton.icon(
                onPressed: () => _saveOffline(ameId),
                style: FilledButton.styleFrom(
                  backgroundColor: _isOffline
                      ? Colors.orange.shade600
                      : AppColors.primary,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                ),
                icon: Icon(
                  _isOffline ? Icons.save : Icons.send,
                  size: 18,
                ),
                label: Text(
                  _isOffline ? 'Sauvegarder localement' : 'Soumettre le rapport',
                ),
              ),
            ),
          if (isOfflineDraft && soumis)
            SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
                onPressed: _autoSync,
                icon: const Icon(Icons.sync, size: 18),
                label: const Text('Synchroniser maintenant'),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildSectionTitle(String title, IconData icon) {
    return Row(
      children: [
        Container(
          padding: const EdgeInsets.all(6),
          decoration: BoxDecoration(
            color: AppColors.primary.withValues(alpha: 0.15),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Icon(icon, color: AppColors.primaryLight, size: 14),
        ),
        const SizedBox(width: 8),
        Text(
          title,
          style: const TextStyle(
            color: Colors.white,
            fontSize: 14,
            fontWeight: FontWeight.w600,
          ),
        ),
      ],
    );
  }

  Widget _buildCircleButton(IconData icon, bool disabled, VoidCallback onTap) {
    return GestureDetector(
      onTap: disabled ? null : onTap,
      child: Container(
        width: 36, height: 36,
        decoration: BoxDecoration(
          color: disabled
              ? Colors.white.withValues(alpha: 0.03)
              : Colors.white.withValues(alpha: 0.08),
          borderRadius: BorderRadius.circular(18),
          border: Border.all(
            color: disabled
                ? Colors.white.withValues(alpha: 0.05)
                : Colors.white.withValues(alpha: 0.15),
          ),
        ),
        child: Icon(
          icon,
          color: disabled ? Colors.white12 : Colors.white54,
          size: 18,
        ),
      ),
    );
  }
}
