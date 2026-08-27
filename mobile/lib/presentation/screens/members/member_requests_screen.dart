import 'package:flutter/material.dart';
import '../../../../l10n/app_localizations.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../widgets/attachment_picker_field.dart';
import '../../widgets/attachment_chips.dart';
import '../../../data/services/api_service.dart';

class MemberRequestsScreen extends StatefulWidget {
  const MemberRequestsScreen({super.key, this.apiService});

  /// Permet d'injecter un ApiService mocké dans les tests widget.
  final ApiService? apiService;

  @override
  State<MemberRequestsScreen> createState() => _MemberRequestsScreenState();
}

class _MemberRequestsScreenState extends State<MemberRequestsScreen> with SingleTickerProviderStateMixin {
  late final ApiService _apiService = widget.apiService ?? ApiService();
  late TabController _tabController;
  List<dynamic> _myRequests = [];
  List<dynamic> _inbox = [];
  bool _isLoading = true;
  String _selectedType = 'SUGGESTION';
  String _selectedCible = 'PASTEUR';
  final _messageCtrl = TextEditingController();
  final _objetCtrl = TextEditingController();
  final Set<String> _fichierIds = {};

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _loadData();
  }

  @override
  void dispose() {
    _tabController.dispose();
    _messageCtrl.dispose();
    _objetCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final myRes = await _apiService.get('/members/me/requests');
      final inboxRes = await _apiService.get('/members/requests/inbox');
      if (mounted) {
        setState(() {
          _myRequests = myRes.data as List<dynamic>? ?? [];
          _inbox = inboxRes.data as List<dynamic>? ?? [];
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _submitRequest() async {
    if (_messageCtrl.text.trim().isEmpty) return;
    try {
      await _apiService.post('/members/me/requests', data: {
        'type': _selectedType,
        'cible': _selectedCible,
        'message': _messageCtrl.text.trim(),
        if (_objetCtrl.text.trim().isNotEmpty) 'objet': _objetCtrl.text.trim(),
        if (_fichierIds.isNotEmpty) 'fichierIds': _fichierIds.toList(),
      });
      _messageCtrl.clear();
      _objetCtrl.clear();
      _fichierIds.clear();
      _loadData();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(AppLocalizations.of(context).requestSent)));
        Navigator.pop(context);
      }
    } catch (_) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(AppLocalizations.of(context).sendError)));
    }
  }

  Future<void> _updateStatus(String id, String status) async {
    try {
      await _apiService.patch('/members/requests/$id/status', data: {'statut': status});
      _loadData();
    } catch (_) {}
  }

  void _showCreateDialog() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: const Color(0xFF1E293B),
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (ctx) => Padding(
        padding: EdgeInsets.fromLTRB(20, 20, 20, MediaQuery.of(ctx).viewInsets.bottom + 20),
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
            Text(AppLocalizations.of(context).newRequest, style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold)),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(child: _dropdown(AppLocalizations.of(context).requestType, _selectedType, ['SUGGESTION', 'RENDEZ_VOUS', 'SIGNALEMENT'], (v) => setState(() => _selectedType = v!))),
                const SizedBox(width: 8),
                Expanded(child: _dropdown(AppLocalizations.of(context).recipient, _selectedCible, ['PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE'], (v) => setState(() => _selectedCible = v!))),
              ],
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _objetCtrl,
              style: const TextStyle(color: Colors.white),
              decoration: InputDecoration(
                hintText: AppLocalizations.of(context).subjectOptional,
                hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.4)),
                filled: true,
                fillColor: Colors.white.withValues(alpha: 0.06),
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: BorderSide.none),
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _messageCtrl,
              maxLines: 3,
              style: const TextStyle(color: Colors.white),
              decoration: InputDecoration(
                hintText: AppLocalizations.of(context).messageHint,
                hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.4)),
                filled: true,
                fillColor: Colors.white.withValues(alpha: 0.06),
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: BorderSide.none),
              ),
            ),
            const SizedBox(height: 12),
            Text(AppLocalizations.of(context).attachments, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
            const SizedBox(height: 6),
            AttachmentPickerField(
              apiService: _apiService,
              value: _fichierIds,
              onChanged: (ids) => setState(() => _fichierIds..clear()..addAll(ids)),
            ),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: _submitRequest,
                style: ElevatedButton.styleFrom(backgroundColor: Colors.blue, foregroundColor: Colors.white, padding: const EdgeInsets.symmetric(vertical: 14), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12))),
                child: Text(AppLocalizations.of(context).send, style: const TextStyle(fontWeight: FontWeight.w600)),
              ),
            ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _dropdown(String label, String value, List<String> items, ValueChanged<String?> onChanged) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
        const SizedBox(height: 4),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 10),
          decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.06), borderRadius: BorderRadius.circular(10)),
          child: DropdownButton<String>(
            value: value,
            isExpanded: true,
            dropdownColor: const Color(0xFF1E293B),
            underline: const SizedBox(),
            style: const TextStyle(color: Colors.white, fontSize: 13),
            items: items.map((i) => DropdownMenuItem(value: i, child: Text(i.replaceAll('_', ' ')))).toList(),
            onChanged: onChanged,
          ),
        ),
      ],
    );
  }

  Color _typeColor(String? type) {
    switch (type) {
      case 'SUGGESTION': return Colors.blue;
      case 'RENDEZ_VOUS': return Colors.purple;
      case 'SIGNALEMENT': return Colors.red;
      default: return Colors.grey;
    }
  }

  Color _statusColor(String? s) {
    switch (s) {
      case 'OUVERT': return Colors.amber;
      case 'EN_COURS': return Colors.blue;
      case 'RESOLU': return Colors.green;
      case 'REJETE': return Colors.red;
      default: return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context).memberRequestsTitle),
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: Colors.white,
          tabs: [
            Tab(text: AppLocalizations.of(context).tabMyRequests),
            Tab(text: AppLocalizations.of(context).tabInbox),
          ],
        ),
      ),
      drawer: const AppDrawer(),
      floatingActionButton: FloatingActionButton(
        onPressed: _showCreateDialog,
        backgroundColor: Colors.blue,
        child: const Icon(Icons.add, color: Colors.white),
      ),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 3)
          : RefreshIndicator(
              onRefresh: _loadData,
              child: TabBarView(
                controller: _tabController,
                children: [
                  _buildList(_myRequests, isOutbox: true),
                  _buildList(_inbox, isOutbox: false),
                ],
              ),
            ),
    );
  }

  Widget _buildList(List<dynamic> items, {required bool isOutbox}) {
    if (items.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.mail_outline, size: 48, color: Colors.white.withValues(alpha: 0.3)),
            const SizedBox(height: 12),
            Text(isOutbox ? AppLocalizations.of(context).noSentRequests : AppLocalizations.of(context).noReceivedRequests, style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
          ],
        ),
      );
    }
    return ListView.builder(
      padding: const EdgeInsets.all(12),
      itemCount: items.length,
      itemBuilder: (context, index) {
        final r = items[index] as Map<String, dynamic>;
        final type = r['type'] ?? 'SUGGESTION';
        final statut = r['statut'] ?? 'OUVERT';
        final message = r['message'] ?? '';
        final cible = r['cible'] ?? '—';
        final auteurNom = r['auteurNom'] ?? '';
        return GlassCard(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: _typeColor(type).withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(type.replaceAll('_', ' '), style: TextStyle(color: _typeColor(type), fontSize: 10, fontWeight: FontWeight.w600)),
                  ),
                  const SizedBox(width: 6),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: _statusColor(statut).withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(statut, style: TextStyle(color: _statusColor(statut), fontSize: 10, fontWeight: FontWeight.w600)),
                  ),
                  const Spacer(),
                  if (!isOutbox)
                    Text('→ $cible', style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 10)),
                ],
              ),
              if (!isOutbox && auteurNom.isNotEmpty) ...[
                const SizedBox(height: 6),
                Text(AppLocalizations.of(context).fromLabel(auteurNom), style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
              ],
              const SizedBox(height: 6),
              Text(message, style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 13), maxLines: 3, overflow: TextOverflow.ellipsis),
              if (r['piecesJointes'] is List && (r['piecesJointes'] as List).isNotEmpty) ...[
                const SizedBox(height: 8),
                AttachmentChips(pieces: r['piecesJointes'] as List),
              ],
              if (!isOutbox && statut == 'OUVERT') ...[
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(
                      child: OutlinedButton(
                        onPressed: () => _updateStatus(r['id'], 'REJETE'),
                        style: OutlinedButton.styleFrom(foregroundColor: Colors.red, side: BorderSide(color: Colors.red.withValues(alpha: 0.3)), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8))),
                        child: Text(AppLocalizations.of(context).rejectAction, style: const TextStyle(fontSize: 12)),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: ElevatedButton(
                        onPressed: () => _updateStatus(r['id'], 'RESOLU'),
                        style: ElevatedButton.styleFrom(backgroundColor: Colors.green, foregroundColor: Colors.white, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8))),
                        child: Text(AppLocalizations.of(context).resolve, style: const TextStyle(fontSize: 12)),
                      ),
                    ),
                  ],
                ),
              ],
            ],
          ),
        );
      },
    );
  }
}
