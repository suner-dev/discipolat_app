import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';
import '../../../../l10n/app_localizations.dart';

/// PHASE 3 — Réseau fédéré inter-églises — écran mobile branché sur l'API réelle
/// (`/api/v1/network/**`) : ressources partagées, événements inter-églises et annuaire.
class NetworkScreen extends StatefulWidget {
  const NetworkScreen({super.key, this.apiService});

  final ApiService? apiService;

  @override
  State<NetworkScreen> createState() => _NetworkScreenState();
}

class _NetworkScreenState extends State<NetworkScreen>
    with SingleTickerProviderStateMixin {
  late final ApiService _api = widget.apiService ?? ApiService();
  late final TabController _tabs = TabController(length: 3, vsync: this);

  List<dynamic> _resources = [];
  List<dynamic> _events = [];
  List<dynamic> _directory = [];
  bool _isLoading = true;
  String? _error;
  final TextEditingController _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _tabs.dispose();
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() { _isLoading = true; _error = null; });
    try {
      final results = await Future.wait([
        _api.get('/api/v1/network/resources'),
        _api.get('/api/v1/network/events'),
        _api.get('/api/v1/network/directory'),
      ]);
      if (mounted) {
        setState(() {
          _resources = (results[0].data is List ? results[0].data : <dynamic>[]) as List<dynamic>;
          _events = (results[1].data is List ? results[1].data : <dynamic>[]) as List<dynamic>;
          _directory = (results[2].data is List ? results[2].data : <dynamic>[]) as List<dynamic>;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) setState(() { _error = e.toString(); _isLoading = false; });
    }
  }

  Future<void> _searchResources(String query) async {
    try {
      final res = query.trim().isEmpty
          ? await _api.get('/api/v1/network/resources')
          : await _api.get('/api/v1/network/resources/search', params: {'q': query.trim()});
      if (mounted) {
        setState(() {
          _resources = (res.data is List ? res.data : <dynamic>[]) as List<dynamic>;
        });
      }
    } catch (e) {
      if (mounted) setState(() { _error = e.toString(); });
    }
  }

  Future<void> _toggleJoin(Map<String, dynamic> event) async {
    final id = event['id']?.toString();
    if (id == null || id.isEmpty) return;
    final joinedByMe = event['joinedByMe'] == true;
    final res = await _api.post('/api/v1/network/events/$id/${joinedByMe ? 'leave' : 'join'}');
    if (mounted && res.data is Map<String, dynamic>) {
      final updated = res.data as Map<String, dynamic>;
      setState(() {
        _events = _events
            .map((e) => (e as Map<String, dynamic>)['id'] == updated['id'] ? updated : e)
            .toList();
      });
    }
  }

  Future<void> _registerDownload(Map<String, dynamic> resource) async {
    final id = resource['id']?.toString();
    if (id == null || id.isEmpty) return;
    final res = await _api.post('/api/v1/network/resources/$id/download');
    if (mounted && res.data is Map<String, dynamic>) {
      final updated = res.data as Map<String, dynamic>;
      setState(() {
        _resources = _resources
            .map((r) => (r as Map<String, dynamic>)['id'] == updated['id'] ? updated : r)
            .toList();
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.networkTitle),
        backgroundColor: AppColors.primaryDark,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _load)],
        bottom: TabBar(
          controller: _tabs,
          indicatorColor: Colors.white,
          labelColor: Colors.white,
          unselectedLabelColor: Colors.white60,
          tabs: [
            Tab(text: l10n.networkResources),
            Tab(text: l10n.networkEvents),
            Tab(text: l10n.networkDirectory),
          ],
        ),
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 6)
          : _error != null
              ? Center(child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(Icons.error_outline, color: Colors.white.withValues(alpha: 0.3), size: 48),
                    const SizedBox(height: 12),
                    Text(l10n.networkError, style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                    const SizedBox(height: 12),
                    FilledButton.icon(onPressed: _load, icon: const Icon(Icons.refresh, size: 16), label: Text(l10n.retry)),
                  ],
                ))
              : TabBarView(
                  controller: _tabs,
                  children: [
                    _ResourcesTab(
                      resources: _resources,
                      l10n: l10n,
                      searchController: _searchController,
                      onSearch: _searchResources,
                      onDownload: _registerDownload,
                    ),
                    _EventsTab(events: _events, l10n: l10n, onToggleJoin: _toggleJoin),
                    _DirectoryTab(directory: _directory, l10n: l10n),
                  ],
                ),
    );
  }
}

class _ResourcesTab extends StatelessWidget {
  const _ResourcesTab({
    required this.resources,
    required this.l10n,
    required this.searchController,
    required this.onSearch,
    required this.onDownload,
  });

  final List<dynamic> resources;
  final AppLocalizations l10n;
  final TextEditingController searchController;
  final ValueChanged<String> onSearch;
  final ValueChanged<Map<String, dynamic>> onDownload;

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: () async => onSearch(searchController.text),
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          TextField(
            controller: searchController,
            onSubmitted: onSearch,
            style: const TextStyle(color: Colors.white),
            decoration: InputDecoration(
              hintText: l10n.networkSearchResources,
              hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.4)),
              prefixIcon: const Icon(Icons.search, color: Colors.white54),
              filled: true,
              fillColor: Colors.white.withValues(alpha: 0.06),
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
            ),
          ),
          const SizedBox(height: 16),
          if (resources.isEmpty)
            Padding(
              padding: const EdgeInsets.all(24),
              child: Center(child: Text(l10n.networkEmptyResources, style: TextStyle(color: Colors.white.withValues(alpha: 0.5)))),
            )
          else
            ...resources.map((r) => _resourceCard(r as Map<String, dynamic>)),
        ],
      ),
    );
  }

  Widget _resourceCard(Map<String, dynamic> item) {
    final title = item['title']?.toString() ?? '';
    final category = item['category']?.toString() ?? '';
    final downloads = (item['downloads'] as num?)?.toInt() ?? 0;
    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      child: Row(children: [
        Icon(Icons.folder_shared_rounded, color: AppColors.accent, size: 28),
        const SizedBox(width: 12),
        Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(title, style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.white)),
          Text('$category • $downloads ${l10n.networkDownloads}',
              style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 12)),
        ])),
        IconButton(
          icon: const Icon(Icons.download_rounded, color: Colors.white70),
          tooltip: l10n.networkDownloads,
          onPressed: () => onDownload(item),
        ),
      ]),
    );
  }
}

class _EventsTab extends StatelessWidget {
  const _EventsTab({required this.events, required this.l10n, required this.onToggleJoin});

  final List<dynamic> events;
  final AppLocalizations l10n;
  final ValueChanged<Map<String, dynamic>> onToggleJoin;

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: () async {},
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          if (events.isEmpty)
            Padding(
              padding: const EdgeInsets.all(24),
              child: Center(child: Text(l10n.networkEmptyEvents, style: TextStyle(color: Colors.white.withValues(alpha: 0.5)))),
            )
          else
            ...events.map((e) => _eventCard(e as Map<String, dynamic>)),
        ],
      ),
    );
  }

  Widget _eventCard(Map<String, dynamic> item) {
    final title = item['title']?.toString() ?? '';
    final city = item['city']?.toString() ?? '';
    final country = item['country']?.toString() ?? '';
    final isVirtual = item['isVirtual'] == true;
    final participants = (item['currentParticipants'] as num?)?.toInt() ?? 0;
    final maxParticipants = (item['maxParticipants'] as num?)?.toInt();
    final joinedByMe = item['joinedByMe'] == true;
    final isFull = maxParticipants != null && participants >= maxParticipants;
    final date = item['startsAt']?.toString().split('T').first ?? '';
    final place = isVirtual ? l10n.networkVirtual : [city, country].where((s) => s.isNotEmpty).join(', ');
    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          Icon(Icons.event_rounded, color: AppColors.accent, size: 24),
          const SizedBox(width: 12),
          Expanded(child: Text(title, style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.white))),
        ]),
        const SizedBox(height: 6),
        Text(
          '$place • $date • $participants${maxParticipants != null ? '/$maxParticipants' : ''} ${l10n.networkParticipants}',
          style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 12),
        ),
        const SizedBox(height: 8),
        FilledButton.icon(
          onPressed: isFull && !joinedByMe ? null : () => onToggleJoin(item),
          icon: Icon(joinedByMe ? Icons.check_circle : Icons.group_add, size: 16),
          label: Text(joinedByMe ? l10n.networkLeave : l10n.networkJoin),
        ),
      ]),
    );
  }
}

class _DirectoryTab extends StatelessWidget {
  const _DirectoryTab({required this.directory, required this.l10n});

  final List<dynamic> directory;
  final AppLocalizations l10n;

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: () async {},
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          if (directory.isEmpty)
            Padding(
              padding: const EdgeInsets.all(24),
              child: Center(child: Text(l10n.networkEmptyDirectory, style: TextStyle(color: Colors.white.withValues(alpha: 0.5)))),
            )
          else
            ...directory.map((c) => _churchCard(c as Map<String, dynamic>)),
        ],
      ),
    );
  }

  Widget _churchCard(Map<String, dynamic> item) {
    final churchName = item['churchName']?.toString() ?? '';
    final city = item['city']?.toString() ?? '';
    final country = item['country']?.toString() ?? '';
    final denomination = item['denomination']?.toString() ?? '';
    final memberCount = item['memberCount'] as num?;
    final subtitle = [
      [city, country].where((s) => s.isNotEmpty).join(', '),
      if (denomination.isNotEmpty) denomination,
      if (memberCount != null) '$memberCount ${l10n.networkMembers}',
    ].where((s) => s.isNotEmpty).join(' • ');
    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      child: Row(children: [
        Icon(Icons.church_rounded, color: AppColors.primary, size: 28),
        const SizedBox(width: 12),
        Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(churchName, style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.white)),
          if (subtitle.isNotEmpty)
            Text(subtitle, style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 12)),
        ])),
      ]),
    );
  }
}
