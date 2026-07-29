import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../data/services/api_service.dart';
import '../../../data/models/soul.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';

class SoulsListScreen extends StatefulWidget {
  const SoulsListScreen({super.key});

  @override
  State<SoulsListScreen> createState() => _SoulsListScreenState();
}

class _SoulsListScreenState extends State<SoulsListScreen> {
  final _apiService = ApiService();
  List<Soul> _souls = [];
  bool _isLoading = true;
  int _currentNavIndex = 1;

  @override
  void initState() {
    super.initState();
    _loadSouls();
  }

  Future<void> _loadSouls() async {
    try {
      final response = await _apiService.get('/souls', params: {'size': '50'});
      final data = response.data as Map<String, dynamic>;
      if (mounted) {
        setState(() {
          _souls = (data['content'] as List)
              .map((e) => Soul.fromJson(e as Map<String, dynamic>))
              .toList();
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Âmes'),
        actions: [
          IconButton(icon: const Icon(Icons.add), onPressed: () {}),
          IconButton(icon: const Icon(Icons.search), onPressed: () {}),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 6)
          : RefreshIndicator(
              onRefresh: _loadSouls,
              child: _souls.isEmpty
                  ? ListView(children: [
                      SizedBox(height: MediaQuery.of(context).size.height * 0.25),
                      Center(child: Column(children: [
                        Icon(Icons.favorite_outline, size: 64, color: Colors.white.withValues(alpha: 0.15)),
                        const SizedBox(height: 16),
                        Text('Aucune âme trouvée', style: TextStyle(color: Colors.white.withValues(alpha: 0.4))),
                      ])),
                    ])
                  : ListView.builder(
                      padding: const EdgeInsets.all(16),
                      itemCount: _souls.length,
                      itemBuilder: (context, index) {
                        final soul = _souls[index];
                        final isConverti = soul.typeDisciple == 'NOUVEAU_CONVERTI';
                        return GlassCard(
                          margin: const EdgeInsets.only(bottom: 10),
                          padding: const EdgeInsets.all(12),
                          onTap: () => context.go('/souls/${soul.id}'),
                          child: Row(
                            children: [
                              GradientAvatar(
                                text: soul.nom[0],
                                radius: 22,
                                gradientStart: isConverti ? Colors.green : Colors.blue,
                                gradientEnd: isConverti ? Colors.teal : Colors.lightBlue,
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(soul.nomComplet, style: const TextStyle(color: Colors.white, fontSize: 15, fontWeight: FontWeight.w600)),
                                    const SizedBox(height: 2),
                                    Text(soul.email ?? soul.telephone ?? '', style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 12)),
                                  ],
                                ),
                              ),
                              StatusBadge(
                                label: isConverti ? 'Converti' : 'Arrivant',
                                color: isConverti ? Colors.green : Colors.blue,
                              ),
                            ],
                          ),
                        );
                      },
                    ),
            ),
      bottomNavigationBar: GlassBottomNav(currentIndex: _currentNavIndex, onTap: (i) {
        setState(() => _currentNavIndex = i);
        final routes = ['/dashboard', '/souls', '/reports/maker', '/profile'];
        if (i < routes.length) context.go(routes[i]);
      }),
    );
  }
}
