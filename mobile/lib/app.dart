import 'package:go_router/go_router.dart';
import 'presentation/screens/login/login_screen.dart';
import 'presentation/screens/dashboard/dashboard_screen.dart';
import 'presentation/screens/dashboard/pasteur_dashboard_screen.dart';
import 'presentation/screens/dashboard/chef_famille_dashboard_screen.dart';
import 'presentation/screens/dashboard/responsable_dashboard_screen.dart';
import 'presentation/screens/souls/souls_list_screen.dart';
import 'presentation/screens/souls/soul_detail_screen.dart';
import 'presentation/screens/reports/maker_report_screen.dart';
import 'presentation/screens/reports/family_report_screen.dart';
import 'presentation/screens/reports/reports_screen.dart';
import 'presentation/screens/families/families_list_screen.dart';
import 'presentation/screens/alerts/alerts_list_screen.dart';
import 'presentation/screens/notifications/notifications_screen.dart';
import 'presentation/screens/profile/profile_screen.dart';
import 'presentation/screens/prayers/prayers_list_screen.dart';
import 'presentation/screens/events/events_list_screen.dart';
import 'presentation/screens/departments/departments_list_screen.dart';
import 'presentation/screens/departments/department_detail_screen.dart';
import 'presentation/screens/departments/department_report_screen.dart';
import 'presentation/screens/departments/department_management_screen.dart';
import 'presentation/screens/departments/department_stats_screen.dart';
import 'presentation/screens/departments/department_member_dossier_screen.dart';
import 'presentation/screens/departments/department_tools_screen.dart';
import 'presentation/screens/evaluations/evaluations_screen.dart';
import 'presentation/screens/search/search_screen.dart';
import 'presentation/screens/souls/crm_faiseur_screen.dart';
import 'presentation/screens/souls/pastoral_360_screen.dart';
import 'presentation/screens/users/users_list_screen.dart';
import 'presentation/screens/users/user_detail_screen.dart';
import 'presentation/screens/users/permissions_screen.dart';
import 'presentation/screens/users/documents_screen.dart';
import 'presentation/screens/users/audit_screen.dart';
import 'presentation/screens/appointments/appointments_screen.dart';
import 'presentation/screens/visits/visits_screen.dart';
import 'presentation/screens/evangelism/evangelism_screen.dart';
import 'presentation/screens/objectives/objectives_screen.dart';
import 'presentation/screens/badges/badges_screen.dart';
import 'presentation/screens/trainings/trainings_screen.dart';
import 'presentation/screens/messages/messages_screen.dart';
import 'presentation/screens/parallel_followups/parallel_followups_screen.dart';
import 'presentation/screens/map/map_screen.dart';
import 'presentation/screens/members/member_requests_screen.dart';
import 'presentation/screens/transfers/transfers_list_screen.dart';
import 'presentation/screens/transfers/transfer_detail_screen.dart';
import 'presentation/screens/transfers/transfer_create_screen.dart';
import 'presentation/screens/transfers/transfer_admin_screen.dart';
import 'presentation/screens/platform/platform_modules_screen.dart';
import 'presentation/screens/platform/platform_menus_screen.dart';
import 'presentation/screens/platform/platform_pages_screen.dart';
import 'presentation/screens/finances/finance_screen.dart';
import 'presentation/screens/communications/communications_screen.dart';
import 'presentation/screens/not_found_screen.dart';

/// Auth state notifier — singleton that tracks the authenticated user
/// with full multi-role support (roles + activeRole).
class AuthState {
  static final AuthState _instance = AuthState._internal();
  factory AuthState() => _instance;
  AuthState._internal();

  bool _isAuthenticated = false;
  String? _userId;
  String? _email;
  String? _userRole;
  List<String> _roles = [];
  String _activeRole = '';
  String? _firstName;
  String? _lastName;
  bool _estChefDeFamille = false;
  String? _familleGereeId;

  bool get isAuthenticated => _isAuthenticated;
  String? get userId => _userId;
  String? get email => _email;
  String? get userRole => _userRole;
  List<String> get roles => _roles;
  String get activeRole => _activeRole;
  String? get firstName => _firstName;
  String? get lastName => _lastName;
  bool get estChefDeFamille => _estChefDeFamille;
  String? get familleGereeId => _familleGereeId;

  void setAuthenticated(bool value, {Map<String, dynamic>? userData}) {
    _isAuthenticated = value;
    if (userData != null) {
      _userId = userData['userId'] as String?;
      _email = userData['email'] as String?;
      _userRole = userData['role'] as String?;
      _roles = userData['roles'] != null
          ? List<String>.from(userData['roles'] as List)
          : (_userRole != null ? [_userRole!] : []);
      _activeRole = userData['activeRole'] as String? ?? _userRole ?? '';
      _firstName = userData['firstName'] as String?;
      _lastName = userData['lastName'] as String?;
      _estChefDeFamille = userData['estChefDeFamille'] as bool? ?? false;
      _familleGereeId = userData['familleGereeId'] as String?;
    }
  }

  /// Switch the active role (does NOT call the API ; caller must call POST /auth/switch-role)
  void switchActiveRole(String newRole) {
    if (_roles.contains(newRole)) {
      _activeRole = newRole;
      _userRole = newRole;
    }
  }

  /// Check if the user has ANY of the specified roles (across all roles)
  bool hasAnyRole(List<String> checkRoles) {
    return _roles.any((r) => checkRoles.contains(r));
  }

  /// Check if the active role is one of the specified roles
  bool hasActiveRole(List<String> checkRoles) {
    return checkRoles.contains(_activeRole);
  }

  void logout() {
    _isAuthenticated = false;
    _userId = null;
    _email = null;
    _userRole = null;
    _roles = [];
    _activeRole = '';
    _firstName = null;
    _lastName = null;
    _estChefDeFamille = false;
    _familleGereeId = null;
  }
}

/// Tableau de bord racine de chaque espace métier.
/// Un changement de rôle = un changement complet de contexte de travail.
String roleHome(String role) {
  switch (role) {
    case 'FAISEUR':
      return '/crm-faiseur';
    case 'RESPONSABLE':
      return '/dashboard/responsable';
    case 'CHEF_DE_FAMILLE':
      return '/dashboard/chef-famille';
    case 'MEMBRE':
      return '/profile';
    default:
      return '/dashboard'; // ADMIN, PASTEUR
  }
}

/// List of roles allowed per route.
/// null = all authenticated users, [] = no one (public only).
Map<String, List<String>> _routeRoles = {
  '/dashboard': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/dashboard/pasteur': ['ADMIN', 'PASTEUR'],
  '/dashboard/chef-famille': ['ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE'],
  '/dashboard/responsable': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/souls': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/families': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/reports': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/reports/maker': ['ADMIN', 'PASTEUR', 'FAISEUR'],
  '/reports/family': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/prayers': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/events': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/alerts': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/profile': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/notifications': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/departments': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/departments/:id': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/departments/:id/manage': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/departments/:id/stats': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/departments/:id/members/:memberId': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/departments/:id/tools': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/evaluations': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/crm-faiseur': ['ADMIN', 'PASTEUR', 'FAISEUR'],
  '/souls/:id/pastoral-360': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/search': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/users': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/permissions': ['ADMIN'],
  '/documents': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/audit': ['ADMIN', 'PASTEUR'],
  '/appointments': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/visits': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/evangelism': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/objectives': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/badges': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/trainings': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/communications': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/messages': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/parallel-followups': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/map': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/members/requests': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE'],
  '/transfers': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/admin': ['ADMIN', 'PASTEUR'],
  '/finances': ['ADMIN', 'PASTEUR'],
  '/admin/modules': ['ADMIN'],
  '/admin/menus': ['ADMIN'],
  '/admin/pages': ['ADMIN'],
};

final appRouter = GoRouter(
  initialLocation: '/login',
  redirect: (context, state) {
    final auth = AuthState();
    final loginRoute = '/login';
    final isLoginRoute = state.matchedLocation == loginRoute;

    // If not authenticated and trying to access a protected route → redirect to login
    if (!auth.isAuthenticated && !isLoginRoute) {
      return loginRoute;
    }

    // If authenticated and on login page → redirect to dashboard
    if (auth.isAuthenticated && isLoginRoute) {
      return '/dashboard';
    }

    // Garde par rôle ACTIF — isolation stricte des espaces métiers.
    // Un utilisateur multi-rôles n'accède qu'à l'espace métier courant :
    // changer de rôle change complètement l'application.
    if (auth.isAuthenticated && auth.activeRole.isNotEmpty) {
      final role = auth.activeRole;

      // 1) Chaque route n'est accessible que si le rôle ACTIF est autorisé.
      //    Priorité à la route exacte (ex. /admin/modules réservé ADMIN), puis
      //    repli sur le premier segment (ex. /souls/:id/pastoral-360).
      final segments = state.matchedLocation.split('/');
      final basePath = '/${segments.length > 1 ? segments[1] : ''}';
      final allowedRoles = _routeRoles[state.matchedLocation] ?? _routeRoles[basePath];
      if (allowedRoles != null && !auth.hasActiveRole(allowedRoles)) {
        // L'Admin actif dispose des capacités du Pasteur (super-utilisateurs
        // qui partagent la vue complète de l'application).
        if (!(role == 'ADMIN' && allowedRoles.contains('PASTEUR'))) {
          return roleHome(role); // Ramené vers son propre espace métier
        }
      }

      // 2) Isolation des tableaux de bord : les rôles opérationnels sont
      //    ramenés vers leur propre espace et ne peuvent pas ouvrir le
      //    dashboard d'un autre métier (responsable, chef, faiseur, membre).
      if (!auth.hasActiveRole(['ADMIN', 'PASTEUR'])) {
        final home = roleHome(role);
        final location = state.matchedLocation;
        final otherHomes = ['/dashboard/responsable', '/dashboard/chef-famille', '/crm-faiseur'];
        if (location == '/dashboard' || (otherHomes.contains(location) && location != home)) {
          return home;
        }
      }
    }

    return null; // allow navigation
  },
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
      path: '/dashboard/pasteur',
      name: 'pasteur-dashboard',
      builder: (context, state) => const PasteurDashboardScreen(),
    ),
    GoRoute(
      path: '/dashboard/chef-famille',
      name: 'chef-famille-dashboard',
      builder: (context, state) => const ChefFamilleDashboardScreen(),
    ),
    GoRoute(
      path: '/dashboard/responsable',
      name: 'responsable-dashboard',
      builder: (context, state) => const ResponsableDashboardScreen(),
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
      path: '/reports',
      name: 'reports',
      builder: (context, state) => const ReportsScreen(),
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
    GoRoute(
      path: '/prayers',
      name: 'prayers',
      builder: (context, state) => const PrayersListScreen(),
    ),
    GoRoute(
      path: '/events',
      name: 'events',
      builder: (context, state) => const EventsListScreen(),
    ),
    GoRoute(
      path: '/departments',
      name: 'departments',
      builder: (context, state) => const DepartmentsListScreen(),
    ),
    GoRoute(
      path: '/departments/:id',
      name: 'department-detail',
      builder: (context, state) => DepartmentDetailScreen(
        departmentId: state.pathParameters['id']!,
      ),
    ),
    GoRoute(
      path: '/departments/:id/report',
      name: 'department-report',
      builder: (context, state) => DepartmentReportScreen(
        departmentId: state.pathParameters['id']!,
      ),
    ),
    GoRoute(
      path: '/departments/:id/manage',
      name: 'department-manage',
      builder: (context, state) => DepartmentManagementScreen(
        departmentId: state.pathParameters['id']!,
      ),
    ),
    GoRoute(
      path: '/departments/:id/stats',
      name: 'department-stats',
      builder: (context, state) => DepartmentStatsScreen(
        departmentId: state.pathParameters['id']!,
      ),
    ),
    GoRoute(
      path: '/departments/:id/members/:memberId',
      name: 'department-member-dossier',
      builder: (context, state) => DepartmentMemberDossierScreen(
        departmentId: state.pathParameters['id']!,
        memberId: state.pathParameters['memberId']!,
      ),
    ),
    GoRoute(
      path: '/departments/:id/tools',
      name: 'department-tools',
      builder: (context, state) => DepartmentToolsScreen(
        departmentId: state.pathParameters['id']!,
      ),
    ),
    GoRoute(
      path: '/evaluations',
      name: 'evaluations',
      builder: (context, state) => const EvaluationsScreen(),
    ),
    GoRoute(
      path: '/crm-faiseur',
      name: 'crm-faiseur',
      builder: (context, state) => const CrmFaiseurScreen(),
    ),
    GoRoute(
      path: '/souls/:id/pastoral-360',
      name: 'soul-pastoral-360',
      builder: (context, state) => Pastoral360Screen(
        soulId: state.pathParameters['id']!,
      ),
    ),
    GoRoute(
      path: '/search',
      name: 'search',
      builder: (context, state) => const SearchScreen(),
    ),
    GoRoute(
      path: '/users',
      name: 'users',
      builder: (context, state) => const UsersListScreen(),
    ),
    GoRoute(
      path: '/users/:id',
      name: 'user-detail',
      builder: (context, state) => UserDetailScreen(
        userId: state.pathParameters['id']!,
      ),
    ),
    GoRoute(
      path: '/permissions',
      name: 'permissions',
      builder: (context, state) => const PermissionsScreen(),
    ),
    GoRoute(
      path: '/documents',
      name: 'documents',
      builder: (context, state) => const DocumentsScreen(),
    ),
    GoRoute(
      path: '/audit',
      name: 'audit',
      builder: (context, state) => const AuditScreen(),
    ),
    GoRoute(
      path: '/appointments',
      name: 'appointments',
      builder: (context, state) => const AppointmentsScreen(),
    ),
    GoRoute(
      path: '/visits',
      name: 'visits',
      builder: (context, state) => const VisitsScreen(),
    ),
    GoRoute(
      path: '/evangelism',
      name: 'evangelism',
      builder: (context, state) => const EvangelismScreen(),
    ),
    GoRoute(
      path: '/objectives',
      name: 'objectives',
      builder: (context, state) => const ObjectivesScreen(),
    ),
    GoRoute(
      path: '/parallel-followups',
      name: 'parallel-followups',
      builder: (context, state) => const ParallelFollowupsScreen(),
    ),
    GoRoute(
      path: '/badges',
      name: 'badges',
      builder: (context, state) => const BadgesScreen(),
    ),
    GoRoute(
      path: '/trainings',
      name: 'trainings',
      builder: (context, state) => const TrainingsScreen(),
    ),
    GoRoute(
      path: '/messages',
      name: 'messages',
      builder: (context, state) => const MessagesScreen(),
    ),
    GoRoute(
      path: '/map',
      name: 'map',
      builder: (context, state) => const MapScreen(),
    ),
    GoRoute(
      path: '/members/requests',
      name: 'member-requests',
      builder: (context, state) => const MemberRequestsScreen(),
    ),
    GoRoute(
      path: '/transfers',
      name: 'transfers',
      builder: (context, state) => const TransfersListScreen(),
    ),
    GoRoute(
      path: '/transfers/new',
      name: 'transfer-create',
      builder: (context, state) => const TransferCreateScreen(),
    ),
    GoRoute(
      path: '/transfers/:id',
      name: 'transfer-detail',
      builder: (context, state) => TransferDetailScreen(
        transferId: state.pathParameters['id']!,
      ),
    ),
    GoRoute(
      path: '/admin/transfers',
      name: 'transfer-admin',
      builder: (context, state) => const TransferAdminScreen(),
    ),
    GoRoute(
      path: '/admin/modules',
      name: 'platform-modules',
      builder: (context, state) => const PlatformModulesScreen(),
    ),
    GoRoute(
      path: '/admin/menus',
      name: 'platform-menus',
      builder: (context, state) => const PlatformMenusScreen(),
    ),
    GoRoute(
      path: '/admin/pages',
      name: 'platform-pages',
      builder: (context, state) => const PlatformPagesScreen(),
    ),
    GoRoute(
      path: '/finances',
      name: 'finances',
      builder: (context, state) => const FinanceScreen(),
    ),
    GoRoute(
      path: '/communications',
      name: 'communications',
      builder: (context, state) => const CommunicationsScreen(),
    ),
  ],
  errorBuilder: (context, state) => NotFoundScreen(path: state.matchedLocation),
);
