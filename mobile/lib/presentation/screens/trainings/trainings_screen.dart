import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

class TrainingsScreen extends StatefulWidget {
  const TrainingsScreen({super.key});

  @override
  State<TrainingsScreen> createState() => _TrainingsScreenState();
}

class _TrainingsScreenState extends State<TrainingsScreen> with SingleTickerProviderStateMixin {
  final _apiService = ApiService();
  late TabController _tabController;
  List<dynamic> _courses = [];
  List<dynamic> _enrollments = [];
  List<dynamic> _certificates = [];
  bool _isLoading = true;

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
      final coursesRes = await _apiService.get('/trainings/courses');
      final enrollRes = await _apiService.get('/trainings/my-enrollments');
      final certRes = await _apiService.get('/trainings/my-certificates');
      if (mounted) {
        setState(() {
          _courses = (coursesRes.data is Map ? coursesRes.data['content'] : coursesRes.data) as List<dynamic>? ?? [];
          _enrollments = (enrollRes.data is Map ? enrollRes.data['content'] : enrollRes.data) as List<dynamic>? ?? [];
          _certificates = (certRes.data is Map ? certRes.data['content'] : certRes.data) as List<dynamic>? ?? [];
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _enroll(String courseId) async {
    try {
      await _apiService.post('/trainings/courses/$courseId/enroll');
      _loadData();
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Inscription réussie !')));
    } catch (_) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Erreur d\'inscription')));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Formations'),
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: Colors.white,
          tabs: const [
            Tab(text: 'Catalogue'),
            Tab(text: 'Mes cours'),
            Tab(text: 'Certificats'),
          ],
        ),
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 3)
          : RefreshIndicator(
              onRefresh: _loadData,
              child: TabBarView(
                controller: _tabController,
                children: [
                  _buildCatalog(),
                  _buildEnrollments(),
                  _buildCertificates(),
                ],
              ),
            ),
    );
  }

  Widget _buildCatalog() {
    if (_courses.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.school_outlined, size: 48, color: Colors.white.withValues(alpha: 0.3)),
            const SizedBox(height: 12),
            Text('Aucun cours disponible', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
          ],
        ),
      );
    }
    return ListView.builder(
      padding: const EdgeInsets.all(12),
      itemCount: _courses.length,
      itemBuilder: (context, index) {
        final c = _courses[index] as Map<String, dynamic>;
        final titre = c['titre'] ?? c['title'] ?? 'Cours';
        final desc = c['description'] ?? '';
        final modules = c['nombreModules'] ?? c['modules'] ?? 0;
        final enrolled = _enrollments.any((e) => (e as Map)['coursId'] == c['id'] || (e as Map)['courseId'] == c['id']);
        return GlassCard(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(Icons.play_circle, color: Colors.blue, size: 24),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(titre, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                  ),
                ],
              ),
              if (desc.isNotEmpty) ...[
                const SizedBox(height: 8),
                Text(desc, style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 12), maxLines: 2, overflow: TextOverflow.ellipsis),
              ],
              const SizedBox(height: 8),
              Row(
                children: [
                  Icon(Icons.book, size: 14, color: Colors.white.withValues(alpha: 0.4)),
                  const SizedBox(width: 4),
                  Text('$modules modules', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                  const Spacer(),
                  if (!enrolled)
                    ElevatedButton(
                      onPressed: () => _enroll(c['id']),
                      style: ElevatedButton.styleFrom(backgroundColor: Colors.blue, foregroundColor: Colors.white, minimumSize: const Size(80, 32), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8))),
                      child: const Text('S\'inscrire', style: TextStyle(fontSize: 12)),
                    )
                  else
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(color: Colors.green.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(12)),
                      child: const Text('Inscrit', style: TextStyle(color: Colors.green, fontSize: 11, fontWeight: FontWeight.w600)),
                    ),
                ],
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildEnrollments() {
    if (_enrollments.isEmpty) {
      return Center(
        child: Text('Aucune inscription', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
      );
    }
    return ListView.builder(
      padding: const EdgeInsets.all(12),
      itemCount: _enrollments.length,
      itemBuilder: (context, index) {
        final e = _enrollments[index] as Map<String, dynamic>;
        final titre = e['coursTitre'] ?? e['titre'] ?? 'Cours';
        final progress = e['progression'] ?? e['progress'] ?? 0;
        final modulesCompleted = e['modulesComplete'] ?? 0;
        final totalModules = e['totalModules'] ?? 1;
        final ratio = totalModules > 0 ? modulesCompleted / totalModules : 0.0;
        return GlassCard(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(titre, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
              const SizedBox(height: 8),
              ClipRRect(
                borderRadius: BorderRadius.circular(4),
                child: LinearProgressIndicator(
                  value: ratio,
                  backgroundColor: Colors.white.withValues(alpha: 0.08),
                  valueColor: AlwaysStoppedAnimation(ratio >= 1.0 ? Colors.green : Colors.blue),
                  minHeight: 6,
                ),
              ),
              const SizedBox(height: 6),
              Text('$modulesCompleted/$totalModules modules • ${((ratio * 100).round())}%',
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
            ],
          ),
        );
      },
    );
  }

  Widget _buildCertificates() {
    if (_certificates.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.workspace_premium_outlined, size: 48, color: Colors.white.withValues(alpha: 0.3)),
            const SizedBox(height: 12),
            Text('Aucun certificat obtenu', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
          ],
        ),
      );
    }
    return ListView.builder(
      padding: const EdgeInsets.all(12),
      itemCount: _certificates.length,
      itemBuilder: (context, index) {
        final cert = _certificates[index] as Map<String, dynamic>;
        final titre = cert['coursTitre'] ?? cert['titre'] ?? 'Certificat';
        final date = cert['dateObtention'] ?? cert['date'] ?? '';
        return GlassCard(
          padding: const EdgeInsets.all(14),
          child: Row(
            children: [
              Icon(Icons.workspace_premium, color: Colors.amber, size: 28),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(titre, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                    if (date.toString().isNotEmpty)
                      Text(date.toString().substring(0, 10), style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}
