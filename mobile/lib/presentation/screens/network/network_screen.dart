import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';
import '../../../data/local/database.dart';
import '../../../../l10n/app_localizations.dart';

/// PHASE 3 — Réseau fédéré inter-églises — écran mobile offline-first
/// Charge depuis le cache Drift, sync depuis l'API en ligne.
class NetworkScreen extends ConsumerStatefulWidget {
  const NetworkScreen({super.key, this.apiService});

  final ApiService? apiService;

  @override
  ConsumerState<NetworkScreen> createState() => _NetworkScreenState();
}

class _NetworkScreenState extends ConsumerState<NetworkScreen>
    with SingleTickerProviderStateMixin {
  late final ApiService _api;
  late final AppDatabase _db;
  late final TabController _tabs;

  List<dynamic> _resources = [];
  List<dynamic> _events = [];
  List<dynamic> _directory = [];
  bool _isLoading = true;
  bool _isOnline = true;
  String? _error;
  final TextEditingController _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _api = widget.apiService ?? ApiService();
    _db = AppDatabase();
    _tabs = TabController(length: 3, vsync: this);
    _load();
  }

  @override
  void dispose() {
    _tabs.dispose();
    _searchController.dispose();
    _db.close();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    // 1. Load from local cache first (offline-first)
    try {
      final cachedResources = await _db.getLocalNetworkResources();
      final cachedEvents = await _db.getLocalNetworkEvents();
      final cachedDirectory = await _db.getLocalNetworkDirectory();

      if (cachedResources.isNotEmpty || cachedEvents.isNotEmpty) {
        setState(() {
          _resources = cachedResources.map(_resourceToMap).toList();
          _events = cachedEvents.map(_eventToMap).toList();
          _directory = cachedDirectory.map(_directoryToMap).toList();
          _isLoading = false;
        });
      }
    } catch (e) {
      // Cache read failed, continue to network
    }

    // 2. Try to sync from network
    try {
      final results = await Future.wait([
        _api.get('/network/resources'),
        _api.get('/network/events'),
        _api.get('/network/directory'),
      ]);

      final resourceList = (results[0].data is List ? results[0].data as List : <dynamic>[]);
      final eventList = (results[1].data is List ? results[1].data as List : <dynamic>[]);
      final directoryList = (results[2].data is List ? results[2].data as List : <dynamic>[]);

      // Update cache
      final now = DateTime.now().toIso8601String();
      await _db.saveNetworkResources(resourceList.map((r) => _mapToResourceLocal(r, now)).toList());
      await _db.saveNetworkEvents(eventList.map((e) => _mapToEventLocal(e, now)).toList());
      await _db.saveNetworkDirectory(directoryList.map((d) => _mapToDirectoryLocal(d, now)).toList());

      if (mounted) {
        setState(() {
          _resources = resourceList;
          _events = eventList;
          _directory = directoryList;
          _isOnline = true;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isOnline = false;
          _isLoading = false;
          if (_resources.isEmpty) _error = e.toString();
        });
      }
    }
  }

  Future<void> _searchResources(String query) async {
    try {
      final res = query.trim().isEmpty
          ? await _api.get('/network/resources')
          : await _api.get('/network/resources/search', params: {'q': query.trim()});
      if (mounted) {
        setState(() {
          _resources = (res.data is List ? res.data : <dynamic>[]) as List<dynamic>;
        });
      }
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    }
  }

  Future<void> _toggleJoin(Map<String, dynamic> event) async {
    final id = event['id']?.toString();
    if (id == null || id.isEmpty) return;
    final joinedByMe = event['joinedByMe'] == true;
    try {
      final res = await _api.post('/network/events/$id/${joinedByMe ? 'leave' : 'join'}');
      if (mounted && res.data is Map<String, dynamic>) {
        final updated = res.data as Map<String, dynamic>;
        setState(() {
          _events = _events
              .map((e) => (e as Map<String, dynamic>)['id'] == updated['id'] ? updated : e)
              .toList();
        });
        // Update cache
        await _db.saveNetworkEvents(_events.map((e) => _mapToEventLocal(e, DateTime.now().toIso8601String())).toList());
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(_isOnline ? 'Erreur: $e' : 'Mode hors-ligne — synchronisé à la reconnexion')),
        );
      }
    }
  }

  Future<void> _registerDownload(Map<String, dynamic> resource) async {
    final id = resource['id']?.toString();
    if (id == null || id.isEmpty) return;
    try {
      final res = await _api.post('/network/resources/$id/download');
      if (mounted && res.data is Map<String, dynamic>) {
        final updated = res.data as Map<String, dynamic>;
        setState(() {
          _resources = _resources
              .map((r) => (r as Map<String, dynamic>)['id'] == updated['id'] ? updated : r)
              .toList();
        });
      }
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.networkTitle),
        backgroundColor: AppColors.primaryDark,
        foregroundColor: Colors.white,
        actions: [
          if (!_isOnline)
            const Padding(
              padding: EdgeInsets.only(right: 8),
              child: Icon(Icons.cloud_off, color: Colors.amber, size: 18),
            ),
          IconButton(icon: const Icon(Icons.refresh), onPressed: _load),
        ],
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
          ? const Center(child: CircularProgressIndicator())
          : _error != null && _resources.isEmpty
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.error_outline,
                          color: Colors.white.withValues(alpha: 0.3), size: 48),
                      const SizedBox(height: 12),
                      Text(l10n.networkError,
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                      const SizedBox(height: 12),
                      FilledButton.icon(
                        onPressed: _load,
                        icon: const Icon(Icons.refresh, size: 16),
                        label: Text(l10n.retry),
                      ),
                    ],
                  ),
                )
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

  // ==================== MAPPING HELPERS ====================

  Map<String, dynamic> _resourceToMap(NetworkResourceLocal r) => {
        'id': r.id,
        'title': r.title,
        'description': r.description,
        'category': r.category,
        'resourceType': r.resourceType,
        'fileUrl': r.fileUrl,
        'content': r.content,
        'sharedWithPublic': r.sharedWithPublic,
        'downloads': r.downloads,
        'isActive': r.isActive,
      };

  Map<String, dynamic> _eventToMap(NetworkEventLocal e) => {
        'id': e.id,
        'title': e.title,
        'description': e.description,
        'eventType': e.eventType,
        'location': e.location,
        'city': e.city,
        'country': e.country,
        'startsAt': e.startsAt,
        'endsAt': e.endsAt,
        'maxParticipants': e.maxParticipants,
        'currentParticipants': e.currentParticipants,
        'isVirtual': e.isVirtual,
        'sharedWithPublic': e.sharedWithPublic,
        'isActive': e.isActive,
        'joinedByMe': e.joinedByMe,
      };

  Map<String, dynamic> _directoryToMap(NetworkDirectoryLocal d) => {
        'id': d.id,
        'churchName': d.churchName,
        'city': d.city,
        'country': d.country,
        'denomination': d.denomination,
        'pastorName': d.pastorName,
        'contactEmail': d.contactEmail,
        'contactPhone': d.contactPhone,
        'memberCount': d.memberCount,
        'isListed': d.isListed,
      };

  NetworkResourceLocal _mapToResourceLocal(dynamic r, String now) => NetworkResourceLocal(
        id: r['id']?.toString() ?? '',
        tenantId: '',
        title: r['title']?.toString() ?? '',
        description: r['description']?.toString(),
        category: r['category']?.toString() ?? 'BEST_PRACTICE',
        resourceType: r['resourceType']?.toString() ?? 'GUIDE',
        fileUrl: r['fileUrl']?.toString(),
        content: r['content']?.toString(),
        sharedWithPublic: r['sharedWithPublic'] == true,
        downloads: r['downloads'] as int? ?? 0,
        isActive: r['isActive'] != false,
        lastSyncAt: now,
      );

  NetworkEventLocal _mapToEventLocal(dynamic e, String now) => NetworkEventLocal(
        id: e['id']?.toString() ?? '',
        tenantId: '',
        title: e['title']?.toString() ?? '',
        description: e['description']?.toString(),
        eventType: e['eventType']?.toString() ?? 'CONFERENCE',
        location: e['location']?.toString(),
        city: e['city']?.toString(),
        country: e['country']?.toString(),
        startsAt: e['startsAt']?.toString() ?? '',
        endsAt: e['endsAt']?.toString(),
        maxParticipants: e['maxParticipants'] as int?,
        currentParticipants: e['currentParticipants'] as int? ?? 0,
        isVirtual: e['isVirtual'] == true,
        sharedWithPublic: e['sharedWithPublic'] == true,
        isActive: e['isActive'] != false,
        joinedByMe: e['joinedByMe'] == true,
        lastSyncAt: now,
      );

  NetworkDirectoryLocal _mapToDirectoryLocal(dynamic d, String now) => NetworkDirectoryLocal(
        id: d['id']?.toString() ?? '',
        tenantId: '',
        churchName: d['churchName']?.toString(),
        city: d['city']?.toString(),
        country: d['country']?.toString(),
        denomination: d['denomination']?.toString(),
        pastorName: d['pastorName']?.toString(),
        contactEmail: d['contactEmail']?.toString(),
        contactPhone: d['contactPhone']?.toString(),
        memberCount: d['memberCount'] as int?,
        isListed: d['isListed'] == true,
        lastSyncAt: now,
      );
}

// ==================== WIDGETS ====================

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
              border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
            ),
          ),
          const SizedBox(height: 16),
          if (resources.isEmpty)
            Padding(
              padding: const EdgeInsets.all(24),
              child: Center(
                child: Text(l10n.networkEmptyResources,
                    style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
              ),
            )
          else
            ...resources.map((item) {
              final map = item is Map<String, dynamic> ? item : <String, dynamic>{};
              final title = map['title']?.toString() ?? '';
              final category = map['category']?.toString() ?? '';
              final downloads = (map['downloads'] as num?)?.toInt() ?? 0;
              return GlassCard(
                margin: EdgeInsets.only(bottom: 8),
                padding: EdgeInsets.all(12),
                child: Row(children: [
                  Icon(Icons.folder_shared_rounded, color: AppColors.accent, size: 28),
                  SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(title,
                            style: const TextStyle(
                                fontWeight: FontWeight.bold, color: Colors.white)),
                        Text('$category • $downloads ${l10n.networkDownloads}',
                            style: TextStyle(
                                color: Colors.white.withValues(alpha: 0.4), fontSize: 12)),
                      ],
                    ),
                  ),
                  IconButton(
                    icon: const Icon(Icons.download_rounded, color: Colors.white70),
                    tooltip: l10n.networkDownloads,
                    onPressed: () => onDownload(map),
                  ),
                ]),
              );
            }),
        ],
      ),
    );
  }
}

class _EventsTab extends StatelessWidget {
  const _EventsTab(
      {required this.events, required this.l10n, required this.onToggleJoin});

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
              child: Center(
                child: Text(l10n.networkEmptyEvents,
                    style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
              ),
            )
          else
            ...events.map((item) {
              final map = item is Map<String, dynamic> ? item : <String, dynamic>{};
              final title = map['title']?.toString() ?? '';
              final city = map['city']?.toString() ?? '';
              final country = map['country']?.toString() ?? '';
              final isVirtual = map['isVirtual'] == true;
              final participants = (map['currentParticipants'] as num?)?.toInt() ?? 0;
              final maxParticipants = map['maxParticipants'] as int?;
              final joinedByMe = map['joinedByMe'] == true;
              final isFull = maxParticipants != null && participants >= maxParticipants;
              final date = map['startsAt']?.toString().split('T').first ?? '';
              final place = isVirtual
                  ? l10n.networkVirtual
                  : [city, country].where((s) => s.isNotEmpty).join(', ');
              return GlassCard(
                margin: EdgeInsets.only(bottom: 8),
                padding: EdgeInsets.all(12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(children: [
                      Icon(Icons.event_rounded, color: AppColors.accent, size: 24),
                      SizedBox(width: 12),
                      Expanded(
                        child: Text(title,
                            style: const TextStyle(
                                fontWeight: FontWeight.bold, color: Colors.white)),
                      ),
                    ]),
                    const SizedBox(height: 6),
                    Text(
                      '$place • $date • $participants${maxParticipants != null ? '/$maxParticipants' : ''} ${l10n.networkParticipants}',
                      style: TextStyle(
                          color: Colors.white.withValues(alpha: 0.4), fontSize: 12),
                    ),
                    const SizedBox(height: 8),
                    FilledButton.icon(
                      onPressed: isFull && !joinedByMe
                          ? null
                          : () => onToggleJoin(map),
                      icon: Icon(joinedByMe ? Icons.check_circle : Icons.group_add,
                          size: 16),
                      label: Text(joinedByMe ? l10n.networkLeave : l10n.networkJoin),
                    ),
                  ],
                ),
              );
            }),
        ],
      ),
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
              child: Center(
                child: Text(l10n.networkEmptyDirectory,
                    style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
              ),
            )
          else
            ...directory.map((item) {
              final map = item is Map<String, dynamic> ? item : <String, dynamic>{};
              final churchName = map['churchName']?.toString() ?? '';
              final city = map['city']?.toString() ?? '';
              final country = map['country']?.toString() ?? '';
              final denomination = map['denomination']?.toString() ?? '';
              final memberCount = map['memberCount'] as num?;
              final subtitle = [
                [city, country].where((s) => s.isNotEmpty).join(', '),
                if (denomination.isNotEmpty) denomination,
                if (memberCount != null) '$memberCount ${l10n.networkMembers}',
              ].where((s) => s.isNotEmpty).join(' • ');
              return GlassCard(
                margin: EdgeInsets.only(bottom: 8),
                padding: EdgeInsets.all(12),
                child: Row(children: [
                  Icon(Icons.church_rounded, color: AppColors.primary, size: 28),
                  SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(churchName,
                            style: const TextStyle(
                                fontWeight: FontWeight.bold, color: Colors.white)),
                        if (subtitle.isNotEmpty)
                          Text(subtitle,
                              style: TextStyle(
                                  color: Colors.white.withValues(alpha: 0.4),
                                  fontSize: 12)),
                      ],
                    ),
                  ),
                ]),
              );
            }),
        ],
      ),
    );
  }
}
