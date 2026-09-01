import 'package:flutter/material.dart';
import 'dart:convert';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';
import '../../../data/models/soul.dart';

class FamiliesListScreen extends StatefulWidget {
  const FamiliesListScreen({super.key});

  @override
  State<FamiliesListScreen> createState() => _FamiliesListScreenState();
}

class _FamiliesListScreenState extends State<FamiliesListScreen> {
  final _apiService = ApiService();
  List<dynamic> _families = [];
  bool _isLoading = true;
  String _searchQuery = '';
  String _riskFilter = '';
  int _page = 0;
  bool _hasMore = true;

  // CRUD state
  ViewMode _view = ViewMode.list;
  Map<String, dynamic>? _selectedFamily;
  Map<String, dynamic>? _familyDetail;
  String? _editingId;
  final _formKey = GlobalKey<FormState>();
  final _nomCtrl = TextEditingController();
  String? _chefFamilleId;
  List<dynamic> _users = [];
  bool _showDeleteConfirm = false;
  String? _deleteTargetId;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  @override
  void dispose() {
    _nomCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final params = <String, String>{'size': '20', 'page': '$_page'};
      if (_searchQuery.isNotEmpty) params['search'] = _searchQuery;
      if (_riskFilter.isNotEmpty) params['niveauRisque'] = _riskFilter;
      final res = await _apiService.get('/families', params: params);
      if (mounted) {
        setState(() {
          _families = (res.data is Map ? res.data['content'] : res.data) as List<dynamic>? ?? [];
          final totalPages = (res.data is Map ? res.data['totalPages'] : 1) ?? 1;
          _hasMore = _page < totalPages - 1;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _loadUsers() async {
    try {
      final res = await _apiService.get('/users', params: {'size': '200'});
      if (mounted) {
        setState(() {
          _users = (res.data is Map ? res.data['content'] : res.data) as List<dynamic>? ?? [];
        });
      }
    } catch (_) {}
  }

  Future<void> _loadFamilyDetail(String id) async {
    try {
      final res = await _apiService.get('/families/$id');
      if (mounted) setState(() => _familyDetail = res.data);
    } catch (_) {}
  }

  Future<void> _createFamily() async {
    if (!_formKey.currentState!.validate()) return;
    try {
      await _apiService.post('/families', data: {
        'nom': _nomCtrl.text.trim(),
        if (_chefFamilleId != null && _chefFamilleId!.isNotEmpty)
          'chefFamilleId': _chefFamilleId,
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Famille créée'), backgroundColor: Color(0xFF2E7D32)),
        );
        setState(() => _view = ViewMode.list);
        _loadData();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Erreur: $e'), backgroundColor: const Color(0xFFC62828)),
        );
      }
    }
  }

  Future<void> _updateFamily(String id) async {
    if (!_formKey.currentState!.validate()) return;
    try {
      await _apiService.put('/families/$id', data: {
        'nom': _nomCtrl.text.trim(),
        if (_chefFamilleId != null && _chefFamilleId!.isNotEmpty)
          'chefFamilleId': _chefFamilleId,
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Famille mise à jour'), backgroundColor: Color(0xFF2E7D32)),
        );
        setState(() => _view = ViewMode.list);
        _editingId = null;
        _loadData();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Erreur: $e'), backgroundColor: const Color(0xFFC62828)),
        );
      }
    }
  }

  Future<void> _deleteFamily(String id) async {
    try {
      await _apiService.delete('/families/$id');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Famille supprimée'), backgroundColor: Color(0xFF2E7D32)),
        );
        setState(() { _showDeleteConfirm = false; _deleteTargetId = null; });
        _loadData();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Erreur: $e'), backgroundColor: const Color(0xFFC62828)),
        );
      }
    }
  }

  // ==================== RISK HELPERS ====================

  Color _riskColor(String? risk) {
    switch (risk) {
      case 'A_RISQUE': return const Color(0xFFE53935);
      case 'SOUS_SURVEILLANCE': return const Color(0xFFFFB300);
      default: return const Color(0xFF4CAF50);
    }
  }

  String _riskLabel(String? risk) {
    switch (risk) {
      case 'A_RISQUE': return 'À risque';
      case 'SOUS_SURVEILLANCE': return 'Sous surveillance';
      default: return 'Normal';
    }
  }

  // ==================== BUILD ====================

  @override
  Widget build(BuildContext context) {
    switch (_view) {
      case ViewMode.detail:
        return _buildDetail();
      case ViewMode.create:
      case ViewMode.edit:
        return _buildForm();
      case ViewMode.list:
      default:
        return _buildList();
    }
  }

  // ==================== LIST VIEW ====================

  Widget _buildList() {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Familles'),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData),
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
                      _statMini('Total', '${_families.length}', Colors.purple),
                      const SizedBox(width: 8),
                      _statMini('Actives', '${_families.where((f) => (f as Map)['actif'] != false).length}', Colors.green),
                    ],
                  ),
                  const SizedBox(height: 12),
                  // Search + Filters
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12),
                    decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.06),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: TextField(
                      onChanged: (v) {
                        setState(() { _searchQuery = v; _page = 0; });
                        _loadData();
                      },
                      style: const TextStyle(color: Colors.white),
                      decoration: InputDecoration(
                        hintText: 'Rechercher une famille...',
                        hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.4)),
                        prefixIcon: Icon(Icons.search, color: Colors.white.withValues(alpha: 0.4), size: 20),
                        border: InputBorder.none,
                      ),
                    ),
                  ),
                  const SizedBox(height: 8),
                  // Risk filter chips
                  SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: [
                        _filterChip('Tous', '', Colors.white),
                        const SizedBox(width: 6),
                        _filterChip('À risque', 'A_RISQUE', const Color(0xFFE53935)),
                        const SizedBox(width: 6),
                        _filterChip('Surveillance', 'SOUS_SURVEILLANCE', const Color(0xFFFFB300)),
                        const SizedBox(width: 6),
                        _filterChip('Normal', 'NORMAL', const Color(0xFF4CAF50)),
                      ],
                    ),
                  ),
                  const SizedBox(height: 12),
                  // FAB
                  Align(
                    alignment: Alignment.centerRight,
                    child: ElevatedButton.icon(
                      onPressed: () {
                        _nomCtrl.clear();
                        _chefFamilleId = null;
                        _editingId = null;
                        _loadUsers();
                        setState(() => _view = ViewMode.create);
                      },
                      icon: const Icon(Icons.add, size: 18),
                      label: const Text('Nouvelle famille'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: const Color(0xFF7C3AED),
                        foregroundColor: Colors.white,
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                  // Family cards
                  if (_families.isEmpty)
                    GlassCard(
                      padding: const EdgeInsets.all(32),
                      child: Column(
                        children: [
                          Icon(Icons.family_restroom_outlined, size: 48, color: Colors.white.withValues(alpha: 0.3)),
                          const SizedBox(height: 12),
                          Text('Aucune famille', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                        ],
                      ),
                    )
                  else
                    ..._families.map((f) {
                      final family = f as Map;
                      final nom = family['nom'] ?? 'Famille';
                      final chefNom = family['chefFamilleNom'] ?? '—';
                      final statut = family['statut'] ?? 'ACTIVE';
                      final risk = family['niveauRisque'] as String?;
                      final nbDisciples = family['nombreDisciples'] ?? '—';
                      final dateCreation = family['createdAt']?.toString().substring(0, 10) ?? '';
                      final id = family['id']?.toString() ?? '';

                      return GlassCard(
                        margin: const EdgeInsets.only(bottom: 10),
                        padding: const EdgeInsets.all(14),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                GradientAvatar(
                                  text: nom.toString().substring(0, nom.toString().length.clamp(0, 2)),
                                  radius: 24,
                                  gradientStart: Colors.purple,
                                  gradientEnd: Colors.indigo,
                                ),
                                const SizedBox(width: 12),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(nom, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 15)),
                                      Text('Chef: $chefNom', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                                    ],
                                  ),
                                ),
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                                  decoration: BoxDecoration(
                                    color: _riskColor(risk).withAlpha(38),
                                    borderRadius: BorderRadius.circular(999),
                                  ),
                                  child: Text(_riskLabel(risk),
                                      style: TextStyle(color: _riskColor(risk), fontSize: 10, fontWeight: FontWeight.bold)),
                                ),
                              ],
                            ),
                            const SizedBox(height: 10),
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceAround,
                              children: [
                                _miniStat(Icons.people, 'Disciples', '$nbDisciples'),
                                _miniStat(Icons.shield, 'Statut', statut),
                                _miniStat(Icons.calendar_today, 'Créée', dateStr(dateCreation)),
                              ],
                            ),
                            const SizedBox(height: 10),
                            Row(
                              mainAxisAlignment: MainAxisAlignment.end,
                              children: [
                                IconButton(
                                  icon: Icon(Icons.visibility, color: Colors.white.withValues(alpha: 0.6), size: 18),
                                  onPressed: () {
                                    setState(() { _selectedFamily = family; _view = ViewMode.detail; });
                                    _loadFamilyDetail(id);
                                  },
                                ),
                                IconButton(
                                  icon: Icon(Icons.edit, color: Colors.white.withValues(alpha: 0.6), size: 18),
                                  onPressed: () {
                                    _nomCtrl.text = nom.toString();
                                    _chefFamilleId = family['chefFamilleId']?.toString();
                                    _editingId = id;
                                    _loadUsers();
                                    setState(() => _view = ViewMode.edit);
                                  },
                                ),
                                IconButton(
                                  icon: Icon(Icons.delete, color: const Color(0xFFE53935).withAlpha(180), size: 18),
                                  onPressed: () => setState(() { _showDeleteConfirm = true; _deleteTargetId = id; }),
                                ),
                              ],
                            ),
                          ],
                        ),
                      );
                    }),
                  // Pagination
                  if (_hasMore || _page > 0)
                    Padding(
                      padding: const EdgeInsets.only(top: 12),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          TextButton(
                            onPressed: _page > 0 ? () { setState(() => _page--); _loadData(); } : null,
                            child: const Text('← Précédent', style: TextStyle(color: Colors.white70)),
                          ),
                          Text('Page ${_page + 1}', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                          TextButton(
                            onPressed: _hasMore ? () { setState(() => _page++); _loadData(); } : null,
                            child: const Text('Suivant →', style: TextStyle(color: Colors.white70)),
                          ),
                        ],
                      ),
                    ),
                ],
              ),
            ),
      // Delete confirmation dialog
      bottomSheet: _showDeleteConfirm
          ? Container(
              padding: const EdgeInsets.all(20),
              decoration: const BoxDecoration(
                color: Color(0xFF1E293B),
                borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.warning_amber_rounded, color: Color(0xFFFFB300), size: 40),
                  const SizedBox(height: 12),
                  const Text('Supprimer cette famille ?',
                      style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
                  const SizedBox(height: 8),
                  Text('Les âmes ne seront pas supprimées.',
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 13)),
                  const SizedBox(height: 16),
                  Row(
                    children: [
                      Expanded(
                        child: OutlinedButton(
                          onPressed: () => setState(() { _showDeleteConfirm = false; _deleteTargetId = null; }),
                          child: const Text('Annuler'),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: ElevatedButton(
                          onPressed: _deleteTargetId != null ? () => _deleteFamily(_deleteTargetId!) : null,
                          style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFFE53935)),
                          child: const Text('Supprimer', style: TextStyle(color: Colors.white)),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            )
          : null,
    );
  }

  // ==================== DETAIL VIEW ====================

  Widget _buildDetail() {
    final family = _selectedFamily ?? {};
    final nom = family['nom'] ?? 'Famille';
    final chefNom = family['chefFamilleNom'] ?? '—';
    final statut = family['statut'] ?? 'ACTIVE';
    final risk = family['niveauRisque'] as String?;
    final id = family['id']?.toString() ?? '';
    final souls = _familyDetail != null ? (_familyDetail!['souls'] as List<dynamic>?) ?? [] : [];

    return Scaffold(
      appBar: AppBar(
        title: Text(nom),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => setState(() { _view = ViewMode.list; _selectedFamily = null; _familyDetail = null; }),
        ),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Info cards
          Row(
            children: [
              _infoCard('Statut', statut, Colors.blue),
              const SizedBox(width: 8),
              _infoCard('Risque', _riskLabel(risk), _riskColor(risk)),
            ],
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              _infoCard('Chef', chefNom, Colors.white),
              const SizedBox(width: 8),
              _infoCard('Créée', family['createdAt']?.toString().substring(0, 10) ?? '', Colors.white),
            ],
          ),
          if (family['chefAdjointNom'] != null) ...[
            const SizedBox(height: 8),
            _infoCard('Chef adjoint', family['chefAdjointNom'], Colors.white),
          ],
          const SizedBox(height: 20),
          // Action buttons
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () {
                    _nomCtrl.text = nom;
                    _chefFamilleId = family['chefFamilleId']?.toString();
                    _editingId = id;
                    _loadUsers();
                    setState(() => _view = ViewMode.edit);
                  },
                  icon: const Icon(Icons.edit, size: 16),
                  label: const Text('Modifier'),
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          // Souls
          if (_familyDetail == null)
            const Center(child: CircularProgressIndicator())
          else if (souls.isEmpty)
            Text('Aucune âme dans cette famille',
                style: TextStyle(color: Colors.white.withValues(alpha: 0.5)))
          else ...[
            Text('Âmes (${souls.length})',
                style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 15)),
            const SizedBox(height: 8),
            ...souls.map((s) {
              final soul = s as Map;
              final prenom = soul['prenom'] ?? '';
              final nomSoul = soul['nom'] ?? '';
              return GlassCard(
                margin: const EdgeInsets.only(bottom: 6),
                padding: const EdgeInsets.all(12),
                child: Row(
                  children: [
                    CircleAvatar(
                      radius: 16,
                      backgroundColor: const Color(0xFF7C3AED).withAlpha(50),
                      child: Text(
                        '${prenom}'.isNotEmpty ? '${prenom}'.substring(0, 1).toUpperCase() : '?',
                        style: const TextStyle(color: Color(0xFF7C3AED), fontWeight: FontWeight.bold, fontSize: 12),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Text('$prenom $nomSoul',
                          style: const TextStyle(color: Colors.white, fontSize: 14)),
                    ),
                  ],
                ),
              );
            }),
          ],
        ],
      ),
    );
  }

  // ==================== FORM VIEW (CREATE / EDIT) ====================

  Widget _buildForm() {
    final isEdit = _editingId != null;
    return Scaffold(
      appBar: AppBar(
        title: Text(isEdit ? 'Modifier la famille' : 'Nouvelle famille'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => setState(() { _view = ViewMode.list; _editingId = null; }),
        ),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            GlassCard(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Text('Nom de la famille *',
                      style: TextStyle(color: Colors.white70, fontSize: 13)),
                  const SizedBox(height: 6),
                  TextFormField(
                    controller: _nomCtrl,
                    style: const TextStyle(color: Colors.white),
                    decoration: InputDecoration(
                      hintText: 'Ex: Famille Mukendi',
                      hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.3)),
                      filled: true,
                      fillColor: Colors.white.withValues(alpha: 0.06),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(10),
                        borderSide: BorderSide.none,
                      ),
                    ),
                    validator: (v) => (v == null || v.trim().isEmpty) ? 'Le nom est obligatoire' : null,
                  ),
                  const SizedBox(height: 16),
                  const Text('Chef de famille',
                      style: TextStyle(color: Colors.white70, fontSize: 13)),
                  const SizedBox(height: 6),
                  DropdownButtonFormField<String>(
                    value: _chefFamilleId,
                    dropdownColor: const Color(0xFF1E293B),
                    style: const TextStyle(color: Colors.white),
                    decoration: InputDecoration(
                      hintText: 'Sélectionner...',
                      hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.3)),
                      filled: true,
                      fillColor: Colors.white.withValues(alpha: 0.06),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(10),
                        borderSide: BorderSide.none,
                      ),
                    ),
                    items: [
                      const DropdownMenuItem<String>(value: null, child: Text('— Aucun —')),
                      ..._users.map((u) {
                        final user = u as Map;
                        return DropdownMenuItem<String>(
                          value: user['id']?.toString(),
                          child: Text('${user['firstName'] ?? ''} ${user['lastName'] ?? ''}'),
                        );
                      }),
                    ],
                    onChanged: (v) => setState(() => _chefFamilleId = v),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: () => setState(() { _view = ViewMode.list; _editingId = null; }),
                    child: const Text('Annuler'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: isEdit ? () => _updateFamily(_editingId!) : _createFamily,
                    icon: Icon(isEdit ? Icons.save : Icons.add, size: 18),
                    label: Text(isEdit ? 'Enregistrer' : 'Créer'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFF7C3AED),
                      foregroundColor: Colors.white,
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  // ==================== WIDGET HELPERS ====================

  String dateStr(String d) => d.length >= 10 ? '${d.substring(5, 7)}/${d.substring(8, 10)}' : d;

  Widget _miniStat(IconData icon, String label, String value) {
    return Column(
      children: [
        Icon(icon, color: Colors.white.withValues(alpha: 0.4), size: 16),
        const SizedBox(height: 2),
        Text(value, style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w600)),
        Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 9)),
      ],
    );
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

  Widget _filterChip(String label, String value, Color color) {
    final isActive = _riskFilter == value;
    return FilterChip(
      label: Text(label, style: TextStyle(
        color: isActive ? Colors.white : color,
        fontSize: 12,
        fontWeight: FontWeight.w500,
      )),
      selected: isActive,
      onSelected: (_) {
        setState(() { _riskFilter = value; _page = 0; });
        _loadData();
      },
      backgroundColor: color.withAlpha(20),
      selectedColor: color.withAlpha(80),
      checkmarkColor: Colors.white,
      side: BorderSide(color: color.withAlpha(60)),
      padding: const EdgeInsets.symmetric(horizontal: 4),
      materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
    );
  }

  Widget _infoCard(String label, String value, Color color) {
    return Expanded(
      child: GlassCard(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
            const SizedBox(height: 4),
            Text(value, style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 13)),
          ],
        ),
      ),
    );
  }
}

enum ViewMode { list, detail, create, edit }
