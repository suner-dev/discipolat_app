import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';
import '../../../l10n/app_localizations.dart';

/// Timeline des activités du membre : présences, événements, prières, formations, notes.
class MemberActivitiesScreen extends StatefulWidget {
  const MemberActivitiesScreen({super.key});

  @override
  State<MemberActivitiesScreen> createState() => _MemberActivitiesScreenState();
}

class _MemberActivitiesScreenState extends State<MemberActivitiesScreen> {
  final _apiService = ApiService();
  List<_ActivityItem> _activities = [];
  bool _isLoading = true;
  String _filter = 'all';

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    final List<_ActivityItem> items = [];

    try {
      // Présences
      try {
        final presRes = await _apiService.get('/members/me/presences');
        final presences = (presRes.data as List?) ?? [];
        for (final p in presences) {
          final pres = p as Map<String, dynamic>;
          final date = pres['semaine']?.toString() ?? '';
          items.add(_ActivityItem(
            type: 'presence',
            title: pres['present'] == true ? AppLocalizations.of(context).presenceConfirmed : AppLocalizations.of(context).absenceRecorded,
            subtitle: '${AppLocalizations.of(context).weekOf} $date${pres['notes'] != null ? ' · ${pres['notes']}' : ''}',
            date: date,
            icon: pres['present'] == true ? Icons.check_circle : Icons.cancel,
            color: pres['present'] == true ? Colors.green : Colors.red,
          ));
        }
      } catch (_) {}

      // Événements
      try {
        final evRes = await _apiService.get('/members/me/events');
        final events = (evRes.data as List?) ?? [];
        for (final e in events) {
          final ev = e as Map<String, dynamic>;
          items.add(_ActivityItem(
            type: 'event',
            title: ev['titre']?.toString() ?? '',
            subtitle: '${ev['dateDebut'] ?? ''}${ev['lieu'] != null ? ' · ${ev['lieu']}' : ''}',
            date: ev['dateDebut']?.toString() ?? '',
            icon: Icons.event,
            color: Colors.blue,
          ));
        }
      } catch (_) {}

      // Notes du faiseur
      try {
        final noteRes = await _apiService.get('/members/me/notes');
        final notes = (noteRes.data as List?) ?? [];
        for (final n in notes) {
          final note = n as Map<String, dynamic>;
          items.add(_ActivityItem(
            type: 'note',
            title: AppLocalizations.of(context).makerNote,
            subtitle: note['contenu']?.toString() ?? '',
            date: note['createdAt']?.toString() ?? '',
            icon: Icons.sticky_note_2,
            color: Colors.amber,
          ));
        }
      } catch (_) {}

      // Progression
      try {
        final progRes = await _apiService.get('/members/me/progression');
        final prog = progRes.data as Map<String, dynamic>?;
        if (prog != null) {
          items.add(_ActivityItem(
            type: 'progression',
            title: AppLocalizations.of(context).spiritualLevel(prog['niveauActuel']?.toString() ?? '—'),
            subtitle: AppLocalizations.of(context).progressOverview,
            date: '',
            icon: Icons.trending_up,
            color: Colors.purple,
          ));
        }
      } catch (_) {}

      // Trier par date décroissante
      items.sort((a, b) => b.date.compareTo(a.date));
    } catch (_) {}

    if (mounted) {
      setState(() {
        _activities = items;
        _isLoading = false;
      });
    }
  }

  List<_ActivityItem> get _filteredActivities {
    if (_filter == 'all') return _activities;
    return _activities.where((a) => a.type == _filter).toList();
  }

  @override
  Widget build(BuildContext context) {
    final filtered = _filteredActivities;

    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context).myActivities),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 8)
          : RefreshIndicator(
              onRefresh: _loadData,
              child: Column(
                children: [
                  // Filter chips
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                    child: SingleChildScrollView(
                      scrollDirection: Axis.horizontal,
                      child: Row(
                        children: [
                          _chip(AppLocalizations.of(context).filterAll, 'all', Icons.list, Colors.white),
                          const SizedBox(width: 6),
                          _chip(AppLocalizations.of(context).filterPresences, 'presence', Icons.check_circle, Colors.green),
                          const SizedBox(width: 6),
                          _chip(AppLocalizations.of(context).filterEvents, 'event', Icons.event, Colors.blue),
                          const SizedBox(width: 6),
                          _chip(AppLocalizations.of(context).filterNotes, 'note', Icons.sticky_note_2, Colors.amber),
                          const SizedBox(width: 6),
                          _chip(AppLocalizations.of(context).filterProgression, 'progression', Icons.trending_up, Colors.purple),
                        ],
                      ),
                    ),
                  ),

                  // Timeline
                  Expanded(
                    child: filtered.isEmpty
                        ? Center(
                            child: Column(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Icon(Icons.timeline, color: Colors.white.withValues(alpha: 0.2), size: 48),
                                const SizedBox(height: 12),
                                Text(AppLocalizations.of(context).noActivities,
                                    style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                              ],
                            ),
                          )
                        : ListView.builder(
                            padding: const EdgeInsets.symmetric(horizontal: 12),
                            itemCount: filtered.length,
                            itemBuilder: (context, index) {
                              final item = filtered[index];
                              final isLast = index == filtered.length - 1;
                              return _timelineTile(item, isLast);
                            },
                          ),
                  ),
                ],
              ),
            ),
    );
  }

  Widget _timelineTile(_ActivityItem item, bool isLast) {
    return IntrinsicHeight(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Timeline line + dot
          SizedBox(
            width: 32,
            child: Column(
              children: [
                Container(
                  width: 24,
                  height: 24,
                  decoration: BoxDecoration(
                    color: item.color.withValues(alpha: 0.2),
                    shape: BoxShape.circle,
                    border: Border.all(color: item.color.withValues(alpha: 0.4)),
                  ),
                  child: Icon(item.icon, color: item.color, size: 12),
                ),
                if (!isLast)
                  Expanded(
                    child: Container(
                      width: 2,
                      color: Colors.white.withValues(alpha: 0.08),
                    ),
                  ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          // Content
          Expanded(
            child: GlassCard(
              margin: EdgeInsets.only(bottom: isLast ? 0 : 8, top: 2),
              padding: const EdgeInsets.all(12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(item.title,
                            style: const TextStyle(
                                color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13)),
                      ),
                      if (item.date.isNotEmpty)
                        Text(
                          item.date.length > 10 ? item.date.substring(0, 10) : item.date,
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 10),
                        ),
                    ],
                  ),
                  if (item.subtitle.isNotEmpty) ...[
                    const SizedBox(height: 4),
                    Text(item.subtitle,
                        style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11),
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis),
                  ],
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _chip(String label, String value, IconData icon, Color color) {
    final isActive = _filter == value;
    return GestureDetector(
      onTap: () => setState(() => _filter = value),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
        decoration: BoxDecoration(
          color: isActive ? color.withValues(alpha: 0.15) : Colors.white.withValues(alpha: 0.05),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: isActive ? color.withValues(alpha: 0.4) : Colors.white.withValues(alpha: 0.08)),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 12, color: isActive ? color : Colors.white.withValues(alpha: 0.4)),
            const SizedBox(width: 4),
            Text(label,
                style: TextStyle(
                  color: isActive ? color : Colors.white.withValues(alpha: 0.5),
                  fontSize: 11,
                  fontWeight: isActive ? FontWeight.w600 : FontWeight.normal,
                )),
          ],
        ),
      ),
    );
  }
}

class _ActivityItem {
  final String type;
  final String title;
  final String subtitle;
  final String date;
  final IconData icon;
  final Color color;

  const _ActivityItem({
    required this.type,
    required this.title,
    required this.subtitle,
    required this.date,
    required this.icon,
    required this.color,
  });
}
