import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

class ParallelFollowupsScreen extends StatefulWidget {
  const ParallelFollowupsScreen({super.key});

  @override
  State<ParallelFollowupsScreen> createState() => _ParallelFollowupsScreenState();
}

class _ParallelFollowupsScreenState extends State<ParallelFollowupsScreen> with SingleTickerProviderStateMixin {
  final _apiService = ApiService();
  late TabController _tabController;
  List<dynamic> _all = [];
  List<dynamic> _active = [];
  List<dynamic> _souls = [];
  bool _isLoading = true;

  // Création état
  final _createMotifCtrl = TextEditingController();
  final _createDescriptionCtrl = TextEditingController();
  String? _createAmeId;
  bool _isCreating = false;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _loadData();
    _loadSouls();
  }

  @override
  void dispose() {
    _tabController.dispose();
    _createMotifCtrl.dispose();
    _createDescriptionCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadSouls() async {
    try {
      final res = await _apiService.get('/souls', params: {'size': '100'});
      if (mounted) {
        setState(() {
          _souls = (res.data is Map ? res.data['content'] : res.data) as List<dynamic>? ?? [];
        });
      }
    } catch (_) {}
  }

  Future<void> _createFollowup() async {
    if (_createAmeId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Sélectionnez une âme')),
      );
      return;
    }
    if (_createMotifCtrl.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Le motif est requis')),
      );
      return;
    }

    setState(() => _isCreating = true);
    try {
      await _apiService.post('/parallel-followups', data: {
        'ameId': _createAmeId,
        'motif': _createMotifCtrl.text.trim(),
        'description': _createDescriptionCtrl.text.trim(),
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Suivi créé avec succès'), backgroundColor: Color(0xFF2E7D32)),
        );
        _createMotifCtrl.clear();
        _createDescriptionCtrl.clear();
        _createAmeId = null;
        _loadData();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Erreur: $e'), backgroundColor: const Color(0xFFC62828)),
        );
      }
    } finally {
      if (mounted) setState(() => _isCreating = false);
    }
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final allRes = await _apiService.get('/parallel-followups', params: {'size': '50'});
      final activeRes = await _apiService.get('/parallel-followups/active');
      if (mounted) {
        setState(() {
          _all = (allRes.data is Map ? allRes.data['content'] : allRes.data) as List<dynamic>? ?? [];
          _active = (activeRes.data is Map ? activeRes.data['content'] : activeRes.data) as List<dynamic>? ?? [];
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Color _statusColor(String? s) {
    switch (s) {
      case 'ACTIF': return Colors.green;
      case 'EN_COURS': return Colors.amber;
      case 'CLOTURE': return Colors.grey;
      default: return Colors.blue;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Suivis parallèles'),
        actions: [
          IconButton(icon: const Icon(Icons.add), onPressed: _showCreateSheet),
        ],
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: Colors.white,
          tabs: const [
            Tab(text: 'Actifs'),
            Tab(text: 'Tous'),
          ],
        ),
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 3)
          : RefreshIndicator(
              onRefresh: _loadData,
              child: TabBarView(
                controller: _tabController,
                children: [
                  _buildList(_active),
                  _buildList(_all),
                ],
              ),
            ),
    );
  }

  void _showCreateSheet() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setSheetState) => Container(
          height: MediaQuery.of(ctx).size.height * 0.7,
          decoration: const BoxDecoration(
            color: Color(0xFF1E2A4A),
            borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
          ),
          child: SafeArea(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(20),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Center(
                    child: Container(width: 32, height: 4, decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.2), borderRadius: BorderRadius.circular(2))),
                  ),
                  const SizedBox(height: 16),
                  const Text('Nouveau suivi parallèle', style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 16),
                  DropdownButtonFormField<String>(
                    value: _createAmeId,
                    dropdownColor: const Color(0xFF1E2A4A),
                    style: const TextStyle(color: Colors.white, fontSize: 14),
                    decoration: InputDecoration(
                      labelText: 'Âme (disciple) *',
                      labelStyle: TextStyle(color: Colors.white.withValues(alpha: 0.5)),
                      filled: true,
                      fillColor: Colors.white.withValues(alpha: 0.06),
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
                    ),
                    items: _souls.map((s) => DropdownMenuItem<String>(
                      value: s['id'] as String,
                      child: Text('${s['prenom'] ?? ''} ${s['nom'] ?? ''}'),
                    )).toList(),
                    onChanged: (v) => setSheetState(() => _createAmeId = v),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: _createMotifCtrl,
                    style: const TextStyle(color: Colors.white),
                    decoration: InputDecoration(
                      labelText: 'Motif *',
                      labelStyle: TextStyle(color: Colors.white.withValues(alpha: 0.5)),
                      filled: true,
                      fillColor: Colors.white.withValues(alpha: 0.06),
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
                    ),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: _createDescriptionCtrl,
                    maxLines: 3,
                    style: const TextStyle(color: Colors.white),
                    decoration: InputDecoration(
                      labelText: 'Description',
                      labelStyle: TextStyle(color: Colors.white.withValues(alpha: 0.5)),
                      filled: true,
                      fillColor: Colors.white.withValues(alpha: 0.06),
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
                    ),
                  ),
                  const SizedBox(height: 20),
                  Row(mainAxisAlignment: MainAxisAlignment.end, children: [
                    TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
                    const SizedBox(width: 8),
                    FilledButton.icon(
                      onPressed: _isCreating ? null : () { _createFollowup(); Navigator.pop(ctx); },
                      icon: _isCreating ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2)) : const Icon(Icons.add, size: 18),
                      label: const Text('Créer'),
                    ),
                  ]),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildList(List<dynamic> items) {
    if (items.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.track_changes_outlined, size: 48, color: Colors.white.withValues(alpha: 0.3)),
            const SizedBox(height: 12),
            Text('Aucun suivi', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
          ],
        ),
      );
    }
    return ListView.builder(
      padding: const EdgeInsets.all(12),
      itemCount: items.length,
      itemBuilder: (context, index) {
        final f = items[index] as Map<String, dynamic>;
        final titre = f['titre'] ?? f['nom'] ?? 'Suivi';
        final statut = f['statut'] ?? 'ACTIF';
        final dateDebut = f['dateDebut'] ?? f['createdAt'] ?? '';
        final description = f['description'] ?? f['motif'] ?? '';
        final responsable = f['responsableNom'] ?? f['faiseurNom'] ?? '—';
        return GlassCard(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: _statusColor(statut).withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Icon(Icons.track_changes, color: _statusColor(statut), size: 20),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(titre, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                        Text('Responsable: $responsable', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
                      ],
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: _statusColor(statut).withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(statut, style: TextStyle(color: _statusColor(statut), fontSize: 10, fontWeight: FontWeight.w600)),
                  ),
                ],
              ),
              if (description.toString().isNotEmpty) ...[
                const SizedBox(height: 8),
                Text(
                  description,
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 12),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
              if (dateDebut.toString().isNotEmpty) ...[
                const SizedBox(height: 6),
                Row(
                  children: [
                    Icon(Icons.access_time, size: 12, color: Colors.white.withValues(alpha: 0.4)),
                    const SizedBox(width: 4),
                    Text(dateDebut.toString().substring(0, 10), style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                  ],
                ),
              ],
            ],
          ),
        );
      },
    );
  }
}
