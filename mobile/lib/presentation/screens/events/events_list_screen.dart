import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../widgets/attachment_picker_field.dart';
import '../../widgets/attachment_chips.dart';
import '../../../data/services/api_service.dart';
import '../../../l10n/app_localizations.dart';

class EventsListScreen extends StatefulWidget {
  const EventsListScreen({super.key});

  @override
  State<EventsListScreen> createState() => _EventsListScreenState();
}

class _EventsListScreenState extends State<EventsListScreen> {
  final _apiService = ApiService();
  List<dynamic> _events = [];
  bool _isLoading = true;
  String _filter = 'TOUS';

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final res = await _apiService.get('/events', params: {'size': '50'});
      if (mounted) {
        setState(() {
          _events = (res.data is Map ? res.data['content'] : res.data) as List<dynamic>? ?? [];
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _showCreateSheet() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => _CreateEventSheet(
        apiService: _apiService,
        onDone: () {
          Navigator.pop(ctx);
          _loadData();
        },
      ),
    );
  }

  Color _typeColor(String? type) {
    switch (type) {
      case 'CULTE': return Colors.purple;
      case 'REUNION': return Colors.blue;
      case 'SEMINAIRE': return Colors.teal;
      case 'VISITE': return Colors.green;
      case 'EVANGELISATION': return Colors.orange;
      case 'FORMATION': return Colors.indigo;
      case 'ANNIVERSAIRE': return Colors.pink;
      case 'CELEBRATION': return Colors.amber;
      default: return Colors.grey;
    }
  }

  IconData _typeIcon(String? type) {
    switch (type) {
      case 'CULTE': return Icons.church;
      case 'REUNION': return Icons.groups;
      case 'SEMINAIRE': return Icons.school;
      case 'VISITE': return Icons.map;
      case 'EVANGELISATION': return Icons.share;
      case 'FORMATION': return Icons.menu_book;
      case 'ANNIVERSAIRE': return Icons.cake;
      case 'CELEBRATION': return Icons.celebration;
      default: return Icons.event;
    }
  }

  @override
  Widget build(BuildContext context) {
    final upcoming = _events.where((e) => (e as Map)['statut'] == 'PLANIFIE').length;
    final termine = _events.where((e) => (e as Map)['statut'] == 'TERMINE').length;

    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context).evtTitle),
        actions: [
          IconButton(icon: const Icon(Icons.add), onPressed: _showCreateSheet),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 4)
          : RefreshIndicator(
              onRefresh: _loadData,
              child: ListView(
                padding: const EdgeInsets.all(12),
                children: [
                  // Stats
                  Row(
                    children: [
                      _statMini(AppLocalizations.of(context).evtStatTotal, '${_events.length}', Colors.blue),
                      const SizedBox(width: 8),
                      _statMini(AppLocalizations.of(context).evtStatUpcoming, '$upcoming', Colors.green),
                      const SizedBox(width: 8),
                      _statMini(AppLocalizations.of(context).evtStatDone, '$termine', Colors.grey),
                    ],
                  ),
                  const SizedBox(height: 12),
                  // Filter chips
                  SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: ['TOUS', 'PLANIFIE', 'EN_COURS', 'TERMINE'].map((f) {
                        final isActive = _filter == f;
                        final label = f == 'TOUS' ? AppLocalizations.of(context).evtFilterAll : f == 'PLANIFIE' ? AppLocalizations.of(context).evtFilterUpcoming : f == 'EN_COURS' ? AppLocalizations.of(context).evtFilterOngoing : AppLocalizations.of(context).evtStatDone;
                        return Padding(
                          padding: const EdgeInsets.only(right: 6),
                          child: ChoiceChip(
                            label: Text(label, style: TextStyle(color: isActive ? Colors.white : Colors.white.withValues(alpha: 0.6), fontSize: 12)),
                            selected: isActive,
                            onSelected: (_) => setState(() => _filter = f),
                            selectedColor: Colors.blue,
                            backgroundColor: Colors.white.withValues(alpha: 0.06),
                            side: BorderSide.none,
                          ),
                        );
                      }).toList(),
                    ),
                  ),
                  const SizedBox(height: 12),
                  // Events list
                  if (_filtered.isEmpty)
                    GlassCard(
                      padding: const EdgeInsets.all(32),
                      child: Column(
                        children: [
                          Icon(Icons.event_outlined, size: 48, color: Colors.white.withValues(alpha: 0.3)),
                          const SizedBox(height: 12),
                          Text(AppLocalizations.of(context).evtEmpty, style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                        ],
                      ),
                    )
                  else
                    ..._filtered.map((e) {
                      final event = e as Map;
                      final titre = event['titre'] ?? 'Événement';
                      final type = event['typeEvenement'] ?? 'AUTRE';
                      final lieu = event['lieu'] ?? '';
                      final statut = event['statut'] ?? 'PLANIFIE';
                      final dateStr = event['dateDebut']?.toString().substring(0, 16).replaceAll('T', ' ') ?? '';
                      final nbInscrits = event['nbInscrits'] ?? 0;
                      final limitePlaces = event['limitePlaces'];
                      final desc = event['description'] ?? '';

                      return GlassCard(
                        margin: const EdgeInsets.only(bottom: 8),
                        padding: const EdgeInsets.all(14),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Container(
                                  padding: const EdgeInsets.all(8),
                                  decoration: BoxDecoration(
                                    color: _typeColor(type).withValues(alpha: 0.15),
                                    borderRadius: BorderRadius.circular(10),
                                  ),
                                  child: Icon(_typeIcon(type), color: _typeColor(type), size: 20),
                                ),
                                const SizedBox(width: 10),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(titre, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                                      Text(type, style: TextStyle(color: _typeColor(type), fontSize: 11)),
                                    ],
                                  ),
                                ),
                                StatusBadge(
                                  label: statut,
                                  color: statut == 'TERMINE' ? Colors.green : statut == 'EN_COURS' ? Colors.amber : Colors.blue,
                                ),
                              ],
                            ),
                            if (desc.toString().isNotEmpty) ...[
                              const SizedBox(height: 8),
                              Text(desc, style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 12), maxLines: 2, overflow: TextOverflow.ellipsis),
                            ],
                            const SizedBox(height: 8),
                            Row(
                              children: [
                                Icon(Icons.access_time, size: 12, color: Colors.white.withValues(alpha: 0.4)),
                                const SizedBox(width: 3),
                                Text(dateStr, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
                              ],
                            ),
                            if (lieu.toString().isNotEmpty) ...[
                              const SizedBox(height: 4),
                              Row(
                                children: [
                                  Icon(Icons.location_on, size: 12, color: Colors.white.withValues(alpha: 0.4)),
                                  const SizedBox(width: 3),
                                  Text(lieu, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
                                ],
                              ),
                            ],
                            if (limitePlaces != null) ...[
                              const SizedBox(height: 4),
                              Row(
                                children: [
                                  Icon(Icons.people, size: 12, color: Colors.white.withValues(alpha: 0.4)),
                                  const SizedBox(width: 3),
                                  Text(AppLocalizations.of(context).evtInscrits(nbInscrits, limitePlaces), style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
                                ],
                              ),
                            ],
                            if (event['piecesJointes'] is List && (event['piecesJointes'] as List).isNotEmpty) ...[
                              const SizedBox(height: 6),
                              AttachmentChips(pieces: event['piecesJointes'] as List),
                            ],
                          ],
                        ),
                      );
                    }),
                ],
              ),
            ),
    );
  }

  List<dynamic> get _filtered {
    if (_filter == 'TOUS') return _events;
    return _events.where((e) => (e as Map)['statut'] == _filter).toList();
  }

  Widget _statMini(String label, String value, Color color) {
    return Expanded(
      child: GlassCard(
        padding: const EdgeInsets.all(10),
        child: Column(
          children: [
            Text(value, style: TextStyle(color: color, fontSize: 18, fontWeight: FontWeight.bold)),
            Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 10)),
          ],
        ),
      ),
    );
  }
}

class _CreateEventSheet extends StatefulWidget {
  const _CreateEventSheet({required this.apiService, required this.onDone});

  final ApiService apiService;
  final VoidCallback onDone;

  @override
  State<_CreateEventSheet> createState() => _CreateEventSheetState();
}

class _CreateEventSheetState extends State<_CreateEventSheet> {
  final _titreCtrl = TextEditingController();
  final _descCtrl = TextEditingController();
  final _lieuCtrl = TextEditingController();
  String _type = 'CULTE';
  DateTime _dateDebut = DateTime.now().add(const Duration(days: 1));
  bool _isProcessing = false;
  final Set<String> _fichierIds = {};

  static const _types = ['CULTE', 'REUNION', 'SEMINAIRE', 'VISITE', 'EVANGELISATION', 'FORMATION', 'ANNIVERSAIRE', 'CELEBRATION'];

  @override
  void dispose() {
    _titreCtrl.dispose();
    _descCtrl.dispose();
    _lieuCtrl.dispose();
    super.dispose();
  }

  Future<void> _pickDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _dateDebut,
      firstDate: DateTime.now().subtract(const Duration(days: 30)),
      lastDate: DateTime.now().add(const Duration(days: 365)),
      builder: (context, child) => Theme(
        data: ThemeData.dark(),
        child: child!,
      ),
    );
    if (picked != null) {
      setState(() {
        _dateDebut = DateTime(picked.year, picked.month, picked.day, _dateDebut.hour, _dateDebut.minute);
      });
    }
  }

  Future<void> _create() async {
    if (_titreCtrl.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(AppLocalizations.of(context).evtTitleRequired)),
      );
      return;
    }
    setState(() => _isProcessing = true);
    try {
      await widget.apiService.post('/events', data: {
        'typeEvenement': _type,
        'titre': _titreCtrl.text.trim(),
        'description': _descCtrl.text.trim().isEmpty ? null : _descCtrl.text.trim(),
        'lieu': _lieuCtrl.text.trim().isEmpty ? null : _lieuCtrl.text.trim(),
        'dateDebut': _dateDebut.toIso8601String(),
        if (_fichierIds.isNotEmpty) 'fichierIds': _fichierIds.toList(),
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(AppLocalizations.of(context).evtCreated)),
        );
        widget.onDone();
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(AppLocalizations.of(context).evtCreateError)),
        );
      }
    } finally {
      if (mounted) setState(() => _isProcessing = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFF16213A),
        borderRadius: const BorderRadius.vertical(top: Radius.circular(24)),
        border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
      ),
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      child: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Center(
                child: Container(
                  width: 32, height: 4,
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.2),
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              const SizedBox(height: 16),
              Text(
                AppLocalizations.of(context).evtNew,
                style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 16),
              TextField(
                controller: _titreCtrl,
                style: const TextStyle(color: Colors.white),
                decoration: InputDecoration(
                  labelText: 'Titre *',
                  labelStyle: TextStyle(color: Colors.white.withValues(alpha: 0.5)),
                  filled: true,
                  fillColor: Colors.white.withValues(alpha: 0.06),
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _descCtrl,
                style: const TextStyle(color: Colors.white),
                maxLines: 3,
                decoration: InputDecoration(
                  labelText: 'Description',
                  labelStyle: TextStyle(color: Colors.white.withValues(alpha: 0.5)),
                  filled: true,
                  fillColor: Colors.white.withValues(alpha: 0.06),
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _lieuCtrl,
                style: const TextStyle(color: Colors.white),
                decoration: InputDecoration(
                  labelText: 'Lieu',
                  labelStyle: TextStyle(color: Colors.white.withValues(alpha: 0.5)),
                  filled: true,
                  fillColor: Colors.white.withValues(alpha: 0.06),
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
                ),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  const Text('Type : ', style: TextStyle(color: Colors.white70, fontSize: 14)),
                  const SizedBox(width: 8),
                  Expanded(
                    child: DropdownButton<String>(
                      value: _type,
                      dropdownColor: const Color(0xFF1E2A4A),
                      style: const TextStyle(color: Colors.white),
                      isExpanded: true,
                      items: _types.map((t) => DropdownMenuItem(value: t, child: Text(t))).toList(),
                      onChanged: (v) => setState(() => _type = v ?? _type),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Text('Pièces jointes', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
              const SizedBox(height: 6),
              AttachmentPickerField(
                apiService: widget.apiService,
                value: _fichierIds,
                onChanged: (ids) => setState(() => _fichierIds..clear()..addAll(ids)),
              ),
              const SizedBox(height: 12),
              InkWell(
                onTap: _pickDate,
                child: Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.06),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Row(
                    children: [
                      Icon(Icons.calendar_today, size: 16, color: Colors.white.withValues(alpha: 0.5)),
                      const SizedBox(width: 8),
                      Text(
                        'Début : ${_dateDebut.day.toString().padLeft(2, '0')}/${_dateDebut.month.toString().padLeft(2, '0')}/${_dateDebut.year}',
                        style: const TextStyle(color: Colors.white),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 16),
              Row(mainAxisAlignment: MainAxisAlignment.end, children: [
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text('Annuler'),
                ),
                const SizedBox(width: 8),
                FilledButton.icon(
                  onPressed: _isProcessing ? null : _create,
                  icon: _isProcessing
                      ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                      : const Icon(Icons.add, size: 18),
                  label: const Text('Créer'),
                ),
              ]),
            ],
          ),
        ),
      ),
    );
  }
}
