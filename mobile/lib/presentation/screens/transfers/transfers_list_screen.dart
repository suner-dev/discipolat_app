import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../app.dart';
import '../../../data/services/api_service.dart';
import '../../../l10n/app_localizations.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import 'transfer_labels.dart';

class TransfersListScreen extends StatefulWidget {
  const TransfersListScreen({super.key});

  @override
  State<TransfersListScreen> createState() => _TransfersListScreenState();
}

class _TransfersListScreenState extends State<TransfersListScreen> {
  final _apiService = ApiService();
  List<Map<String, dynamic>> _transfers = [];
  bool _isLoading = true;
  String _statut = '';
  String _type = '';

  @override
  void initState() { super.initState(); _loadTransfers(); }

  Future<void> _loadTransfers() async {
    try {
      final params = <String, String>{'size': '50'};
      if (_statut.isNotEmpty) params['statut'] = _statut;
      if (_type.isNotEmpty) params['type'] = _type;
      final response = await _apiService.get('/transfers', params: params);
      final data = response.data as Map<String, dynamic>;
      if (mounted) {
        setState(() {
        _transfers = (data['content'] as List).map((e) => e as Map<String, dynamic>).toList();
        _isLoading = false;
      });
      }
    } catch (e) { if (mounted) setState(() => _isLoading = false); }
  }

  Future<void> _submit(String id) async {
    final l10n = AppLocalizations.of(context);
    try {
      await _apiService.post('/transfers/$id/submit');
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(l10n.transferSubmitted)));
      _loadTransfers();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(l10n.transferListSubmitError)));
      }
    }
  }

  Future<void> _cancel(String id) async {
    final l10n = AppLocalizations.of(context);
    try {
      await _apiService.post('/transfers/$id/cancel');
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(l10n.transferListCancelSuccess)));
      _loadTransfers();
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return Scaffold(
      appBar: AppBar(title: Text(l10n.transferListTitle)),
      drawer: const AppDrawer(),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.push('/transfers/new'),
        backgroundColor: AppColors.primary,
        icon: const Icon(Icons.add, color: Colors.white),
        label: Text(l10n.transferListNewRequest, style: const TextStyle(color: Colors.white)),
      ),
      body: Column(children: [
        // Filtres
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 4),
          child: Row(children: [
            Expanded(
              child: DropdownButtonFormField<String>(
                initialValue: _statut.isEmpty ? null : _statut,
                isExpanded: true,
                decoration: InputDecoration(labelText: l10n.transferListStatusFilter, isDense: true),
                items: [for (final e in kTransferStatusLabels.entries) DropdownMenuItem(value: e.key, child: Text(e.value, overflow: TextOverflow.ellipsis))],
                onChanged: (v) { setState(() => _statut = v ?? ''); _loadTransfers(); },
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: DropdownButtonFormField<String>(
                initialValue: _type.isEmpty ? null : _type,
                isExpanded: true,
                decoration: InputDecoration(labelText: l10n.transferListTypeFilter, isDense: true),
                items: [for (final e in kTransferTypeLabels.entries) DropdownMenuItem(value: e.key, child: Text(e.value, overflow: TextOverflow.ellipsis))],
                onChanged: (v) { setState(() => _type = v ?? ''); _loadTransfers(); },
              ),
            ),
          ]),
        ),
        Expanded(
          child: _isLoading
              ? const ShimmerLoading(itemCount: 5)
              : RefreshIndicator(
                  onRefresh: _loadTransfers,
                  child: _transfers.isEmpty
                      ? ListView(physics: const AlwaysScrollableScrollPhysics(), children: [
                          Padding(
                            padding: const EdgeInsets.only(top: 120),
                            child: Column(children: [
                              const Icon(Icons.swap_horiz, size: 56, color: Colors.white24),
                              const SizedBox(height: 12),
                              Text(l10n.transferListEmpty, style: const TextStyle(color: Colors.white54)),
                            ]),
                          ),
                        ])
                      : ListView.builder(
                          padding: const EdgeInsets.all(16),
                          itemCount: _transfers.length,
                          itemBuilder: (context, index) {
                            final t = _transfers[index];
                            final statut = t['statut'] ?? '';
                            final color = transferStatusColor(statut);
                            final nouvelle = (t['nouvelleAffectation'] as Map?)?['nom'] ?? '';
                            final ancienne = (t['ancienneAffectation'] as Map?)?['nom'];
                            final totalEtapes = (t['totalEtapes'] as num?)?.toInt() ?? 0;
                            final approbations = (t['approbationsObtenues'] as num?)?.toInt() ?? 0;
                            final estDemandeur = t['demandeurId'] == AuthState().userId;
                            final peutSoumettre = (statut == 'BROUILLON' || statut == 'SOUMIS') && estDemandeur;
                            final peutAnnuler = ['BROUILLON', 'SOUMIS', 'EN_ATTENTE_VALIDATION', 'VALIDATION_PARTIELLE'].contains(statut) && estDemandeur;

                            return GlassCard(
                              margin: const EdgeInsets.only(bottom: 8),
                              padding: const EdgeInsets.all(12),
                              borderColor: color.withValues(alpha: 0.25),
                              onTap: () => context.push('/transfers/${t['id']}'),
                              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                                Row(children: [
                                  Expanded(child: Text(
                                    transferTypeLabel(t['type'] ?? ''),
                                    style: const TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w600),
                                  )),
                                  StatusBadge(label: transferStatusLabel(statut), color: color),
                                ]),
                                const SizedBox(height: 8),
                                Text(l10n.transferListPersonLabel(t['personneNom'] ?? '—'), style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 13)),
                                const SizedBox(height: 4),
                                Row(children: [
                                  if (ancienne != null) ...[
                                    Text(ancienne, style: TextStyle(color: Colors.white.withValues(alpha: 0.35), fontSize: 12, decoration: TextDecoration.lineThrough)),
                                    const SizedBox(width: 6),
                                  ],
                                  Icon(Icons.arrow_forward, size: 12, color: Colors.white.withValues(alpha: 0.4)),
                                  const SizedBox(width: 6),
                                  Text(nouvelle, style: const TextStyle(color: Color(0xFF4ADE80), fontSize: 12, fontWeight: FontWeight.w600)),
                                ]),
                                if (totalEtapes > 0) ...[
                                  const SizedBox(height: 8),
                                  ClipRRect(
                                    borderRadius: BorderRadius.circular(4),
                                    child: LinearProgressIndicator(
                                      value: totalEtapes == 0 ? 0 : approbations / totalEtapes,
                                      minHeight: 4,
                                      backgroundColor: Colors.white.withValues(alpha: 0.08),
                                      valueColor: AlwaysStoppedAnimation(AppColors.primaryLight),
                                    ),
                                  ),
                                  const SizedBox(height: 2),
                                  Text(l10n.transferListValidations(approbations, totalEtapes), style: TextStyle(color: Colors.white.withValues(alpha: 0.35), fontSize: 10)),
                                ],
                                if (peutSoumettre || peutAnnuler) ...[
                                  const SizedBox(height: 8),
                                  Row(children: [
                                    if (peutSoumettre)
                                      Expanded(child: FilledButton(
                                        style: FilledButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 8)),
                                        onPressed: () => _submit(t['id'] as String),
                                        child: Text(l10n.transferSubmit, style: const TextStyle(fontSize: 13)),
                                      )),
                                    if (peutAnnuler) ...[
                                      const SizedBox(width: 8),
                                      Expanded(child: OutlinedButton(
                                        style: OutlinedButton.styleFrom(
                                          padding: const EdgeInsets.symmetric(vertical: 8),
                                          foregroundColor: Colors.redAccent,
                                          side: BorderSide(color: Colors.redAccent.withValues(alpha: 0.4)),
                                        ),
                                        onPressed: () => _cancel(t['id'] as String),
                                        child: Text(l10n.transferCancel, style: const TextStyle(fontSize: 13)),
                                      )),
                                    ],
                                  ]),
                                ],
                              ]),
                            );
                          },
                        ),
                ),
        ),
      ]),
    );
  }
}
