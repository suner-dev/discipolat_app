import 'package:go_router/go_router.dart';
import 'presentation/screens/login/login_screen.dart';
import 'presentation/screens/dashboard/dashboard_screen.dart';
import 'presentation/screens/souls/souls_list_screen.dart';
import 'presentation/screens/souls/soul_detail_screen.dart';
import 'presentation/screens/reports/maker_report_screen.dart';
import 'presentation/screens/reports/family_report_screen.dart';
import 'presentation/screens/families/families_list_screen.dart';
import 'presentation/screens/alerts/alerts_list_screen.dart';
import 'presentation/screens/notifications/notifications_screen.dart';
import 'presentation/screens/profile/profile_screen.dart';

final appRouter = GoRouter(
  initialLocation: '/login',
  routes: [
    GoRoute(
      path: '/login',
      name: 'login',
      builder: (context, state) => const LoginScreen(),
    ),
    GoRoute(
      path: '/dashboard',
      name: 'dashboard',
      builder: (context, state) => const DashboardScreen(),
    ),
    GoRoute(
      path: '/souls',
      name: 'souls',
      builder: (context, state) => const SoulsListScreen(),
    ),
    GoRoute(
      path: '/souls/:id',
      name: 'soul-detail',
      builder: (context, state) => SoulDetailScreen(
        soulId: state.pathParameters['id']!,
      ),
    ),
    GoRoute(
      path: '/reports/maker',
      name: 'maker-report',
      builder: (context, state) => const MakerReportScreen(),
    ),
    GoRoute(
      path: '/reports/family',
      name: 'family-report',
      builder: (context, state) => const FamilyReportScreen(),
    ),
    GoRoute(
      path: '/families',
      name: 'families',
      builder: (context, state) => const FamiliesListScreen(),
    ),
    GoRoute(
      path: '/alerts',
      name: 'alerts',
      builder: (context, state) => const AlertsListScreen(),
    ),
    GoRoute(
      path: '/notifications',
      name: 'notifications',
      builder: (context, state) => const NotificationsScreen(),
    ),
    GoRoute(
      path: '/profile',
      name: 'profile',
      builder: (context, state) => const ProfileScreen(),
    ),
  ],
);
