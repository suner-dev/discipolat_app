import 'package:flutter/material.dart';
import '../../data/services/session_timeout_service.dart';
import '../../data/services/data_saver_service.dart';
import '../widgets/bottom_nav_bar.dart';
import 'dashboard/dashboard_screen.dart';
import 'members/member_requests_screen.dart';
import 'prayers/prayers_list_screen.dart';
import 'messages/messages_screen.dart';

/// Main scaffold with bottom navigation bar.
/// Replaces the default drawer-only navigation with a modern mobile pattern.
class MainScaffold extends StatefulWidget {
  const MainScaffold({super.key});

  @override
  State<MainScaffold> createState() => _MainScaffoldState();
}

class _MainScaffoldState extends State<MainScaffold> {
  int _currentIndex = 0;

  final List<Widget> _pages = [
    const DashboardScreen(),
    const MemberRequestsScreen(),
    const PrayersListScreen(),
    const MessagesScreen(),
    const _MorePage(),
  ];

  @override
  Widget build(BuildContext context) {
    // Reset session timeout on any interaction
    return GestureDetector(
      onTap: () => SessionTimeoutService.instance.resetTimer(),
      onPanDown: (_) => SessionTimeoutService.instance.resetTimer(),
      child: Scaffold(
        body: IndexedStack(
          index: _currentIndex,
          children: _pages,
        ),
        bottomNavigationBar: BottomNavBar(
          currentIndex: _currentIndex,
          onTap: (index) {
            SessionTimeoutService.instance.resetTimer();
            setState(() {
              _currentIndex = index;
            });
          },
        ),
      ),
    );
  }
}

/// "More" page with quick access to additional features
class _MorePage extends StatelessWidget {
  const _MorePage();

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        const SizedBox(height: 8),
        const Text(
          'Plus de fonctionnalités',
          style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 16),
        // IA & Intelligence
        _moreItem(context, Icons.auto_awesome, 'Prédictions IA', 'Analyse prédictive'),
        _moreItem(context, Icons.translate, 'Traduction sermons', 'Whisper + LLM temps réel', route: '/sermon-translations'),
        _moreItem(context, Icons.description, 'Formulaires', 'Builder drag & drop', route: '/forms'),
        _moreItem(context, Icons.trending_up, 'Projection croissance', 'Simulation IA', route: '/growth-projection'),
        _moreItem(context, Icons.psychology, 'Matching compétences', 'IA membres↔départements', route: '/skill-matching'),
        // Communication
        _moreItem(context, Icons.calendar_month, 'Calendrier', 'Sync Google/Outlook'),
        _moreItem(context, Icons.store, 'Marketplace', 'Échanges communautaires'),
        _moreItem(context, Icons.people, 'Communauté', 'Fil social & témoignages'),
        _moreItem(context, Icons.play_circle, 'Streaming', 'Cultes en direct'),
        // Gestion
        _moreItem(context, Icons.bar_chart, 'KPIs', 'Objectifs départementaux'),
        _moreItem(context, Icons.inventory_2, 'Inventaire', 'Gestion matérielle'),
        _moreItem(context, Icons.how_to_vote, 'Sondages', 'Votes & opinions'),
        _moreItem(context, Icons.assignment_turned_in, 'Témoignages', 'Partager un témoignage'),
        _moreItem(context, Icons.person_add, 'Parrainage', 'Inviter un proche'),
        _moreItem(context, Icons.event_busy, 'Absences', 'Demandes de congé'),
        _moreItem(context, Icons.request_page, 'Demandes admin', 'Baptême, dédicace', route: '/admin-requests'),
        _moreItem(context, Icons.how_to_reg, 'Bénévoles', 'Gestion & matching', route: '/volunteers'),
        // Accompagnement
        _moreItem(context, Icons.trending_up, 'Plan développement', 'Objectifs individuels', route: '/dev-plans'),
        _moreItem(context, Icons.timeline, 'Parcours faiseur', 'Suivi compétences', route: '/maker-tracking'),
        _moreItem(context, Icons.balance, 'Comparaison églises', 'Benchmark réseau', route: '/church-comparison'),
        _moreItem(context, Icons.psychology, 'Mentorat IA', 'Approches personnalisées', route: '/ai-mentoring'),
        _moreItem(context, Icons.swap_horiz, 'Mentorat inversé', 'Demander de l\'aide', route: '/reverse-mentoring'),
        _moreItem(context, Icons.account_tree, 'Succession', 'Plan de transition', route: '/succession'),
        _moreItem(context, Icons.groups, 'Réunion famille', 'Ordre du jour auto', route: '/family-meeting'),
        _moreItem(context, Icons.favorite, 'Cohésion familiale', 'Score & indicateurs', route: '/family-cohesion'),
        _moreItem(context, Icons.folder_shared, 'Ressources famille', 'Partage contrôlé', route: '/family-resources'),
        _moreItem(context, Icons.record_voice_over, 'Notes IA visites', 'Whisper → résumé', route: '/ai-visit-notes'),
        _moreItem(context, Icons.sticky_note_2, 'KPI narratif', 'Drill-down récits', route: '/kpi-narrative'),
        // Espace membre
        _moreItem(context, Icons.book, 'Journal spirituel', 'Prières & réflexions', route: '/spiritual-journal'),
        _moreItem(context, Icons.menu_book, 'Plan lecture biblique', 'Progression partagée', route: '/bible-reading'),
        _moreItem(context, Icons.auto_stories, 'Journal prière', 'Suivi réponses', route: '/prayer-journal'),
        _moreItem(context, Icons.flash_on, 'Défis spirituels', 'Défis & badges', route: '/spiritual-challenges'),
        _moreItem(context, Icons.track_changes, 'Objectifs personnels', 'Suivi progression', route: '/personal-objectives'),
        _moreItem(context, Icons.book_online, 'Annuaire église', 'Fiches publiques', route: '/church-directory'),
        _moreItem(context, Icons.emoji_events, 'Récompenses', 'Badges & gamification'),
        // Analytics
        _moreItem(context, Icons.analytics, 'Analytics engagement', 'Pages vues & funnel', route: '/engagement-analytics'),
        _moreItem(context, Icons.insights, 'Prédictions ML', 'Séries historiques', route: '/predictions'),
        _moreItem(context, Icons.dashboard, 'Centre intelligence', '50+ KPIs temps réel', route: '/intelligence-center'),
        _moreItem(context, Icons.leaderboard, 'Insights exécutifs', 'Dashboard IA', route: '/executive-insights'),
        // Opérationnel
        _moreItem(context, Icons.checklist, 'Checklist événement', 'Tâches auto-générées', route: '/event-checklist'),
        _moreItem(context, Icons.grid_view, 'Matrice compétences', 'Gaps & besoins', route: '/skills-matrix'),
        _moreItem(context, Icons.group_add, 'Messagerie groupe', 'Conversations équipe', route: '/group-messages'),
        _moreItem(context, Icons.broadcast_on_personal, 'Diffusion', 'Ciblé + accusé lecture', route: '/broadcast'),
        const SizedBox(height: 16),
        _moreItem(context, Icons.person, 'Mon profil', 'Informations personnelles'),
        _moreItem(context, Icons.shield, 'Sécurité', 'Session, biométrie, données', route: '/mobile-security'),
        _moreItem(context, Icons.settings, 'Paramètres', 'Configuration'),
      ],
    );
  }

  Widget _moreItem(BuildContext context, IconData icon, String title, String subtitle, {String? route}) {
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: Colors.purple.withOpacity(0.1),
          child: Icon(icon, color: Colors.purple.shade600, size: 20),
        ),
        title: Text(title, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
        subtitle: Text(subtitle, style: const TextStyle(fontSize: 12)),
        trailing: const Icon(Icons.chevron_right, size: 20),
        onTap: () {
          if (route != null) {
            Navigator.pushNamed(context, route);
          } else {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text('$title — bientôt disponible')),
            );
          }
        },
      ),
    );
  }
}
