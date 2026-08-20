import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

/// Écrans de discipline pour le Responsable.
/// - Liste des événements disciplinaires (par catégorie, statut, recherche)
/// - Création d'un nouvel événement (POST /souls/{soulId}/discipline)
/// - Résolution (PATCH /souls/{soulId}/discipline/{id}/resolve)
/// - Suppression (DELETE /souls/{soulId}/discipline/{id})
/// - Lien vers la fiche âme
class DisciplineScreen extends StatefulWidget {
  const DisciplineScreen({super.key});

  @override
  State<DisciplineScreen> createState() => _DisciplineScreenState();
}

class _DisciplineScreenState extends State<DisciplineScreen> {
  final _apiService = ApiService();
  final _searchCtrl = TextEditingController();

  List<_DisciplineEvent> _events = [];
  bool _isLoading = true;
  String _filterCategorie = 'all';
  String _filterStatut = 'all'; // 'all', 'EN_COURS', 'RESOLU'
  String _searchQuery = '';

  // Create form state
  bool _showCreateForm = false;
  String _formCategorie = 'COMPORTEMENT';
  String _formType = 'REPROCHE';
  String _formGravite = 'MOYENNE';
  final _formTitreCtrl = TextEditingController();
  final _formDescCtrl = TextEditingController();
  String _formDate = DateTime.now().toIso8601String().substring(0, 10);
  String? _formSoulId;
  bool _isCreating = false;

  // Souls list for picker
  List<dynamic> _souls = [];

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    _formTitreCtrl.dispose();
    _formDescCtrl.dispose();
    super.dispose();
  }

  // ── Data loading ─────────────────────────────────────────────────────

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      // Load souls for the picker
      final soulsRes = await _apiService.get('/souls', params: {'size': 200});
      _souls = (soulsRes.data is Map ? soulsRes.data['content'] : soulsRes.data) as List<dynamic>? ?? [];

      // Load discipline events for each soul (up to 50 for performance)
      final List<_DisciplineEvent> allEvents = [];
      for (final soul in _souls.take(50)) {
        final s = soul as Map<String, dynamic>;
        final soulId = s['id']?.toString();
        if (soulId == null || soulId.isEmpty) continue;
        try {
          final discRes = await _apiService.get('/souls/$soulId/discipline', params: {'size': 50});
          final pageData = discRes.data as Map<String, dynamic>?;
          final items = (pageData?['content'] as List<dynamic>?) ?? [];
          for (final e in items) {
            final ev = e as Map<String, dynamic>;
            allEvents.add(_DisciplineEvent(
              id: ev['id']?.toString() ?? '',
              soulId: soulId,
              soulNom: s['nom']?.toString() ?? '',
              categorie: ev['categorie']?.toString() ?? '',
              typeEvenement: ev['typeEvenement']?.toString() ?? '',
              titre: ev['titre']?.toString() ?? '',
              description: ev['description']?.toString(),
              gravite: ev['gravite']?.toString() ?? 'MOYENNE',
              dateEvenement: ev['dateEvenement']?.toString() ?? '',
              resolu: ev['resolu'] == true,
              dateResolution: ev['dateResolution']?.toString(),
              createdAt: ev['createdAt']?.toString() ?? '',
            ));
          }
        } catch (_) {}
      }

      // Sort by date descending
      allEvents.sort((a, b) => b.createdAt.compareTo(a.createdAt));

      if (mounted) {
        setState(() {
          _events = allEvents;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  // ── Filtering ────────────────────────────────────────────────────────

  List<_DisciplineEvent> get _filteredEvents {
    return _events.where((e) {
      if (_filterCategorie != 'all' && e.categorie != _filterCategorie) return false;
      if (_filterStatut == 'EN_COURS' && e.resolu) return false;
      if (_filterStatut == 'RESOLU' && !e.resolu) return false;
      if (_searchQuery.isNotEmpty) {
        final q = _searchQuery.toLowerCase();
        if (!e.soulNom.toLowerCase().contains(q) &&
            !e.titre.toLowerCase().contains(q) &&
            !(e.description?.toLowerCase().contains(q) ?? false)) {
          return false;
        }
      }
      return true;
    }).toList();
  }

  // ── Actions ──────────────────────────────────────────────────────────

  Future<void> _resolveEvent(_DisciplineEvent event) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.cardDark,
        title: const Text('Résoudre cet événement ?', style: TextStyle(color: Colors.white)),
        content: Text(
          'Marquer "${event.titre}" comme résolu ?',
          style: TextStyle(color: Colors.white.withValues(alpha: 0.8)),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: FilledButton.styleFrom(backgroundColor: Colors.green),
            child: const Text('Résoudre'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;

    try {
      await _apiService.patch('/souls/${event.soulId}/discipline/${event.id}/resolve');
      HapticFeedback.lightImpact();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('✅ Événement résolu'), backgroundColor: Colors.green),
        );
        _loadData();
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Échec de la résolution'), backgroundColor: Colors.red),
        );
      }
    }
  }

  Future<void> _deleteEvent(_DisciplineEvent event) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.cardDark,
        title: const Text('Supprimer cet événement ?', style: TextStyle(color: Colors.white)),
        content: Text(
          'Supprimer définitivement "${event.titre}" ?',
          style: TextStyle(color: Colors.white.withValues(alpha: 0.8)),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: FilledButton.styleFrom(backgroundColor: Colors.red),
            child: const Text('Supprimer'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;

    try {
      await _apiService.delete('/souls/${event.soulId}/discipline/${event.id}');
      HapticFeedback.lightImpact();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Événement supprimé'), backgroundColor: Colors.orange),
        );
        _loadData();
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Échec de la suppression'), backgroundColor: Colors.red),
        );
      }
    }
  }

  Future<void> _createEvent() async {
    if (_formSoulId == null || _formTitreCtrl.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Sélectionnez un membre et saisissez un titre')),
      );
      return;
    }

    setState(() => _isCreating = true);
    try {
      await _apiService.post('/souls/$_formSoulId/discipline', data: {
        'categorie': _formCategorie,
        'typeEvenement': _formType,
        'titre': _formTitreCtrl.text.trim(),
        'description': _formDescCtrl.text.trim().isEmpty ? null : _formDescCtrl.text.trim(),
        'gravite': _formGravite,
        'dateEvenement': _formDate,
      });
      HapticFeedback.mediumImpact();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('✅ Événement disciplinaire enregistré'), backgroundColor: Colors.green),
        );
        setState(() {
          _showCreateForm = false;
          _formTitreCtrl.clear();
          _formDescCtrl.clear();
          _formSoulId = null;
        });
        _loadData();
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Échec de la création'), backgroundColor: Colors.red),
        );
      }
    } finally {
      if (mounted) setState(() => _isCreating = false);
    }
  }

  // ── Build ────────────────────────────────────────────────────────────

  @override
  Widget build(BuildContext context) {
    final filtered = _filteredEvents;
    final enCours = _events.where((e) => !e.resolu).length;
    final resolus = _events.where((e) => e.resolu).length;

    return Scaffold(
      appBar: AppBar(
        title: Text('Discipline · ${_events.length}'),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 6)
          : Column(
              children: [
                // ═══ HEADER ═══
                GlassCard(
                  margin: const EdgeInsets.fromLTRB(12, 12, 12, 0),
                  padding: const EdgeInsets.all(12),
                  child: Column(
                    children: [
                      // Stats
                      Row(
                        children: [
                          _statChip('Total', '${_events.length}', Colors.blue),
                          const SizedBox(width: 6),
                          _statChip('En cours', '$enCours', Colors.amber),
                          const SizedBox(width: 6),
                          _statChip('Résolus', '$resolus', Colors.green),
                        ],
                      ),
                      const SizedBox(height: 10),

                      // Statut filter
                      Row(
                        children: [
                          _statusChip('Tous', 'all', Colors.blue),
                          const SizedBox(width: 6),
                          _statusChip('En cours', 'EN_COURS', Colors.amber),
                          const SizedBox(width: 6),
                          _statusChip('Résolus', 'RESOLU', Colors.green),
                        ],
                      ),
                      const SizedBox(height: 8),

                      // Catégorie filter
                      SingleChildScrollView(
                        scrollDirection: Axis.horizontal,
                        child: Row(
                          children: [
                            _catChip('Toutes', 'all'),
                            for (final cat in _categorieLabels.entries) ...[
                              const SizedBox(width: 6),
                              _catChip(cat.value, cat.key),
                            ],
                          ],
                        ),
                      ),
                    ],
                  ),
                ),

                // ═══ SEARCH ═══
                if (_events.length > 5)
                  Padding(
                    padding: const EdgeInsets.fromLTRB(12, 8, 12, 0),
                    child: TextField(
                      controller: _searchCtrl,
                      style: const TextStyle(color: Colors.white, fontSize: 13),
                      onChanged: (v) => setState(() => _searchQuery = v),
                      decoration: InputDecoration(
                        hintText: 'Rechercher par nom, titre…',
                        hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.3)),
                        prefixIcon: Icon(Icons.search, color: Colors.white.withValues(alpha: 0.4), size: 18),
                        suffixIcon: _searchQuery.isNotEmpty
                            ? IconButton(
                                icon: Icon(Icons.clear, color: Colors.white.withValues(alpha: 0.4), size: 16),
                                onPressed: () {
                                  _searchCtrl.clear();
                                  setState(() => _searchQuery = '');
                                },
                              )
                            : null,
                        isDense: true,
                        contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(10),
                          borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.1)),
                        ),
                        enabledBorder: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(10),
                          borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.1)),
                        ),
                        filled: true,
                        fillColor: Colors.white.withValues(alpha: 0.05),
                      ),
                    ),
                  ),

                const SizedBox(height: 8),

                // ═══ EVENTS LIST ═══
                Expanded(
                  child: filtered.isEmpty
                      ? _buildEmptyState()
                      : ListView.builder(
                          padding: const EdgeInsets.symmetric(horizontal: 12),
                          itemCount: filtered.length,
                          itemBuilder: (context, index) => _buildEventCard(filtered[index]),
                        ),
                ),
              ],
            ),

      // ═══ FAB ═══
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => setState(() => _showCreateForm = !_showCreateForm),
        icon: Icon(_showCreateForm ? Icons.close : Icons.add),
        label: Text(_showCreateForm ? 'Annuler' : 'Nouvel événement'),
        backgroundColor: _showCreateForm ? Colors.red : AppColors.primary,
      ),

      // ═══ CREATE FORM BOTTOM SHEET ═══
      bottomSheet: _showCreateForm ? _buildCreateSheet() : null,
    );
  }

  // ── Create form ──────────────────────────────────────────────────────

  Widget _buildCreateSheet() {
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFF1A1F2E),
        borderRadius: const BorderRadius.vertical(top: Radius.circular(20)),
        border: Border(top: BorderSide(color: Colors.white.withValues(alpha: 0.1))),
      ),
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Handle bar
            Center(
              child: Container(
                width: 40,
                height: 4,
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.2),
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            const SizedBox(height: 12),
            const Text('Nouvel événement disciplinaire',
                style: TextStyle(color: Colors.white, fontWeight: FontWeight.w700, fontSize: 15)),
            const SizedBox(height: 12),

            // Membre picker
            _label('Membre *'),
            DropdownButton<String>(
              value: _formSoulId,
              dropdownColor: AppColors.cardDark,
              isExpanded: true,
              underline: const SizedBox(),
              style: const TextStyle(color: Colors.white, fontSize: 13),
              hint: Text('Sélectionner un membre', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
              items: _souls.cast<Map<String, dynamic>>().map((s) {
                return DropdownMenuItem<String>(
                  value: s['id']?.toString(),
                  child: Text(s['nom']?.toString() ?? ''),
                );
              }).toList(),
              onChanged: (v) => setState(() => _formSoulId = v),
            ),
            const SizedBox(height: 10),

            // Catégorie + Type
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      _label('Catégorie *'),
                      DropdownButton<String>(
                        value: _formCategorie,
                        dropdownColor: AppColors.cardDark,
                        isExpanded: true,
                        underline: const SizedBox(),
                        style: const TextStyle(color: Colors.white, fontSize: 12),
                        items: _categorieLabels.entries.map((e) {
                          return DropdownMenuItem(value: e.key, child: Text(e.value));
                        }).toList(),
                        onChanged: (v) => setState(() => _formCategorie = v ?? _formCategorie),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      _label('Type *'),
                      DropdownButton<String>(
                        value: _formType,
                        dropdownColor: AppColors.cardDark,
                        isExpanded: true,
                        underline: const SizedBox(),
                        style: const TextStyle(color: Colors.white, fontSize: 12),
                        items: _typeLabels.entries.map((e) {
                          return DropdownMenuItem(value: e.key, child: Text(e.value));
                        }).toList(),
                        onChanged: (v) => setState(() => _formType = v ?? _formType),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 10),

            // Titre
            _label('Titre *'),
            TextField(
              controller: _formTitreCtrl,
              style: const TextStyle(color: Colors.white, fontSize: 13),
              decoration: _inputDecoration('Ex: Absence répétée aux cultes'),
            ),
            const SizedBox(height: 10),

            // Description
            _label('Description'),
            TextField(
              controller: _formDescCtrl,
              style: const TextStyle(color: Colors.white, fontSize: 13),
              maxLines: 3,
              decoration: _inputDecoration('Détails de l\'événement…'),
            ),
            const SizedBox(height: 10),

            // Gravité + Date
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      _label('Gravité'),
                      DropdownButton<String>(
                        value: _formGravite,
                        dropdownColor: AppColors.cardDark,
                        isExpanded: true,
                        underline: const SizedBox(),
                        style: const TextStyle(color: Colors.white, fontSize: 12),
                        items: _graviteLabels.entries.map((e) {
                          return DropdownMenuItem(value: e.key, child: Text(e.value));
                        }).toList(),
                        onChanged: (v) => setState(() => _formGravite = v ?? _formGravite),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      _label('Date'),
                      GestureDetector(
                        onTap: () async {
                          final picked = await showDatePicker(
                            context: context,
                            initialDate: DateTime.parse(_formDate),
                            firstDate: DateTime(2020),
                            lastDate: DateTime.now().add(const Duration(days: 1)),
                          );
                          if (picked != null) {
                            setState(() => _formDate = picked.toIso8601String().substring(0, 10));
                          }
                        },
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                          decoration: BoxDecoration(
                            border: Border.all(color: Colors.white.withValues(alpha: 0.15)),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Row(
                            children: [
                              Icon(Icons.calendar_today, color: Colors.white.withValues(alpha: 0.5), size: 14),
                              const SizedBox(width: 8),
                              Text(_formDate, style: const TextStyle(color: Colors.white, fontSize: 12)),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),

            // Submit
            SizedBox(
              width: double.infinity,
              height: 44,
              child: FilledButton.icon(
                onPressed: _isCreating || _formSoulId == null || _formTitreCtrl.text.trim().isEmpty
                    ? null
                    : _createEvent,
                icon: _isCreating
                    ? const SizedBox(
                        width: 16, height: 16,
                        child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                      )
                    : const Icon(Icons.send, size: 16),
                label: const Text('Enregistrer'),
                style: FilledButton.styleFrom(backgroundColor: AppColors.primary),
              ),
            ),
          ],
        ),
      ),
    );
  }

  // ── Event card ───────────────────────────────────────────────────────

  Widget _buildEventCard(_DisciplineEvent ev) {
    final graviteColor = _graviteColors[ev.gravite] ?? Colors.grey;

    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      borderColor: ev.resolu
          ? Colors.green.withValues(alpha: 0.2)
          : (ev.gravite == 'GRAVE' || ev.gravite == 'CRITIQUE')
              ? Colors.red.withValues(alpha: 0.3)
              : Colors.amber.withValues(alpha: 0.2),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header: avatar + nom + badges
          Row(
            children: [
              Container(
                width: 36,
                height: 36,
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    colors: ev.resolu
                        ? [Colors.green, Colors.green.withValues(alpha: 0.7)]
                        : [Colors.amber, Colors.amber.withValues(alpha: 0.7)],
                  ),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Center(
                  child: ev.resolu
                      ? const Icon(Icons.check, color: Colors.white, size: 18)
                      : Icon(Icons.gavel, color: Colors.white, size: 16),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Soul name — tappable
                    GestureDetector(
                      onTap: () => context.go('/souls/${ev.soulId}'),
                      child: Text(ev.soulNom,
                          style: TextStyle(
                            color: AppColors.primaryLight,
                            fontWeight: FontWeight.w600,
                            fontSize: 13,
                            decoration: TextDecoration.underline,
                            decorationColor: AppColors.primaryLight.withValues(alpha: 0.4),
                          )),
                    ),
                    const SizedBox(height: 2),
                    Text(ev.titre,
                        style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w500, fontSize: 12),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),

          // Badges row
          Wrap(
            spacing: 6,
            runSpacing: 4,
            children: [
              StatusBadge(
                label: ev.resolu ? 'Résolu' : 'En cours',
                color: ev.resolu ? Colors.green : Colors.amber,
              ),
              StatusBadge(label: _categorieLabels[ev.categorie] ?? ev.categorie, color: Colors.lightBlue),
              StatusBadge(label: ev.gravite, color: graviteColor),
              if (ev.typeEvenement.isNotEmpty)
                StatusBadge(label: _typeLabels[ev.typeEvenement] ?? ev.typeEvenement, color: Colors.white70),
            ],
          ),

          // Description
          if (ev.description != null && ev.description!.isNotEmpty) ...[
            const SizedBox(height: 8),
            Text(ev.description!,
                style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 12)),
          ],

          // Date + actions
          const SizedBox(height: 8),
          Row(
            children: [
              Icon(Icons.calendar_today, color: Colors.white.withValues(alpha: 0.3), size: 12),
              const SizedBox(width: 4),
              Text(ev.dateEvenement, style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 10)),
              if (ev.dateResolution != null) ...[
                const SizedBox(width: 8),
                Icon(Icons.check_circle, color: Colors.green.withValues(alpha: 0.5), size: 12),
                const SizedBox(width: 4),
                Text('Résolu ${ev.dateResolution}',
                    style: TextStyle(color: Colors.green.withValues(alpha: 0.5), fontSize: 10)),
              ],
              const Spacer(),
              // Actions
              if (!ev.resolu)
                _actionButton(
                  icon: Icons.check_circle_outline,
                  color: Colors.green,
                  tooltip: 'Résoudre',
                  onTap: () => _resolveEvent(ev),
                ),
              _actionButton(
                icon: Icons.delete_outline,
                color: Colors.red,
                tooltip: 'Supprimer',
                onTap: () => _deleteEvent(ev),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.gavel, color: Colors.white.withValues(alpha: 0.15), size: 56),
          const SizedBox(height: 12),
          Text(
            _searchQuery.isNotEmpty
                ? 'Aucun résultat pour "$_searchQuery"'
                : 'Aucun événement disciplinaire',
            style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 14),
          ),
          if (_searchQuery.isNotEmpty) ...[
            const SizedBox(height: 8),
            TextButton(
              onPressed: () {
                _searchCtrl.clear();
                setState(() => _searchQuery = '');
              },
              child: const Text('Effacer la recherche'),
            ),
          ],
        ],
      ),
    );
  }

  // ── Helper widgets ───────────────────────────────────────────────────

  Widget _statChip(String label, String value, Color color) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 6),
        decoration: BoxDecoration(
          color: color.withValues(alpha: 0.1),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Column(
          children: [
            Text(value, style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 15)),
            Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 9)),
          ],
        ),
      ),
    );
  }

  Widget _statusChip(String label, String value, Color color) {
    final isActive = _filterStatut == value;
    return GestureDetector(
      onTap: () => setState(() => _filterStatut = value),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        decoration: BoxDecoration(
          color: isActive ? color.withValues(alpha: 0.15) : Colors.white.withValues(alpha: 0.05),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: isActive ? color.withValues(alpha: 0.4) : Colors.white.withValues(alpha: 0.08)),
        ),
        child: Text(label,
            style: TextStyle(
              color: isActive ? color : Colors.white.withValues(alpha: 0.5),
              fontSize: 11,
              fontWeight: isActive ? FontWeight.w600 : FontWeight.normal,
            )),
      ),
    );
  }

  Widget _catChip(String label, String value) {
    final isActive = _filterCategorie == value;
    return GestureDetector(
      onTap: () => setState(() => _filterCategorie = value),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
        decoration: BoxDecoration(
          color: isActive ? AppColors.primary.withValues(alpha: 0.15) : Colors.white.withValues(alpha: 0.05),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: isActive ? AppColors.primary.withValues(alpha: 0.4) : Colors.white.withValues(alpha: 0.08)),
        ),
        child: Text(label,
            style: TextStyle(
              color: isActive ? AppColors.primaryLight : Colors.white.withValues(alpha: 0.45),
              fontSize: 10,
              fontWeight: isActive ? FontWeight.w600 : FontWeight.normal,
            )),
      ),
    );
  }

  Widget _actionButton({
    required IconData icon,
    required Color color,
    required String tooltip,
    required VoidCallback onTap,
  }) {
    return Tooltip(
      message: tooltip,
      child: GestureDetector(
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.all(6),
          decoration: BoxDecoration(
            color: color.withValues(alpha: 0.1),
            borderRadius: BorderRadius.circular(6),
          ),
          child: Icon(icon, color: color, size: 16),
        ),
      ),
    );
  }

  Widget _label(String text) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 4),
      child: Text(text, style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 11, fontWeight: FontWeight.w600)),
    );
  }

  InputDecoration _inputDecoration(String hint) {
    return InputDecoration(
      hintText: hint,
      hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.3)),
      isDense: true,
      contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8),
        borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.15)),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8),
        borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.15)),
      ),
    );
  }

  // ── Constants ────────────────────────────────────────────────────────

  static const _categorieLabels = {
    'COMPORTEMENT': 'Comportement',
    'CONDUITE': 'Conduite',
    'HABILLEMENT': 'Habillement',
    'VIE_SPIRITUELLE': 'Vie spirituelle',
    'PONCTUALITE': 'Ponctualité',
    'PARTICIPATION': 'Participation',
    'FIDELITE': 'Fidélité',
    'ENGAGEMENT': 'Engagement',
    'REPROCHE': 'Reproche',
    'SANCTION': 'Sanction',
    'LITIGE': 'Litige',
    'CONFLIT': 'Conflit',
    'SCANDALE': 'Scandale',
    'RELATION_PROBLEMATIQUE': 'Relation problématique',
    'FLIRT_INAPPROPRIE': 'Flirt inapproprié',
    'DEGAT_MATERIEL': 'Dégât matériel',
    'DEGAT_RELATIONNEL': 'Dégât relationnel',
    'RESOLUTION': 'Résolution',
    'TEMOIGNAGE_MEMBRE': 'Témoignage membre',
    'TEMOIGNAGE_RESPONSABLE': 'Témoignage responsable',
    'TEMOIGNAGE_CHEF': 'Témoignage chef',
    'COMMENTAIRE_PASTORAL': 'Commentaire pastoral',
    'AUTRE': 'Autre',
  };

  static const _typeLabels = {
    'REPROCHE': 'Reproche',
    'SANCTION': 'Sanction',
    'LITIGE': 'Litige',
    'CONFLIT': 'Conflit',
    'SCANDALE': 'Scandale',
    'OBSERVATION': 'Observation',
    'TEMOIGNAGE': 'Témoignage',
    'ENTRETIEN': 'Entretien pastoral',
    'RESOLUTION': 'Résolution',
    'AUTRE': 'Autre',
  };

  static const _graviteLabels = {
    'FAIBLE': 'Faible',
    'MOYENNE': 'Moyenne',
    'GRAVE': 'Grave',
    'CRITIQUE': 'Critique',
  };

  static const _graviteColors = {
    'FAIBLE': Colors.blue,
    'MOYENNE': Colors.amber,
    'GRAVE': Colors.red,
    'CRITIQUE': Colors.deepOrange,
  };
}

// ── Data model ───────────────────────────────────────────────────────

class _DisciplineEvent {
  final String id;
  final String soulId;
  final String soulNom;
  final String categorie;
  final String typeEvenement;
  final String titre;
  final String? description;
  final String gravite;
  final String dateEvenement;
  final bool resolu;
  final String? dateResolution;
  final String createdAt;

  const _DisciplineEvent({
    required this.id,
    required this.soulId,
    required this.soulNom,
    required this.categorie,
    required this.typeEvenement,
    required this.titre,
    this.description,
    required this.gravite,
    required this.dateEvenement,
    required this.resolu,
    this.dateResolution,
    required this.createdAt,
  });
}
