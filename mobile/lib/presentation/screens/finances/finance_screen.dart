import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';

/// Outil métier FINANCES (ADMIN / PASTEUR) — parité web.
///
/// Recettes, dépenses et solde calculés sur les transactions réelles
/// (GET /finances/stats). Liste filtrable par type, ajout et suppression
/// de transactions (POST / DELETE /finances/transactions).
class FinanceScreen extends StatefulWidget {
  const FinanceScreen({super.key, this.apiService});

  /// Permet d'injecter un ApiService mocké dans les tests widget.
  final ApiService? apiService;

  @override
  State<FinanceScreen> createState() => _FinanceScreenState();
}

class _FinanceScreenState extends State<FinanceScreen> {
  late final ApiService _apiService = widget.apiService ?? ApiService();
  List<Map<String, dynamic>> _transactions = [];
  Map<String, dynamic>? _stats;
  bool _isLoading = true;
  String _typeFilter = '';

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final year = DateTime.now().year;
      final txRes = await _apiService.get('/finances/transactions${_typeFilter.isEmpty ? '' : '?type=$_typeFilter'}');
      final statsRes = await _apiService.get('/finances/stats?annee=$year');
      if (mounted) {
        setState(() {
          _transactions = (txRes.data as List).map((e) => e as Map<String, dynamic>).toList();
          _stats = statsRes.data as Map<String, dynamic>;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _delete(Map<String, dynamic> tx) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        title: const Text('Supprimer la transaction ?'),
        content: Text('${tx['categorie']} — ${_money(tx['montant'])}'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(c, false), child: const Text('Non')),
          TextButton(onPressed: () => Navigator.pop(c, true), child: const Text('Oui')),
        ],
      ),
    );
    if (ok != true) return;
    try {
      await _apiService.delete('/finances/transactions/${tx['id']}');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Transaction supprimée')));
      }
      _load();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Erreur lors de la suppression')));
      }
    }
  }

  Future<void> _openAdd() async {
    final type = await showModalBottomSheet<Map<String, dynamic>>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (c) => const _AddTransactionSheet(),
    );
    if (type != null) {
      try {
        await _apiService.post('/finances/transactions', data: type);
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Transaction enregistrée')));
        }
        _load();
      } catch (_) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Échec de l'enregistrement")));
        }
      }
    }
  }

  String _money(Object? value) {
    final n = (value is num) ? value.toDouble() : double.tryParse('$value') ?? 0;
    return '${n.toStringAsFixed(0)} FCFA';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Finances')),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _openAdd,
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        icon: const Icon(Icons.add),
        label: const Text('Transaction'),
      ),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 4)
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  _kpiRow(),
                  const SizedBox(height: 12),
                  Row(children: [
                    _filterChip('', 'Toutes'),
                    const SizedBox(width: 6),
                    _filterChip('RECETTE', 'Recettes'),
                    const SizedBox(width: 6),
                    _filterChip('DEPENSE', 'Dépenses'),
                  ]),
                  const SizedBox(height: 12),
                  if (_transactions.isEmpty)
                    const Padding(
                      padding: EdgeInsets.only(top: 80),
                      child: Column(children: [
                        Icon(Icons.account_balance_wallet_rounded, size: 56, color: Colors.white24),
                        SizedBox(height: 12),
                        Text('Aucune transaction', style: TextStyle(color: Colors.white54)),
                      ]),
                    )
                  else
                    ..._transactions.map(_txCard),
                  const SizedBox(height: 80),
                ],
              ),
            ),
    );
  }

  Widget _filterChip(String value, String label) {
    final active = _typeFilter == value;
    return GestureDetector(
      onTap: () {
        setState(() => _typeFilter = value);
        _load();
      },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        decoration: BoxDecoration(
          color: active ? AppColors.primary.withValues(alpha: 0.25) : Colors.white.withValues(alpha: 0.05),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: active ? AppColors.primary : Colors.white.withValues(alpha: 0.1)),
        ),
        child: Text(label,
            style: TextStyle(
                color: active ? AppColors.primaryLight : Colors.white54, fontSize: 12, fontWeight: FontWeight.w600)),
      ),
    );
  }

  Widget _kpiRow() {
    final recettes = (_stats?['totalRecettes'] as num?)?.toDouble() ?? 0;
    final depenses = (_stats?['totalDepenses'] as num?)?.toDouble() ?? 0;
    final solde = (_stats?['solde'] as num?)?.toDouble() ?? 0;
    return Row(children: [
      Expanded(child: _kpiCard('Recettes', _money(recettes), const Color(0xFF22C55E), Icons.trending_up)),
      const SizedBox(width: 8),
      Expanded(child: _kpiCard('Dépenses', _money(depenses), const Color(0xFFEF4444), Icons.trending_down)),
      const SizedBox(width: 8),
      Expanded(child: _kpiCard('Solde', _money(solde), solde >= 0 ? const Color(0xFF3B82F6) : const Color(0xFFEF4444), Icons.scale)),
    ]);
  }

  Widget _kpiCard(String label, String value, Color color, IconData icon) {
    return GlassCard(
      padding: const EdgeInsets.all(12),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          Icon(icon, color: color, size: 14),
          const SizedBox(width: 4),
          Text(label,
              style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 9, fontWeight: FontWeight.w600)),
        ]),
        const SizedBox(height: 4),
        Text(value,
            style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.bold),
            overflow: TextOverflow.ellipsis),
      ]),
    );
  }

  Widget _txCard(Map<String, dynamic> tx) {
    final isRecette = tx['type'] == 'RECETTE';
    final color = isRecette ? const Color(0xFF22C55E) : const Color(0xFFEF4444);
    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      child: Row(children: [
        Container(
          width: 36,
          height: 36,
          decoration: BoxDecoration(
            color: color.withValues(alpha: 0.12),
            borderRadius: BorderRadius.circular(10),
          ),
          child: Icon(isRecette ? Icons.arrow_downward_rounded : Icons.arrow_upward_rounded, color: color, size: 18),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Row(children: [
              Flexible(
                child: Text('${tx['categorie'] ?? ''}',
                    style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w600),
                    overflow: TextOverflow.ellipsis),
              ),
              const SizedBox(width: 6),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                decoration: BoxDecoration(
                  color: color.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Text(isRecette ? 'Recette' : 'Dépense',
                    style: TextStyle(color: color, fontSize: 8, fontWeight: FontWeight.bold)),
              ),
            ]),
            const SizedBox(height: 2),
            Text(
              '${tx['dateTransaction'] ?? ''}${(tx['description'] as String? ?? '').isEmpty ? '' : ' — ${tx['description']}'}',
              style: TextStyle(color: Colors.white.withValues(alpha: 0.35), fontSize: 10),
              overflow: TextOverflow.ellipsis,
            ),
          ]),
        ),
        const SizedBox(width: 6),
        Text(_money(tx['montant']),
            style: TextStyle(color: color, fontSize: 12, fontWeight: FontWeight.bold)),
        IconButton(
          visualDensity: VisualDensity.compact,
          icon: const Icon(Icons.delete_rounded, color: Colors.redAccent, size: 16),
          onPressed: () => _delete(tx),
        ),
      ]),
    );
  }
}

class _AddTransactionSheet extends StatefulWidget {
  const _AddTransactionSheet();

  @override
  State<_AddTransactionSheet> createState() => _AddTransactionSheetState();
}

class _AddTransactionSheetState extends State<_AddTransactionSheet> {
  final _formKey = GlobalKey<FormState>();
  String _type = 'RECETTE';
  final _categorieCtrl = TextEditingController();
  final _montantCtrl = TextEditingController();
  final _descriptionCtrl = TextEditingController();
  DateTime _date = DateTime.now();

  @override
  void dispose() {
    _categorieCtrl.dispose();
    _montantCtrl.dispose();
    _descriptionCtrl.dispose();
    super.dispose();
  }

  String _iso(DateTime d) =>
      '${d.year.toString().padLeft(4, '0')}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

  Future<void> _pickDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _date,
      firstDate: DateTime(2000),
      lastDate: DateTime(2100),
    );
    if (picked != null) setState(() => _date = picked);
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: const BoxDecoration(
          gradient: LinearGradient(colors: [Color(0xFF111C33), Color(0xFF0B1220)]),
          borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        ),
        child: Form(
          key: _formKey,
          child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
            const Text('Nouvelle transaction',
                style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
            const SizedBox(height: 16),
            SegmentedButton<String>(
              segments: const [
                ButtonSegment(value: 'RECETTE', label: Text('Recette'), icon: Icon(Icons.arrow_downward)),
                ButtonSegment(value: 'DEPENSE', label: Text('Dépense'), icon: Icon(Icons.arrow_upward)),
              ],
              selected: {_type},
              onSelectionChanged: (s) => setState(() => _type = s.first),
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _categorieCtrl,
              style: const TextStyle(color: Colors.white),
              decoration: _input('Catégorie', Icons.label),
              validator: (v) => (v == null || v.trim().isEmpty) ? 'Catégorie requise' : null,
            ),
            const SizedBox(height: 10),
            TextFormField(
              controller: _montantCtrl,
              style: const TextStyle(color: Colors.white),
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'[0-9.,]'))],
              decoration: _input('Montant', Icons.payments),
              validator: (v) {
                final n = double.tryParse((v ?? '').replaceAll(',', '.'));
                return (n == null || n <= 0) ? 'Montant invalide' : null;
              },
            ),
            const SizedBox(height: 10),
            TextFormField(
              controller: _descriptionCtrl,
              style: const TextStyle(color: Colors.white),
              decoration: _input('Description (optionnel)', Icons.notes),
            ),
            const SizedBox(height: 10),
            InkWell(
              onTap: _pickDate,
              child: InputDecorator(
                decoration: _input('Date', Icons.calendar_today).copyWith(errorText: null),
                child: Text(_iso(_date), style: const TextStyle(color: Colors.white, fontSize: 14)),
              ),
            ),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.primary,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                ),
                onPressed: () {
                  if (_formKey.currentState!.validate()) {
                    Navigator.pop(context, {
                      'type': _type,
                      'categorie': _categorieCtrl.text.trim().toUpperCase(),
                      'montant': double.parse(_montantCtrl.text.replaceAll(',', '.')),
                      'description': _descriptionCtrl.text.trim().isEmpty ? null : _descriptionCtrl.text.trim(),
                      'dateTransaction': _iso(_date),
                    });
                  }
                },
                child: const Text('Enregistrer', style: TextStyle(fontWeight: FontWeight.bold)),
              ),
            ),
          ]),
        ),
      ),
    );
  }

  InputDecoration _input(String label, IconData icon) {
    return InputDecoration(
      labelText: label,
      labelStyle: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12),
      prefixIcon: Icon(icon, color: Colors.white38, size: 18),
      filled: true,
      fillColor: Colors.white.withValues(alpha: 0.05),
      border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
      contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
    );
  }
}
