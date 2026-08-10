import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import 'transfer_labels.dart';

const List<String> kRoles = ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'];
const List<String> kModes = ['SEQUENTIEL', 'PARALLELE', 'N_VALIDATIONS_REQUISES'];

class TransferAdminScreen extends StatefulWidget {
  const TransferAdminScreen({super.key});

  @override
  State<TransferAdminScreen> createState() => _TransferAdminScreenState();
}

class _TransferAdminScreenState extends State<TransferAdminScreen> {
  final _apiService = ApiService();
  List<Map<String, dynamic>> _configs = [];
  bool _isLoading = true;

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    try {
      final res = await _apiService.get('/admin/transfers/workflows');
      if (mounted) setState(() {
        _configs = (res.data as List).map((e) => e as Map<String, dynamic>).toList();
        _isLoading = false;
      });
    } catch (e) { if (mounted) setState(() => _isLoading = false); }
  }

  Future<void> _toggle(String id, bool actif) async {
    try {
      await _apiService.patch('/admin/transfers/workflows/$id/toggle', data: {'actif': actif});
      _load();
    } catch (_) {}
  }

  Future<void> _delete(String id) async {
    try {
      await _apiService.delete('/admin/transfers/workflows/$id');
      _load();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Suppression impossible : des demandes utilisent cette configuration')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Workflow de transfert')),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 5)
          : RefreshIndicator(
              onRefresh: _load,
              child: _configs.isEmpty
                  ? ListView(physics: const AlwaysScrollableScrollPhysics(), children: const [
                      Padding(
                        padding: EdgeInsets.only(top: 120),
                        child: Column(children: [
                          Icon(Icons.account_tree_outlined, size: 56, color: Colors.white24),
                          SizedBox(height: 12),
                          Text('Aucune configuration', style: TextStyle(color: Colors.white54)),
                        ]),
                      ),
                    ])
                  : ListView.builder(
                      padding: const EdgeInsets.all(16),
                      itemCount: _configs.length,
                      itemBuilder: (context, index) {
                        final c = _configs[index];
                        final actif = c['actif'] == true;
                        final steps = (c['steps'] as List? ?? []);
                        return GlassCard(
                          margin: const EdgeInsets.only(bottom: 8),
                          padding: const EdgeInsets.all(14),
                          borderColor: actif ? Colors.green.withValues(alpha: 0.25) : Colors.white.withValues(alpha: 0.05),
                          onTap: () => _openEditor(c),
                          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                            Row(children: [
                              Expanded(child: Text(c['label'] ?? '', style: const TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w600))),
                              StatusBadge(label: actif ? 'Actif' : 'Inactif', color: actif ? Colors.green : Colors.blueGrey),
                            ]),
                            const SizedBox(height: 6),
                            Text(transferTypeLabel(c['transferType'] ?? ''),
                                style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                            const SizedBox(height: 4),
                            Text('${c['modeValidation']} · ${c['delaiTraitementHeures']}h · ${steps.length} étape(s)',
                                style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                            const SizedBox(height: 8),
                            Row(children: [
                              IconButton(
                                visualDensity: VisualDensity.compact,
                                icon: Icon(actif ? Icons.power_settings_new : Icons.power_off, color: actif ? Colors.green : Colors.blueGrey, size: 18),
                                onPressed: () => _toggle(c['id'] as String, !actif),
                              ),
                              const Spacer(),
                              Icon(Icons.chevron_right, color: Colors.white24, size: 20),
                            ]),
                          ]),
                        );
                      },
                    ),
            ),
    );
  }

  /// Éditeur simple : rôles initiateurs, mode, nombre requis, délai, étapes.
  Future<void> _openEditor(Map<String, dynamic> config) async {
    var mode = (config['modeValidation'] ?? 'SEQUENTIEL') as String;
    final nbRequisCtrl = TextEditingController(text: '${(config['nombreValidationsRequises'] as num?)?.toInt() ?? 1}');
    final delaiCtrl = TextEditingController(text: '${(config['delaiTraitementHeures'] as num?)?.toInt() ?? 72}');
    final roles = List<String>.from((config['rolesInitiateurs'] as List? ?? ['PASTEUR']));
    final steps = (config['steps'] as List? ?? []).map((s) {
      final m = s as Map<String, dynamic>;
      return {
        'etapeOrdre': (m['etapeOrdre'] as num?)?.toInt() ?? 1,
        'rolesValidateurs': List<String>.from(m['rolesValidateurs'] as List? ?? ['PASTEUR']),
        'label': m['label'] ?? 'Étape',
      };
    }).toList();

    await showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: const Color(0xFF0F172A),
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setSheetState) => Padding(
          padding: EdgeInsets.only(bottom: MediaQuery.of(ctx).viewInsets.bottom),
          child: DraggableScrollableSheet(
            expand: false,
            initialChildSize: 0.85,
            builder: (ctx, scrollController) => ListView(
              controller: scrollController,
              padding: const EdgeInsets.all(20),
              children: [
                Text(config['label'] ?? 'Configuration', style: const TextStyle(color: Colors.white, fontSize: 17, fontWeight: FontWeight.bold)),
                const SizedBox(height: 16),
                _sectionTitle('Rôles initiateurs'),
                Wrap(spacing: 6, runSpacing: 6, children: [
                  for (final r in kRoles)
                    FilterChip(
                      label: Text(r, style: const TextStyle(fontSize: 11)),
                      selected: roles.contains(r),
                      onSelected: (sel) => setSheetState(() {
                        if (sel && !roles.contains(r)) { roles.add(r); }
                        if (!sel) { roles.remove(r); }
                      }),
                    ),
                ]),
                const SizedBox(height: 16),
                _sectionTitle('Mode de validation'),
                DropdownButtonFormField<String>(
                  initialValue: mode,
                  items: [for (final m in kModes) DropdownMenuItem(value: m, child: Text(m))],
                  onChanged: (v) => setSheetState(() => mode = v ?? mode),
                ),
                const SizedBox(height: 16),
                Row(children: [
                  Expanded(child: _sectionTitle('Validations requises')),
                  const SizedBox(width: 8),
                  Expanded(child: _sectionTitle('Délai (heures)')),
                ]),
                Row(children: [
                  Expanded(child: TextField(
                    controller: nbRequisCtrl,
                    keyboardType: TextInputType.number,
                    decoration: const InputDecoration(isDense: true),
                  )),
                  const SizedBox(width: 12),
                  Expanded(child: TextField(
                    controller: delaiCtrl,
                    keyboardType: TextInputType.number,
                    decoration: const InputDecoration(isDense: true),
                  )),
                ]),
                const SizedBox(height: 20),
                _sectionTitle('Étapes du circuit'),
                for (int i = 0; i < steps.length; i++) ...[
                  Card(
                    color: Colors.white.withValues(alpha: 0.04),
                    child: Padding(
                      padding: const EdgeInsets.all(12),
                      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        Text('Étape ${i + 1} — ${steps[i]['label']}', style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w600)),
                        const SizedBox(height: 8),
                        Wrap(spacing: 6, runSpacing: 6, children: [
                          for (final r in kRoles)
                            FilterChip(
                              label: Text(r, style: const TextStyle(fontSize: 10)),
                              selected: (steps[i]['rolesValidateurs'] as List).contains(r),
                              onSelected: (sel) => setSheetState(() {
                                final v = steps[i]['rolesValidateurs'] as List;
                                if (sel && !v.contains(r)) { v.add(r); }
                                if (!sel) { v.remove(r); }
                              }),
                            ),
                        ]),
                      ]),
                    ),
                  ),
                  const SizedBox(height: 8),
                ],
                const SizedBox(height: 24),
                FilledButton(
                  onPressed: () async {
                    final nbRequis = int.tryParse(nbRequisCtrl.text.trim()) ?? 1;
                    final delai = int.tryParse(delaiCtrl.text.trim()) ?? 72;
                    Navigator.pop(ctx);
                    await _save(config['id'] as String, roles, mode, nbRequis, delai, steps);
                  },
                  child: const Text('Enregistrer'),
                ),
                const SizedBox(height: 8),
                OutlinedButton(
                  style: OutlinedButton.styleFrom(foregroundColor: Colors.redAccent),
                  onPressed: () async {
                    final ok = await showDialog<bool>(
                      context: ctx,
                      builder: (c) => AlertDialog(
                        title: const Text('Supprimer cette configuration ?'),
                        actions: [
                          TextButton(onPressed: () => Navigator.pop(c, false), child: const Text('Non')),
                          TextButton(onPressed: () => Navigator.pop(c, true), child: const Text('Oui')),
                        ],
                      ),
                    );
                    if (ok == true) {
                      Navigator.pop(ctx);
                      await _delete(config['id'] as String);
                    }
                  },
                  child: const Text('Supprimer'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _sectionTitle(String title) => Text(title, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12));

  Future<void> _save(String id, List<String> roles, String mode, int nbRequis, int delai, List<dynamic> steps) async {
    try {
      await _apiService.put('/admin/transfers/workflows/$id', data: {
        'rolesInitiateurs': roles,
        'modeValidation': mode,
        'nombreValidationsRequises': nbRequis,
        'delaiTraitementHeures': delai,
        'steps': steps.map((s) => {
          'etapeOrdre': s['etapeOrdre'],
          'rolesValidateurs': s['rolesValidateurs'],
          'label': s['label'],
        }).toList(),
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Configuration enregistrée')));
      }
      _load();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Erreur lors de l\u2019enregistrement')));
      }
    }
  }
}
