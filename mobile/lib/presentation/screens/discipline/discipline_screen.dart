import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

/// Gestion de la discipline pour le Responsable.
/// Liste des événements disciplinaires et création de nouveaux événements.
class DisciplineScreen extends StatefulWidget {
  const DisciplineScreen({super.key});

  @override
  State<DisciplineScreen> createState() => _DisciplineScreenState();
}

class _DisciplineScreenState extends State<DisciplineScreen> {
  final _apiService = ApiService();
  List<dynamic> _events = [];
  bool _isLoading = true;
  String _filterCategorie = 'all';
  final String _filterStatut = 'all';

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      // Charge les événements disciplinaires via la recherche globale
      final res = await _apiService.get('/souls', params: {'size': 200});
      final souls = (res.data is Map ? res.data['content'] : res.data) as List<dynamic>? ?? [];

      // Pour chaque disciple, récupère les événements disciplinaires
      final List<dynamic> allEvents = [];
      for (final soul in souls.take(50)) {
        final s = soul as Map<String, dynamic>;
        final soulId = s['id']?.toString();
        if (soulId == null || soulId.isEmpty) continue;
        try {
          final discRes = await _apiService.get('/souls/$soulId/discipline', params: {'size': 50});
          final events = (discRes.data is Map ? discRes.data['content'] : discRes.data) as List<dynamic>? ?? [];
          for (final e in events) {
            final ev = e as Map<String, dynamic>;
            ev['soulNom'] = s['nom'];
            ev['soulId'] = soulId;
            allEvents.add(ev);
          }
        } catch (_) {}
      }

      // Trier par date décroissante
      allEvents.sort((a, b) {
        final da = a['createdAt']?.toString() ?? '';
        final db = b['createdAt']?.toString() ?? '';
        return db.compareTo(da);
      });

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

  List<dynamic> get _filteredEvents {
    return _events.where((e) {
      final ev = e as Map<String, dynamic>;
      if (_filterCategorie != 'all' && ev['categorie']?.toString() != _filterCategorie) return false;
      if (_filterStatut != 'all' && ev['statut']?.toString() != _filterStatut) return false;
      return true;
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    final filtered = _filteredEvents;
    final enCours = _events.where((e) => (e as Map)['statut']?.toString() == 'EN_COURS').length;
    final resolus = _events.where((e) => (e as Map)['statut']?.toString() == 'RESOLU').length;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Discipline'),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 6)
          : RefreshIndicator(
              onRefresh: _loadData,
              child: Column(
                children: [
                  // Stats header
                  GlassCard(
                    margin: const EdgeInsets.all(12),
                    padding: const EdgeInsets.all(12),
                    child: Column(
                      children: [
                        Row(
                          children: [
                            _statChip('Total', '${_events.length}', Colors.blue),
                            const SizedBox(width: 8),
                            _statChip('En cours', '$enCours', Colors.amber),
                            const SizedBox(width: 8),
                            _statChip('Résolus', '$resolus', Colors.green),
                          ],
                        ),
                        const SizedBox(height: 12),
                        // Filter chips
                        SingleChildScrollView(
                          scrollDirection: Axis.horizontal,
                          child: Row(
                            children: [
                              _filterChip('Tous', 'all'),
                              const SizedBox(width: 6),
                              _filterChip('Comportement', 'COMPORTEMENT'),
                              const SizedBox(width: 6),
                              _filterChip('Assiduité', 'ASSIDUITE'),
                              const SizedBox(width: 6),
                              _filterChip('Discipline', 'DISCIPLINE'),
                              const SizedBox(width: 6),
                              _filterChip('Incident', 'INCIDENT'),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),

                  // Events list
                  Expanded(
                    child: filtered.isEmpty
                        ? Center(
                            child: Column(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Icon(Icons.gavel, color: Colors.white.withValues(alpha: 0.2), size: 48),
                                const SizedBox(height: 12),
                                Text('Aucun événement disciplinaire',
                                    style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                              ],
                            ),
                          )
                        : ListView.builder(
                            padding: const EdgeInsets.symmetric(horizontal: 12),
                            itemCount: filtered.length,
                            itemBuilder: (context, index) {
                              final ev = filtered[index] as Map<String, dynamic>;
                              return _eventCard(ev);
                            },
                          ),
                  ),
                ],
              ),
            ),
    );
  }

  Widget _eventCard(Map<String, dynamic> ev) {
    final statut = ev['statut']?.toString() ?? 'EN_COURS';
    final categorie = ev['categorie']?.toString() ?? '';
    final typeEvenement = ev['typeEvenement']?.toString() ?? '';
    final gravite = ev['gravite']?.toString() ?? 'MOYENNE';
    final soulNom = ev['soulNom']?.toString() ?? '';
    final description = ev['description']?.toString() ?? '';
    final createdAt = ev['createdAt']?.toString() ?? '';
    final resolvedAt = ev['resolvedAt']?.toString();

    final statutColor = statut == 'RESOLU' ? Colors.green : Colors.amber;
    final graviteColor = gravite == 'GRAVE' || gravite == 'CRITIQUE'
        ? Colors.red
        : gravite == 'MOYENNE'
            ? Colors.amber
            : Colors.blue;

    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      borderColor: statut == 'EN_COURS' ? Colors.amber.withValues(alpha: 0.3) : null,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              // Avatar
              Container(
                width: 36,
                height: 36,
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    colors: [statutColor, statutColor.withValues(alpha: 0.7)],
                  ),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Center(
                  child: statut == 'RESOLU'
                      ? const Icon(Icons.check, color: Colors.white, size: 18)
                      : Icon(Icons.gavel, color: Colors.white, size: 16),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(soulNom, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13)),
                    Text(categorie.replaceAll('_', ' '),
                        style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                  ],
                ),
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  StatusBadge(label: statut.replaceAll('_', ' '), color: statutColor),
                  const SizedBox(height: 4),
                  StatusBadge(label: gravite, color: graviteColor),
                ],
              ),
            ],
          ),
          if (description.isNotEmpty) ...[
            const SizedBox(height: 8),
            Text(description, style: const TextStyle(color: Colors.white, fontSize: 12)),
          ],
          const SizedBox(height: 6),
          Row(
            children: [
              if (typeEvenement.isNotEmpty) ...[
                StatusBadge(label: typeEvenement.replaceAll('_', ' '), color: Colors.lightBlue),
                const SizedBox(width: 8),
              ],
              Text(createdAt.length > 10 ? createdAt.substring(0, 10) : createdAt,
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 10)),
              if (resolvedAt != null) ...[
                const SizedBox(width: 8),
                Text('Résolu ${resolvedAt.length > 10 ? resolvedAt.substring(0, 10) : resolvedAt}',
                    style: TextStyle(color: Colors.green.withValues(alpha: 0.6), fontSize: 10)),
              ],
            ],
          ),
        ],
      ),
    );
  }

  Widget _statChip(String label, String value, Color color) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 8),
        decoration: BoxDecoration(
          color: color.withValues(alpha: 0.1),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Column(
          children: [
            Text(value, style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 16)),
            Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 10)),
          ],
        ),
      ),
    );
  }

  Widget _filterChip(String label, String value) {
    final isActive = _filterCategorie == value;
    return GestureDetector(
      onTap: () => setState(() => _filterCategorie = value),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        decoration: BoxDecoration(
          color: isActive ? AppColors.primary.withValues(alpha: 0.15) : Colors.white.withValues(alpha: 0.05),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: isActive ? AppColors.primary.withValues(alpha: 0.4) : Colors.white.withValues(alpha: 0.08)),
        ),
        child: Text(label,
            style: TextStyle(
              color: isActive ? AppColors.primaryLight : Colors.white.withValues(alpha: 0.5),
              fontSize: 11,
              fontWeight: isActive ? FontWeight.w600 : FontWeight.normal,
            )),
      ),
    );
  }
}
