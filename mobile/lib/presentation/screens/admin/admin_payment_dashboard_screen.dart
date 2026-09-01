import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../widgets/secure_screen.dart';

/// Admin payment dashboard — KPIs, operator/purpose breakdowns, recurring metrics.
class AdminPaymentDashboardScreen extends StatefulWidget {
  const AdminPaymentDashboardScreen({super.key});

  @override
  State<AdminPaymentDashboardScreen> createState() => _AdminPaymentDashboardScreenState();
}

class _AdminPaymentDashboardScreenState extends State<AdminPaymentDashboardScreen> {
  final _api = ApiService();
  Map<String, dynamic>? _data;
  bool _loading = true;

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
    _load();
  }

  Future<void> _load() async {
    try {
      final res = await _api.get('/payments/dashboard');
      if (mounted) setState(() { _data = res.data as Map<String, dynamic>; _loading = false; });
    } catch (_) {
      if (mounted) setState(() => _loading = false);
    }
  }

  String _fmt(dynamic n) {
    final v = (n is num) ? n.toInt() : 0;
    return v.toString().replaceAllMapped(RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))'), (m) => '${m[1]}.');
  }

  @override
  Widget build(BuildContext context) {
    return SecureScreen(
      screenName: 'AdminPaymentDashboard',
      auditAction: AuditActions.viewPayments,
      child: Scaffold(
        appBar: AppBar(title: const Text('Tableau de bord paiements')),
        drawer: const AppDrawer(),
        body: _loading
            ? const Center(child: CircularProgressIndicator())
            : _data == null
                ? const Center(child: Text('Erreur de chargement'))
                : RefreshIndicator(
                    onRefresh: _load,
                    child: ListView(
                      padding: const EdgeInsets.all(16),
                      children: [
                        _buildKPIs(),
                        const SizedBox(height: 16),
                        _buildAmountStats(),
                        const SizedBox(height: 16),
                        _buildOperatorBreakdown(),
                        const SizedBox(height: 16),
                        _buildPurposeBreakdown(),
                        const SizedBox(height: 16),
                        _buildMonthlyTrend(),
                        const SizedBox(height: 16),
                        _buildRecurringSection(),
                      ],
                    ),
                  ),
      ),
    );
  }

  Widget _buildKPIs() {
    final d = _data!;
    final items = [
      {'label': 'Total', 'value': d['total'], 'color': const Color(0xFF2196F3)},
      {'label': 'Confirmés', 'value': d['confirmed'], 'color': const Color(0xFF4CAF50)},
      {'label': 'En attente', 'value': d['pending'], 'color': const Color(0xFFFFB300)},
      {'label': 'Échoués', 'value': d['failed'], 'color': const Color(0xFFE53935)},
    ];
    return GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2, mainAxisSpacing: 10, crossAxisSpacing: 10, childAspectRatio: 1.8),
      itemCount: items.length,
      itemBuilder: (ctx, i) {
        final it = items[i];
        return GlassCard(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(_fmt(it['value']),
                  style: TextStyle(color: it['color'] as Color, fontSize: 22, fontWeight: FontWeight.bold)),
              const SizedBox(height: 4),
              Text(it['label'] as String,
                  style: TextStyle(color: Colors.white.withAlpha(180), fontSize: 11)),
            ],
          ),
        );
      },
    );
  }

  Widget _buildAmountStats() {
    final d = _data!;
    return GlassCard(
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceAround,
        children: [
          _statColumn('Moyen', d['avgAmount']),
          _statColumn('Max', d['maxAmount']),
          _statColumn('Min', d['minAmount']),
          _statColumn('Taux', '${d['confirmationRate']}%'),
        ],
      ),
    );
  }

  Widget _statColumn(String label, dynamic value) {
    final display = (value is num && label != 'Taux')
        ? '${_fmt(value)} XOF'
        : '$value';
    return Column(
      children: [
        Text(display, style: const TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w600)),
        const SizedBox(height: 4),
        Text(label, style: TextStyle(color: Colors.white.withAlpha(150), fontSize: 11)),
      ],
    );
  }

  Widget _buildOperatorBreakdown() {
    final list = (_data!['byOperator'] as List?) ?? [];
    if (list.isEmpty) return const SizedBox.shrink();
    final maxVal = list.fold<double>(1, (prev, e) {
      final v = (e['total'] as num).toDouble();
      return v > prev ? v : prev;
    });
    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Par opérateur', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 15)),
          const SizedBox(height: 12),
          ...list.map((o) {
            final total = (o['total'] as num).toDouble();
            final count = o['count'];
            return Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(_operators[o['operator']] ?? o['operator'] ?? '',
                          style: const TextStyle(color: Colors.white, fontSize: 13)),
                      Text('${_fmt(total)} XOF ($count)',
                          style: TextStyle(color: Colors.white.withAlpha(200), fontSize: 12)),
                    ],
                  ),
                  const SizedBox(height: 4),
                  ClipRRect(
                    borderRadius: BorderRadius.circular(4),
                    child: LinearProgressIndicator(
                      value: total / maxVal,
                      minHeight: 6,
                      backgroundColor: Colors.white.withAlpha(30),
                      valueColor: const AlwaysStoppedAnimation(Color(0xFF00BCD4)),
                    ),
                  ),
                ],
              ),
            );
          }),
        ],
      ),
    );
  }

  Widget _buildPurposeBreakdown() {
    final list = (_data!['byPurpose'] as List?) ?? [];
    if (list.isEmpty) return const SizedBox.shrink();
    final maxVal = list.fold<double>(1, (prev, e) {
      final v = (e['total'] as num).toDouble();
      return v > prev ? v : prev;
    });
    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Par destination', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 15)),
          const SizedBox(height: 12),
          ...list.map((p) {
            final total = (p['total'] as num).toDouble();
            final count = p['count'];
            return Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(_purposes[p['purpose']] ?? p['purpose'] ?? '',
                          style: const TextStyle(color: Colors.white, fontSize: 13)),
                      Text('${_fmt(total)} XOF ($count)',
                          style: TextStyle(color: Colors.white.withAlpha(200), fontSize: 12)),
                    ],
                  ),
                  const SizedBox(height: 4),
                  ClipRRect(
                    borderRadius: BorderRadius.circular(4),
                    child: LinearProgressIndicator(
                      value: total / maxVal,
                      minHeight: 6,
                      backgroundColor: Colors.white.withAlpha(30),
                      valueColor: const AlwaysStoppedAnimation(Color(0xFF9C27B0)),
                    ),
                  ),
                ],
              ),
            );
          }),
        ],
      ),
    );
  }

  Widget _buildMonthlyTrend() {
    final list = (_data!['monthlyTrend'] as List?) ?? [];
    if (list.isEmpty) return const SizedBox.shrink();
    final maxVal = list.fold<double>(1, (prev, e) {
      final v = (e['total'] as num).toDouble();
      return v > prev ? v : prev;
    });
    // Display last 12 months reversed
    final display = list.reversed.take(12).toList();
    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Tendance mensuelle',
              style: TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 15)),
          const SizedBox(height: 12),
          SizedBox(
            height: 120,
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: display.map((m) {
                final total = (m['total'] as num).toDouble();
                final pct = maxVal > 0 ? total / maxVal : 0.0;
                final month = (m['month'] as String);
                return Expanded(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.end,
                    children: [
                      Flexible(
                        child: Container(
                          margin: const EdgeInsets.symmetric(horizontal: 2),
                          decoration: BoxDecoration(
                            borderRadius: const BorderRadius.vertical(top: Radius.circular(4)),
                            gradient: const LinearGradient(
                              colors: [Color(0xFF4CAF50), Color(0xFF00E676)],
                              begin: Alignment.bottomCenter,
                              end: Alignment.topCenter,
                            ),
                          ),
                          height: (pct * 100).clamp(4, 100),
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(month.length > 5 ? month.substring(5) : month,
                          style: TextStyle(color: Colors.white.withAlpha(150), fontSize: 9)),
                    ],
                  ),
                );
              }).toList(),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildRecurringSection() {
    final r = (_data!['recurring'] as Map<String, dynamic>?) ?? {};
    final byFreq = (_data!['recurringByFrequency'] as List?) ?? [];
    final byOp = (_data!['recurringByOperator'] as List?) ?? [];

    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.repeat_rounded, color: Color(0xFF9C27B0), size: 20),
              const SizedBox(width: 8),
              const Text('Dons récurrents',
                  style: TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 15)),
            ],
          ),
          const SizedBox(height: 12),
          // KPIs
          Wrap(
            spacing: 10,
            runSpacing: 10,
            children: [
              _recurringKpi('Actifs', '${r['activeCount'] ?? 0}', const Color(0xFF9C27B0)),
              _recurringKpi('Engagement/mois', '${_fmt(r['monthlyCommitment'])} XOF', const Color(0xFF2196F3)),
              _recurringKpi('Moyen', '${_fmt(r['avgCommitment'])} XOF', const Color(0xFF4CAF50)),
              _recurringKpi('Total traités', '${r['totalProcessed'] ?? 0}', const Color(0xFFFFB300)),
              _recurringKpi('Total donné', '${_fmt(r['totalRecurringDonated'])} XOF', const Color(0xFF00BCD4)),
            ],
          ),
          if (byFreq.isNotEmpty || byOp.isNotEmpty) ...[
            const SizedBox(height: 16),
            if (byFreq.isNotEmpty) ...[
              Text('Par fréquence', style: TextStyle(color: Colors.white.withAlpha(180), fontSize: 12)),
              const SizedBox(height: 6),
              ...byFreq.map((f) => Padding(
                padding: const EdgeInsets.only(bottom: 6),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(_frequencies[f['frequency']] ?? f['frequency'] ?? '',
                        style: const TextStyle(color: Colors.white, fontSize: 13)),
                    Text('${_fmt(f['total'])} XOF (${f['count']})',
                        style: TextStyle(color: Colors.white.withAlpha(200), fontSize: 12)),
                  ],
                ),
              )),
            ],
            if (byOp.isNotEmpty) ...[
              const SizedBox(height: 8),
              Text('Par opérateur', style: TextStyle(color: Colors.white.withAlpha(180), fontSize: 12)),
              const SizedBox(height: 6),
              ...byOp.map((o) => Padding(
                padding: const EdgeInsets.only(bottom: 6),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(_operators[o['operator']] ?? o['operator'] ?? '',
                        style: const TextStyle(color: Colors.white, fontSize: 13)),
                    Text('${_fmt(o['total'])} XOF (${o['count']})',
                        style: TextStyle(color: Colors.white.withAlpha(200), fontSize: 12)),
                  ],
                ),
              )),
            ],
          ],
        ],
      ),
    );
  }

  Widget _recurringKpi(String label, String value, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: color.withAlpha(30),
        borderRadius: BorderRadius.circular(10),
      ),
      child: Column(
        children: [
          Text(value, style: TextStyle(color: color, fontSize: 14, fontWeight: FontWeight.bold)),
          const SizedBox(height: 2),
          Text(label, style: TextStyle(color: Colors.white.withAlpha(180), fontSize: 10)),
        ],
      ),
    );
  }
}
