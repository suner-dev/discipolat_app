import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

/// Actions de grâce — prières exaucées.
/// Affiche les prières marquées comme exaucées / témoignages.
class ActionsDeGraceScreen extends StatefulWidget {
  const ActionsDeGraceScreen({super.key});

  @override
  State<ActionsDeGraceScreen> createState() => _ActionsDeGraceScreenState();
}

class _ActionsDeGraceScreenState extends State<ActionsDeGraceScreen> {
  final _apiService = ApiService();
  List<dynamic> _actions = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final res = await _apiService.get('/prayers', params: {
        'statut': 'EXAUCED',
        'size': 100,
      });
      final data = (res.data is Map ? res.data['content'] : res.data) as List<dynamic>? ?? [];
      if (mounted) {
        setState(() {
          _actions = data;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Actions de grâce'),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 6)
          : RefreshIndicator(
              onRefresh: _loadData,
              child: _actions.isEmpty
                  ? Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.favorite, color: Colors.white.withValues(alpha: 0.2), size: 48),
                          const SizedBox(height: 12),
                          Text('Aucune action de grâce',
                              style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                          const SizedBox(height: 4),
                          Text('Les prières exaucées apparaîtront ici',
                              style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 12)),
                        ],
                      ),
                    )
                  : ListView.builder(
                      padding: const EdgeInsets.all(12),
                      itemCount: _actions.length,
                      itemBuilder: (context, index) {
                        final prayer = _actions[index] as Map<String, dynamic>;
                        return _actionCard(prayer);
                      },
                    ),
            ),
    );
  }

  Widget _actionCard(Map<String, dynamic> prayer) {
    final titre = prayer['titre']?.toString() ?? '';
    final contenu = prayer['contenu']?.toString() ?? '';
    final categorie = prayer['categorie']?.toString() ?? '';
    final auteurNom = prayer['auteurNom']?.toString() ?? '';
    final createdAt = prayer['createdAt']?.toString() ?? '';
    final familleNom = prayer['familleNom']?.toString();

    return GlassCard(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(14),
      borderColor: Colors.green.withValues(alpha: 0.3),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header with celebration icon
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: Colors.green.withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: const Icon(Icons.celebration, color: Colors.green, size: 20),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(titre,
                        style: const TextStyle(
                            color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                    if (categorie.isNotEmpty)
                      Text(categorie.replaceAll('_', ' '),
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                  ],
                ),
              ),
            ],
          ),
          if (contenu.isNotEmpty) ...[
            const SizedBox(height: 10),
            Text(contenu, style: const TextStyle(color: Colors.white, fontSize: 13)),
          ],
          const SizedBox(height: 8),
          Row(
            children: [
              if (auteurNom.isNotEmpty) ...[
                Icon(Icons.person, color: Colors.white.withValues(alpha: 0.3), size: 12),
                const SizedBox(width: 4),
                Text(auteurNom,
                    style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
              ],
              if (familleNom != null && familleNom.isNotEmpty) ...[
                const SizedBox(width: 8),
                Icon(Icons.group, color: Colors.white.withValues(alpha: 0.3), size: 12),
                const SizedBox(width: 4),
                Text(familleNom,
                    style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
              ],
              const Spacer(),
              Text(createdAt.length > 10 ? createdAt.substring(0, 10) : createdAt,
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 10)),
            ],
          ),
        ],
      ),
    );
  }
}
