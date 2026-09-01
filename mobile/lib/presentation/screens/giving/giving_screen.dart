import 'package:flutter/material.dart';
import 'dart:async';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';
import '../../../../l10n/app_localizations.dart';
import '../../widgets/secure_screen.dart';

/// Dîmes & Offrandes 2.0 — don par Mobile Money avec suivi du statut + dons récurrents.
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

  // Recurring donation state
  bool _showRecurringForm = false;
  final _rcAmountCtrl = TextEditingController();
  final _rcPhoneCtrl = TextEditingController();
  String _rcOperator = 'ORANGE_MONEY';
  String _rcPurpose = 'DIME';
  String _rcFrequency = 'MONTHLY';
  bool _rcSubmitting = false;
  List<dynamic> _recurring = [];
  bool _recurringLoading = true;

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
  static const _frequencies = {
    'WEEKLY': 'Hebdomadaire',
    'BIWEEKLY': 'Bimensuel',
    'MONTHLY': 'Mensuel',
    'QUARTERLY': 'Trimestriel',
    'YEARLY': 'Annuel',
  };

  @override
  void initState() {
    super.initState();
    _loadMine();
    _loadRecurring();
  }

  @override
  void dispose() {
    _pollTimer?.cancel();
    _amountCtrl.dispose();
    _phoneCtrl.dispose();
    _rcAmountCtrl.dispose();
    _rcPhoneCtrl.dispose();
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

  Future<void> _loadRecurring() async {
    try {
      final res = await _apiService.get('/payments/recurring/mine');
      if (!mounted) return;
      setState(() {
        _recurring = (res.data is List ? res.data : []) as List<dynamic>;
        _recurringLoading = false;
      });
    } catch (_) {
      if (mounted) setState(() => _recurringLoading = false);
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
            content: Text(AppLocalizations.of(context).paymentInitiated(ref ?? '')),
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
                ScaffoldMessenger.of(context).showSnackBar(SnackBar(
                    content:
                        Text(AppLocalizations.of(context).paymentConfirmed),
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
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
            content: Text(AppLocalizations.of(context).paymentFailed),
            backgroundColor: Color(0xFFC62828)));
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  Future<void> _createRecurring() async {
    final amount = num.tryParse(_rcAmountCtrl.text.trim());
    if (amount == null || amount <= 0) return;
    setState(() => _rcSubmitting = true);
    try {
      await _apiService.post('/payments/recurring', data: {
        'operator': _rcOperator,
        'amount': amount,
        'purpose': _rcPurpose,
        'frequency': _rcFrequency,
        'currency': 'XOF',
        if (_rcPhoneCtrl.text.trim().isNotEmpty)
          'phoneNumber': _rcPhoneCtrl.text.trim(),
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
            content: Text(AppLocalizations.of(context).recurringCreated),
            backgroundColor: const Color(0xFF2E7D32)));
        setState(() {
          _showRecurringForm = false;
          _rcAmountCtrl.clear();
          _rcPhoneCtrl.clear();
        });
        _loadRecurring();
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
            content: Text(AppLocalizations.of(context).paymentFailed),
            backgroundColor: Color(0xFFC62828)));
      }
    } finally {
      if (mounted) setState(() => _rcSubmitting = false);
    }
  }

  Future<void> _cancelRecurring(String id) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        title: Text(AppLocalizations.of(context).cancel),
        content: Text(AppLocalizations.of(context).recurringCancelled),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(c, false),
              child: Text(AppLocalizations.of(context).cancel)),
          TextButton(
              onPressed: () => Navigator.pop(c, true),
              child: const Text('Oui')),
        ],
      ),
    );
    if (ok != true) return;
    try {
      await _apiService.post('/payments/recurring/$id/cancel');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
            content: Text(AppLocalizations.of(context).recurringCancelled),
            backgroundColor: const Color(0xFF2E7D32)));
        _loadRecurring();
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
            content: Text(AppLocalizations.of(context).paymentFailed),
            backgroundColor: Color(0xFFC62828)));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return SecureScreen(
      screenName: 'GivingScreen',
      auditAction: AuditActions.viewPayments,
      child: Scaffold(
      appBar: AppBar(title: Text(AppLocalizations.of(context).tithesAndOfferings)),
      drawer: const AppDrawer(),
      body: RefreshIndicator(
        onRefresh: () async {
          await _loadMine();
          await _loadRecurring();
        },
        child: ListView(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
          children: [
            // ── Formulaire de don unique ──
            GlassCard(
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Text(AppLocalizations.of(context).giveNow,
                        style: const TextStyle(
                            color: Colors.white,
                            fontSize: 16,
                            fontWeight: FontWeight.w600)),
                    const SizedBox(height: 12),
                    TextFormField(
                      controller: _amountCtrl,
                      keyboardType: TextInputType.number,
                      style: const TextStyle(color: Colors.white),
                      decoration: InputDecoration(
                        labelText: AppLocalizations.of(context).amountXOF,
                        labelStyle: TextStyle(color: Colors.white54),
                        prefixIcon: Icon(Icons.payments_rounded,
                            color: Colors.white54),
                      ),
                      validator: (v) => (v == null ||
                              num.tryParse(v) == null ||
                              num.parse(v) <= 0)
                          ? AppLocalizations.of(context).invalidAmount
                          : null,
                    ),
                    const SizedBox(height: 10),
                    DropdownButtonFormField<String>(
                      value: _operator,
                      dropdownColor: const Color(0xFF1E293B),
                      style: const TextStyle(color: Colors.white),
                      decoration: InputDecoration(
                        labelText: AppLocalizations.of(context).operatorLabel,
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
                      value: _purpose,
                      dropdownColor: const Color(0xFF1E293B),
                      style: const TextStyle(color: Colors.white),
                      decoration: InputDecoration(
                        labelText: AppLocalizations.of(context).destinationLabel,
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
                      decoration: InputDecoration(
                        labelText: AppLocalizations.of(context).mobilePhoneOptional,
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
                      label: Text(AppLocalizations.of(context).giveNow),
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
                                AppLocalizations.of(context).waitingConfirmation(_pendingRef!),
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

            // ── Section dons récurrents ──
            GlassCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Row(
                        children: [
                          const Icon(Icons.repeat_rounded,
                              color: Color(0xFF9C27B0), size: 20),
                          const SizedBox(width: 8),
                          Text(AppLocalizations.of(context).recurringTitle,
                              style: const TextStyle(
                                  color: Colors.white,
                                  fontSize: 15,
                                  fontWeight: FontWeight.w600)),
                        ],
                      ),
                      TextButton.icon(
                        onPressed: () =>
                            setState(() => _showRecurringForm = !_showRecurringForm),
                        icon: Icon(
                          _showRecurringForm
                              ? Icons.keyboard_arrow_up
                              : Icons.keyboard_arrow_down,
                          color: Colors.white54,
                        ),
                        label: Text(
                          _showRecurringForm
                              ? AppLocalizations.of(context).recurringHide
                              : AppLocalizations.of(context).recurringNew,
                          style: const TextStyle(color: Colors.white54, fontSize: 12),
                        ),
                      ),
                    ],
                  ),
                  if (_showRecurringForm) ...[
                    const SizedBox(height: 12),
                    _buildRecurringForm(),
                  ],
                  const SizedBox(height: 12),
                  _buildRecurringList(),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // ── Historique des dons ──
            ..._buildHistory(),
          ],
        ),
      ),
    ),
    );
  }

  Widget _buildRecurringForm() {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.05),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          TextFormField(
            controller: _rcAmountCtrl,
            keyboardType: TextInputType.number,
            style: const TextStyle(color: Colors.white),
            decoration: InputDecoration(
              labelText: AppLocalizations.of(context).amountXOF,
              labelStyle: TextStyle(color: Colors.white54),
              prefixIcon: Icon(Icons.payments_rounded, color: Colors.white54),
              isDense: true,
            ),
          ),
          const SizedBox(height: 8),
          DropdownButtonFormField<String>(
            value: _rcPurpose,
            dropdownColor: const Color(0xFF1E293B),
            style: const TextStyle(color: Colors.white, fontSize: 13),
            isDense: true,
            decoration: InputDecoration(
              labelText: AppLocalizations.of(context).destinationLabel,
              labelStyle: TextStyle(color: Colors.white54),
              prefixIcon: Icon(Icons.church_rounded, color: Colors.white54, size: 18),
            ),
            items: _purposes.entries
                .map((e) => DropdownMenuItem(value: e.key, child: Text(e.value)))
                .toList(),
            onChanged: (v) => setState(() => _rcPurpose = v ?? _rcPurpose),
          ),
          const SizedBox(height: 8),
          DropdownButtonFormField<String>(
            value: _rcFrequency,
            dropdownColor: const Color(0xFF1E293B),
            style: const TextStyle(color: Colors.white, fontSize: 13),
            isDense: true,
            decoration: InputDecoration(
              labelText: AppLocalizations.of(context).recurringFrequency,
              labelStyle: TextStyle(color: Colors.white54),
              prefixIcon: Icon(Icons.schedule_rounded, color: Colors.white54, size: 18),
            ),
            items: _frequencies.entries
                .map((e) => DropdownMenuItem(value: e.key, child: Text(e.value)))
                .toList(),
            onChanged: (v) => setState(() => _rcFrequency = v ?? _rcFrequency),
          ),
          const SizedBox(height: 8),
          TextFormField(
            controller: _rcPhoneCtrl,
            keyboardType: TextInputType.phone,
            style: const TextStyle(color: Colors.white),
            decoration: InputDecoration(
              labelText: AppLocalizations.of(context).mobilePhoneOptional,
              labelStyle: TextStyle(color: Colors.white54),
              prefixIcon: Icon(Icons.phone_rounded, color: Colors.white54, size: 18),
            ),
          ),
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              onPressed: _rcSubmitting ? null : _createRecurring,
              icon: _rcSubmitting
                  ? const SizedBox(
                      width: 14, height: 14,
                      child: CircularProgressIndicator(strokeWidth: 2))
                  : const Icon(Icons.repeat_rounded, size: 18),
              label: Text(AppLocalizations.of(context).recurringCreate),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildRecurringList() {
    if (_recurringLoading) {
      return const Padding(
        padding: EdgeInsets.all(12),
        child: Center(
            child: SizedBox(
                width: 16,
                height: 16,
                child: CircularProgressIndicator(strokeWidth: 2))),
      );
    }
    if (_recurring.isEmpty) {
      return Padding(
        padding: const EdgeInsets.all(8),
        child: Text(
          AppLocalizations.of(context).recurringEmpty,
          textAlign: TextAlign.center,
          style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 12),
        ),
      );
    }
    return Column(
      children: _recurring.map((rd) {
        final m = rd as Map<String, dynamic>;
        final active = m['active'] == true;
        final color = active ? const Color(0xFF9C27B0) : Colors.white24;
        return Padding(
          padding: const EdgeInsets.only(bottom: 8),
          child: Row(
            children: [
              Icon(Icons.repeat_rounded, color: color, size: 18),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '${m['amount']} ${m['currency'] ?? 'XOF'} · ${_operators[m['operator']] ?? m['operator']}',
                      style: const TextStyle(
                          color: Colors.white, fontSize: 13, fontWeight: FontWeight.w600),
                    ),
                    Text(
                      '${_frequencies[m['frequency']] ?? m['frequency']}${m['nextDonationDate'] != null ? ' · ${AppLocalizations.of(context).recurringNextDate}: ${m['nextDonationDate']}' : ''}',
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11),
                    ),
                  ],
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                decoration: BoxDecoration(
                  color: color.withAlpha(38),
                  borderRadius: BorderRadius.circular(999),
                ),
                child: Text(
                  active ? AppLocalizations.of(context).recurringActive : AppLocalizations.of(context).recurringInactive,
                  style: TextStyle(color: color, fontSize: 10, fontWeight: FontWeight.bold),
                ),
              ),
              if (active)
                IconButton(
                  visualDensity: VisualDensity.compact,
                  icon: const Icon(Icons.pause_circle_outline, color: Colors.white38, size: 20),
                  onPressed: () => _cancelRecurring(m['id'] as String),
                ),
            ],
          ),
        );
      }).toList(),
    );
  }

  List<Widget> _buildHistory() {
    if (_isLoading) {
      return [
        const Center(
            child: Padding(
                padding: EdgeInsets.all(20),
                child: CircularProgressIndicator())),
      ];
    }
    if (_mine.isEmpty) {
      return [
        GlassCard(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Center(
              child: Text(
                  AppLocalizations.of(context).noDonationsYet,
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
        'CONFIRMED' => AppLocalizations.of(context).statusConfirmed,
        'PENDING' => AppLocalizations.of(context).statusPending,
        'FAILED' => AppLocalizations.of(context).statusFailed,
        'CANCELLED' => AppLocalizations.of(context).statusCancelled,
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
