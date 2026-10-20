// Parcours « Archives » mobile (V272) — lecture des instantanés d'événements
// clôturés. Miroir du parcours web (ArchivesPage) : filtres année/mois portés
// par GET /archives/facets, liste bornée via GET /archives/search (résumés,
// jamais le snapshot complet), détail via GET /archives/{id}, navigation
// entre versions d'un même événement via search?eventId=.
//
// Immuable par contrat : aucun bouton d'édition ici — une archive se lit,
// elle ne se modifie pas (la seule porte d'écriture est POST /{id}/archive,
// dans EventDetailScreen).

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import 'package:discipolat_mobile/features/events/models/event_archive_model.dart';
import 'package:discipolat_mobile/features/events/services/events_service.dart';
import 'package:discipolat_mobile/l10n/app_localizations.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

class ArchivesScreen extends ConsumerStatefulWidget {
  const ArchivesScreen({super.key});

  @override
  ConsumerState<ArchivesScreen> createState() => _ArchivesScreenState();
}

class _ArchivesScreenState extends ConsumerState<ArchivesScreen> {
  static const int _pageSize = 12;

  List<EventArchiveFacet> _facets = const [];
  int? _year;
  int? _month;
  int _page = 0;
  late Future<EventArchivePage> _pageFuture;
  Object? _facetsError;

  @override
  void initState() {
    super.initState();
    _pageFuture = ref.read(eventsServiceProvider).getArchives(size: _pageSize);
    _loadFacets();
  }

  Future<void> _loadFacets() async {
    try {
      final facets = await ref.read(eventsServiceProvider).getArchiveFacets();
      if (mounted) setState(() => _facets = facets);
    } catch (e) {
      // Les filtres sont un confort : leur échec ne doit pas vider la liste.
      if (mounted) setState(() => _facetsError = e);
    }
  }

  void _reloadPage() {
    setState(() {
      _pageFuture = ref
          .read(eventsServiceProvider)
          .getArchives(page: _page, size: _pageSize, year: _year, month: _month);
    });
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.black,
        title: Text(l10n.archivesTitle),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded),
          onPressed: () =>
              context.canPop() ? context.pop() : context.go('/events'),
        ),
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 4, 16, 8),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  l10n.archivesSubtitle,
                  style: TextStyle(
                      color: AppColors.surface.withValues(alpha: 0.7),
                      fontSize: 12),
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(child: _buildYearDropdown(l10n)),
                    const SizedBox(width: 8),
                    Expanded(child: _buildMonthDropdown(l10n)),
                  ],
                ),
                if (_facetsError != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 6),
                    child: Text(
                      l10n.archivesLoadError,
                      style: const TextStyle(
                          color: Colors.orangeAccent, fontSize: 11),
                    ),
                  ),
              ],
            ),
          ),
          const Divider(height: 1, color: Colors.white12),
          Expanded(
            child: FutureBuilder<EventArchivePage>(
              future: _pageFuture,
              builder: (context, snap) {
                if (snap.connectionState == ConnectionState.waiting) {
                  return Center(
                    child:
                        CircularProgressIndicator(color: AppColors.primary),
                  );
                }
                if (snap.hasError) {
                  return _ErrorState(
                    message: l10n.archivesLoadError,
                    details: snap.error.toString(),
                    onRetry: _reloadPage,
                  );
                }
                final page = snap.data!;
                if (page.content.isEmpty) {
                  return Center(
                    child: Padding(
                      padding: const EdgeInsets.all(32),
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.archive_outlined,
                              size: 56,
                              color: AppColors.surface.withValues(alpha: 0.3)),
                          const SizedBox(height: 12),
                          Text(
                            l10n.archivesEmpty,
                            textAlign: TextAlign.center,
                            style: TextStyle(
                                color:
                                    AppColors.surface.withValues(alpha: 0.7)),
                          ),
                        ],
                      ),
                    ),
                  );
                }
                return ListView.separated(
                  padding: const EdgeInsets.symmetric(vertical: 8),
                  itemCount: page.content.length,
                  separatorBuilder: (_, __) =>
                      const Divider(height: 1, color: Colors.white10),
                  itemBuilder: (context, i) {
                    final a = page.content[i];
                    return ListTile(
                      leading: CircleAvatar(
                        backgroundColor:
                            AppColors.primary.withValues(alpha: 0.15),
                        child: Icon(Icons.archive_rounded,
                            color: AppColors.primary, size: 20),
                      ),
                      title: Text(
                        a.eventTitle,
                        style: const TextStyle(
                            color: Colors.white, fontWeight: FontWeight.w600),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      subtitle: Text(
                        _subtitleFor(l10n, a),
                        style: TextStyle(
                            color:
                                AppColors.surface.withValues(alpha: 0.7)),
                      ),
                      trailing: const Icon(Icons.chevron_right_rounded,
                          color: Colors.white38),
                      onTap: () {
                        Navigator.of(context).push(MaterialPageRoute(
                          builder: (_) =>
                              ArchiveDetailScreen(archiveId: a.id),
                        ));
                      },
                    );
                  },
                );
              },
            ),
          ),
          _buildFooter(l10n),
        ],
      ),
    );
  }

  String _subtitleFor(AppLocalizations l10n, EventArchiveSummary a) {
    final parts = <String>[];
    if (a.eventYear != null) {
      if (a.eventMonth != null) {
        parts.add('${_monthLabel(l10n, a.eventMonth!)} ${a.eventYear}');
      } else {
        parts.add('${a.eventYear}');
      }
    }
    if (a.version != null) parts.add('v${a.version}');
    if (a.attendanceCount != null) {
      parts.add('${a.attendanceCount} ${l10n.archivesAttendance}');
    }
    if (a.archivedAt != null) {
      parts.add(
          '${l10n.archivesArchivedAt} ${DateFormat('dd/MM/yyyy').format(a.archivedAt!.toLocal())}');
    }
    return parts.join(' · ');
  }

  String _monthLabel(AppLocalizations l10n, int m) {
    // Noms de mois dans la locale active ; si les données intl de la locale
    // ne sont pas initialisées, retombe proprement sur le numéro — jamais un
    // écran cassé pour un libellé.
    try {
      final loc = Localizations.localeOf(context).toString();
      final name = DateFormat('MMMM', loc).format(DateTime(2000, m));
      return name;
    } catch (_) {
      return '$m';
    }
  }

  Widget _buildYearDropdown(AppLocalizations l10n) {
    final years = _facets.map((f) => f.eventYear).toSet().toList()..sort();
    return DropdownButtonFormField<int?>(
      initialValue: _year,
      isExpanded: true,
      dropdownColor: AppColors.cardDark,
      style: const TextStyle(color: Colors.white),
      decoration: InputDecoration(
        labelText: l10n.archivesYearLabel,
        labelStyle: TextStyle(color: AppColors.surface.withValues(alpha: 0.7)),
        border: const OutlineInputBorder(),
        enabledBorder: const OutlineInputBorder(
            borderSide: BorderSide(color: Colors.white24)),
      ),
      items: [
        DropdownMenuItem(
            value: null, child: Text(l10n.archivesYearAll,
                style: const TextStyle(color: Colors.white))),
        for (final y in years)
          DropdownMenuItem(value: y,
              child: Text('$y', style: const TextStyle(color: Colors.white))),
      ],
      onChanged: (v) {
        setState(() {
          _year = v;
          // Le mois n'a de sens que dans l'année choisie : on le réinitialise
          // (les facets le rechargeront) et on revient à la première page.
          _month = null;
          _page = 0;
        });
        _reloadPage();
      },
    );
  }

  Widget _buildMonthDropdown(AppLocalizations l10n) {
    final months = <int>{
      for (final f in _facets)
        if (_year == null || f.eventYear == _year) ...f.months,
    }.toList()
      ..sort();
    return DropdownButtonFormField<int?>(
      initialValue: _month,
      isExpanded: true,
      dropdownColor: AppColors.cardDark,
      style: const TextStyle(color: Colors.white),
      decoration: InputDecoration(
        labelText: l10n.archivesMonthLabel,
        labelStyle: TextStyle(color: AppColors.surface.withValues(alpha: 0.7)),
        border: const OutlineInputBorder(),
        enabledBorder: const OutlineInputBorder(
            borderSide: BorderSide(color: Colors.white24)),
      ),
      items: [
        DropdownMenuItem(
            value: null, child: Text(l10n.archivesMonthAll,
                style: const TextStyle(color: Colors.white))),
        for (final m in months)
          DropdownMenuItem(value: m,
              child: Text(_monthLabel(l10n, m),
                  style: const TextStyle(color: Colors.white))),
      ],
      onChanged: (v) {
        setState(() {
          _month = v;
          _page = 0;
        });
        _reloadPage();
      },
    );
  }

  Widget _buildFooter(AppLocalizations l10n) {
    return FutureBuilder<EventArchivePage>(
      future: _pageFuture,
      builder: (context, snap) {
        final page = snap.data;
        if (page == null) return const SizedBox(height: 56);
        return Container(
          decoration: BoxDecoration(
            color: AppColors.cardDark,
            border: Border(top: BorderSide(color: Colors.white12)),
          ),
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              TextButton.icon(
                onPressed: page.hasPrevious
                    ? () {
                        setState(() => _page--);
                        _reloadPage();
                      }
                    : null,
                icon: const Icon(Icons.chevron_left_rounded),
                label: Text(l10n.archivesPrevious),
              ),
              Text(
                '${page.totalElements} · ${page.page + 1}/${page.totalPages < 1 ? 1 : page.totalPages}',
                style: TextStyle(
                    color: AppColors.surface.withValues(alpha: 0.7),
                    fontSize: 12),
              ),
              TextButton.icon(
                onPressed: page.hasNext
                    ? () {
                        setState(() => _page++);
                        _reloadPage();
                      }
                    : null,
                icon: const Icon(Icons.chevron_right_rounded),
                label: Text(l10n.archivesNext),
              ),
            ],
          ),
        );
      },
    );
  }
}

/// Détail lecture seule d'une archive + navigation entre ses versions.
class ArchiveDetailScreen extends ConsumerStatefulWidget {
  final String archiveId;
  const ArchiveDetailScreen({super.key, required this.archiveId});

  @override
  ConsumerState<ArchiveDetailScreen> createState() =>
      _ArchiveDetailScreenState();
}

class _ArchiveDetailScreenState extends ConsumerState<ArchiveDetailScreen> {
  late Future<EventArchive> _future;
  Future<List<EventArchiveSummary>>? _versionsFuture;

  @override
  void initState() {
    super.initState();
    _load();
  }

  void _load() {
    _future = ref.read(eventsServiceProvider).getArchive(widget.archiveId);
    _versionsFuture = null;
    // Les versions ne sont utiles qu'une fois l'archive connue (eventId) :
    // on les charge après le premier succès (voir _onArchiveLoaded).
  }

  void _onArchiveLoaded(EventArchive a) {
    if (_versionsFuture != null) return;
    final service = ref.read(eventsServiceProvider);
    // size=50 : borne haute du contrat /archives/search.
    _versionsFuture =
        service.getArchives(page: 0, size: 50, eventId: a.eventId).then((p) => p.content);
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.black,
        title: Text(l10n.archivesTitle),
      ),
      body: FutureBuilder<EventArchive>(
        future: _future,
        builder: (context, snap) {
          if (snap.connectionState == ConnectionState.waiting) {
            return Center(
                child: CircularProgressIndicator(color: AppColors.primary));
          }
          if (snap.hasError) {
            return _ErrorState(
              message: l10n.archivesLoadError,
              details: snap.error.toString(),
              onRetry: _load,
            );
          }
          final a = snap.data!;
          WidgetsBinding.instance.addPostFrameCallback((_) {
            if (mounted) _onArchiveLoaded(a);
          });
          final attendance = a.attendanceCount ??
              (a.snapshotJson['attendanceCount'] is num
                  ? (a.snapshotJson['attendanceCount'] as num).toInt()
                  : null);
          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              Text(
                a.eventTitle,
                style: const TextStyle(
                    color: Colors.white,
                    fontSize: 20,
                    fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  if (a.version != null) _chip(l10n.archivesVersion, 'v${a.version}'),
                  if (a.eventYear != null)
                    _chip(
                        '${a.eventYear}${a.eventMonth != null ? '/${a.eventMonth}' : ''}',
                        a.eventStartAt != null
                            ? DateFormat('dd/MM/yyyy')
                                .format(a.eventStartAt!.toLocal())
                            : ''),
                  if (attendance != null)
                    _chip('$attendance', l10n.archivesAttendance),
                  if (a.archivedAt != null)
                    _chip(l10n.archivesArchivedAt,
                        DateFormat('dd/MM/yyyy HH:mm').format(a.archivedAt!.toLocal())),
                ],
              ),
              const SizedBox(height: 20),
              FutureBuilder<List<EventArchiveSummary>>(
                future: _versionsFuture,
                builder: (context, vs) {
                  final versions = vs.data ?? const <EventArchiveSummary>[];
                  if (versions.length < 2) return const SizedBox.shrink();
                  return Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(l10n.archivesVersions,
                          style: const TextStyle(
                              color: Colors.white,
                              fontWeight: FontWeight.w600)),
                      const SizedBox(height: 8),
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: [
                          for (final v in versions)
                            ChoiceChip(
                              label: Text('v${v.version ?? '?'}'),
                              selected: v.id == a.id,
                              selectedColor: AppColors.primary,
                              labelStyle: TextStyle(
                                  color: v.id == a.id
                                      ? Colors.black
                                      : Colors.white),
                              onSelected: (_) {
                                if (v.id == a.id) return;
                                Navigator.of(context).pushReplacement(
                                  MaterialPageRoute(
                                    builder: (_) =>
                                        ArchiveDetailScreen(archiveId: v.id),
                                  ),
                                );
                              },
                            ),
                        ],
                      ),
                      const SizedBox(height: 20),
                    ],
                  );
                },
              ),
              Text(
                '${l10n.archivesSnapshot} · ${a.snapshotJson.length}',
                style: TextStyle(
                    color: AppColors.surface.withValues(alpha: 0.7),
                    fontSize: 12),
              ),
              const SizedBox(height: 8),
              for (final key in a.snapshotJson.keys.toList()..sort())
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 4),
                  child: Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: AppColors.cardDark,
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: Colors.white10),
                    ),
                    child: Row(
                      children: [
                        Icon(Icons.bubble_chart_rounded,
                            size: 16, color: AppColors.primary),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            key,
                            style: const TextStyle(
                                color: Colors.white, fontSize: 13),
                          ),
                        ),
                        Text('${_sizeOf(a.snapshotJson[key])}',
                            style: TextStyle(
                                color:
                                    AppColors.surface.withValues(alpha: 0.6),
                                fontSize: 12)),
                      ],
                    ),
                  ),
                ),
              const SizedBox(height: 16),
              Text(
                l10n.archivesImmutableNote,
                style: TextStyle(
                    color: AppColors.surface.withValues(alpha: 0.5),
                    fontSize: 11,
                    fontStyle: FontStyle.italic),
              ),
            ],
          );
        },
      ),
    );
  }

  int _sizeOf(dynamic v) {
    if (v is List || v is Map) return v.length;
    return 0;
  }

  Widget _chip(String label, String value) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: AppColors.primary.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: AppColors.primary.withValues(alpha: 0.3)),
      ),
      child: Text(
        value.isEmpty ? label : '$label · $value',
        style:
            const TextStyle(fontSize: 12, fontWeight: FontWeight.w500, color: Colors.white),
      ),
    );
  }
}

class _ErrorState extends StatelessWidget {
  final String message;
  final String details;
  final VoidCallback onRetry;
  const _ErrorState(
      {required this.message, required this.details, required this.onRetry});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.cloud_off_rounded,
                size: 56, color: Colors.red.withValues(alpha: 0.7)),
            const SizedBox(height: 12),
            Text(message,
                textAlign: TextAlign.center,
                style: const TextStyle(color: Colors.white)),
            const SizedBox(height: 8),
            Text(details,
                textAlign: TextAlign.center,
                style: TextStyle(
                    color: AppColors.surface.withValues(alpha: 0.6),
                    fontSize: 12)),
            const SizedBox(height: 16),
            OutlinedButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh_rounded),
              label: Text(AppLocalizations.of(context).retry),
            ),
          ],
        ),
      ),
    );
  }
}
