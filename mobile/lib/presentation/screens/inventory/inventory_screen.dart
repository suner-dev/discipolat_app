import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../l10n/app_localizations.dart';

/// Inventaire — branché sur `GET /api/v1/inventory`.
class InventoryScreen extends StatefulWidget {
  const InventoryScreen({super.key, this.apiService});

  final ApiService? apiService;

  @override
  State<InventoryScreen> createState() => _InventoryScreenState();
}

class _InventoryScreenState extends State<InventoryScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _items = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _isLoading = _items.isEmpty;
      _error = null;
    });
    try {
      final res = await _api.get('/inventory');
      final data = res.data;
      final list = data is Map && data['content'] != null
          ? data['content']
          : (data is List ? data : []);
      if (mounted) {
        setState(() {
          _items = List<dynamic>.from(list);
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _error = AppLocalizations.of(context).inventoryError;
        });
      }
    }
  }

  bool _isLowStock(Map<String, dynamic> it) {
    final dispo = (it['quantiteDisponible'] as num?)?.toInt() ?? (it['quantite'] as num?)?.toInt() ?? 0;
    return dispo <= 2;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context).inventoryTitle),
        backgroundColor: Colors.indigo.shade600,
        foregroundColor: Colors.white,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _error != null && _items.isEmpty
              ? _errorView()
              : RefreshIndicator(onRefresh: _load, child: _content()),
      floatingActionButton: FloatingActionButton(
        onPressed: () {},
        backgroundColor: Colors.indigo,
        child: const Icon(Icons.add, color: Colors.white),
      ),
    );
  }

  Widget _errorView() {
    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        const SizedBox(height: 60),
        const Icon(Icons.cloud_off, size: 56, color: Colors.grey),
        const SizedBox(height: 12),
        Center(child: Text(_error ?? 'Erreur', textAlign: TextAlign.center)),
        const SizedBox(height: 16),
        Center(
          child: FilledButton.icon(
            onPressed: _load,
            icon: const Icon(Icons.refresh),
            label: Text(AppLocalizations.of(context).retry),
          ),
        ),
      ],
    );
  }

  Widget _content() {
    final lowCount = _items.where((i) => _isLowStock(i)).length;
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        if (lowCount > 0)
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.amber.shade50,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: Colors.amber.shade200),
            ),
            child: Row(
              children: [
                Icon(Icons.warning_amber, color: Colors.amber.shade600),
                const SizedBox(width: 8),
                Expanded(
                    child: Text(AppLocalizations.of(context).lowStockBanner(lowCount),
                        style: const TextStyle(fontSize: 13))),
              ],
            ),
          ),
        const SizedBox(height: 16),
        TextField(
          decoration: InputDecoration(
            hintText: AppLocalizations.of(context).searchItemHint,
            prefixIcon: const Icon(Icons.search, size: 20),
            border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
            contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          ),
        ),
        const SizedBox(height: 16),
        if (_items.isEmpty)
          Padding(
            padding: const EdgeInsets.all(24),
            child: Center(
              child: Text(AppLocalizations.of(context).inventoryEmpty,
                  style: const TextStyle(color: Colors.grey)),
            ),
          )
        else
          ..._items.map((i) {
            final nom = i['nom']?.toString() ?? 'Article';
            final categorie = i['categorie']?.toString() ?? 'MATERIEL';
            final lieu = i['lieuStockage']?.toString() ?? '';
            final qte = (i['quantite'] as num?)?.toInt() ?? 0;
            final isLow = _isLowStock(i);
            final color = _categoryColor(categorie);
            return _itemCard(nom, categorie, AppLocalizations.of(context).unitsCount(qte), lieu, color, isLow);
          }),
      ],
    );
  }

  Color _categoryColor(String c) {
    switch (c.toUpperCase()) {
      case 'MOBILIER':
        return Colors.blue;
      case 'LITTERATURE':
      case 'AUTRE':
        return Colors.green;
      case 'TECHNIQUE':
        return Colors.purple;
      case 'VESTIMENTAIRE':
        return Colors.orange;
      default:
        return Colors.indigo;
    }
  }

  Widget _itemCard(String name, String category, String quantity, String location, Color color, bool isLow) {
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: color.withOpacity(0.1),
          child: Icon(Icons.inventory_2, color: color, size: 20),
        ),
        title: Text(name, style: const TextStyle(fontWeight: FontWeight.w600)),
        subtitle: Text('$category • $location', style: const TextStyle(fontSize: 12)),
        trailing: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(quantity,
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.bold,
                  color: isLow ? Colors.red : Colors.grey.shade700,
                )),
            if (isLow)
              Text(AppLocalizations.of(context).lowStockTag, style: const TextStyle(fontSize: 10, color: Colors.red)),
          ],
        ),
      ),
    );
  }
}
