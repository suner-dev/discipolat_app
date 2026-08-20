import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

/// Saisie des présences du département par le Responsable.
/// Permet de pointer la présence / absence de chaque membre pour une semaine donnée,
/// avec option de filtrer par programme / sous-programme.
class PresenceEntryScreen extends StatefulWidget {
  final String departmentId;

  const PresenceEntryScreen({super.key, required this.departmentId});

  @override
  State<PresenceEntryScreen> createState() => _PresenceEntryScreenState();
}

class _PresenceEntryScreenState extends State<PresenceEntryScreen> {
  final _apiService = ApiService();
  List<dynamic> _members = [];
  Map<String, bool?> _presenceForm = {};
  Map<String, String> _notes = {};
  bool _isLoading = true;
  bool _isSaving = false;
  DateTime _selectedWeek = DateTime.now();
  String? _selectedProgram;
  String? _selectedSousProgram;
  List<dynamic> _programTypes = [];

  String get _deptId => widget.departmentId;

  @override
  void initState() {
    super.initState();
    _loadPrograms();
    _loadMembers();
  }

  String _weekMonday(DateTime date) {
    final d = date.subtract(Duration(days: (date.weekday - 1) % 7));
    return '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
  }

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
      final params = <String, String>{'semaine': semaine};
      final res = await _apiService.get('/members/departments/$_deptId/presences', params: params);
      final data = (res.data as List?) ?? [];
      if (mounted) {
        setState(() {
          _members = data;
          _presenceForm = {};
          _notes = {};
          // Pré-remplir les statuts existants
          for (final m in data) {
            final member = m as Map<String, dynamic>;
            final soulId = member['soulId']?.toString() ?? '';
            if (soulId.isNotEmpty && member['present'] != null) {
              _presenceForm[soulId] = member['present'] as bool;
            }
            if (soulId.isNotEmpty && member['notes'] != null) {
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
          const SnackBar(content: Text('Présences enregistrées ✅')),
        );
        context.pop();
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Échec de l\'enregistrement')),
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

  @override
  Widget build(BuildContext context) {
    final checkedCount = _presenceForm.values.where((v) => v != null).length;
    final presentCount = _presenceForm.values.where((v) => v == true).length;
    final absentCount = _presenceForm.values.where((v) => v == false).length;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Saisie des présences'),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _loadMembers),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 8)
          : Column(
              children: [
                // Header controls
                GlassCard(
                  margin: const EdgeInsets.all(12),
                  padding: const EdgeInsets.all(12),
                  child: Column(
                    children: [
                      // Semaine
                      Row(
                        children: [
                          Icon(Icons.calendar_today, color: AppColors.primaryLight, size: 18),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              'Semaine du ${_weekMonday(_selectedWeek)}',
                              style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600),
                            ),
                          ),
                          TextButton.icon(
                            onPressed: _pickWeek,
                            icon: const Icon(Icons.edit_calendar, size: 16),
                            label: const Text('Changer', style: TextStyle(fontSize: 12)),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      // Programme selector
                      if (_programTypes.isNotEmpty)
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
                          ],
                        ),
                      const SizedBox(height: 8),
                      // Stats summary
                      Row(
                        children: [
                          _miniStat('Pointés', '$checkedCount/${_members.length}', Colors.blue),
                          const SizedBox(width: 8),
                          _miniStat('Présents', '$presentCount', Colors.green),
                          const SizedBox(width: 8),
                          _miniStat('Absents', '$absentCount', Colors.red),
                        ],
                      ),
                    ],
                  ),
                ),

                // Members list
                Expanded(
                  child: _members.isEmpty
                      ? Center(
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Icon(Icons.people_outline, color: Colors.white.withValues(alpha: 0.2), size: 48),
                              const SizedBox(height: 12),
                              Text('Aucun membre à pointer',
                                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                            ],
                          ),
                        )
                      : ListView.builder(
                          padding: const EdgeInsets.symmetric(horizontal: 12),
                          itemCount: _members.length,
                          itemBuilder: (context, index) {
                            final m = _members[index] as Map<String, dynamic>;
                            final soulId = m['soulId']?.toString() ?? '';
                            final nom = m['nom']?.toString() ?? '—';
                            final familleNom = m['familleNom']?.toString();
                            final statut = m['statut']?.toString();
                            final isPresent = _presenceForm[soulId] == true;
                            final isAbsent = _presenceForm[soulId] == false;


                            return GlassCard(
                              margin: const EdgeInsets.only(bottom: 8),
                              padding: const EdgeInsets.all(12),
                              borderColor: isPresent
                                  ? Colors.green.withValues(alpha: 0.3)
                                  : isAbsent
                                      ? Colors.red.withValues(alpha: 0.3)
                                      : null,
                              child: Column(
                                children: [
                                  Row(
                                    children: [
                                      // Avatar with status color
                                      Container(
                                        width: 40,
                                        height: 40,
                                        decoration: BoxDecoration(
                                          gradient: LinearGradient(
                                            colors: isPresent
                                                ? [Colors.green, Colors.green.withValues(alpha: 0.7)]
                                                : isAbsent
                                                    ? [Colors.red, Colors.red.withValues(alpha: 0.7)]
                                                    : [Colors.grey, Colors.grey.withValues(alpha: 0.7)],
                                          ),
                                          borderRadius: BorderRadius.circular(10),
                                        ),
                                        child: Center(
                                          child: isPresent
                                              ? const Icon(Icons.check, color: Colors.white, size: 20)
                                              : isAbsent
                                                  ? const Icon(Icons.close, color: Colors.white, size: 20)
                                                  : const Icon(Icons.help_outline, color: Colors.white, size: 18),
                                        ),
                                      ),
                                      const SizedBox(width: 12),
                                      // Name + info
                                      Expanded(
                                        child: Column(
                                          crossAxisAlignment: CrossAxisAlignment.start,
                                          children: [
                                            Text(nom,
                                                style: const TextStyle(
                                                    color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                                            if (familleNom != null || statut != null)
                                              Text(
                                                '${familleNom != null ? 'Fam. $familleNom' : ''}'
                                                '${familleNom != null && statut != null ? ' · ' : ''}'
                                                '${statut != null ? statut.replaceAll('_', ' ') : ''}',
                                                style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11),
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
                                            onTap: soulId.isNotEmpty
                                                ? () => setState(() => _presenceForm[soulId] = true)
                                                : null,
                                          ),
                                          const SizedBox(width: 4),
                                          _toggleButton(
                                            icon: Icons.cancel,
                                            isActive: isAbsent,
                                            activeColor: Colors.red,
                                            onTap: soulId.isNotEmpty
                                                ? () => setState(() => _presenceForm[soulId] = false)
                                                : null,
                                          ),
                                        ],
                                      ),
                                      const SizedBox(width: 8),
                                      // Status label
                                      Container(
                                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                                        decoration: BoxDecoration(
                                          color: isPresent
                                              ? Colors.green.withValues(alpha: 0.15)
                                              : isAbsent
                                                  ? Colors.red.withValues(alpha: 0.15)
                                                  : Colors.white.withValues(alpha: 0.05),
                                          borderRadius: BorderRadius.circular(8),
                                        ),
                                        child: Text(
                                          isPresent ? 'Présent' : isAbsent ? 'Absent' : '—',
                                          style: TextStyle(
                                            color: isPresent
                                                ? Colors.green
                                                : isAbsent
                                                    ? Colors.red
                                                    : Colors.white.withValues(alpha: 0.3),
                                            fontSize: 10,
                                            fontWeight: FontWeight.w600,
                                          ),
                                        ),
                                      ),
                                    ],
                                  ),
                                  // Note input
                                  if (_presenceForm[soulId] != null || _notes[soulId]?.isNotEmpty == true)
                                    Padding(
                                      padding: const EdgeInsets.only(top: 8),
                                      child: TextField(
                                        style: const TextStyle(color: Colors.white, fontSize: 12),
                                        controller: TextEditingController(text: _notes[soulId] ?? ''),
                                        onChanged: (v) => _notes[soulId] = v,
                                        decoration: InputDecoration(
                                          hintText: 'Note (optionnel)',
                                          hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.3)),
                                          isDense: true,
                                          contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                                          border: OutlineInputBorder(
                                            borderRadius: BorderRadius.circular(8),
                                            borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.1)),
                                          ),
                                          enabledBorder: OutlineInputBorder(
                                            borderRadius: BorderRadius.circular(8),
                                            borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.1)),
                                          ),
                                        ),
                                      ),
                                    ),
                                ],
                              ),
                            );
                          },
                        ),
                ),

                // Submit button
                SafeArea(
                  child: Padding(
                    padding: const EdgeInsets.all(12),
                    child: SizedBox(
                      width: double.infinity,
                      height: 48,
                      child: FilledButton.icon(
                        onPressed: _isSaving || checkedCount == 0 ? null : _submitPresences,
                        icon: _isSaving
                            ? const SizedBox(
                                width: 18,
                                height: 18,
                                child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                              )
                            : const Icon(Icons.save, size: 18),
                        label: Text(
                          _isSaving
                              ? 'Enregistrement…'
                              : 'Enregistrer ($checkedCount membre${checkedCount > 1 ? 's' : ''})',
                        ),
                        style: FilledButton.styleFrom(
                          backgroundColor: AppColors.primary,
                          disabledBackgroundColor: AppColors.primary.withValues(alpha: 0.3),
                        ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
    );
  }

  Widget _miniStat(String label, String value, Color color) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 6),
        decoration: BoxDecoration(
          color: color.withValues(alpha: 0.1),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Column(
          children: [
            Text(value, style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 14)),
            Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 9)),
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
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
          color: isActive ? activeColor.withValues(alpha: 0.2) : Colors.white.withValues(alpha: 0.05),
          borderRadius: BorderRadius.circular(8),
          border: Border.all(
            color: isActive ? activeColor.withValues(alpha: 0.4) : Colors.white.withValues(alpha: 0.08),
          ),
        ),
        child: Icon(icon, color: isActive ? activeColor : Colors.white.withValues(alpha: 0.3), size: 20),
      ),
    );
  }
}
