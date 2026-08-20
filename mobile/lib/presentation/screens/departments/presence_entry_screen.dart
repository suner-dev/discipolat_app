import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

/// Saisie des présences du département par le Responsable.
/// - Sélecteur semaine + programme + sous-programme
/// - Recherche par nom
/// - Actions bulk : tout présents / tout absents
/// - Swipe droite = présent, swipe gauche = absent
/// - Badge "Pas de compte" pour les membres sans userId
/// - Note optionnelle par membre
/// - Enregistrement groupé via POST /members/departments/{deptId}/presences
class PresenceEntryScreen extends StatefulWidget {
  final String departmentId;

  const PresenceEntryScreen({super.key, required this.departmentId});

  @override
  State<PresenceEntryScreen> createState() => _PresenceEntryScreenState();
}

class _PresenceEntryScreenState extends State<PresenceEntryScreen> {
  final _apiService = ApiService();
  final _searchCtrl = TextEditingController();
  final _scrollCtrl = ScrollController();

  List<dynamic> _members = [];
  final Map<String, bool?> _presenceForm = {};
  final Map<String, String> _notes = {};
  final Map<String, TextEditingController> _noteControllers = {};

  bool _isLoading = true;
  bool _isSaving = false;
  DateTime _selectedWeek = DateTime.now();
  String? _selectedProgram;
  String? _selectedSousProgram;
  List<dynamic> _programTypes = [];
  String _searchQuery = '';

  String get _deptId => widget.departmentId;

  @override
  void initState() {
    super.initState();
    _loadPrograms();
    _loadMembers();
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    _scrollCtrl.dispose();
    for (final c in _noteControllers.values) {
      c.dispose();
    }
    super.dispose();
  }

  // ── Helpers ──────────────────────────────────────────────────────────

  String _weekMonday(DateTime date) {
    final d = date.subtract(Duration(days: (date.weekday - 1) % 7));
    return '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
  }

  TextEditingController _getNoteController(String soulId) {
    return _noteControllers.putIfAbsent(soulId, () {
      return TextEditingController(text: _notes[soulId] ?? '');
    });
  }

  List<dynamic> get _filteredMembers {
    if (_searchQuery.isEmpty) return _members;
    final q = _searchQuery.toLowerCase();
    return _members.where((m) {
      final member = m as Map<String, dynamic>;
      final nom = (member['nom']?.toString() ?? '').toLowerCase();
      final famille = (member['familleNom']?.toString() ?? '').toLowerCase();
      return nom.contains(q) || famille.contains(q);
    }).toList();
  }

  int get _checkedCount => _presenceForm.values.where((v) => v != null).length;
  int get _presentCount => _presenceForm.values.where((v) => v == true).length;
  int get _absentCount => _presenceForm.values.where((v) => v == false).length;
  int get _unmarkedCount => _members.length - _checkedCount;

  // ── Data loading ─────────────────────────────────────────────────────

  Future<void> _loadPrograms() async {
    try {
      final res = await _apiService.get('/programs/active');
      if (mounted) {
        setState(() => _programTypes = (res.data as List?) ?? []);
      }
    } catch (_) {}
  }

  Future<void> _loadMembers() async {
    setState(() => _isLoading = true);
    try {
      final semaine = _weekMonday(_selectedWeek);
      final res = await _apiService.get(
        '/members/departments/$_deptId/presences',
        params: {'semaine': semaine},
      );
      final data = (res.data as List?) ?? [];
      if (mounted) {
        setState(() {
          _members = data;
          _presenceForm.clear();
          _notes.clear();
          // Dispose old controllers
          for (final c in _noteControllers.values) {
            c.dispose();
          }
          _noteControllers.clear();

          // Pré-remplir les statuts existants
          for (final m in data) {
            final member = m as Map<String, dynamic>;
            final soulId = member['soulId']?.toString() ?? '';
            if (soulId.isEmpty) continue;
            if (member['present'] != null) {
              _presenceForm[soulId] = member['present'] as bool;
            }
            if (member['notes'] != null && (member['notes'] as String).isNotEmpty) {
              _notes[soulId] = member['notes'].toString();
            }
          }
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  // ── Actions ──────────────────────────────────────────────────────────

  void _markAllPresent() {
    setState(() {
      for (final m in _members) {
        final soulId = (m as Map<String, dynamic>)['soulId']?.toString() ?? '';
        if (soulId.isNotEmpty) _presenceForm[soulId] = true;
      }
    });
    HapticFeedback.lightImpact();
  }

  void _markAllAbsent() {
    setState(() {
      for (final m in _members) {
        final soulId = (m as Map<String, dynamic>)['soulId']?.toString() ?? '';
        if (soulId.isNotEmpty) _presenceForm[soulId] = false;
      }
    });
    HapticFeedback.lightImpact();
  }

  void _resetAll() {
    setState(() {
      _presenceForm.clear();
      for (final c in _noteControllers.values) {
        c.clear();
      }
      _notes.clear();
    });
    HapticFeedback.lightImpact();
  }

  void _togglePresence(String soulId, bool present) {
    // Toggle : si déjà ce statut, on déselectionne
    if (_presenceForm[soulId] == present) {
      _presenceForm[soulId] = null;
    } else {
      _presenceForm[soulId] = present;
    }
    HapticFeedback.selectionClick();
    setState(() {});
  }

  Future<void> _submitPresences() async {
    final items = _presenceForm.entries
        .where((e) => e.value != null)
        .map((e) => {
              'soulId': e.key,
              'present': e.value,
              if (_notes[e.key]?.isNotEmpty == true) 'notes': _notes[e.key],
            })
        .toList();

    if (items.isEmpty) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Cochez au moins un membre')),
        );
      }
      return;
    }

    setState(() => _isSaving = true);
    try {
      await _apiService.post('/members/departments/$_deptId/presences', data: {
        'semaine': _weekMonday(_selectedWeek),
        if (_selectedProgram != null) 'typeProgramme': _selectedProgram,
        if (_selectedSousProgram != null) 'sousProgramme': _selectedSousProgram,
        'presences': items,
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('✅ ${items.length} présence${items.length > 1 ? 's' : ''} enregistrée${items.length > 1 ? 's' : ''}'),
            backgroundColor: Colors.green.shade700,
          ),
        );
        context.pop();
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Échec de l\'enregistrement'), backgroundColor: Colors.red),
        );
      }
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  Future<void> _pickWeek() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _selectedWeek,
      firstDate: DateTime(2024),
      lastDate: DateTime.now().add(const Duration(days: 7)),
    );
    if (picked != null) {
      setState(() => _selectedWeek = picked);
      _loadMembers();
    }
  }

  // ── Build ────────────────────────────────────────────────────────────

  @override
  Widget build(BuildContext context) {
    final filtered = _filteredMembers;
    final sousProgrammes = _getSousProgrammes();

    return Scaffold(
      appBar: AppBar(
        title: Text('Présences · ${_members.length} membres'),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _loadMembers),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 8)
          : Column(
              children: [
                // ═══ HEADER ═══
                _buildHeader(sousProgrammes),

                // ═══ SEARCH BAR ═══
                if (_members.length > 5)
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 12),
                    child: TextField(
                      controller: _searchCtrl,
                      style: const TextStyle(color: Colors.white, fontSize: 13),
                      onChanged: (v) => setState(() => _searchQuery = v),
                      decoration: InputDecoration(
                        hintText: 'Rechercher un membre…',
                        hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.3)),
                        prefixIcon: Icon(Icons.search, color: Colors.white.withValues(alpha: 0.4), size: 18),
                        suffixIcon: _searchQuery.isNotEmpty
                            ? IconButton(
                                icon: Icon(Icons.clear, color: Colors.white.withValues(alpha: 0.4), size: 16),
                                onPressed: () {
                                  _searchCtrl.clear();
                                  setState(() => _searchQuery = '');
                                },
                              )
                            : null,
                        isDense: true,
                        contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(10),
                          borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.1)),
                        ),
                        enabledBorder: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(10),
                          borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.1)),
                        ),
                        filled: true,
                        fillColor: Colors.white.withValues(alpha: 0.05),
                      ),
                    ),
                  ),

                const SizedBox(height: 8),

                // ═══ MEMBERS LIST ═══
                Expanded(
                  child: filtered.isEmpty
                      ? _buildEmptyState()
                      : ListView.builder(
                          controller: _scrollCtrl,
                          padding: const EdgeInsets.symmetric(horizontal: 12),
                          itemCount: filtered.length,
                          itemBuilder: (context, index) => _buildMemberTile(filtered[index]),
                        ),
                ),

                // ═══ SUBMIT BUTTON ═══
                _buildSubmitBar(),
              ],
            ),
    );
  }

  // ── Sub-widgets ──────────────────────────────────────────────────────

  Widget _buildHeader(List<dynamic> sousProgrammes) {
    return GlassCard(
      margin: const EdgeInsets.fromLTRB(12, 12, 12, 0),
      padding: const EdgeInsets.all(12),
      child: Column(
        children: [
          // Semaine
          Row(
            children: [
              Icon(Icons.calendar_today, color: AppColors.primaryLight, size: 16),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  'Semaine du ${_weekMonday(_selectedWeek)}',
                  style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13),
                ),
              ),
              TextButton.icon(
                onPressed: _pickWeek,
                icon: const Icon(Icons.edit_calendar, size: 14),
                label: const Text('Changer', style: TextStyle(fontSize: 11)),
                style: TextButton.styleFrom(padding: const EdgeInsets.symmetric(horizontal: 8)),
              ),
            ],
          ),

          // Programme + sous-programme
          if (_programTypes.isNotEmpty) ...[
            const SizedBox(height: 8),
            Row(
              children: [
                Expanded(
                  child: DropdownButton<String>(
                    value: _selectedProgram,
                    dropdownColor: AppColors.cardDark,
                    isExpanded: true,
                    underline: const SizedBox(),
                    style: const TextStyle(color: Colors.white, fontSize: 12),
                    hint: Text('Programme', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                    items: [
                      const DropdownMenuItem<String>(value: null, child: Text('Général')),
                      ..._programTypes.map((p) {
                        final prog = p as Map<String, dynamic>;
                        return DropdownMenuItem<String>(
                          value: prog['code']?.toString(),
                          child: Text(prog['label']?.toString() ?? ''),
                        );
                      }),
                    ],
                    onChanged: (v) => setState(() {
                      _selectedProgram = v;
                      _selectedSousProgram = null;
                    }),
                  ),
                ),
                if (sousProgrammes.isNotEmpty) ...[
                  const SizedBox(width: 8),
                  Expanded(
                    child: DropdownButton<String>(
                      value: _selectedSousProgram,
                      dropdownColor: AppColors.cardDark,
                      isExpanded: true,
                      underline: const SizedBox(),
                      style: const TextStyle(color: Colors.white, fontSize: 12),
                      hint: Text('Sous-programme', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                      items: [
                        const DropdownMenuItem<String>(value: null, child: Text('—')),
                        ...sousProgrammes.map((sp) {
                          final label = sp['label']?.toString() ?? '';
                          final heure = sp['heureDebut']?.toString();
                          return DropdownMenuItem<String>(
                            value: label,
                            child: Text(heure != null ? '$label · $heure' : label),
                          );
                        }),
                      ],
                      onChanged: (v) => setState(() => _selectedSousProgram = v),
                    ),
                  ),
                ],
              ],
            ),
          ],

          const SizedBox(height: 10),

          // Stats + bulk actions
          Row(
            children: [
              _miniStat('Pointés', '$_checkedCount/${_members.length}', Colors.blue),
              const SizedBox(width: 6),
              _miniStat('Présents', '$_presentCount', Colors.green),
              const SizedBox(width: 6),
              _miniStat('Absents', '$_absentCount', Colors.red),
              const SizedBox(width: 6),
              if (_unmarkedCount > 0)
                _miniStat('?', '$_unmarkedCount', Colors.grey),
            ],
          ),
          const SizedBox(height: 8),

          // Bulk actions
          Row(
            children: [
              Expanded(
                child: _bulkButton('Tous présents', Icons.check_circle, Colors.green, _markAllPresent),
              ),
              const SizedBox(width: 6),
              Expanded(
                child: _bulkButton('Tous absents', Icons.cancel, Colors.red, _markAllAbsent),
              ),
              const SizedBox(width: 6),
              Expanded(
                child: _bulkButton('Réinitialiser', Icons.restart_alt, Colors.grey, _resetAll),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildMemberTile(dynamic memberData) {
    final m = memberData as Map<String, dynamic>;
    final soulId = m['soulId']?.toString() ?? '';
    final userId = m['userId']?.toString();
    final nom = m['nom']?.toString() ?? '—';
    final familleNom = m['familleNom']?.toString();
    final statut = m['statut']?.toString();
    final hasNoAccount = userId == null || userId.isEmpty;
    final isPresent = _presenceForm[soulId] == true;
    final isAbsent = _presenceForm[soulId] == false;

    return Dismissible(
      key: Key('presence-$soulId'),
      direction: soulId.isNotEmpty ? DismissDirection.horizontal : DismissDirection.none,
      confirmDismiss: (direction) async {
        if (direction == DismissDirection.startToEnd) {
          _togglePresence(soulId, true);
        } else {
          _togglePresence(soulId, false);
        }
        return false; // Never actually dismiss — just toggle
      },
      background: Container(
        alignment: Alignment.centerLeft,
        padding: const EdgeInsets.only(left: 20),
        margin: const EdgeInsets.only(bottom: 8),
        decoration: BoxDecoration(
          color: Colors.green.withValues(alpha: 0.15),
          borderRadius: BorderRadius.circular(14),
        ),
        child: const Icon(Icons.check_circle, color: Colors.green),
      ),
      secondaryBackground: Container(
        alignment: Alignment.centerRight,
        padding: const EdgeInsets.only(right: 20),
        margin: const EdgeInsets.only(bottom: 8),
        decoration: BoxDecoration(
          color: Colors.red.withValues(alpha: 0.15),
          borderRadius: BorderRadius.circular(14),
        ),
        child: const Icon(Icons.cancel, color: Colors.red),
      ),
      child: GlassCard(
        margin: const EdgeInsets.only(bottom: 8),
        padding: const EdgeInsets.all(10),
        borderColor: isPresent
            ? Colors.green.withValues(alpha: 0.3)
            : isAbsent
                ? Colors.red.withValues(alpha: 0.3)
                : null,
        child: Column(
          children: [
            Row(
              children: [
                // Avatar
                Container(
                  width: 38,
                  height: 38,
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      colors: isPresent
                          ? [Colors.green, Colors.green.withValues(alpha: 0.7)]
                          : isAbsent
                              ? [Colors.red, Colors.red.withValues(alpha: 0.7)]
                              : [Colors.grey.shade700, Colors.grey.withValues(alpha: 0.7)],
                    ),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Center(
                    child: isPresent
                        ? const Icon(Icons.check, color: Colors.white, size: 18)
                        : isAbsent
                            ? const Icon(Icons.close, color: Colors.white, size: 18)
                            : Icon(Icons.person, color: Colors.white.withValues(alpha: 0.5), size: 16),
                  ),
                ),
                const SizedBox(width: 10),
                // Name + info
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Flexible(
                            child: Text(nom,
                                style: const TextStyle(
                                    color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13),
                                overflow: TextOverflow.ellipsis),
                          ),
                          if (hasNoAccount) ...[
                            const SizedBox(width: 4),
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                              decoration: BoxDecoration(
                                color: Colors.orange.withValues(alpha: 0.15),
                                borderRadius: BorderRadius.circular(6),
                              ),
                              child: Text('Pas de compte',
                                  style: TextStyle(color: Colors.orange.shade300, fontSize: 8, fontWeight: FontWeight.w600)),
                            ),
                          ],
                        ],
                      ),
                      const SizedBox(height: 2),
                      Text(
                        '${familleNom != null ? 'Fam. $familleNom' : 'Sans famille'}'
                        '${statut != null ? ' · ${statut.replaceAll('_', ' ').toLowerCase()}' : ''}',
                        style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 10),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ],
                  ),
                ),
                // Toggle buttons
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    _toggleButton(
                      icon: Icons.check_circle,
                      isActive: isPresent,
                      activeColor: Colors.green,
                      onTap: soulId.isNotEmpty ? () => _togglePresence(soulId, true) : null,
                    ),
                    const SizedBox(width: 4),
                    _toggleButton(
                      icon: Icons.cancel,
                      isActive: isAbsent,
                      activeColor: Colors.red,
                      onTap: soulId.isNotEmpty ? () => _togglePresence(soulId, false) : null,
                    ),
                  ],
                ),
                const SizedBox(width: 6),
                // Status label
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                  decoration: BoxDecoration(
                    color: isPresent
                        ? Colors.green.withValues(alpha: 0.15)
                        : isAbsent
                            ? Colors.red.withValues(alpha: 0.15)
                            : Colors.white.withValues(alpha: 0.05),
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: Text(
                    isPresent ? 'Présent' : isAbsent ? 'Absent' : '—',
                    style: TextStyle(
                      color: isPresent
                          ? Colors.green
                          : isAbsent
                              ? Colors.red
                              : Colors.white.withValues(alpha: 0.3),
                      fontSize: 9,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ],
            ),
            // Note input — only when member is marked
            if (_presenceForm[soulId] != null || (_notes[soulId]?.isNotEmpty == true))
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: TextField(
                  style: const TextStyle(color: Colors.white, fontSize: 11),
                  controller: _getNoteController(soulId),
                  onChanged: (v) => _notes[soulId] = v,
                  textInputAction: TextInputAction.done,
                  decoration: InputDecoration(
                    hintText: 'Note (optionnel)',
                    hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.3)),
                    isDense: true,
                    contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                      borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.1)),
                    ),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                      borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.1)),
                    ),
                    suffixIcon: _notes[soulId]?.isNotEmpty == true
                        ? GestureDetector(
                            onTap: () {
                              _getNoteController(soulId).clear();
                              setState(() => _notes[soulId] = '');
                            },
                            child: Icon(Icons.clear, color: Colors.white.withValues(alpha: 0.3), size: 14),
                          )
                        : null,
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.people_outline, color: Colors.white.withValues(alpha: 0.15), size: 56),
          const SizedBox(height: 12),
          Text(
            _searchQuery.isNotEmpty ? 'Aucun résultat pour "$_searchQuery"' : 'Aucun membre à pointer',
            style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 14),
          ),
          if (_searchQuery.isNotEmpty) ...[
            const SizedBox(height: 8),
            TextButton(
              onPressed: () {
                _searchCtrl.clear();
                setState(() => _searchQuery = '');
              },
              child: const Text('Effacer la recherche'),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildSubmitBar() {
    return SafeArea(
      child: Container(
        padding: const EdgeInsets.fromLTRB(12, 8, 12, 12),
        decoration: BoxDecoration(
          color: Colors.black.withValues(alpha: 0.3),
          border: Border(top: BorderSide(color: Colors.white.withValues(alpha: 0.06))),
        ),
        child: Row(
          children: [
            // Progress indicator
            if (_checkedCount > 0)
              SizedBox(
                width: 40,
                child: Text(
                  '${(_checkedCount / _members.length * 100).round()}%',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    color: _checkedCount == _members.length ? Colors.green : Colors.blue,
                    fontWeight: FontWeight.bold,
                    fontSize: 12,
                  ),
                ),
              ),
            const SizedBox(width: 8),
            // Submit button
            Expanded(
              child: SizedBox(
                height: 46,
                child: FilledButton.icon(
                  onPressed: _isSaving || _checkedCount == 0 ? null : _submitPresences,
                  icon: _isSaving
                      ? const SizedBox(
                          width: 16,
                          height: 16,
                          child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                        )
                      : const Icon(Icons.save, size: 16),
                  label: Text(
                    _isSaving
                        ? 'Enregistrement…'
                        : 'Enregistrer ($_checkedCount/${_members.length})',
                    style: const TextStyle(fontSize: 13),
                  ),
                  style: FilledButton.styleFrom(
                    backgroundColor: AppColors.primary,
                    disabledBackgroundColor: AppColors.primary.withValues(alpha: 0.3),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  // ── Helper widgets ───────────────────────────────────────────────────

  Widget _miniStat(String label, String value, Color color) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 5),
        decoration: BoxDecoration(
          color: color.withValues(alpha: 0.1),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Column(
          children: [
            Text(value, style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 13)),
            Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 8)),
          ],
        ),
      ),
    );
  }

  Widget _bulkButton(String label, IconData icon, Color color, VoidCallback onTap) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 8),
        decoration: BoxDecoration(
          color: color.withValues(alpha: 0.1),
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: color.withValues(alpha: 0.2)),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, color: color, size: 14),
            const SizedBox(width: 4),
            Text(label, style: TextStyle(color: color, fontSize: 10, fontWeight: FontWeight.w600)),
          ],
        ),
      ),
    );
  }

  Widget _toggleButton({
    required IconData icon,
    required bool isActive,
    required Color activeColor,
    VoidCallback? onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(7),
        decoration: BoxDecoration(
          color: isActive ? activeColor.withValues(alpha: 0.2) : Colors.white.withValues(alpha: 0.04),
          borderRadius: BorderRadius.circular(8),
          border: Border.all(
            color: isActive ? activeColor.withValues(alpha: 0.4) : Colors.white.withValues(alpha: 0.08),
          ),
        ),
        child: Icon(icon, color: isActive ? activeColor : Colors.white.withValues(alpha: 0.25), size: 18),
      ),
    );
  }

  List<dynamic> _getSousProgrammes() {
    if (_selectedProgram == null) return [];
    final selected = _programTypes.cast<Map<String, dynamic>>().firstWhere(
          (p) => p['code']?.toString() == _selectedProgram,
          orElse: () => {},
        );
    if (selected['aSousProgrammes'] == true) {
      return (selected['sousProgrammes'] as List<dynamic>?) ?? [];
    }
    return [];
  }
}
