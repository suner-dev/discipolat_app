import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';
import '../../../../l10n/app_localizations.dart';

/// Administration des dictionnaires (statuts, catégories, types).
class AdminDictionariesScreen extends StatefulWidget {
  const AdminDictionariesScreen({super.key});

  @override
  State<AdminDictionariesScreen> createState() => _AdminDictionariesScreenState();
}

class _AdminDictionariesScreenState extends State<AdminDictionariesScreen> {
  final _apiService = ApiService();
  List<dynamic> _dictionaries = [];
  bool _isLoading = true;

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    setState(() => _isLoading = true);
    try {
      final res = await _apiService.get('/dictionaries');
      _dictionaries = (res.data is List ? res.data : []) as List<dynamic>;
    } catch (_) {}
    if (mounted) setState(() => _isLoading = false);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(AppLocalizations.of(context).dictTitle), actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _load)]),
      drawer: const AppDrawer(),
      body: _isLoading ? const ShimmerLoading(itemCount: 5) : _dictionaries.isEmpty
          ? Center(child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
              Icon(Icons.book, color: Colors.white.withValues(alpha: 0.15), size: 48),
              const SizedBox(height: 12),
              Text(AppLocalizations.of(context).dictEmpty, style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
            ]))
          : ListView.builder(padding: const EdgeInsets.all(12), itemCount: _dictionaries.length, itemBuilder: (_, i) {
              final d = _dictionaries[i] as Map<String, dynamic>;
              final entries = (d['entries'] as List<dynamic>?) ?? [];
              return GlassCard(margin: const EdgeInsets.only(bottom: 8), padding: const EdgeInsets.all(12), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Row(children: [
                  Container(width: 36, height: 36, decoration: BoxDecoration(color: AppColors.accent.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(8)),
                    child: Icon(Icons.book, color: AppColors.accent, size: 18)),
                  const SizedBox(width: 10),
                  Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Text(d['name']?.toString() ?? '', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13)),
                    Text('${entries.length} entrée${entries.length > 1 ? 's' : ''}', style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                  ])),
                ]),
                if (entries.isNotEmpty) ...[
                  const SizedBox(height: 8),
                  Wrap(spacing: 4, runSpacing: 4, children: entries.map((e) {
                    final entry = e as Map<String, dynamic>;
                    return Container(padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3), decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.06), borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: Colors.white.withValues(alpha: 0.1))),
                      child: Text(entry['label']?.toString() ?? entry['code']?.toString() ?? '', style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 10)));
                  }).toList()),
                ],
              ]));
            }),
    );
  }
}
