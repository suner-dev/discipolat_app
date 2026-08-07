import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

class PrayersListScreen extends StatefulWidget {
  const PrayersListScreen({super.key});

  @override
  State<PrayersListScreen> createState() => _PrayersListScreenState();
}

class _PrayersListScreenState extends State<PrayersListScreen> with SingleTickerProviderStateMixin {
  final _apiService = ApiService();
  late TabController _tabController;
  List<dynamic> _prayers = [];
  bool _isLoading = true;
  String _filter = 'TOUS';

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
    _loadData();
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final res = await _apiService.get('/prayers', params: {'size': '50'});
      if (mounted) {
        setState(() {
          _prayers = (res.data is Map ? res.data['content'] : res.data) as List<dynamic>? ?? [];
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _showCreateSheet() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => _CreatePrayerSheet(
        apiService: _apiService,
        onDone: () {
          Navigator.pop(ctx);
          _loadData();
        },
      ),
    );
  }

  Color _catColor(String? cat) {
    switch (cat) {
      case 'SANTE': return Colors.red;
      case 'FAMILLE': return Colors.purple;
      case 'TRAVAIL': return Colors.blue;
      case 'SPIRITUEL': return Colors.teal;
      case 'ETUDES': return Colors.orange;
      default: return Colors.grey;
    }
  }

  IconData _catIcon(String? cat) {
    switch (cat) {
      case 'SANTE': return Icons.medical_services;
      case 'FAMILLE': return Icons.family_restroom;
      case 'TRAVAIL': return Icons.work;
      case 'SPIRITUEL': return Icons.auto_awesome;
      case 'ETUDES': return Icons.school;
      default: return Icons.book;
    }
  }

  @override
  Widget build(BuildContext context) {
    final enCours = _prayers.where((p) => (p as Map)['statut'] == 'EN_COURS').length;
    final exauces = _prayers.where((p) => (p as Map)['statut'] == 'EXAUCE').length;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Prières'),
        actions: [
          IconButton(icon: const Icon(Icons.add), onPressed: _showCreateSheet),
        ],
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: Colors.white,
          tabs: [
            Tab(text: 'En cours ($enCours)'),
            Tab(text: 'Exaucées ($exauces)'),
            const Tab(text: 'Toutes'),
          ],
        ),
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 4)
          : RefreshIndicator(
              onRefresh: _loadData,
              child: TabBarView(
                controller: _tabController,
                children: [
                  _buildList(_prayers.where((p) => (p as Map)['statut'] == 'EN_COURS').toList()),
                  _buildList(_prayers.where((p) => (p as Map)['statut'] == 'EXAUCE').toList()),
                  _buildList(_prayers),
                ],
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
            Icon(Icons.book_outlined, size: 48, color: Colors.white.withValues(alpha: 0.3)),
            const SizedBox(height: 12),
            Text('Aucune prière', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
          ],
        ),
      );
    }
    return ListView.builder(
      padding: const EdgeInsets.all(12),
      itemCount: items.length,
      itemBuilder: (context, index) {
        final p = items[index] as Map;
        final titre = p['titre'] ?? 'Prière';
        final categorie = p['categorie'] ?? 'AUTRE';
        final statut = p['statut'] ?? 'EN_COURS';
        final priorite = p['priorite'] ?? 'MOYENNE';
        final description = p['description'] ?? '';
        final visibilite = p['visibilite'] ?? 'PARTAGEE';
        final isExaucee = statut == 'EXAUCE';
        final dateStr = p['createdAt']?.toString().substring(0, 10) ?? '';

        return GlassCard(
          margin: const EdgeInsets.only(bottom: 8),
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: _catColor(categorie).withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Icon(_catIcon(categorie), color: _catColor(categorie), size: 20),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(titre, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                        Text(categorie, style: TextStyle(color: _catColor(categorie), fontSize: 11)),
                      ],
                    ),
                  ),
                  if (isExaucee)
                    const Icon(Icons.check_circle, color: Colors.green, size: 22)
                  else
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(
                        color: priorite == 'HAUTE' ? Colors.red.withValues(alpha: 0.15) : Colors.amber.withValues(alpha: 0.15),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(priorite, style: TextStyle(color: priorite == 'HAUTE' ? Colors.red : Colors.amber, fontSize: 9, fontWeight: FontWeight.w600)),
                    ),
                ],
              ),
              if (description.toString().isNotEmpty) ...[
                const SizedBox(height: 8),
                Text(description, style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 12), maxLines: 2, overflow: TextOverflow.ellipsis),
              ],
              const SizedBox(height: 6),
              Row(
                children: [
                  Icon(Icons.visibility, size: 12, color: Colors.white.withValues(alpha: 0.3)),
                  const SizedBox(width: 3),
                  Text(visibilite, style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 10)),
                  const Spacer(),
                  if (dateStr.isNotEmpty)
                    Text(dateStr, style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 10)),
                ],
              ),
            ],
          ),
        );
      },
    );
  }
}

class _CreatePrayerSheet extends StatefulWidget {
  const _CreatePrayerSheet({required this.apiService, required this.onDone});

  final ApiService apiService;
  final VoidCallback onDone;

  @override
  State<_CreatePrayerSheet> createState() => _CreatePrayerSheetState();
}

class _CreatePrayerSheetState extends State<_CreatePrayerSheet> {
  final _titreCtrl = TextEditingController();
  final _descCtrl = TextEditingController();
  String _categorie = 'SPIRITUEL';
  String _priorite = 'MOYENNE';
  String _visibilite = 'PRIVEE';
  bool _isProcessing = false;

  @override
  void dispose() {
    _titreCtrl.dispose();
    _descCtrl.dispose();
    super.dispose();
  }

  Future<void> _create() async {
    if (_titreCtrl.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Le titre est requis')),
      );
      return;
    }
    setState(() => _isProcessing = true);
    try {
      await widget.apiService.post('/prayers', data: {
        'titre': _titreCtrl.text.trim(),
        'description': _descCtrl.text.trim().isEmpty ? null : _descCtrl.text.trim(),
        'categorie': _categorie,
        'priorite': _priorite,
        'visibilite': _visibilite,
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Demande de prière ajoutée')),
        );
        widget.onDone();
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erreur lors de la création')),
        );
      }
    } finally {
      if (mounted) setState(() => _isProcessing = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFF16213A),
        borderRadius: const BorderRadius.vertical(top: Radius.circular(24)),
        border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
      ),
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      child: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Center(
                child: Container(
                  width: 32, height: 4,
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.2),
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              const SizedBox(height: 16),
              const Text(
                'Nouvelle demande de prière',
                style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 16),
              TextField(
                controller: _titreCtrl,
                style: const TextStyle(color: Colors.white),
                decoration: InputDecoration(
                  labelText: 'Titre *',
                  labelStyle: TextStyle(color: Colors.white.withValues(alpha: 0.5)),
                  filled: true,
                  fillColor: Colors.white.withValues(alpha: 0.06),
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _descCtrl,
                style: const TextStyle(color: Colors.white),
                maxLines: 3,
                decoration: InputDecoration(
                  labelText: 'Description',
                  labelStyle: TextStyle(color: Colors.white.withValues(alpha: 0.5)),
                  filled: true,
                  fillColor: Colors.white.withValues(alpha: 0.06),
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
                ),
              ),
              const SizedBox(height: 12),
              _dropdown('Catégorie', _categorie, const ['SANTE', 'FAMILLE', 'TRAVAIL', 'SPIRITUEL', 'AUTRE'], (v) => setState(() => _categorie = v ?? _categorie)),
              const SizedBox(height: 12),
              _dropdown('Priorité', _priorite, const ['BASSE', 'MOYENNE', 'HAUTE'], (v) => setState(() => _priorite = v ?? _priorite)),
              const SizedBox(height: 12),
              _dropdown('Visibilité', _visibilite, const ['PRIVEE', 'PARTAGEE', 'GENERALE', 'PASTEUR_RESPONSABLE', 'FAISEUR'], (v) => setState(() => _visibilite = v ?? _visibilite)),
              const SizedBox(height: 16),
              Row(mainAxisAlignment: MainAxisAlignment.end, children: [
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text('Annuler'),
                ),
                const SizedBox(width: 8),
                FilledButton.icon(
                  onPressed: _isProcessing ? null : _create,
                  icon: _isProcessing
                      ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                      : const Icon(Icons.add, size: 18),
                  label: const Text('Créer'),
                ),
              ]),
            ],
          ),
        ),
      ),
    );
  }

  Widget _dropdown(String label, String value, List<String> options, ValueChanged<String?> onChanged) {
    return Row(
      children: [
        Text('$label : ', style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 14)),
        const SizedBox(width: 8),
        Expanded(
          child: DropdownButton<String>(
            value: value,
            dropdownColor: const Color(0xFF1E2A4A),
            style: const TextStyle(color: Colors.white),
            isExpanded: true,
            items: options.map((o) => DropdownMenuItem(value: o, child: Text(o))).toList(),
            onChanged: onChanged,
          ),
        ),
      ],
    );
  }
}
