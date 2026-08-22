import 'package:flutter/material.dart';
import 'dart:async';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

/// Dîmes & Offrandes 2.0 — don par Mobile Money avec suivi du statut.
class GivingScreen extends StatefulWidget {
  const GivingScreen({super.key});

  @override
  State<GivingScreen> createState() => _GivingScreenState();
}

class _GivingScreenState extends State<GivingScreen> {
  final _apiService = ApiService();
  final _formKey = GlobalKey<FormState>();
  final _amountCtrl = TextEditingController();
  final _phoneCtrl = TextEditingController();

  String _operator = 'ORANGE_MONEY';
  String _purpose = 'OFFRANDE';
  bool _submitting = false;
  String? _pendingRef;
  Timer? _pollTimer;
  List<dynamic> _mine = [];
  bool _isLoading = true;

  static const _operators = {
    'M_PESA': 'M-Pesa',
    'MTN_MOMO': 'MTN MoMo',
    'ORANGE_MONEY': 'Orange Money',
    'AIRTEL_MONEY': 'Airtel Money',
    'WAVE': 'Wave',
    'CARD': 'Carte bancaire',
    'CASH': 'Espèces',
  };
  static const _purposes = {
    'DIME': 'Dîme',
    'OFFRANDE': 'Offrande',
    'PROMESSE': 'Promesse',
    'PROJET_SPECIAL': 'Projet spécial',
    'DON_DIASPORA': 'Don diaspora',
  };

  @override
  void initState() {
    super.initState();
    _loadMine();
  }

  @override
  void dispose() {
    _pollTimer?.cancel();
    _amountCtrl.dispose();
    _phoneCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadMine() async {
    try {
      final res = await _apiService.get('/payments/mine');
      if (!mounted) return;
      setState(() {
        _mine = (res.data is List ? res.data : []) as List<dynamic>;
        _isLoading = false;
      });
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _give() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _submitting = true);
    try {
      final res = await _apiService.post('/payments/initiate', data: {
        'operator': _operator,
        'amount': num.tryParse(_amountCtrl.text.trim()) ?? 0,
        'purpose': _purpose,
        'currency': 'XOF',
        if (_phoneCtrl.text.trim().isNotEmpty)
          'phoneNumber': _phoneCtrl.text.trim(),
      });
      final ref = res.data['providerReference'] as String?;
      final id = res.data['id'] as String?;
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
            content: Text('Paiement initié — référence $ref'),
            backgroundColor: const Color(0xFF2E7D32)));
      }
      _amountCtrl.clear();
      _phoneCtrl.clear();
      _loadMine();
      // Polling du statut pendant ~30 s.
      if (id != null) {
        _pendingRef = ref;
        int attempts = 0;
        _pollTimer?.cancel();
        _pollTimer = Timer.periodic(const Duration(seconds: 3), (t) async {
          attempts++;
          try {
            final st = await _apiService.get('/payments/$id');
            final status = st.data['status'];
            if (status == 'CONFIRMED' ||
                status == 'FAILED' ||
                status == 'CANCELLED' ||
                attempts > 10) {
              t.cancel();
              if (mounted && status == 'CONFIRMED') {
                ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
                    content:
                        Text('Paiement confirmé — merci pour votre don !'),
                    backgroundColor: Color(0xFF2E7D32)));
              }
              if (mounted) setState(() => _pendingRef = null);
              _loadMine();
            }
          } catch (_) {
            t.cancel();
            if (mounted) setState(() => _pendingRef = null);
          }
        });
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
            content: Text('Échec de l\'initiation du paiement'),
            backgroundColor: Color(0xFFC62828)));
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Dîmes & Offrandes')),
      drawer: const AppDrawer(),
      body: RefreshIndicator(
        onRefresh: _loadMine,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
          children: [
            GlassCard(
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const Text('Donner maintenant',
                        style: TextStyle(
                            color: Colors.white,
                            fontSize: 16,
                            fontWeight: FontWeight.w600)),
                    const SizedBox(height: 12),
                    TextFormField(
                      controller: _amountCtrl,
                      keyboardType: TextInputType.number,
                      style: const TextStyle(color: Colors.white),
                      decoration: const InputDecoration(
                        labelText: 'Montant (XOF)',
                        labelStyle: TextStyle(color: Colors.white54),
                        prefixIcon: Icon(Icons.payments_rounded,
                            color: Colors.white54),
                      ),
                      validator: (v) => (v == null ||
                              num.tryParse(v) == null ||
                              num.parse(v) <= 0)
                          ? 'Montant invalide'
                          : null,
                    ),
                    const SizedBox(height: 10),
                    DropdownButtonFormField<String>(
                      initialValue: _operator,
                      dropdownColor: const Color(0xFF1E293B),
                      style: const TextStyle(color: Colors.white),
                      decoration: const InputDecoration(
                        labelText: 'Opérateur',
                        labelStyle: TextStyle(color: Colors.white54),
                        prefixIcon: Icon(Icons.smartphone_rounded,
                            color: Colors.white54),
                      ),
                      items: _operators.entries
                          .map((e) => DropdownMenuItem(
                              value: e.key, child: Text(e.value)))
                          .toList(),
                      onChanged: (v) =>
                          setState(() => _operator = v ?? _operator),
                    ),
                    const SizedBox(height: 10),
                    DropdownButtonFormField<String>(
                      initialValue: _purpose,
                      dropdownColor: const Color(0xFF1E293B),
                      style: const TextStyle(color: Colors.white),
                      decoration: const InputDecoration(
                        labelText: 'Destination',
                        labelStyle: TextStyle(color: Colors.white54),
                        prefixIcon: Icon(Icons.church_rounded,
                            color: Colors.white54),
                      ),
                      items: _purposes.entries
                          .map((e) => DropdownMenuItem(
                              value: e.key, child: Text(e.value)))
                          .toList(),
                      onChanged: (v) =>
                          setState(() => _purpose = v ?? _purpose),
                    ),
                    const SizedBox(height: 10),
                    TextFormField(
                      controller: _phoneCtrl,
                      keyboardType: TextInputType.phone,
                      style: const TextStyle(color: Colors.white),
                      decoration: const InputDecoration(
                        labelText: 'Téléphone Mobile Money (optionnel)',
                        labelStyle: TextStyle(color: Colors.white54),
                        prefixIcon: Icon(Icons.phone_rounded,
                            color: Colors.white54),
                      ),
                    ),
                    const SizedBox(height: 14),
                    FilledButton.icon(
                      onPressed: _submitting ? null : _give,
                      icon: _submitting
                          ? const SizedBox(
                              width: 16,
                              height: 16,
                              child: CircularProgressIndicator(
                                  strokeWidth: 2))
                          : const Icon(
                              Icons.volunteer_activism_rounded),
                      label: const Text('Donner maintenant'),
                    ),
                    if (_pendingRef != null)
                      Padding(
                        padding: const EdgeInsets.only(top: 8),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            const SizedBox(
                                width: 14,
                                height: 14,
                                child: CircularProgressIndicator(
                                    strokeWidth: 2)),
                            const SizedBox(width: 8),
                            Expanded(
                              child: Text(
                                'En attente de confirmation ($_pendingRef)…',
                                textAlign: TextAlign.center,
                                style: const TextStyle(
                                    color: Color(0xFFFFB300),
                                    fontSize: 12),
                              ),
                            ),
                          ],
                        ),
                      ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),
            ..._buildHistory(),
          ],
        ),
      ),
    );
  }

  List<Widget> _buildHistory() {
    if (_isLoading) {
      return const [
        Center(
            child: Padding(
                padding: EdgeInsets.all(20),
                child: CircularProgressIndicator())),
      ];
    }
    if (_mine.isEmpty) {
      return const [
        GlassCard(
          child: Padding(
            padding: EdgeInsets.all(20),
            child: Center(
              child: Text(
                  'Aucun don enregistré pour le moment.\nQue le Seigneur bénisse votre générosité !',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.white54)),
            ),
          ),
        ),
      ];
    }
    return _mine.map((p) {
      final m = p as Map<String, dynamic>;
      final status = (m['status'] ?? '') as String;
      final color = switch (status) {
        'CONFIRMED' => const Color(0xFF4CAF50),
        'PENDING' => const Color(0xFFFFB300),
        _ => const Color(0xFFE53935),
      };
      final label = switch (status) {
        'CONFIRMED' => 'Confirmé',
        'PENDING' => 'En attente',
        'FAILED' => 'Échoué',
        'CANCELLED' => 'Annulé',
        _ => status,
      };
      return Padding(
        padding: const EdgeInsets.only(bottom: 10),
        child: GlassCard(
          child: Row(
            children: [
              Icon(Icons.receipt_long_rounded, color: color),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '${_operators[m['operator']] ?? m['operator']} · ${m['amount']} ${m['currency'] ?? 'XOF'}',
                      style: const TextStyle(
                          color: Colors.white,
                          fontWeight: FontWeight.w600),
                    ),
                    Text(
                      '${_purposes[m['purpose']] ?? m['purpose']} · ${m['providerReference'] ?? ''}',
                      style: const TextStyle(
                          color: Colors.white54, fontSize: 12),
                    ),
                  ],
                ),
              ),
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: color.withAlpha(38),
                  borderRadius: BorderRadius.circular(999),
                ),
                child: Text(label,
                    style: TextStyle(
                        color: color,
                        fontSize: 11,
                        fontWeight: FontWeight.bold)),
              ),
            ],
          ),
        ),
      );
    }).toList();
  }
}
