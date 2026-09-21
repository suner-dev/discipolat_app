import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../data/services/api_service.dart';
import '../../../../l10n/app_localizations.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../widgets/secure_screen.dart';

/// FamilySpaceScreen — affiche l'espace famille avec activités et membres
/// Récupère les données depuis l'API backend family endpoints
class FamilySpaceScreen extends ConsumerStatefulWidget {
  const FamilySpaceScreen({super.key});

  @override
  ConsumerState<FamilySpaceScreen> createState() => _FamilySpaceScreenState();
}

class _FamilySpaceScreenState extends ConsumerState<FamilySpaceScreen> {
  final _apiService = ApiService();
  Map<String, dynamic>? _familyData;
  List<dynamic> _members = [];
  bool _isLoading = true;
  String? _error;
  int _currentNavIndex = 0;

  @override
  void initState() {
    super.initState();
    _loadFamilyData();
  }

  Future<void> _loadFamilyData() async {
    setState(() { _isLoading = true; _error = null; });
    try {
      final userId = await _getCurrentUserId();
      if (userId == null) throw Exception('User not authenticated');
      
      final res = await _apiService.get(
        '/api/v1/family/space',
        params: {'userId': userId},
      );
      if (mounted) {
        final data = res.data as Map<String, dynamic>?;
        if (data != null) {
          setState(() {
            _familyData = data;
            _members = data['members'] as List<dynamic>? ?? [];
            _isLoading = false;
          });
        } else {
          setState(() { _isLoading = false; });
        }
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = AppLocalizations.of(context).familyError;
          _isLoading = false;
        });
      }
    }
  }

  Future<String?> _getCurrentUserId() async {
    try {
      final me = await _apiService.get('/users/me');
      return (me.data as Map<String, dynamic>?)?['id'] as String?;
    } catch (_) {
      return null;
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final familyName = _familyData?['familyName']?.toString() ?? l10n.family;
    final memberCount = _familyData?['memberCount'] as int? ?? _members.length;
    final totalActivities = _familyData?['totalActivities'] as int? ?? 0;
    final recentActivity = _familyData?['recentActivity'] as List<dynamic>? ?? [];

    return SecureScreen(
      screenName: 'FamilySpaceScreen',
      auditAction: 'VIEW_FAMILY_SPACE',
      child: Scaffold(
        appBar: AppBar(
          title: Text(l10n.familySpaceTitle),
          backgroundColor: Colors.redAccent,
          foregroundColor: Colors.white,
          actions: [
            IconButton(
              icon: const Icon(Icons.refresh),
              onPressed: _loadFamilyData,
              tooltip: l10n.refresh,
            ),
          ],
        ),
        drawer: const AppDrawer(),
        body: _isLoading
            ? const Center(child: CircularProgressIndicator())
            : _error != null
                ? _buildErrorState()
                : RefreshIndicator(
                    onRefresh: _loadFamilyData,
                    child: SingleChildScrollView(
                      physics: const AlwaysScrollableScrollPhysics(),
                      padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          _buildFamilyHeader(familyName, memberCount, totalActivities, l10n),
                          const SizedBox(height: 24),
                          if (_members.isNotEmpty) ...[
                            _buildMembersList(),
                            const SizedBox(height: 24),
                          ],
                          _buildRecentActivity(recentActivity),
                        ],
                      ),
                    ),
                  ),
        bottomNavigationBar: _buildBottomNav(),
      ),
    );
  }

  Widget _buildErrorState() {
    final l10n = AppLocalizations.of(context);
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, color: Colors.red, size: 48),
            const SizedBox(height: 16),
            Text(_error!, textAlign: TextAlign.center, style: const TextStyle(fontSize: 16)),
            const SizedBox(height: 16),
            ElevatedButton.icon(
              onPressed: _loadFamilyData,
              icon: const Icon(Icons.refresh),
              label: Text(l10n.retry),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildFamilyHeader(String familyName, int memberCount, int totalActivities, AppLocalizations l10n) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        GlassCard(
          padding: const EdgeInsets.all(24),
          child: Column(
            children: [
              Text(
                familyName,
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Icon(Icons.family_restroom, color: Colors.redAccent, size: 28),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          '$memberCount ${l10n.members}',
                          style: TextStyle(
                            color: Colors.white.withValues(alpha: 0.6),
                            fontSize: 16,
                          ),
                        ),
                        Text(
                          '$totalActivities ${l10n.activitiesThisMonth}',
                          style: TextStyle(
                            color: Colors.white.withValues(alpha: 0.5),
                            fontSize: 14,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildMembersList() {
    final l10n = AppLocalizations.of(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          l10n.familyMembers,
          style: const TextStyle(
            color: Colors.white,
            fontSize: 18,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 12),
        ..._members.map((member) {
          final m = member as Map<String, dynamic>;
          final name = m['name']?.toString() ?? '—';
          final role = m['role']?.toString() ?? 'MEMBRE';
          final relationship = m['relationship']?.toString() ?? '';
          final lastActivity = m['lastActivity']?.toString() ?? '';

          return Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: GlassCard(
              padding: const EdgeInsets.all(12),
              child: Row(
                children: [
                  CircleAvatar(
                    backgroundColor: Colors.redAccent.withValues(alpha: 0.2),
                    child: Text(name.isNotEmpty ? name[0] : '?', style: const TextStyle(color: Colors.redAccent, fontWeight: FontWeight.bold)),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(name, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                        Text(role, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
                        if (relationship.isNotEmpty)
                          Text(relationship, style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 10)),
                        if (lastActivity.isNotEmpty)
                          Text('$lastActivity', style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 10)),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          );
        }),
      ],
    );
  }

  Widget _buildRecentActivity(List<dynamic> activities) {
    final l10n = AppLocalizations.of(context);
    
    if (activities.isEmpty) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            l10n.recentActivity,
            style: const TextStyle(
              color: Colors.white,
              fontSize: 18,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 12),
          GlassCard(
            child: Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Text(
                  l10n.noRecentActivity,
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5)),
                ),
              ),
            ),
          ),
        ],
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          l10n.recentActivity,
          style: const TextStyle(
            color: Colors.white,
            fontSize: 18,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 12),
        ...activities.map((activity) {
          final a = activity as Map<String, dynamic>;
          final type = a['type']?.toString() ?? 'activity';
          final title = a['title']?.toString() ?? l10n.activity;
          final date = a['date']?.toString() ?? '';
          final participants = a['participants'] as int? ?? 0;

          return Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: GlassCard(
              padding: const EdgeInsets.all(12),
              child: Row(
                children: [
                  Container(
                    width: 40,
                    height: 40,
                    decoration: BoxDecoration(
                      color: Colors.redAccent.withValues(alpha: 0.2),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: const Icon(Icons.people, color: Colors.redAccent, size: 20),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(title, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                        Text('$type • $date', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
                        if (participants > 0)
                          Text('$participants ${l10n.participants}', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 10)),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          );
        }),
      ],
    );
  }

  Widget _buildBottomNav() {
    const routes = ['/dashboard', '/souls', '/reports/maker', '/profile'];
    return GlassBottomNav(
      currentIndex: _currentNavIndex,
      onTap: (i) {
        setState(() => _currentNavIndex = i);
        if (i < routes.length) GoRouter.of(context).go(routes[i]);
      },
    );
  }
}
