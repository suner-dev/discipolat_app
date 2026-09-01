import 'dart:convert';
import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../widgets/secure_screen.dart';

/// Webhook delivery logs — historique des callbacks opérateurs reçus.
class WebhookLogScreen extends StatefulWidget {
  const WebhookLogScreen({super.key});

  @override
  State<WebhookLogScreen> createState() => _WebhookLogScreenState();
}

class _WebhookLogScreenState extends State<WebhookLogScreen> {
  final _api = ApiService();
  List<dynamic> _logs = [];
  bool _loading = true;
  int _page = 0;
  int _totalPages = 0;
  String? _filterProvider;
  String? _filterStatus;

  static const _providers = {
    'M_PESA': 'M-Pesa',
    'MTN_MOMO': 'MTN MoMo',
    'ORANGE_MONEY': 'Orange Money',
    'GENERIC': 'Générique',
  };

  static const _statusColors = {
    'RECEIVED': Color(0xFF2196F3),
    'VERIFIED': Color(0xFF4CAF50),
    'PROCESSED': Color(0xFF00C853),
    'REJECTED': Color(0xFFF44336),
    'ERROR': Color(0xFFFF9800),
  };

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final params = <String, String>{
        'page': '$_page',
        'size': '20',
      };
      if (_filterProvider != null && _filterProvider!.isNotEmpty) {
        params['provider'] = _filterProvider!;
      }
      if (_filterStatus != null && _filterStatus!.isNotEmpty) {
        params['status'] = _filterStatus!;
      }
      final qs = params.entries.map((e) => '${e.key}=${e.value}').join('&');
      final res = await _api.get('/payments/webhooks/logs?$qs');
      final data = res.data as Map<String, dynamic>;
      if (mounted) {
        setState(() {
          _logs = (data['content'] as List?) ?? [];
          _totalPages = (data['totalPages'] as int?) ?? 0;
          _loading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return SecureScreen(
      screenName: 'WebhookLogScreen',
      auditAction: AuditActions.viewPayments,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Webhook Logs'),
          actions: [
            IconButton(
              icon: const Icon(Icons.refresh),
              onPressed: () { setState(() => _loading = true); _load(); },
            ),
          ],
        ),
        drawer: const AppDrawer(),
        body: Column(
          children: [
            _buildFilters(),
            Expanded(
              child: _loading
                  ? const Center(child: CircularProgressIndicator())
                  : _logs.isEmpty
                      ? Center(
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Icon(Icons.webhook, size: 48, color: Colors.white.withAlpha(50)),
                              const SizedBox(height: 12),
                              Text('Aucun log webhook',
                                  style: TextStyle(color: Colors.white.withAlpha(120))),
                            ],
                          ),
                        )
                      : RefreshIndicator(
                          onRefresh: _load,
                          child: ListView.builder(
                            padding: const EdgeInsets.all(12),
                            itemCount: _logs.length + (_page < _totalPages - 1 ? 1 : 0),
                            itemBuilder: (ctx, i) {
                              if (i == _logs.length) {
                                return TextButton(
                                  onPressed: () { _page++; _load(); },
                                  child: const Text('Charger plus'),
                                );
                              }
                              return _buildLogEntry(_logs[i] as Map<String, dynamic>);
                            },
                          ),
                        ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildFilters() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      color: Colors.white.withAlpha(10),
      child: Row(
        children: [
          Expanded(
            child: DropdownButtonFormField<String>(
              value: _filterProvider,
              isDense: true,
              dropdownColor: const Color(0xFF1E293B),
              style: const TextStyle(color: Colors.white, fontSize: 12),
              decoration: InputDecoration(
                hintText: 'Opérateur',
                hintStyle: TextStyle(color: Colors.white.withAlpha(100)),
                isDense: true,
                contentPadding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
              ),
              items: [
                const DropdownMenuItem(value: null, child: Text('Tous')),
                ..._providers.entries.map((e) => DropdownMenuItem(value: e.key, child: Text(e.value))),
              ],
              onChanged: (v) => setState(() { _filterProvider = v; _page = 0; _loading = true; _load(); }),
            ),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: DropdownButtonFormField<String>(
              value: _filterStatus,
              isDense: true,
              dropdownColor: const Color(0xFF1E293B),
              style: const TextStyle(color: Colors.white, fontSize: 12),
              decoration: InputDecoration(
                hintText: 'Statut',
                hintStyle: TextStyle(color: Colors.white.withAlpha(100)),
                isDense: true,
                contentPadding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
              ),
              items: const [
                DropdownMenuItem(value: null, child: Text('Tous')),
                DropdownMenuItem(value: 'RECEIVED', child: Text('Reçu')),
                DropdownMenuItem(value: 'VERIFIED', child: Text('Vérifié')),
                DropdownMenuItem(value: 'PROCESSED', child: Text('Traité')),
                DropdownMenuItem(value: 'REJECTED', child: Text('Rejeté')),
                DropdownMenuItem(value: 'ERROR', child: Text('Erreur')),
              ],
              onChanged: (v) => setState(() { _filterStatus = v; _page = 0; _loading = true; _load(); }),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildLogEntry(Map<String, dynamic> entry) {
    final provider = entry['provider'] as String? ?? '';
    final status = entry['statusLabel'] as String? ?? 'RECEIVED';
    final color = _statusColors[status] ?? Colors.grey;
    final createdAt = entry['createdAt'] as String?;
    final ip = entry['sourceIp'] as String?;
    final ref = entry['reference'] as String?;
    final duration = entry['durationMs'];
    final statusCode = entry['statusCode'];
    final sigValid = entry['signatureValid'];
    final error = entry['errorMessage'] as String?;

    return GlassCard(
      child: Theme(
        data: Theme.of(context).copyWith(dividerColor: Colors.transparent),
        child: ExpansionTile(
          tilePadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
          childrenPadding: const EdgeInsets.all(12),
          title: Row(
            children: [
              Container(
                width: 8, height: 8,
                decoration: BoxDecoration(color: color, shape: BoxShape.circle),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      _providers[provider] ?? provider,
                      style: const TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w600),
                    ),
                    Text(
                      '${createdAt != null ? _fmtDate(createdAt) : '—'} ${ip != null ? '• $ip' : ''}',
                      style: TextStyle(color: Colors.white.withAlpha(150), fontSize: 11),
                    ),
                  ],
                ),
              ),
            ],
          ),
          trailing: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              if (sigValid != null)
                Icon(
                  sigValid ? Icons.verified_user : Icons.gpp_bad,
                  size: 16,
                  color: sigValid ? Colors.greenAccent : Colors.redAccent,
                ),
              const SizedBox(width: 4),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                decoration: BoxDecoration(
                  color: color.withAlpha(38),
                  borderRadius: BorderRadius.circular(999),
                ),
                child: Text(status, style: TextStyle(color: color, fontSize: 10, fontWeight: FontWeight.bold)),
              ),
              const SizedBox(width: 4),
              if (statusCode != null)
                Text('$statusCode', style: TextStyle(
                    color: (statusCode as int) < 300 ? Colors.greenAccent : Colors.redAccent,
                    fontSize: 11, fontFamily: 'monospace')),
              if (duration != null)
                Text('${duration}ms', style: TextStyle(color: Colors.white.withAlpha(120), fontSize: 10)),
            ],
          ),
          children: [
            if (error != null && error.isNotEmpty)
              _detailRow('Erreur', error, Colors.redAccent),
            if (ref != null && ref.isNotEmpty)
              _detailRow('Référence', ref, Colors.cyanAccent),
            if (entry['endpoint'] != null)
              _detailRow('Endpoint', entry['endpoint'], Colors.white70),
            if (entry['requestBody'] != null)
              _codeBlock('Request Body', entry['requestBody']),
            if (entry['responseBody'] != null)
              _codeBlock('Response', entry['responseBody']),
          ],
        ),
      ),
    );
  }

  Widget _detailRow(String label, String value, Color valueColor) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 80,
            child: Text(label, style: TextStyle(color: Colors.white.withAlpha(120), fontSize: 11)),
          ),
          Expanded(
            child: Text(value, style: TextStyle(color: valueColor, fontSize: 11)),
          ),
        ],
      ),
    );
  }

  Widget _codeBlock(String label, String body) {
    // Try to pretty-print JSON
    String display = body;
    try {
      final obj = _tryParseJson(body);
      if (obj != null) {
        display = const JsonEncoder.withIndent('  ').convert(obj);
      }
    } catch (_) {}

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: TextStyle(color: Colors.white.withAlpha(120), fontSize: 11)),
        const SizedBox(height: 4),
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: Colors.black.withAlpha(60),
            borderRadius: BorderRadius.circular(6),
          ),
          child: SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Text(
              display.length > 500 ? '${display.substring(0, 500)}…' : display,
              style: const TextStyle(color: Colors.white70, fontSize: 10, fontFamily: 'monospace'),
            ),
          ),
        ),
        const SizedBox(height: 8),
      ],
    );
  }

  dynamic _tryParseJson(String s) {
    try {
      return jsonDecode(s);
    } catch (_) {
      return null;
    }
  }

  String _fmtDate(String iso) {
    try {
      final dt = DateTime.parse(iso);
      return '${dt.day}/${dt.month}/${dt.year} ${dt.hour}:${dt.minute.toString().padLeft(2, '0')}:${dt.second.toString().padLeft(2, '0')}';
    } catch (_) {
      return iso;
    }
  }
}
