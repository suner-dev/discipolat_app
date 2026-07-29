import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';

class FamiliesListScreen extends StatefulWidget {
  const FamiliesListScreen({super.key});

  @override
  State<FamiliesListScreen> createState() => _FamiliesListScreenState();
}

class _FamiliesListScreenState extends State<FamiliesListScreen> {
  final _apiService = ApiService();
  List<dynamic> _families = [];
  bool _isLoading = true;

  @override
  void initState() { super.initState(); _loadFamilies(); }

  Future<void> _loadFamilies() async {
    try {
      final response = await _apiService.get('/families', params: {'size': '50'});
      final data = response.data as Map<String, dynamic>;
      if (mounted) setState(() { _families = data['content'] as List<dynamic>; _isLoading = false; });
    } catch (e) { if (mounted) setState(() => _isLoading = false); }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Familles')),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 4)
          : RefreshIndicator(
              onRefresh: _loadFamilies,
              child: _families.isEmpty
                  ? ListView(children: [
                      SizedBox(height: MediaQuery.of(context).size.height * 0.25),
                      Center(child: Column(children: [
                        Icon(Icons.group_outlined, size: 64, color: Colors.white.withValues(alpha: 0.15)),
                        const SizedBox(height: 16),
                        Text('Aucune famille trouvée', style: TextStyle(color: Colors.white.withValues(alpha: 0.4))),
                      ])),
                    ])
                  : ListView.builder(
                      padding: const EdgeInsets.all(16),
                      itemCount: _families.length,
                      itemBuilder: (context, index) {
                        final family = _families[index] as Map<String, dynamic>;
                        final statut = family['statut'] == 'ACTIVE';
                        return GlassCard(
                          margin: const EdgeInsets.only(bottom: 10),
                          padding: const EdgeInsets.all(12),
                          child: Row(children: [
                            GradientAvatar(
                              text: (family['nom'] as String?)?.substring(0, 2) ?? 'F',
                              radius: 24,
                              gradientStart: Colors.purple, gradientEnd: Colors.indigo,
                            ),
                            const SizedBox(width: 12),
                            Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                              Text(family['nom'] ?? 'Sans nom', style: const TextStyle(color: Colors.white, fontSize: 15, fontWeight: FontWeight.w600)),
                              const SizedBox(height: 2),
                              Text('Chef: ${(family['chefFamilleId'] as String?)?.substring(0, 8) ?? 'N/A'}...', style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 12)),
                            ])),
                            StatusBadge(label: statut ? 'Active' : 'Inactive', color: statut ? Colors.green : Colors.grey),
                          ]),
                        );
                      },
                    ),
            ),
    );
  }
}
