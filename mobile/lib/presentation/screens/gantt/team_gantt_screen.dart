import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

/// P1 #31 — Gantt des équipes : planning visuel des affectations
/// par équipe/événement avec détection des surcharges.
class TeamGanttScreen extends StatefulWidget {
  const TeamGanttScreen({super.key});

  @override
  State<TeamGanttScreen> createState() => _TeamGanttScreenState();
}

class _TeamGanttScreenState extends State<TeamGanttScreen> {
  final _api = ApiService();
  List<dynamic> _tasks = [];
  List<dynamic> _teams = [];
  bool _isLoading = true;
  String _selectedTeam = 'all';

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final results = await Future.wait([
        _api.get('/team-tasks'),
        _api.get('/departments'),
      ]);
      if (mounted) {
        setState(() {
          _tasks = (results[0].data as List<dynamic>?) ?? [];
          _teams = (results[1].data as List<dynamic>?) ?? [];
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF030712),
      appBar: AppBar(
        title: const Text('📋 Planning Équipes',
            style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
        backgroundColor: Colors.transparent,
        elevation: 0,
        actions: [
          IconButton(icon: const Icon(Icons.refresh, color: Colors.white70), onPressed: _loadData),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Color(0xFF06B6D4)))
          : _tasks.isEmpty
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.view_timeline, color: Colors.white.withAlpha(40), size: 56),
                      const SizedBox(height: 12),
                      Text('Aucune tâche planifiée',
                          style: TextStyle(color: Colors.white.withAlpha(120), fontSize: 14)),
                    ],
                  ),
                )
              : RefreshIndicator(
                  onRefresh: _loadData,
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      // Team filter
                      _buildTeamFilter(),
                      const SizedBox(height: 16),
                      // Gantt chart
                      _buildGanttChart(),
                      const SizedBox(height: 16),
                      // Task list
                      _buildTaskList(),
                    ],
                  ),
                ),
    );
  }

  Widget _buildTeamFilter() {
    return SizedBox(
      height: 36,
      child: ListView(
        scrollDirection: Axis.horizontal,
        children: [
          _filterChip('Toutes', 'all'),
          ..._teams.map((t) {
            final name = t['nom'] ?? t['name'] ?? '';
            final id = t['id'] ?? '';
            return _filterChip(name, id.toString());
          }),
        ],
      ),
    );
  }

  Widget _filterChip(String label, String value) {
    final isSelected = _selectedTeam == value;
    return Padding(
      padding: const EdgeInsets.only(right: 8),
      child: GestureDetector(
        onTap: () => setState(() => _selectedTeam = value),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
          decoration: BoxDecoration(
            color: isSelected ? const Color(0xFF06B6D4).withAlpha(30) : Colors.white.withAlpha(5),
            borderRadius: BorderRadius.circular(18),
            border: Border.all(
              color: isSelected ? const Color(0xFF06B6D4).withAlpha(60) : Colors.white.withAlpha(10),
            ),
          ),
          child: Text(label,
              style: TextStyle(
                color: isSelected ? const Color(0xFF06B6D4) : Colors.white.withAlpha(140),
                fontSize: 12,
                fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
              )),
        ),
      ),
    );
  }

  Widget _buildGanttChart() {
    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Vue Gantt',
              style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          ..._tasks.take(10).map((task) {
            final t = task as Map<String, dynamic>;
            final title = t['titre'] ?? t['title'] ?? '';
            final progress = (t['progression'] ?? t['progress'] ?? 0).toDouble().clamp(0, 100);
            final status = t['statut'] ?? t['status'] ?? '';
            final color = status == 'TERMINE'
                ? Colors.green
                : status == 'EN_COURS'
                    ? const Color(0xFF06B6D4)
                    : Colors.amber;

            return Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Expanded(
                        child: Text(title,
                            style: TextStyle(color: Colors.white.withAlpha(180), fontSize: 12),
                            overflow: TextOverflow.ellipsis),
                      ),
                      Text('${progress.toInt()}%',
                          style: TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.bold)),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Stack(
                    children: [
                      Container(
                        height: 6,
                        decoration: BoxDecoration(
                          color: Colors.white.withAlpha(8),
                          borderRadius: BorderRadius.circular(3),
                        ),
                      ),
                      FractionallySizedBox(
                        widthFactor: progress / 100,
                        child: Container(
                          height: 6,
                          decoration: BoxDecoration(
                            gradient: LinearGradient(colors: [color, color.withAlpha(150)]),
                            borderRadius: BorderRadius.circular(3),
                          ),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            );
          }),
        ],
      ),
    );
  }

  Widget _buildTaskList() {
    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Toutes les tâches',
              style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          ..._tasks.map((task) {
            final t = task as Map<String, dynamic>;
            return Padding(
              padding: const EdgeInsets.only(bottom: 6),
              child: Row(
                children: [
                  Icon(
                    t['statut'] == 'TERMINE' ? Icons.check_circle : Icons.radio_button_unchecked,
                    color: t['statut'] == 'TERMINE' ? Colors.green : Colors.white38,
                    size: 18,
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(t['titre'] ?? t['title'] ?? '',
                        style: TextStyle(color: Colors.white.withAlpha(180), fontSize: 13)),
                  ),
                  Text(t['assignedTo'] ?? '',
                      style: TextStyle(color: Colors.white.withAlpha(80), fontSize: 11)),
                ],
              ),
            );
          }),
        ],
      ),
    );
  }
}
