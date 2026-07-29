import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';

class FamilyReportScreen extends StatefulWidget {
  const FamilyReportScreen({super.key});

  @override
  State<FamilyReportScreen> createState() => _FamilyReportScreenState();
}

class _FamilyReportScreenState extends State<FamilyReportScreen> {
  final _apiService = ApiService();
  List<Map<String, dynamic>> _families = [];
  bool _isLoading = true;

  @override
  void initState() { super.initState(); _loadFamilies(); }

  Future<void> _loadFamilies() async {
    try {
      final response = await _apiService.get('/families', params: {'size': '50'});
      final data = response.data as Map<String, dynamic>;
      if (mounted) setState(() {
        _families = (data['content'] as List).map((e) => e as Map<String, dynamic>).toList();
        _isLoading = false;
      });
    } catch (e) { if (mounted) setState(() => _isLoading = false); }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Rapport de famille')),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 4)
          : RefreshIndicator(
              onRefresh: _loadFamilies,
              child: ListView.builder(
                padding: const EdgeInsets.all(16),
                itemCount: _families.length,
                itemBuilder: (context, index) {
                  final family = _families[index];
                  final nom = family['nom'] as String? ?? 'Sans nom';
                  final chefId = family['chefFamilleId'] as String? ?? 'N/A';

                  return GlassCard(
                    margin: const EdgeInsets.only(bottom: 12),
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(children: [
                          GradientAvatar(text: nom.substring(0, 2), radius: 22, gradientStart: Colors.blue, gradientEnd: Colors.indigo),
                          const SizedBox(width: 12),
                          Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                            Text(nom, style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
                            Text('Chef: ${chefId.length > 8 ? '${chefId.substring(0, 8)}...' : chefId}', style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 12)),
                          ])),
                          const Icon(Icons.check_circle, color: Colors.green, size: 20),
                        ]),
                        const SizedBox(height: 16),
                        Row(mainAxisAlignment: MainAxisAlignment.spaceAround, children: [
                          _statItem(Icons.person, 'Faiseurs', 'N/A'),
                          _statItem(Icons.favorite, 'Âmes', 'N/A'),
                          _statItem(Icons.trending_up, 'Présence', 'N/A'),
                        ]),
                        const SizedBox(height: 16),
                        GlassCard(
                          padding: const EdgeInsets.all(12),
                          child: Row(children: [
                            Icon(Icons.description, color: Colors.white.withValues(alpha: 0.3), size: 20),
                            const SizedBox(width: 8),
                            Text('Aucun rapport disponible', style: TextStyle(color: Colors.white.withValues(alpha: 0.4))),
                          ]),
                        ),
                      ],
                    ),
                  );
                },
              ),
            ),
    );
  }

  Widget _statItem(IconData icon, String label, String value) {
    return Column(children: [
      Icon(icon, color: AppColors.primaryLight, size: 20),
      const SizedBox(height: 4),
      Text(value, style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold)),
      Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
    ]);
  }
}
