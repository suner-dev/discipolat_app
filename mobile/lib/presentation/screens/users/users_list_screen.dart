import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../data/services/api_service.dart';
import '../../../app.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../l10n/app_localizations.dart';


class UsersListScreen extends StatefulWidget {
  const UsersListScreen({super.key, this.apiService});

  /// Permet d'injecter un ApiService mocké dans les tests widget.
  final ApiService? apiService;

  @override
  State<UsersListScreen> createState() => _UsersListScreenState();
}

class _UsersListScreenState extends State<UsersListScreen> {
  late final ApiService _apiService = widget.apiService ?? ApiService();
  List<dynamic> _users = [];
  List<dynamic> _workload = [];
  Map<String, dynamic> _evalScores = {};
  bool _isLoading = true;
  bool _showCreate = false;
  bool _showWorkload = true;

  final _createEmailCtrl = TextEditingController();
  final _createFirstNameCtrl = TextEditingController();
  final _createLastNameCtrl = TextEditingController();
  String _createRole = 'FAISEUR';
  bool _isProcessing = false;

  AppLocalizations get l10n => AppLocalizations.of(context);

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
      final users = (usersRes.data['content'] as List?) ?? [];
      Map<String, dynamic> evalScores = {};
      // Scores d'évaluation agrégés (une requête groupée pour toute la page).
      final ids = users.map((u) => (u as Map<String, dynamic>)['id']?.toString()).whereType<String>().toList();
      if (ids.isNotEmpty) {
        try {
          final scoresRes = await _apiService.get('/users/evaluation-scores',
              params: {'userIds': ids.join(',')});
          evalScores = (scoresRes.data as Map<String, dynamic>?) ?? {};
        } catch (_) {/* best-effort : la moyenne n'empêche pas l'affichage de la liste */}
      }
      if (mounted) {
        _users = users;
        _workload = (workloadRes.data as List?) ?? [];
        _evalScores = evalScores;
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
      if (mounted) _showSnack(l10n.usersListAccountCreated);
    } catch (_) { if (mounted) _showSnack(l10n.usersListCreateError); }
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
        title: Text(l10n.usersListTitle),
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
                          Text(l10n.usersListEmpty,
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
          Text(l10n.usersListNewUser, style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
        ]),
        const SizedBox(height: 16),
        TextField(controller: _createFirstNameCtrl, decoration: InputDecoration(labelText: l10n.usersListFirstName, hintText: 'Jean')),
        const SizedBox(height: 10),
        TextField(controller: _createLastNameCtrl, decoration: InputDecoration(labelText: l10n.usersListLastName, hintText: 'Dupont')),
        const SizedBox(height: 10),
        TextField(controller: _createEmailCtrl, keyboardType: TextInputType.emailAddress,
            decoration: InputDecoration(labelText: l10n.usersListEmail, hintText: 'jean@email.com')),
        const SizedBox(height: 10),
        DropdownButtonFormField<String>(
          initialValue: _createRole, dropdownColor: const Color(0xFF111827),
          decoration: InputDecoration(labelText: l10n.usersListRole),
          items: ['FAISEUR', 'RESPONSABLE', 'PASTEUR', 'ADMIN'].map((r) =>
            DropdownMenuItem(value: r, child: Text(_roleLabels[r] ?? r)),
          ).toList(),
          onChanged: (v) => setState(() => _createRole = v ?? 'FAISEUR'),
        ),
        const SizedBox(height: 16),
        Row(mainAxisAlignment: MainAxisAlignment.end, children: [
          TextButton(onPressed: () => setState(() => _showCreate = false), child: Text(l10n.usersListAnnuler)),
          const SizedBox(width: 8),
          FilledButton.icon(
            onPressed: _createUser,
            icon: _isProcessing
                ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                : const Icon(Icons.person_add, size: 16),
            label: Text(l10n.usersListCreateBtn),
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
                    charge == 'SURCHARGÉ' ? l10n.usersListSurcharged : charge == 'LEGER' ? l10n.usersListLight : l10n.usersListNormal,
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
                    style: TextStyle(color: AppColors.primary, fontSize: 10, fontWeight: FontWeight.w600)),
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

    return GestureDetector(
      onTap: () => context.go('/users/${u['id']}'),
      child: GlassCard(
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
            _badge(isActive ? Colors.green : Colors.grey, isActive ? l10n.usersListActiveLabel : l10n.usersListInactiveLabel),
            if (u['estChefDeFamille'] == true) ...[
              const SizedBox(width: 6),
              _badge(const Color(0xFFD4AF37), l10n.usersListChefBadge),
            ],
            if (_evalBadge(u['id']) != null) ...[
              const SizedBox(width: 6),
              _evalBadge(u['id'])!,
            ],
            const Spacer(),
            _actionBtn(Icons.person, () => context.go('/users/${u['id']}'), AppColors.primary, tooltip: l10n.usersListActionProfile),
            if (role == 'FAISEUR') ...[
              const SizedBox(width: 4),
              _actionBtn(Icons.history, () => _showActionModal(u, 'history'), const Color(0xFFA855F7), tooltip: l10n.usersListActionHistory),
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
      ),
    );
  }

  /// Badge « moyenne d'évaluation » (parité web : étoiles + note + compteur).
  Widget? _evalBadge(dynamic userId) {
    final scores = _evalScores[userId] as Map<String, dynamic>?;
    if (scores == null || scores.isEmpty) return null;
    final values = scores.values.whereType<Map<String, dynamic>>().toList();
    if (values.isEmpty) return null;
    double totalAvg = values.fold<double>(0, (acc, s) => acc + ((s['moyenne'] as num?) ?? 0));
    totalAvg = totalAvg / values.length;
    final totalCount = values.fold<int>(0, (acc, s) => acc + ((s['total'] as num?) ?? 0).toInt());
    final rounded = totalAvg.round();
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 3),
      decoration: BoxDecoration(
        color: const Color(0xFFD4AF37).withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: const Color(0xFFD4AF37).withValues(alpha: 0.3)),
      ),
      child: Row(mainAxisSize: MainAxisSize.min, children: [
        ...List.generate(5, (i) => Icon(
          i < rounded ? Icons.star_rounded : Icons.star_border_rounded,
          size: 11,
          color: i < rounded ? const Color(0xFFF59E0B) : Colors.white24,
        )),
        const SizedBox(width: 3),
        Text(
          totalAvg > 0 ? totalAvg.toStringAsFixed(1) : '—',
          style: const TextStyle(color: Color(0xFFF59E0B), fontSize: 10, fontWeight: FontWeight.w700),
        ),
        if (totalCount > 0) ...[
          const SizedBox(width: 2),
          Text('($totalCount)', style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 9)),
        ],
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

  Widget _actionBtn(IconData icon, VoidCallback onTap, Color color, {String? tooltip}) {
    return Tooltip(
      message: tooltip ?? '',
      child: GestureDetector(
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.all(6),
          decoration: BoxDecoration(color: color.withValues(alpha: 0.1), borderRadius: BorderRadius.circular(8)),
          child: Icon(icon, color: color, size: 16),
        ),
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
  AppLocalizations get l10n => AppLocalizations.of(context);
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
      if (widget.action == 'promote') {
        await widget.apiService.patch('/users/$id/promote-faiseur');
      } else if (widget.action == 'demote') {
        await widget.apiService.patch('/users/$id/demote', data: {'newRole': _demoteRole});
      } else if (widget.action == 'transfer') {
        final res = await widget.apiService.patch('/users/$id/transfer', data: {
          'nouvelleFamilleId': _transferFamilleId, 'transfererAmes': _transferAmes,
        });
        statutDemande = (res.data is Map) ? (res.data as Map)['statut'] as String? : null;
      } else if (widget.action == 'hardDelete') {
        await widget.apiService.delete('/users/$id/hard-delete');
      }
      widget.onDone();
      if (mounted && widget.action == 'transfer') {
        final message = statutDemande == 'EXECUTE'
            ? l10n.usersListTransferExecuted
            : l10n.usersListTransferPending;
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
      }
    } catch (_) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(l10n.usersListError)));
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
                  widget.action == 'promote' ? l10n.usersListPromoteFaiseur
                      : widget.action == 'demote' ? l10n.usersListDemoteTitle
                      : widget.action == 'transfer' ? l10n.usersListTransferTitle
                      : widget.action == 'history' ? l10n.usersListHistoryTitle
                      : l10n.usersListDeleteTitle,
                  style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold),
                ),
              ]),
              const SizedBox(height: 16),

              if (widget.action == 'history')
                _buildHistoryContent()
              else if (widget.action == 'transfer')
                ..._buildTransferContent()
              else if (widget.action == 'demote')
                _buildDemoteContent(name)
              else
                Text(
                  widget.action == 'promote'
                      ? l10n.usersListPromoteConfirm.replaceAll('{name}', name)
                      : l10n.usersListDeleteConfirmHard.replaceAll('{name}', name),
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 13),
                ),

              if (widget.action != 'history') ...[
                const SizedBox(height: 16),
                Row(mainAxisAlignment: MainAxisAlignment.end, children: [
                  TextButton(onPressed: () => Navigator.pop(context), child: Text(l10n.usersListAnnuler)),
                  const SizedBox(width: 8),
                  FilledButton.icon(
                    onPressed: _execute,
                    icon: _isProcessing
                        ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                        : Icon(widget.action == 'hardDelete' ? Icons.delete_forever : Icons.check, size: 16),
                    label: Text(
                      widget.action == 'promote' ? l10n.usersListPromoteBtn
                          : widget.action == 'demote' ? l10n.usersListDemoteBtn
                          : widget.action == 'transfer' ? l10n.usersListTransferBtn
                          : l10n.usersListDeleteBtn,
                    ),
                    style: FilledButton.styleFrom(
                      backgroundColor: widget.action == 'hardDelete' ? Colors.red : AppColors.primary,
                    ),
                  ),
                ]),
              ] else ...[
                const SizedBox(height: 16),
                Row(mainAxisAlignment: MainAxisAlignment.end, children: [
                  TextButton(
                    onPressed: () => Navigator.pop(context),
                    child: Text(l10n.usersListHistoryClose),
                  ),
                ]),
              ],
            ],
          ),
        ),
      ),
    );
  }

  // ============================================================
  // HISTORIQUE STYLISÉ (parcours du faiseur + âmes suivies)
  // ============================================================

  String _formatDate(dynamic date) {
    if (date == null) return '—';
    final s = date.toString();
    return s.length >= 10 ? '${s.substring(8, 10)}/${s.substring(5, 7)}/${s.substring(0, 4)}' : s;
  }

  String _statutLabel(String? statut) {
    switch (statut) {
      case 'ACTIF': return 'Actif';
      case 'EN_INTEGRATION': return 'Intégration';
      case 'EN_VEILLE': return 'Veille';
      case 'DECROCHE': return 'Décroché';
      default: return statut ?? '—';
    }
  }

  Color _statutColor(String? statut) {
    switch (statut) {
      case 'ACTIF': return Colors.greenAccent;
      case 'EN_INTEGRATION': return Colors.amber;
      case 'EN_VEILLE': return Colors.lightBlueAccent;
      case 'DECROCHE': return Colors.redAccent;
      default: return Colors.white70;
    }
  }

  Widget _buildHistoryContent() {
    if (_history == null) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 32),
        child: Center(child: CircularProgressIndicator(strokeWidth: 2)),
      );
    }
    final h = _history!;
    final ames = (h['amesActuelles'] as List?) ?? [];
    final sorties = (h['sorties'] as List?) ?? [];

    return SizedBox(
      height: 320,
      child: SingleChildScrollView(
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          // Résumé du parcours
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              gradient: LinearGradient(colors: [
                const Color(0xFF7C3AED).withValues(alpha: 0.25),
                const Color(0xFF6D28D9).withValues(alpha: 0.10),
              ]),
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: const Color(0xFFA855F7).withValues(alpha: 0.3)),
            ),
            child: Row(children: [
              Expanded(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text(l10n.usersListHistoryRole, style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 10)),
                  const SizedBox(height: 2),
                  Text(widget.roleLabels[h['role']] ?? h['role']?.toString() ?? '—',
                      style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w600)),
                  if (h['estChef'] == true) ...[
                    const SizedBox(height: 4),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                      decoration: BoxDecoration(
                        color: const Color(0xFFD4AF37).withValues(alpha: 0.15),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: const Text('Chef de famille',
                          style: TextStyle(color: Color(0xFFD4AF37), fontSize: 9, fontWeight: FontWeight.w600)),
                    ),
                  ],
                ]),
              ),
              Column(crossAxisAlignment: CrossAxisAlignment.end, children: [
                Text(l10n.usersListHistorySince, style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 10)),
                const SizedBox(height: 2),
                Text(_formatDate(h['dateCreation']),
                    style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w600)),
              ]),
            ]),
          ),
          const SizedBox(height: 14),

          // Âmes actuellement suivies
          Row(children: [
            const Icon(Icons.favorite, color: Colors.greenAccent, size: 15),
            const SizedBox(width: 6),
            Expanded(
              child: Text(l10n.usersListHistoryCurrentSouls,
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 11, fontWeight: FontWeight.w600)),
            ),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
              decoration: BoxDecoration(color: Colors.green.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(8)),
              child: Text('${h['nombreAmesActuelles'] ?? ames.length}',
                  style: const TextStyle(color: Colors.greenAccent, fontSize: 10, fontWeight: FontWeight.w700)),
            ),
          ]),
          const SizedBox(height: 8),
          if (ames.isEmpty)
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.03), borderRadius: BorderRadius.circular(12)),
              child: Center(
                child: Text(l10n.usersListHistoryNoSouls,
                    style: TextStyle(color: Colors.white.withValues(alpha: 0.35), fontSize: 12)),
              ),
            )
          else
            ...ames.map((raw) {
              final a = raw as Map<String, dynamic>;
              final statut = (a['statut'] as String?) ?? '';
              final nom = a['nom']?.toString() ?? '—';
              final initials = nom.split(' ').where((p) => p.isNotEmpty).map((p) => p[0]).take(2).join();
              return Container(
                margin: const EdgeInsets.only(bottom: 6),
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.04), borderRadius: BorderRadius.circular(12)),
                child: Row(children: [
                  Container(
                    width: 34, height: 34,
                    decoration: BoxDecoration(
                      color: _statutColor(statut).withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Center(child: Text(initials,
                        style: TextStyle(color: _statutColor(statut), fontSize: 11, fontWeight: FontWeight.bold))),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(nom, maxLines: 1, overflow: TextOverflow.ellipsis,
                        style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w500)),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: _statutColor(statut).withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(_statutLabel(statut),
                        style: TextStyle(color: _statutColor(statut), fontSize: 9, fontWeight: FontWeight.w600)),
                  ),
                ]),
              );
            }),

          // Sorties de suivi
          if (sorties.isNotEmpty) ...[
            const SizedBox(height: 10),
            Row(children: [
              const Icon(Icons.person_remove, color: Colors.redAccent, size: 15),
              const SizedBox(width: 6),
              Expanded(
                child: Text(l10n.usersListHistoryExit,
                    style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 11, fontWeight: FontWeight.w600)),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.08), borderRadius: BorderRadius.circular(8)),
                child: Text('${sorties.length}',
                    style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 10, fontWeight: FontWeight.w700)),
              ),
            ]),
            const SizedBox(height: 8),
            ...sorties.map((raw) {
              final ex = raw as Map<String, dynamic>;
              return Container(
                margin: const EdgeInsets.only(bottom: 6),
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: Colors.red.withValues(alpha: 0.06),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: Colors.red.withValues(alpha: 0.2)),
                ),
                child: Row(children: [
                  const Icon(Icons.person_remove, color: Colors.redAccent, size: 14),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(ex['motif']?.toString() ?? l10n.userDetailExitReason, maxLines: 1, overflow: TextOverflow.ellipsis,
                        style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 12)),
                  ),
                  Text(_formatDate(ex['dateSortie']),
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 10)),
                ]),
              );
            }),
          ],
        ]),
      ),
    );
  }

  List<Widget> _buildTransferContent() {
    return [
      Text('Transférer ${widget.user['firstName'] ?? ''} ${widget.user['lastName'] ?? ''} vers :',
          style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 13)),
      const SizedBox(height: 8),
      DropdownButtonFormField<String>(
        initialValue: _transferFamilleId.isEmpty ? null : _transferFamilleId,
        dropdownColor: const Color(0xFF111827),
        decoration: InputDecoration(hintText: l10n.usersListTransferHint),
        items: _families.map((f) =>
          DropdownMenuItem(value: f['id'] as String, child: Text(f['nom'] as String? ?? '')),
        ).toList(),
        onChanged: (v) => setState(() => _transferFamilleId = v ?? ''),
      ),
      const SizedBox(height: 8),
      CheckboxListTile(
        contentPadding: EdgeInsets.zero,
        title: Text(l10n.usersListTransferSouls,
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
      Text(l10n.usersListDemoteConfirm.replaceAll('{name}', name),
          style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 13)),
      const SizedBox(height: 8),
      DropdownButtonFormField<String>(
        initialValue: _demoteRole,
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
