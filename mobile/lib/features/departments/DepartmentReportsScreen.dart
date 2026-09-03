import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Rapports et activité des départements — branché sur /api/v1/departments/{departmentId}/reports.
class DepartmentReportsScreen extends StatefulWidget {
  final String departmentId;
  const DepartmentReportsScreen({super.key, required this.departmentId});

  @override
  State<DepartmentReportsScreen> createState() => _DepartmentReportsScreenState();
}

class _DepartmentReportsScreenState extends State<DepartmentReportsScreen> with SingleTickerProviderStateMixin {
  final _apiService = ApiService();
  late TabController _tabController;
  List<dynamic> _reports = [];
  List<dynamic> _activity = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _loadData();
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  Future<void> _loadData() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final results = await Future.wait([
        _apiService.get('/departments/${widget.departmentId}/reports'),
        _apiService.get('/departments/${widget.departmentId}/activity'),
      ]);
      if (mounted) {
        final rData = results[0].data;
        final aData = results[1].data;
        setState(() {
          _reports = rData is List
              ? rData
              : (rData is Map && rData['content'] is List
                  ? rData['content'] as List<dynamic>
                  : <dynamic>[]);
          _activity = aData is List
              ? aData
              : (aData is Map && aData['content'] is List
                  ? aData['content'] as List<dynamic>
                  : <dynamic>[]);
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _error = 'Erreur lors du chargement';
          _isLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Rapports du département'),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: Colors.white,
          tabs: const [
            Tab(text: 'Rapports'),
            Tab(text: 'Activité'),
          ],
        ),
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 5)
          : _error != null
              ? _buildError()
              : TabBarView(
                  controller: _tabController,
                  children: [
                    _buildReportsTab(),
                    _buildActivityTab(),
                  ],
                ),
    );
  }

  Widget _buildReportsTab() {
    if (_reports.isEmpty) {
      return _buildEmpty('Aucun rapport');
    }
    return RefreshIndicator(
      onRefresh: _loadData,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: _reports.length,
        itemBuilder: (context, i) {
          final r = _reports[i] as Map<String, dynamic>;
          final titre = r['titre']?.toString() ?? r['nom']?.toString() ?? 'Rapport';
          final dateStr = r['date']?.toString().substring(0, 10) ?? r['createdAt']?.toString().substring(0, 10) ?? '';
          final auteur = r['auteur']?.toString() ?? r['author']?.toString() ?? '';
          final type = r['type']?.toString() ?? '';

          return GlassCard(
            margin: const EdgeInsets.only(bottom: 10),
            padding: const EdgeInsets.all(14),
            child: Row(
              children: [
                Container(
                  width: 44,
                  height: 44,
                  decoration: BoxDecoration(
                    color: AppColors.primary.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Icon(Icons.assessment, color: AppColors.primary, size: 20),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(titre,
                          style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                      if (auteur.isNotEmpty || dateStr.isNotEmpty)
                        Text(
                          [if (auteur.isNotEmpty) auteur, if (dateStr.isNotEmpty) dateStr].join('  •  '),
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11),
                        ),
                    ],
                  ),
                ),
                if (type.isNotEmpty)
                  StatusBadge(label: type, color: Colors.blue),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _buildActivityTab() {
    if (_activity.isEmpty) {
      return _buildEmpty('Aucune activité');
    }
    return RefreshIndicator(
      onRefresh: _loadData,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: _activity.length,
        itemBuilder: (context, i) {
          final a = _activity[i] as Map<String, dynamic>;
          final action = a['action']?.toString() ?? a['description']?.toString() ?? '';
          final membre = a['membre']?.toString() ?? a['memberName']?.toString() ?? '';
          final dateStr = a['date']?.toString().substring(0, 10) ?? a['createdAt']?.toString().substring(0, 10) ?? '';

          return GlassCard(
            margin: const EdgeInsets.only(bottom: 8),
            padding: const EdgeInsets.all(12),
            child: Row(
              children: [
                Icon(Icons.circle, color: AppColors.primary, size: 8),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      if (action.isNotEmpty)
                        Text(action,
                            style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w500, fontSize: 13)),
                      if (membre.isNotEmpty)
                        Text(membre,
                            style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
                    ],
                  ),
                ),
                if (dateStr.isNotEmpty)
                  Text(dateStr, style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 10)),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _buildEmpty(String message) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.assessment, size: 48, color: Colors.white.withValues(alpha: 0.2)),
          const SizedBox(height: 12),
          Text(message, style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
        ],
      ),
    );
  }

  Widget _buildError() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.error_outline, color: Colors.white.withValues(alpha: 0.3), size: 48),
          const SizedBox(height: 12),
          Text(_error!, style: TextStyle(color: Colors.white.withValues(alpha: 0.5)), textAlign: TextAlign.center),
          const SizedBox(height: 12),
          FilledButton.icon(onPressed: _loadData, icon: const Icon(Icons.refresh, size: 16), label: const Text('Réessayer')),
        ],
      ),
    );
  }
}
