import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../l10n/app_localizations.dart';

/// Marketplace — branché sur `GET /api/v1/marketplace`.
class MarketplaceScreen extends StatefulWidget {
  const MarketplaceScreen({super.key, this.apiService});

  final ApiService? apiService;

  @override
  State<MarketplaceScreen> createState() => _MarketplaceScreenState();
}

class _MarketplaceScreenState extends State<MarketplaceScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _listings = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _isLoading = _listings.isEmpty;
      _error = null;
    });
    try {
      final res = await _api.get('/marketplace');
      final data = res.data;
      final list = data is List ? data : (data is Map && data['content'] != null ? data['content'] : []);
      if (mounted) {
        setState(() {
          _listings = List<dynamic>.from(list);
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _error = AppLocalizations.of(context).marketplaceError;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context).marketplaceTitle),
        backgroundColor: Colors.teal.shade600,
        foregroundColor: Colors.white,
        actions: [
          IconButton(icon: const Icon(Icons.add_circle_outline), onPressed: () {}),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _error != null && _listings.isEmpty
              ? _errorView()
              : RefreshIndicator(onRefresh: _load, child: _content()),
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
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        TextField(
          decoration: InputDecoration(
            hintText: AppLocalizations.of(context).searchHint,
            prefixIcon: const Icon(Icons.search, size: 20),
            border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
            contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          ),
        ),
        const SizedBox(height: 16),
        Wrap(
          spacing: 8,
          children: [
            AppLocalizations.of(context).filterAll,
            AppLocalizations.of(context).filterOffers,
            AppLocalizations.of(context).filterRequests,
            AppLocalizations.of(context).filterServices,
            AppLocalizations.of(context).filterFree,
          ].map((l) {
            return _chip(l, l == AppLocalizations.of(context).filterAll);
          }).toList(),
        ),
        const SizedBox(height: 16),
        if (_listings.isEmpty)
          Padding(
            padding: const EdgeInsets.all(24),
            child: Center(
              child: Text(AppLocalizations.of(context).marketplaceEmpty,
                  style: const TextStyle(color: Colors.grey)),
            ),
          )
        else
          ..._listings.map((l) {
            final type = l['listingType']?.toString() ?? 'OFFER';
                        return _listingCard(
              l['title']?.toString() ?? 'Annonce',
              l['sellerId']?.toString() ?? '',
              _formatPrice((l['priceCents'] as num?)?.toInt()),
              l['category']?.toString() ?? '',
              _typeLabel(type),
            );
          }),
      ],
    );
  }

  String _typeLabel(String t) {
    final l = AppLocalizations.of(context);
    switch (t) {
      case 'REQUEST':
        return l.filterRequests;
      case 'SERVICE':
        return l.filterServices;
      case 'FREE':
        return l.filterFree;
      default:
        return l.filterOffers;
    }
  }

  String _formatPrice(int? cents) {
    if (cents == null || cents == 0) return '';
    final eur = cents / 100;
    return '${eur.toStringAsFixed(eur % 1 == 0 ? 0 : 2)} €';
  }

  Widget _chip(String label, bool selected) {
    return FilterChip(
      label: Text(label, style: TextStyle(fontSize: 12, color: selected ? Colors.white : Colors.teal)),
      selected: selected,
      onSelected: (_) {},
      backgroundColor: Colors.teal.shade50,
      selectedColor: Colors.teal.shade600,
    );
  }

  Widget _listingCard(String title, String seller, String price, String category, String type) {
    Color typeColor;
    switch (type) {
      case 'Demande':
        typeColor = Colors.orange;
        break;
      case 'Service':
        typeColor = Colors.green;
        break;
      case 'Gratuit':
        typeColor = Colors.purple;
        break;
      default:
        typeColor = Colors.blue;
    }
    final l = AppLocalizations.of(context);
    final sellerLabel = seller.isEmpty ? l.sellerLabel : l.sellerWithId(seller);
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            height: 120,
            width: double.infinity,
            decoration: BoxDecoration(
              color: Colors.teal.shade50,
              borderRadius: const BorderRadius.vertical(top: Radius.circular(12)),
            ),
            child: Center(child: Icon(Icons.store, size: 48, color: Colors.teal.shade200)),
          ),
          Padding(
            padding: const EdgeInsets.all(12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(child: Text(title, style: const TextStyle(fontWeight: FontWeight.bold))),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(
                        color: typeColor.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text(type, style: TextStyle(fontSize: 10, color: typeColor, fontWeight: FontWeight.bold)),
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                Text(sellerLabel, style: const TextStyle(fontSize: 12, color: Colors.grey)),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(category, style: const TextStyle(fontSize: 11, color: Colors.grey)),
                    if (price.isNotEmpty)
                      Text(price, style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: Colors.teal.shade600)),
                  ],
                ),
                const SizedBox(height: 8),
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    onPressed: () {},
                    style: ElevatedButton.styleFrom(backgroundColor: Colors.teal.shade600, foregroundColor: Colors.white),
                    child: Text(AppLocalizations.of(context).contact, style: const TextStyle(fontSize: 12)),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
