import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../app.dart';
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

  void _showCreateDialog() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => _CreateSoulSheet(
        apiService: _apiService,
        onDone: () {
          Navigator.pop(ctx);
          _loadSouls();
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Âmes'),
        actions: [
          IconButton(icon: const Icon(Icons.add), onPressed: _showCreateDialog),
          IconButton(
            icon: const Icon(Icons.search),
            onPressed: () => context.go('/search'),
          ),
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

class _CreateSoulSheet extends StatefulWidget {
  const _CreateSoulSheet({required this.apiService, required this.onDone});

  final ApiService apiService;
  final VoidCallback onDone;

  @override
  State<_CreateSoulSheet> createState() => _CreateSoulSheetState();
}

class _CreateSoulSheetState extends State<_CreateSoulSheet> {
  final _nomCtrl = TextEditingController();
  final _prenomCtrl = TextEditingController();
  final _emailCtrl = TextEditingController();
  final _telephoneCtrl = TextEditingController();
  String _typeDisciple = 'NOUVEL_ARRIVANT';
  bool _isProcessing = false;

  @override
  void dispose() {
    _nomCtrl.dispose();
    _prenomCtrl.dispose();
    _emailCtrl.dispose();
    _telephoneCtrl.dispose();
    super.dispose();
  }

  Future<void> _create() async {
    if (_nomCtrl.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Le nom est requis')),
      );
      return;
    }
    setState(() => _isProcessing = true);
    try {
      final auth = AuthState();
      await widget.apiService.post('/souls', data: {
        'nom': _nomCtrl.text.trim(),
        'prenom': _prenomCtrl.text.trim().isEmpty ? null : _prenomCtrl.text.trim(),
        'email': _emailCtrl.text.trim().isEmpty ? null : _emailCtrl.text.trim(),
        'telephone': _telephoneCtrl.text.trim().isEmpty ? null : _telephoneCtrl.text.trim(),
        'typeDisciple': _typeDisciple,
        'dateIntegration': DateTime.now().toIso8601String().substring(0, 10),
        'faiseurId': auth.userId,
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Âme créée avec succès')),
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
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).viewInsets.bottom,
      ),
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
                'Nouvelle âme',
                style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 16),
              _field(_nomCtrl, 'Nom *'),
              const SizedBox(height: 12),
              _field(_prenomCtrl, 'Prénom'),
              const SizedBox(height: 12),
              _field(_emailCtrl, 'Email'),
              const SizedBox(height: 12),
              _field(_telephoneCtrl, 'Téléphone'),
              const SizedBox(height: 12),
              Row(
                children: [
                  const Text('Type : ', style: TextStyle(color: Colors.white70, fontSize: 14)),
                  const SizedBox(width: 8),
                  DropdownButton<String>(
                    value: _typeDisciple,
                    dropdownColor: const Color(0xFF1E2A4A),
                    style: const TextStyle(color: Colors.white),
                    items: const [
                      DropdownMenuItem(value: 'NOUVEL_ARRIVANT', child: Text('Nouvel arrivant')),
                      DropdownMenuItem(value: 'NOUVEAU_CONVERTI', child: Text('Nouveau converti')),
                    ],
                    onChanged: (v) => setState(() => _typeDisciple = v ?? 'NOUVEL_ARRIVANT'),
                  ),
                ],
              ),
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

  Widget _field(TextEditingController controller, String label) {
    return TextField(
      controller: controller,
      style: const TextStyle(color: Colors.white),
      decoration: InputDecoration(
        labelText: label,
        labelStyle: TextStyle(color: Colors.white.withValues(alpha: 0.5)),
        filled: true,
        fillColor: Colors.white.withValues(alpha: 0.06),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide.none,
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: AppColors.primary, width: 1),
        ),
      ),
    );
  }
}
