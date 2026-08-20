import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';

/// Inventory Management screen — church equipment, stock, assignments, maintenance.
class InventoryScreen extends StatefulWidget {
  final ApiService? apiService;
  const InventoryScreen({super.key, this.apiService});

  @override
  State<InventoryScreen> createState() => _InventoryScreenState();
}

class _InventoryScreenState extends State<InventoryScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _items = [];
  Map<String, dynamic> _stats = {};
  bool _isLoading = true;
  String _search = '';
  String _filterCategorie = '';
  String _filterStatut = '';

  static const Map<String, String> _categories = {
    'MATERIEL': 'Matériel',
    'MOBILIER': 'Mobilier',
    'TECHNIQUE': 'Technique',
    'VESTIMENTAIRE': 'Vestimentaire',
    'AUTRE': 'Autre',
  };

  static const Map<String, String> _statuts = {
    'DISPONIBLE': 'Disponible',
    'AFFECTE': 'Affecté',
    'EN_MAINTENANCE': 'En maintenance',
    'PERDU': 'Perdu',
    'RETIRE': 'Retiré',
  };

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final params = <String, dynamic>{'size': '100'};
      if (_search.isNotEmpty) params['q'] = _search;
      if (_filterCategorie.isNotEmpty) params['categorie'] = _filterCategorie;
      if (_filterStatut.isNotEmpty) params['statut'] = _filterStatut;

      final results = await Future.wait([
        _api.get('/inventory', params: params),
        _api.get('/inventory/stats'),
      ]);

      final data = results[0].data;
      List<dynamic> items = [];
      if (data is Map && data.containsKey('content')) {
        items = data['content'] as List<dynamic>;
      } else if (data is List) {
        items = data;
      }

      if (mounted) {
        setState(() {
          _items = items;
          _stats = results[1].data is Map ? results[1].data as Map<String, dynamic> : {};
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) setState(() { _items = []; _isLoading = false; });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0A0A1A),
      appBar: AppBar(
        title: const Text('📦 Inventaire', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
        backgroundColor: Colors.transparent,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh, color: Colors.white),
            onPressed: _loadData,
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Colors.cyanAccent))
          : RefreshIndicator(
              onRefresh: _loadData,
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  _buildStatsCards(),
                  const SizedBox(height: 16),
                  _buildSearchBar(),
                  const SizedBox(height: 12),
                  _buildFilters(),
                  const SizedBox(height: 16),
                  _buildItemList(),
                ],
              ),
            ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _showCreateDialog(),
        backgroundColor: Colors.cyanAccent,
        child: const Icon(Icons.add, color: Colors.black),
      ),
    );
  }

  Widget _buildStatsCards() {
    return Row(
      children: [
        _buildStatCard('Total', '${_stats['total'] ?? 0}', Icons.inventory_2, Colors.cyanAccent),
        const SizedBox(width: 8),
        _buildStatCard('Disponibles', '${_stats['disponibles'] ?? 0}', Icons.check_circle, Colors.green),
        const SizedBox(width: 8),
        _buildStatCard('Affectés', '${_stats['affectes'] ?? 0}', Icons.person, Colors.amber),
        const SizedBox(width: 8),
        _buildStatCard('Maintenance', '${_stats['enMaintenance'] ?? 0}', Icons.build, Colors.orange),
      ],
    );
  }

  Widget _buildStatCard(String label, String value, IconData icon, Color color) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 8),
        decoration: BoxDecoration(
          color: Colors.white.withAlpha(6),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: color.withAlpha(30)),
        ),
        child: Column(
          children: [
            Icon(icon, color: color, size: 18),
            const SizedBox(height: 4),
            Text(value, style: TextStyle(color: color, fontSize: 18, fontWeight: FontWeight.bold)),
            Text(label, style: TextStyle(color: Colors.white.withAlpha(120), fontSize: 10)),
          ],
        ),
      ),
    );
  }

  Widget _buildSearchBar() {
    return TextField(
      style: const TextStyle(color: Colors.white),
      decoration: InputDecoration(
        hintText: 'Rechercher un matériel...',
        hintStyle: TextStyle(color: Colors.white.withAlpha(100)),
        prefixIcon: Icon(Icons.search, color: Colors.white.withAlpha(100)),
        filled: true,
        fillColor: Colors.white.withAlpha(10),
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
      ),
      onChanged: (v) => setState(() { _search = v; _loadData(); }),
    );
  }

  Widget _buildFilters() {
    return Row(
      children: [
        Expanded(
          child:              DropdownButtonFormField<String>(
                initialValue: _filterCategorie.isEmpty ? null : _filterCategorie,
            dropdownColor: const Color(0xFF1A1A3A),
            style: const TextStyle(color: Colors.white, fontSize: 12),
            decoration: InputDecoration(
              hintText: 'Catégorie',
              hintStyle: TextStyle(color: Colors.white.withAlpha(100)),
              filled: true,
              fillColor: Colors.white.withAlpha(10),
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: BorderSide.none),
              contentPadding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
            ),
            items: [
              const DropdownMenuItem(value: null, child: Text('Toutes')),
              ..._categories.entries.map((e) => DropdownMenuItem(value: e.key, child: Text(e.value))),
            ],
            onChanged: (v) => setState(() { _filterCategorie = v ?? ''; _loadData(); }),
          ),
        ),
        const SizedBox(width: 8),
        Expanded(
          child:              DropdownButtonFormField<String>(
                initialValue: _filterStatut.isEmpty ? null : _filterStatut,
            dropdownColor: const Color(0xFF1A1A3A),
            style: const TextStyle(color: Colors.white, fontSize: 12),
            decoration: InputDecoration(
              hintText: 'Statut',
              hintStyle: TextStyle(color: Colors.white.withAlpha(100)),
              filled: true,
              fillColor: Colors.white.withAlpha(10),
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: BorderSide.none),
              contentPadding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
            ),
            items: [
              const DropdownMenuItem(value: null, child: Text('Tous')),
              ..._statuts.entries.map((e) => DropdownMenuItem(value: e.key, child: Text(e.value))),
            ],
            onChanged: (v) => setState(() { _filterStatut = v ?? ''; _loadData(); }),
          ),
        ),
      ],
    );
  }

  Widget _buildItemList() {
    if (_items.isEmpty) {
      return Container(
        padding: const EdgeInsets.all(40),
        child: Column(
          children: [
            Icon(Icons.inventory_2, color: Colors.white.withAlpha(50), size: 48),
            const SizedBox(height: 12),
            Text('Aucun matériel', style: TextStyle(color: Colors.white.withAlpha(120), fontSize: 16)),
            const SizedBox(height: 8),
            Text('Appuyez sur + pour ajouter', style: TextStyle(color: Colors.white.withAlpha(80), fontSize: 12)),
          ],
        ),
      );
    }

    return Column(
      children: List.generate(_items.length, (i) => _buildItemCard(_items[i])),
    );
  }

  Widget _buildItemCard(dynamic item) {
    final nom = item['nom']?.toString() ?? 'Sans nom';
    // final categorie = item['categorie']?.toString() ?? '';
    final statut = item['statut']?.toString() ?? '';
    final quantite = item['quantite'] ?? 0;
    final quantiteDispo = item['quantiteDisponible'] ?? 0;

    Color statutColor;
    switch (statut) {
      case 'DISPONIBLE': statutColor = Colors.green; break;
      case 'AFFECTE': statutColor = Colors.blue; break;
      case 'EN_MAINTENANCE': statutColor = Colors.orange; break;
      case 'PERDU': statutColor = Colors.red; break;
      default: statutColor = Colors.white.withAlpha(100);
    }

    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white.withAlpha(6),
        borderRadius: BorderRadius.circular(12),
      ),
      child: ListTile(
        contentPadding: EdgeInsets.zero,
        leading: Container(
          padding: const EdgeInsets.all(10),
          decoration: BoxDecoration(
            color: statutColor.withAlpha(20),
            borderRadius: BorderRadius.circular(10),
          ),
          child: Icon(Icons.inventory_2, color: statutColor, size: 22),
        ),
        title: Text(nom, style: const TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w500)),
        subtitle: Row(
          children: [
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
              decoration: BoxDecoration(
                color: statutColor.withAlpha(20),
                borderRadius: BorderRadius.circular(4),
              ),
              child: Text(_statuts[statut] ?? statut, style: TextStyle(color: statutColor, fontSize: 10)),
            ),
            const SizedBox(width: 8),
            Text('$quantiteDispo/$quantite', style: TextStyle(color: Colors.white.withAlpha(150), fontSize: 11)),
          ],
        ),
        trailing: PopupMenuButton<String>(
          icon: Icon(Icons.more_vert, color: Colors.white.withAlpha(100)),
          onSelected: (action) => _handleAction(action, item),
          itemBuilder: (_) => [
            if (statut == 'DISPONIBLE')
              const PopupMenuItem(value: 'assign', child: Text('Affecter')),
            if (statut == 'AFFECTE')
              const PopupMenuItem(value: 'unassign', child: Text('Libérer')),
            if (statut != 'EN_MAINTENANCE')
              const PopupMenuItem(value: 'maintenance', child: Text('Maintenance')),
            const PopupMenuItem(value: 'edit', child: Text('Modifier')),
            const PopupMenuItem(value: 'delete', child: Text('Supprimer', style: TextStyle(color: Colors.red))),
          ],
        ),
      ),
    );
  }

  void _handleAction(String action, dynamic item) async {
    final id = item['id']?.toString();
    if (id == null) return;

    switch (action) {
      case 'assign':
        // Show member picker (simplified)
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Sélectionnez un membre à affecter'), backgroundColor: Colors.blue),
        );
        break;
      case 'unassign':
        await _api.post('/inventory/$id/unassign');
        _loadData();
        break;
      case 'maintenance':
        await _api.post('/inventory/$id/maintenance');
        _loadData();
        break;
      case 'edit':
        _showEditDialog(item);
        break;
      case 'delete':
        final confirmed = await showDialog<bool>(
          context: context,
          builder: (_) => AlertDialog(
            backgroundColor: const Color(0xFF1A1A3A),
            title: const Text('Supprimer ?', style: TextStyle(color: Colors.white)),
            content: Text('Supprimer "${item['nom']}" ?', style: TextStyle(color: Colors.white.withAlpha(180))),
            actions: [
              TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Annuler')),
              TextButton(onPressed: () => Navigator.pop(context, true), child: const Text('Supprimer', style: TextStyle(color: Colors.red))),
            ],
          ),
        );
        if (confirmed == true) {
          await _api.delete('/inventory/$id');
          _loadData();
        }
        break;
    }
  }

  void _showCreateDialog() {
    final nomCtrl = TextEditingController();
    final descCtrl = TextEditingController();
    String categorie = 'MATERIEL';
    int quantite = 1;

    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        backgroundColor: const Color(0xFF1A1A3A),
        title: const Text('Nouveau matériel', style: TextStyle(color: Colors.white)),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: nomCtrl,
                style: const TextStyle(color: Colors.white),
                decoration: InputDecoration(labelText: 'Nom', labelStyle: TextStyle(color: Colors.white.withAlpha(150))),
              ),
              TextField(
                controller: descCtrl,
                style: const TextStyle(color: Colors.white),
                decoration: InputDecoration(labelText: 'Description', labelStyle: TextStyle(color: Colors.white.withAlpha(150))),
              ),
              const SizedBox(height: 12),
              DropdownButtonFormField<String>(
                initialValue: categorie,
                dropdownColor: const Color(0xFF1A1A3A),
                style: const TextStyle(color: Colors.white),
                items: _categories.entries.map((e) => DropdownMenuItem(value: e.key, child: Text(e.value))).toList(),
                onChanged: (v) => categorie = v ?? 'MATERIEL',
                decoration: InputDecoration(labelText: 'Catégorie', labelStyle: TextStyle(color: Colors.white.withAlpha(150))),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Annuler')),
          ElevatedButton(
            onPressed: () async {
              if (nomCtrl.text.isNotEmpty) {
                await _api.post('/inventory', data: {
                  'nom': nomCtrl.text,
                  'description': descCtrl.text,
                  'quantite': quantite,
                  'quantiteDisponible': quantite,
                });
                if (mounted) { Navigator.pop(context); }
                _loadData();
              }
            },
            style: ElevatedButton.styleFrom(backgroundColor: Colors.cyanAccent, foregroundColor: Colors.black),
            child: const Text('Créer'),
          ),
        ],
      ),
    );
  }

  void _showEditDialog(dynamic item) {
    final nomCtrl = TextEditingController(text: item['nom']?.toString() ?? '');
    final descCtrl = TextEditingController(text: item['description']?.toString() ?? '');

    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        backgroundColor: const Color(0xFF1A1A3A),
        title: const Text('Modifier', style: TextStyle(color: Colors.white)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: nomCtrl,
              style: const TextStyle(color: Colors.white),
              decoration: InputDecoration(labelText: 'Nom', labelStyle: TextStyle(color: Colors.white.withAlpha(150))),
            ),
            TextField(
              controller: descCtrl,
              style: const TextStyle(color: Colors.white),
              decoration: InputDecoration(labelText: 'Description', labelStyle: TextStyle(color: Colors.white.withAlpha(150))),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Annuler')),
          ElevatedButton(
            onPressed: () async {
              await _api.put('/inventory/${item['id']}', data: {
                ...item,
                'nom': nomCtrl.text,
                'description': descCtrl.text,
              });
              if (mounted) { Navigator.pop(context); }
              _loadData();
            },
            style: ElevatedButton.styleFrom(backgroundColor: Colors.cyanAccent, foregroundColor: Colors.black),
            child: const Text('Sauver'),
          ),
        ],
      ),
    );
  }
}
