import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';

/// Dashboard personnel du Membre.
/// Consomme : /members/me/dashboard, /members/me/progression,
///            /members/me/events, /members/me/notes, /members/me/presences
class MemberDashboardScreen extends StatefulWidget {
  const MemberDashboardScreen({super.key});

  @override
  State<MemberDashboardScreen> createState() => _MemberDashboardScreenState();
}

class _MemberDashboardScreenState extends State<MemberDashboardScreen>
    with SingleTickerProviderStateMixin {
  final _apiService = ApiService();
  Map<String, dynamic>? _memberDashboard;
  Map<String, dynamic>? _progression;
  List<dynamic> _events = [];
  List<dynamic> _notes = [];
  List<dynamic> _presences = [];
  bool _isLoading = true;

  late final AnimationController _animCtrl;
  late final Animation<double> _fadeAnim;

  @override
  void initState() {
    super.initState();
    _animCtrl = AnimationController(vsync: this, duration: const Duration(milliseconds: 600));
    _fadeAnim = CurvedAnimation(parent: _animCtrl, curve: Curves.easeOut);
    _loadData();
  }

  @override
  void dispose() {
    _animCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      // Dashboard principal
      final dashRes = await _apiService.get('/members/me/dashboard');
      _memberDashboard = dashRes.data as Map<String, dynamic>?;

      // Progression
      try {
        final progRes = await _apiService.get('/members/me/progression');
        _progression = progRes.data as Map<String, dynamic>?;
      } catch (_) {}

      // Événements à venir
      try {
        final evRes = await _apiService.get('/members/me/events');
        _events = (evRes.data as List?) ?? [];
      } catch (_) {}

      // Notes du faiseur
      try {
        final noteRes = await _apiService.get('/members/me/notes');
        _notes = (noteRes.data as List?) ?? [];
      } catch (_) {}

      // Présences récentes
      try {
        final presRes = await _apiService.get('/members/me/presences');
        _presences = (presRes.data as List?) ?? [];
      } catch (_) {}

      if (mounted) {
        setState(() => _isLoading = false);
        _animCtrl.forward(from: 0);
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final user = _memberDashboard?['user'] as Map<String, dynamic>? ?? {};
    final soul = _memberDashboard?['soul'] as Map<String, dynamic>? ?? {};
    final famille = _memberDashboard?['famille'] as Map<String, dynamic>? ?? {};
    final faiseur = _memberDashboard?['faiseur'] as Map<String, dynamic>? ?? {};
    final departements = _memberDashboard?['departements'] as List<dynamic>? ?? [];
    final age = _memberDashboard?['age'];

    // Présences stats
    final presencesCount = _presences.where((p) => (p as Map<String, dynamic>)['present'] == true).length;
    final totalPresences = _presences.length;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Mon espace'),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 6)
          : RefreshIndicator(
              onRefresh: _loadData,
              child: FadeTransition(
                opacity: _fadeAnim,
                child: SingleChildScrollView(
                  physics: const AlwaysScrollableScrollPhysics(),
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // ==================== HEADER PROFIL ====================
                      Container(
                        padding: const EdgeInsets.all(20),
                        decoration: BoxDecoration(
                          gradient: LinearGradient(
                            colors: [AppColors.primary.withValues(alpha: 0.15), Colors.transparent],
                            begin: Alignment.topLeft,
                            end: Alignment.bottomRight,
                          ),
                          borderRadius: BorderRadius.circular(16),
                          border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
                        ),
                        child: Row(
                          children: [
                            GradientAvatar(
                              text: '${user['firstName'] ?? ''} ${user['lastName'] ?? ''}',
                              radius: 30,
                              showGlow: true,
                            ),
                            const SizedBox(width: 16),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    '${user['firstName'] ?? ''} ${user['lastName'] ?? ''}'.trim(),
                                    style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold),
                                  ),
                                  const SizedBox(height: 2),
                                  Text(
                                    user['email'] ?? '',
                                    style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12),
                                  ),
                                  if (age != null) ...[
                                    const SizedBox(height: 2),
                                    Text('$age ans', style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                                  ],
                                ],
                              ),
                            ),
                            IconButton(
                              icon: Icon(Icons.edit, color: Colors.white.withValues(alpha: 0.5)),
                              onPressed: () => context.go('/profile'),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 16),

                      // ==================== STATS PERSONNELLES ====================
                      GridView.count(
                        shrinkWrap: true,
                        physics: const NeverScrollableScrollPhysics(),
                        crossAxisCount: 2,
                        crossAxisSpacing: 10,
                        mainAxisSpacing: 10,
                        childAspectRatio: 1.5,
                        children: [
                          GlassStatCard(
                            label: 'Présences',
                            value: '$presencesCount/$totalPresences',
                            icon: Icons.check_circle,
                            gradientStart: Colors.green,
                            gradientEnd: Colors.teal,
                            onTap: () => context.go('/profile'),
                          ),
                          GlassStatCard(
                            label: 'Progression',
                            value: '${_progression?['niveauActuel'] ?? soul['niveauCroissance'] ?? '—'}',
                            icon: Icons.trending_up,
                            gradientStart: Colors.purple,
                            gradientEnd: Colors.indigo,
                            onTap: () => context.go('/profile'),
                          ),
                          GlassStatCard(
                            label: 'Statut',
                            value: (soul['statut'] ?? 'MEMBRE').toString().replaceAll('_', ' '),
                            icon: Icons.emoji_people,
                            gradientStart: Colors.blue,
                            gradientEnd: Colors.cyan,
                          ),
                          GlassStatCard(
                            label: 'Événements',
                            value: '${_events.length}',
                            icon: Icons.event,
                            gradientStart: Colors.orange,
                            gradientEnd: Colors.deepOrange,
                            onTap: () => context.go('/events'),
                          ),
                        ],
                      ),
                      const SizedBox(height: 16),

                      // ==================== MA FAMILLE ====================
                      if (famille.isNotEmpty) ...[
                        SectionTitle(title: 'Ma famille', icon: Icons.home),
                        GlassCard(
                          padding: const EdgeInsets.all(16),
                          child: Row(
                            children: [
                              Container(
                                padding: const EdgeInsets.all(10),
                                decoration: BoxDecoration(
                                  color: const Color(0xFFD4AF37).withValues(alpha: 0.15),
                                  borderRadius: BorderRadius.circular(12),
                                ),
                                child: const Icon(Icons.home, color: Color(0xFFD4AF37), size: 22),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(famille['nom'] ?? '', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 15)),
                                    Text('Chef : ${famille['chefNom'] ?? '—'}', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                                  ],
                                ),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 16),
                      ],

                      // ==================== MON FAISEUR ====================
                      if (faiseur.isNotEmpty) ...[
                        SectionTitle(title: 'Mon faiseur', icon: Icons.person),
                        GlassCard(
                          padding: const EdgeInsets.all(16),
                          child: Row(
                            children: [
                              GradientAvatar(text: faiseur['nom'] ?? '?', radius: 20),
                              const SizedBox(width: 12),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(faiseur['nom'] ?? '', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600)),
                                    const Text('Accompagnateur', style: TextStyle(color: Colors.white38, fontSize: 11)),
                                  ],
                                ),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 16),
                      ],

                      // ==================== MES DÉPARTEMENTS ====================
                      if (departements.isNotEmpty) ...[
                        SectionTitle(title: 'Mes départements', icon: Icons.business),
                        ...departements.map((d) {
                          final dept = d as Map<String, dynamic>;
                          return GlassCard(
                            margin: const EdgeInsets.only(bottom: 8),
                            padding: const EdgeInsets.all(12),
                            onTap: () => context.go('/departments'),
                            child: Row(
                              children: [
                                Container(
                                  padding: const EdgeInsets.all(8),
                                  decoration: BoxDecoration(color: AppColors.primary.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(8)),
                                  child: Icon(Icons.business, color: AppColors.primaryLight, size: 18),
                                ),
                                const SizedBox(width: 10),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(dept['nom'] ?? '', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13)),
                                      Text('Responsable : ${dept['responsableNom'] ?? '—'}',
                                          style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                          );
                        }),
                        const SizedBox(height: 16),
                      ],

                      // ==================== PROGRESSION SPIRITUELLE ====================
                      if (_progression != null) ...[
                        SectionTitle(title: 'Ma progression', icon: Icons.trending_up),
                        GlassCard(
                          padding: const EdgeInsets.all(16),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              _progressionRow('Niveau actuel', '${_progression!['niveauActuel'] ?? soul['niveauCroissance'] ?? 1}', Colors.purple),
                              const SizedBox(height: 8),
                              _progressionRow('Présences cette semaine', '${_progression!['presencesSemaine'] ?? presencesCount}', Colors.green),
                              const SizedBox(height: 8),
                              _progressionRow('Activités', '${_progression!['activites'] ?? 0}', Colors.blue),
                              if (_progression!['prochaineEtape'] != null) ...[
                                const SizedBox(height: 12),
                                Container(
                                  padding: const EdgeInsets.all(10),
                                  decoration: BoxDecoration(
                                    color: AppColors.primary.withValues(alpha: 0.1),
                                    borderRadius: BorderRadius.circular(8),
                                  ),
                                  child: Row(
                                    children: [
                                      Icon(Icons.flag, color: AppColors.primaryLight, size: 16),
                                      const SizedBox(width: 8),
                                      Expanded(
                                        child: Text('Prochaine étape : ${_progression!['prochaineEtape']}',
                                            style: TextStyle(color: AppColors.primaryLight, fontSize: 12, fontWeight: FontWeight.w600)),
                                      ),
                                    ],
                                  ),
                                ),
                              ],
                            ],
                          ),
                        ),
                        const SizedBox(height: 16),
                      ],

                      // ==================== NOTES DU FAISEUR ====================
                      if (_notes.isNotEmpty) ...[
                        SectionTitle(title: 'Notes de mon faiseur', icon: Icons.sticky_note_2),
                        ..._notes.take(3).map((n) {
                          final note = n as Map<String, dynamic>;
                          return GlassCard(
                            margin: const EdgeInsets.only(bottom: 8),
                            padding: const EdgeInsets.all(12),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Row(
                                  children: [
                                    Icon(Icons.sticky_note_2, color: AppColors.primaryLight, size: 14),
                                    const SizedBox(width: 6),
                                    Text(note['type'] ?? 'Note', style: TextStyle(color: AppColors.primaryLight, fontSize: 10, fontWeight: FontWeight.w600)),
                                    const Spacer(),
                                    Text(note['createdAt'] ?? '', style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 10)),
                                  ],
                                ),
                                const SizedBox(height: 6),
                                Text(note['contenu'] ?? note['texte'] ?? '', style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 13)),
                              ],
                            ),
                          );
                        }),
                        const SizedBox(height: 16),
                      ],

                      // ==================== DERNIÈRES PRÉSENCES ====================
                      if (_presences.isNotEmpty) ...[
                        SectionTitle(title: 'Mes présences récentes', icon: Icons.check_circle),
                        ..._presences.take(5).map((p) {
                          final pres = p as Map<String, dynamic>;
                          final isPresent = pres['present'] == true;
                          return GlassCard(
                            margin: const EdgeInsets.only(bottom: 6),
                            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                            child: Row(
                              children: [
                                Icon(
                                  isPresent ? Icons.check_circle : Icons.cancel,
                                  color: isPresent ? Colors.green : Colors.red,
                                  size: 20,
                                ),
                                const SizedBox(width: 10),
                                Expanded(
                                  child: Text(pres['semaine'] ?? pres['typeProgramme'] ?? '—',
                                      style: const TextStyle(color: Colors.white, fontSize: 13)),
                                ),
                                Text(
                                  isPresent ? 'Présent' : 'Absent',
                                  style: TextStyle(color: isPresent ? Colors.green : Colors.red, fontWeight: FontWeight.w600, fontSize: 12),
                                ),
                              ],
                            ),
                          );
                        }),
                        const SizedBox(height: 16),
                      ],

                      // ==================== ÉVÉNEMENTS À VENIR ====================
                      if (_events.isNotEmpty) ...[
                        SectionTitle(title: 'Événements à venir', icon: Icons.event),
                        ..._events.take(4).map((ev) {
                          final event = ev as Map<String, dynamic>;
                          return GlassCard(
                            margin: const EdgeInsets.only(bottom: 8),
                            padding: const EdgeInsets.all(12),
                            onTap: () => context.go('/events'),
                            child: Row(
                              children: [
                                Container(
                                  padding: const EdgeInsets.all(8),
                                  decoration: BoxDecoration(color: AppColors.primary.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(8)),
                                  child: Icon(Icons.event, color: AppColors.primaryLight, size: 18),
                                ),
                                const SizedBox(width: 10),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(event['titre'] ?? '', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13),
                                          maxLines: 1, overflow: TextOverflow.ellipsis),
                                      Text('${event['dateDebut'] ?? '—'}${event['lieu'] != null ? ' · ${event['lieu']}' : ''}',
                                          style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                          );
                        }),
                        const SizedBox(height: 16),
                      ],

                      // ==================== ACTIONS RAPIDES ====================
                      SectionTitle(title: 'Actions rapides', icon: Icons.flash_on),
                      GridView.count(
                        shrinkWrap: true,
                        physics: const NeverScrollableScrollPhysics(),
                        crossAxisCount: 4,
                        crossAxisSpacing: 8,
                        mainAxisSpacing: 8,
                        childAspectRatio: 0.9,
                        children: [
                          _quickAction(Icons.person, 'Profil', () => context.go('/profile')),
                          _quickAction(Icons.check_circle, 'Présences', () => context.go('/profile')),
                          _quickAction(Icons.event, 'Événements', () => context.go('/events')),
                          _quickAction(Icons.book, 'Prières', () => context.go('/prayers')),
                          _quickAction(Icons.mail, 'Demandes', () => context.go('/members/requests')),
                          _quickAction(Icons.school, 'Formations', () => context.go('/trainings')),
                          _quickAction(Icons.emoji_events, 'Badges', () => context.go('/badges')),
                          _quickAction(Icons.calendar_today, 'RDV', () => context.go('/appointments')),
                        ],
                      ),
                      const SizedBox(height: 80),
                    ],
                  ),
                ),
              ),
            ),
    );
  }

  Widget _progressionRow(String label, String value, Color color) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 13)),
        Text(value, style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 15)),
      ],
    );
  }

  Widget _quickAction(IconData icon, String label, VoidCallback onTap) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        decoration: BoxDecoration(
          color: Colors.white.withValues(alpha: 0.05),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, color: AppColors.primaryLight, size: 22),
            const SizedBox(height: 4),
            Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 10)),
          ],
        ),
      ),
    );
  }
}
