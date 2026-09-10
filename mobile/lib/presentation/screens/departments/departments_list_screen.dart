import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';
import '../../../../l10n/app_localizations.dart';

class DepartmentsListScreen extends StatefulWidget {
  const DepartmentsListScreen({super.key});

  @override
  State<DepartmentsListScreen> createState() => _DepartmentsListScreenState();
}

class _DepartmentsListScreenState extends State<DepartmentsListScreen> {
  final _apiService = ApiService();
  List<dynamic> _departments = [];
  List<dynamic> _users = [];
  bool _isLoading = true;
  String _searchQuery = '';

  // Création état
  final _createNomCtrl = TextEditingController();
  final _createDescCtrl = TextEditingController();
  String? _createResponsableId;
  bool _createNewResponsable = false;
  final _createRespPrenomCtrl = TextEditingController();
  final _createRespNomCtrl = TextEditingController();
  final _createRespEmailCtrl = TextEditingController();
  final _createRespPhoneCtrl = TextEditingController();
  bool _isCreating = false;

  AppLocalizations get l10n => AppLocalizations.of(context);

  @override
  void initState() {
    super.initState();
    _loadData();
    _loadUsers();
  }

  @override
  void dispose() {
    _createNomCtrl.dispose();
    _createDescCtrl.dispose();
    _createRespPrenomCtrl.dispose();
    _createRespNomCtrl.dispose();
    _createRespEmailCtrl.dispose();
    _createRespPhoneCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadUsers() async {
    try {
      final res = await _apiService.get('/users', params: {'role': 'RESPONSABLE', 'size': '100'});
      if (mounted) {
        setState(() {
          _users = (res.data is Map ? res.data['content'] : res.data) as List<dynamic>? ?? [];
        });
      }
    } catch (_) {}
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final res = await _apiService.get('/departments', params: {'size': '50'});
      if (mounted) {
        setState(() {
          _departments = (res.data is Map ? res.data['content'] : res.data) as List<dynamic>? ?? [];
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _createDepartment() async {
    if (_createNomCtrl.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Le nom du département est requis')),
      );
      return;
    }
    if (!_createNewResponsable && _createResponsableId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Sélectionnez un responsable')),
      );
      return;
    }
    if (_createNewResponsable && (_createRespPrenomCtrl.text.trim().isEmpty || _createRespEmailCtrl.text.trim().isEmpty)) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Prénom et email du nouveau responsable sont requis')),
      );
      return;
    }

    setState(() => _isCreating = true);
    try {
      final data = <String, dynamic>{
        'nom': _createNomCtrl.text.trim(),
        'description': _createDescCtrl.text.trim(),
        'createNewResponsable': _createNewResponsable,
        if (!_createNewResponsable) 'responsableId': _createResponsableId,
        if (_createNewResponsable) ...{
          'newRespFirstName': _createRespPrenomCtrl.text.trim(),
          'newRespLastName': _createRespNomCtrl.text.trim(),
          'newRespEmail': _createRespEmailCtrl.text.trim(),
          'newRespPhone': _createRespPhoneCtrl.text.trim(),
        },
      };
      await _apiService.post('/departments', data: data);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Département créé avec succès'), backgroundColor: Color(0xFF2E7D32)),
        );
        _resetCreateForm();
        _loadData();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Erreur: $e'), backgroundColor: const Color(0xFFC62828)),
        );
      }
    } finally {
      if (mounted) setState(() => _isCreating = false);
    }
  }

  void _resetCreateForm() {
    _createNomCtrl.clear();
    _createDescCtrl.clear();
    _createResponsableId = null;
    _createNewResponsable = false;
    _createRespPrenomCtrl.clear();
    _createRespNomCtrl.clear();
    _createRespEmailCtrl.clear();
    _createRespPhoneCtrl.clear();
  }

  List<dynamic> get _filtered {
    if (_searchQuery.isEmpty) return _departments;
    return _departments.where((d) {
      final nom = (d as Map)['nom']?.toString().toLowerCase() ?? '';
      return nom.contains(_searchQuery.toLowerCase());
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Départements'),
        actions: [
          IconButton(icon: const Icon(Icons.add), onPressed: _showCreateSheet),
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
                  // Stats row
                  Row(
                    children: [
                      _statMini('Total', '${_departments.length}', Colors.blue),
                      const SizedBox(width: 8),
                      _statMini('Actifs', '${_departments.where((d) => (d as Map)['actif'] != false).length}', Colors.green),
                    ],
                  ),
                  const SizedBox(height: 12),
                  // Search bar
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12),
                    decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.06),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: TextField(
                      onChanged: (v) => setState(() => _searchQuery = v),
                      style: const TextStyle(color: Colors.white),
                      decoration: InputDecoration(
                        hintText: 'Rechercher un département...',
                        hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.4)),
                        prefixIcon: Icon(Icons.search, color: Colors.white.withValues(alpha: 0.4), size: 20),
                        border: InputBorder.none,
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                  // Department cards
                  if (_filtered.isEmpty)
                    GlassCard(
                      padding: const EdgeInsets.all(32),
                      child: Column(
                        children: [
                          Icon(Icons.business_outlined, size: 48, color: Colors.white.withValues(alpha: 0.3)),
                          const SizedBox(height: 12),
                          Text('Aucun département', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                        ],
                      ),
                    )
                  else
                    ..._filtered.map((d) {
                      final dept = d as Map;
                      final nom = dept['nom'] ?? 'Département';
                      final desc = dept['description'] ?? '';
                      final statut = dept['statut'] ?? 'ACTIF';
                      final responsable = dept['responsableNom'] ?? '—';
                      final nbMembres = dept['nombreMembres'] ?? '—';
                      return GlassCard(
                        margin: const EdgeInsets.only(bottom: 10),
                        padding: const EdgeInsets.all(14),
                        onTap: () => context.go('/departments/${dept['id']}'),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Container(
                                  padding: const EdgeInsets.all(10),
                                  decoration: BoxDecoration(
                                    color: Colors.amber.withValues(alpha: 0.15),
                                    borderRadius: BorderRadius.circular(12),
                                  ),
                                  child: const Icon(Icons.business, color: Colors.amber, size: 22),
                                ),
                                const SizedBox(width: 12),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(nom, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 15)),
                                      if (responsable != '—')
                                        Text('Resp: $responsable', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                                    ],
                                  ),
                                ),
                                StatusBadge(
                                  label: statut,
                                  color: statut == 'ACTIF' ? Colors.green : Colors.grey,
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
                                Icon(Icons.people, size: 14, color: Colors.white.withValues(alpha: 0.4)),
                                const SizedBox(width: 4),
                                Text('$nbMembres membres', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                              ],
                            ),
                          ],
                        ),
                      );
                    }),
                ],
              ),
            ),
    );
  }

  void _showCreateSheet() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setSheetState) => Container(
          height: MediaQuery.of(ctx).size.height * 0.85,
          decoration: const BoxDecoration(
            color: Color(0xFF1E2A4A),
            borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
          ),
          child: SafeArea(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(20),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Center(
                    child: Container(width: 32, height: 4, decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.2), borderRadius: BorderRadius.circular(2))),
                  ),
                  const SizedBox(height: 16),
                  const Text('Nouveau département', style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 16),
                  _field(_createNomCtrl, 'Nom du département *'),
                  const SizedBox(height: 12),
                  _field(_createDescCtrl, 'Description'),
                  const SizedBox(height: 16),
                  const Text('Responsable', style: TextStyle(color: Colors.white70, fontSize: 14)),
                  const SizedBox(height: 8),
                  SegmentedButton<bool>(
                    segments: const [
                      ButtonSegment(value: false, label: Text('Existant', style: TextStyle(fontSize: 12))),
                      ButtonSegment(value: true, label: Text('Nouveau', style: TextStyle(fontSize: 12))),
                    ],
                    selected: {_createNewResponsable},
                    onSelectionChanged: (v) => setSheetState(() {
                      _createNewResponsable = v.first;
                      _createResponsableId = null;
                    }),
                    style: ButtonStyle(visualDensity: VisualDensity.compact),
                  ),
                  const SizedBox(height: 12),
                  if (!_createNewResponsable)
                    DropdownButtonFormField<String>(
                      value: _createResponsableId,
                      dropdownColor: const Color(0xFF1E2A4A),
                      style: const TextStyle(color: Colors.white, fontSize: 14),
                      decoration: InputDecoration(
                        labelText: 'Sélectionner un responsable',
                        labelStyle: TextStyle(color: Colors.white.withValues(alpha: 0.5)),
                        filled: true,
                        fillColor: Colors.white.withValues(alpha: 0.06),
                        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
                      ),
                      items: _users.map((u) => DropdownMenuItem<String>(
                        value: u['id'] as String,
                        child: Text('${u['firstName'] ?? ''} ${u['lastName'] ?? ''}'),
                      )).toList(),
                      onChanged: (v) => setSheetState(() => _createResponsableId = v),
                    )
                  else ...[
                    _field(_createRespPrenomCtrl, 'Prénom du responsable *'),
                    const SizedBox(height: 12),
                    _field(_createRespNomCtrl, 'Nom du responsable'),
                    const SizedBox(height: 12),
                    _field(_createRespEmailCtrl, 'Email du responsable *'),
                    const SizedBox(height: 12),
                    _field(_createRespPhoneCtrl, 'Téléphone'),
                  ],
                  const SizedBox(height: 20),
                  Row(mainAxisAlignment: MainAxisAlignment.end, children: [
                    TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
                    const SizedBox(width: 8),
                    FilledButton.icon(
                      onPressed: _isCreating ? null : () { _createDepartment(); Navigator.pop(ctx); },
                      icon: _isCreating ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2)) : const Icon(Icons.add, size: 18),
                      label: const Text('Créer'),
                    ),
                  ]),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _field(TextEditingController controller, String label) {
    return TextField(
      controller: controller,
      style: const TextStyle(color: Colors.white),
      decoration: InputDecoration(
        labelText: label,
        labelStyle: TextStyle(color: Colors.white.withValues(alpha: 0.5)),
        filled: true,
        fillColor: Colors.white.withValues(alpha: 0.06),
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
      ),
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
}
