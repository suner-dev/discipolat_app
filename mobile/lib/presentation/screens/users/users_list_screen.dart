import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../app.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';


class UsersListScreen extends StatefulWidget {
  const UsersListScreen({super.key});

  @override
  State<UsersListScreen> createState() => _UsersListScreenState();
}

class _UsersListScreenState extends State<UsersListScreen> {
  final _apiService = ApiService();
  List<dynamic> _users = [];
  List<dynamic> _workload = [];
  bool _isLoading = true;
  bool _showCreate = false;
  bool _showWorkload = true;

  final _createEmailCtrl = TextEditingController();
  final _createFirstNameCtrl = TextEditingController();
  final _createLastNameCtrl = TextEditingController();
  String _createRole = 'FAISEUR';
  bool _isProcessing = false;

  static const _roleLabels = {
    'ADMIN': 'Administrateur',
    'PASTEUR': 'Pasteur',
    'RESPONSABLE': 'Responsable',
    'CHEF_DE_FAMILLE': 'Chef de famille',
    'FAISEUR': 'Faiseur',
    'MEMBRE': 'Membre',
  };

  static const _roleBadgeColors = {
    'ADMIN': Color(0xFF3B82F6),
    'PASTEUR': Color(0xFF22C55E),
    'RESPONSABLE': Color(0xFFF59E0B),
    'CHEF_DE_FAMILLE': Color(0xFFD4AF37),
    'FAISEUR': Color(0xFF14B8A6),
    'MEMBRE': Color(0xFF6B7280),
  };

  @override
  void initState() { super.initState(); _loadData(); }

  @override
  void dispose() {
    _createEmailCtrl.dispose();
    _createFirstNameCtrl.dispose();
    _createLastNameCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final usersRes = await _apiService.get('/users', params: {'size': '50'});
      final workloadRes = await _apiService.get('/users/faiseur-workload');
      if (mounted) {
        _users = (usersRes.data['content'] as List?) ?? [];
        _workload = (workloadRes.data as List?) ?? [];
        setState(() => _isLoading = false);
      }
    } catch (_) { if (mounted) setState(() => _isLoading = false); }
  }

  Future<void> _createUser() async {
    setState(() => _isProcessing = true);
    try {
      await _apiService.post('/users', data: {
        'email': _createEmailCtrl.text.trim(),
        'password': 'password123',
        'firstName': _createFirstNameCtrl.text.trim(),
        'lastName': _createLastNameCtrl.text.trim(),
        'role': _createRole,
      });
      _createEmailCtrl.clear(); _createFirstNameCtrl.clear(); _createLastNameCtrl.clear();
      _createRole = 'FAISEUR';
      setState(() => _showCreate = false);
      _loadData();
      if (mounted) _showSnack('Compte créé avec succès');
    } catch (_) { if (mounted) _showSnack('Erreur lors de la création'); }
    finally { if (mounted) setState(() => _isProcessing = false); }
  }

  void _showSnack(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
  }

  Color _roleColor(String role) => _roleBadgeColors[role] ?? Colors.grey;

  void _showActionModal(Map<String, dynamic> user, String action) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => _ActionModal(
        user: user,
        action: action,
        roleLabels: _roleLabels,
        apiService: _apiService,
        onDone: () { Navigator.pop(ctx); _loadData(); },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Utilisateurs'),
        actions: [
          IconButton(icon: const Icon(Icons.add), onPressed: () => setState(() => _showCreate = !_showCreate)),
          IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 6)
          : RefreshIndicator(
              onRefresh: _loadData,
              child: SingleChildScrollView(
                physics: const AlwaysScrollableScrollPhysics(),
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (_showCreate) _buildCreateForm(),
                    if (_workload.isNotEmpty && _showWorkload) _buildWorkloadSection(),
                    if (_users.isEmpty)
                      GlassCard(
                        padding: const EdgeInsets.all(32),
                        child: Column(children: [
                          Icon(Icons.people_outline, size: 48, color: Colors.white.withValues(alpha: 0.15)),
                          const SizedBox(height: 12),
                          Text('Aucun utilisateur',
                              style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 16)),
                        ]),
                      )
                    else
                      ..._users.asMap().entries.map((entry) => _buildUserCard(entry.value, entry.key)),
                    const SizedBox(height: 32),
                  ],
                ),
              ),
            ),
    );
  }

  Widget _buildCreateForm() {
    return GlassCard(
      padding: const EdgeInsets.all(16),
      margin: const EdgeInsets.only(bottom: 16),
      borderColor: AppColors.primary.withValues(alpha: 0.3),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          Icon(Icons.person_add, color: AppColors.primary, size: 18),
          const SizedBox(width: 8),
          const Text('Nouvel utilisateur', style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
        ]),
        const SizedBox(height: 16),
        TextField(controller: _createFirstNameCtrl, decoration: const InputDecoration(labelText: 'Prénom', hintText: 'Jean')),
        const SizedBox(height: 10),
        TextField(controller: _createLastNameCtrl, decoration: const InputDecoration(labelText: 'Nom', hintText: 'Dupont')),
        const SizedBox(height: 10),
        TextField(controller: _createEmailCtrl, keyboardType: TextInputType.emailAddress,
            decoration: const InputDecoration(labelText: 'Email', hintText: 'jean@email.com')),
        const SizedBox(height: 10),
        DropdownButtonFormField<String>(
          value: _createRole, dropdownColor: const Color(0xFF111827),
          decoration: const InputDecoration(labelText: 'Rôle'),
          items: ['FAISEUR', 'RESPONSABLE', 'PASTEUR', 'ADMIN'].map((r) =>
            DropdownMenuItem(value: r, child: Text(_roleLabels[r] ?? r)),
          ).toList(),
          onChanged: (v) => setState(() => _createRole = v ?? 'FAISEUR'),
        ),
        const SizedBox(height: 16),
        Row(mainAxisAlignment: MainAxisAlignment.end, children: [
          TextButton(onPressed: () => setState(() => _showCreate = false), child: const Text('Annuler')),
          const SizedBox(width: 8),
          FilledButton.icon(
            onPressed: _createUser,
            icon: _isProcessing
                ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                : const Icon(Icons.person_add, size: 16),
            label: const Text('Créer'),
          ),
        ]),
      ]),
    );
  }

  Widget _buildWorkloadSection() {
    // Le backend scope la charge de travail par rôle actif :
    // responsable → faiseurs de ses départements ; super-utilisateurs → tous les faiseurs.
    final isResponsable = AuthState().activeRole == 'RESPONSABLE';
    final title = isResponsable ? 'Charge de travail de mon département' : 'Charge de travail des Faiseurs';
    return GlassCard(
      padding: const EdgeInsets.all(16),
      margin: const EdgeInsets.only(bottom: 16),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          Icon(Icons.bar_chart, color: AppColors.primary, size: 18),
          const SizedBox(width: 8),
          Expanded(
            child: Text(title, style: const TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.bold)),
          ),
          GestureDetector(onTap: () => setState(() => _showWorkload = false),
              child: Icon(Icons.expand_less, color: Colors.white38, size: 20)),
        ]),
        const SizedBox(height: 12),
        ..._workload.take(6).map((w) {
          final charge = (w['charge'] as String?) ?? '';
          final chargeColor = charge == 'SURCHARGÉ'
              ? Colors.redAccent
              : charge == 'LEGER' ? Colors.greenAccent : AppColors.primary;
          return Container(
            margin: const EdgeInsets.only(bottom: 6),
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: 0.04),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Row(children: [
              Expanded(child: Text('${w['faiseurName'] ?? '—'}',
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 12))),
              if (charge.isNotEmpty)
                Container(
                  margin: const EdgeInsets.only(right: 6),
                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                  decoration: BoxDecoration(
                    color: chargeColor.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Text(
                    charge == 'SURCHARGÉ' ? 'Surchargé' : charge == 'LEGER' ? 'Léger' : 'Normal',
                    style: TextStyle(color: chargeColor, fontSize: 9, fontWeight: FontWeight.w600),
                  ),
                ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                decoration: BoxDecoration(
                  color: AppColors.primary.withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text('${w['soulCount'] ?? 0} âmes',
                    style: const TextStyle(color: AppColors.primary, fontSize: 10, fontWeight: FontWeight.w600)),
              ),
            ]),
          );
        }),
      ]),
    );
  }

  Widget _buildUserCard(Map<String, dynamic> u, int index) {
    final role = (u['role'] as String?) ?? 'MEMBRE';
    final isActive = (u['statut'] as String?) == 'ACTIVE';
    final color = _roleColor(role);
    final name = '${u['firstName'] ?? ''} ${u['lastName'] ?? ''}';

    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          Container(
            width: 40, height: 40,
            decoration: BoxDecoration(
              gradient: LinearGradient(colors: [color, color.withValues(alpha: 0.7)]),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Center(child: Text(
              '${(name.isNotEmpty ? name[0] : '?')}${name.length > 1 ? name.split(' ').last[0] : ''}',
              style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 14),
            )),
          ),
          const SizedBox(width: 12),
          Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(name.trim().isEmpty ? '—' : name,
                style: const TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w600)),
            Text('${u['email'] ?? ''}',
                style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
          ])),
          Container(
            width: 8, height: 8,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: isActive ? Colors.green : Colors.grey,
              boxShadow: isActive ? [BoxShadow(color: Colors.green.withValues(alpha: 0.5), blurRadius: 4)] : null,
            ),
          ),
        ]),
        const SizedBox(height: 8),
        Row(children: [
          _badge(color, _roleLabels[role] ?? role),
          const SizedBox(width: 6),
          _badge(isActive ? Colors.green : Colors.grey, isActive ? 'Actif' : 'Inactif'),
          if (u['estChefDeFamille'] == true) ...[
            const SizedBox(width: 6),
            _badge(const Color(0xFFD4AF37), 'Chef'),
          ],
        ]),
        if (role == 'FAISEUR' || role == 'RESPONSABLE' || role == 'PASTEUR')
          Padding(
            padding: const EdgeInsets.only(top: 8),
            child: Row(children: [
              if (role == 'FAISEUR') ...[
                _actionBtn(Icons.arrow_downward, () => _showActionModal(u, 'demote'), Colors.amber),
                const SizedBox(width: 4),
                _actionBtn(Icons.swap_horiz, () => _showActionModal(u, 'transfer'), Colors.blue),
              ],
              if (role != 'FAISEUR' && role != 'ADMIN') ...[
                const SizedBox(width: 4),
                _actionBtn(Icons.arrow_upward, () => _showActionModal(u, 'promote'), Colors.green),
              ],
              const SizedBox(width: 4),
              _actionBtn(Icons.delete_forever, () => _showActionModal(u, 'hardDelete'), Colors.red),
            ]),
          ),
      ]),
    );
  }

  Widget _badge(Color color, String label) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(color: color.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(8)),
      child: Text(label, style: TextStyle(color: color, fontSize: 9, fontWeight: FontWeight.w600)),
    );
  }

  Widget _actionBtn(IconData icon, VoidCallback onTap, Color color) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(6),
        decoration: BoxDecoration(color: color.withValues(alpha: 0.1), borderRadius: BorderRadius.circular(8)),
        child: Icon(icon, color: color, size: 16),
      ),
    );
  }
}

// ============================================================
// ACTION MODAL BOTTOM SHEET
// ============================================================

class _ActionModal extends StatefulWidget {
  final Map<String, dynamic> user;
  final String action;
  final Map<String, String> roleLabels;
  final ApiService apiService;
  final VoidCallback onDone;

  const _ActionModal({
    required this.user, required this.action, required this.roleLabels,
    required this.apiService, required this.onDone,
  });

  @override
  State<_ActionModal> createState() => _ActionModalState();
}

class _ActionModalState extends State<_ActionModal> {
  bool _isProcessing = false;
  String _demoteRole = 'FAISEUR';
  String _transferFamilleId = '';
  bool _transferAmes = false;
  List<dynamic> _families = [];
  Map<String, dynamic>? _history;

  @override
  void initState() {
    super.initState();
    if (widget.action == 'transfer') _loadFamilies();
    if (widget.action == 'history') _loadHistory();
  }

  Future<void> _loadFamilies() async {
    try {
      final res = await widget.apiService.get('/families', params: {'size': '100'});
      if (mounted) _families = (res.data['content'] as List?) ?? [];
    } catch (_) {}
  }

  Future<void> _loadHistory() async {
    setState(() => _isProcessing = true);
    try {
      final res = await widget.apiService.get('/users/${widget.user['id']}/faiseur-history');
      if (mounted) _history = res.data as Map<String, dynamic>?;
    } catch (_) {}
    finally { if (mounted) setState(() => _isProcessing = false); }
  }

  Future<void> _execute() async {
    setState(() => _isProcessing = true);
    try {
      final id = widget.user['id'] as String;
      String? statutDemande;
      if (widget.action == 'promote') await widget.apiService.patch('/users/$id/promote-faiseur');
      else if (widget.action == 'demote') await widget.apiService.patch('/users/$id/demote', data: {'newRole': _demoteRole});
      else if (widget.action == 'transfer') {
        final res = await widget.apiService.patch('/users/$id/transfer', data: {
          'nouvelleFamilleId': _transferFamilleId, 'transfererAmes': _transferAmes,
        });
        statutDemande = (res.data is Map) ? (res.data as Map)['statut'] as String? : null;
      }
      else if (widget.action == 'hardDelete') await widget.apiService.delete('/users/$id/hard-delete');
      widget.onDone();
      if (mounted && widget.action == 'transfer') {
        final message = statutDemande == 'EXECUTE'
            ? 'Faiseur transféré'
            : 'Demande de transfert soumise pour validation';
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
      }
    } catch (_) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Erreur')));
    } finally { if (mounted) setState(() => _isProcessing = false); }
  }

  @override
  Widget build(BuildContext context) {
    final name = '${widget.user['firstName'] ?? ''} ${widget.user['lastName'] ?? ''}';

    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFF111827),
        borderRadius: const BorderRadius.vertical(top: Radius.circular(20)),
        border: Border(top: BorderSide(color: Colors.white.withValues(alpha: 0.06))),
      ),
      child: SafeArea(
        child: Padding(
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
              Row(children: [
                Icon(
                  widget.action == 'promote' ? Icons.arrow_upward
                      : widget.action == 'demote' ? Icons.arrow_downward
                      : widget.action == 'transfer' ? Icons.swap_horiz
                      : widget.action == 'history' ? Icons.history
                      : Icons.delete_forever,
                  color: widget.action == 'hardDelete' ? Colors.red : AppColors.primary, size: 20,
                ),
                const SizedBox(width: 8),
                Text(
                  widget.action == 'promote' ? 'Promouvoir en Faiseur'
                      : widget.action == 'demote' ? 'Rétrograder'
                      : widget.action == 'transfer' ? 'Transférer le Faiseur'
                      : widget.action == 'history' ? 'Historique'
                      : 'Suppression définitive',
                  style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold),
                ),
              ]),
              const SizedBox(height: 16),

              if (widget.action == 'history' && _history != null)
                SizedBox(
                  height: 300,
                  child: SingleChildScrollView(
                    child: Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: Colors.white.withValues(alpha: 0.04),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Text(
                        _history.toString(),
                        style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 11, fontFamily: 'monospace'),
                      ),
                    ),
                  ),
                )
              else if (widget.action == 'transfer')
                ..._buildTransferContent()
              else if (widget.action == 'demote')
                _buildDemoteContent(name)
              else
                Text(
                  widget.action == 'promote'
                      ? 'Promouvoir $name au rôle de Faiseur de disciples ?'
                      : 'Supprimer définitivement $name ? Cette action est irréversible (RGPD).',
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 13),
                ),

              if (widget.action != 'history') ...[
                const SizedBox(height: 16),
                Row(mainAxisAlignment: MainAxisAlignment.end, children: [
                  TextButton(onPressed: () => Navigator.pop(context), child: const Text('Annuler')),
                  const SizedBox(width: 8),
                  FilledButton.icon(
                    onPressed: _execute,
                    icon: _isProcessing
                        ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                        : Icon(widget.action == 'hardDelete' ? Icons.delete_forever : Icons.check, size: 16),
                    label: Text(
                      widget.action == 'promote' ? 'Promouvoir'
                          : widget.action == 'demote' ? 'Rétrograder'
                          : widget.action == 'transfer' ? 'Transférer'
                          : 'Supprimer',
                    ),
                    style: FilledButton.styleFrom(
                      backgroundColor: widget.action == 'hardDelete' ? Colors.red : AppColors.primary,
                    ),
                  ),
                ]),
              ],
            ],
          ),
        ),
      ),
    );
  }

  List<Widget> _buildTransferContent() {
    return [
      Text('Transférer ${widget.user['firstName'] ?? ''} ${widget.user['lastName'] ?? ''} vers :',
          style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 13)),
      const SizedBox(height: 8),
      DropdownButtonFormField<String>(
        value: _transferFamilleId.isEmpty ? null : _transferFamilleId,
        dropdownColor: const Color(0xFF111827),
        decoration: const InputDecoration(hintText: 'Sélectionner une famille...'),
        items: _families.map((f) =>
          DropdownMenuItem(value: f['id'] as String, child: Text(f['nom'] as String? ?? '')),
        ).toList(),
        onChanged: (v) => setState(() => _transferFamilleId = v ?? ''),
      ),
      const SizedBox(height: 8),
      CheckboxListTile(
        contentPadding: EdgeInsets.zero,
        title: Text('Transférer également les âmes suivies',
            style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 12)),
        value: _transferAmes,
        onChanged: (v) => setState(() => _transferAmes = v ?? false),
        dense: true,
        activeColor: AppColors.primary,
      ),
    ];
  }

  Widget _buildDemoteContent(String name) {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Text('Rétrograder $name vers :',
          style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 13)),
      const SizedBox(height: 8),
      DropdownButtonFormField<String>(
        value: _demoteRole,
        dropdownColor: const Color(0xFF111827),
        decoration: const InputDecoration(),
        items: ['RESPONSABLE', 'FAISEUR'].map((r) =>
          DropdownMenuItem(value: r, child: Text(widget.roleLabels[r] ?? r)),
        ).toList(),
        onChanged: (v) => setState(() => _demoteRole = v ?? 'FAISEUR'),
      ),
    ]);
  }
}
