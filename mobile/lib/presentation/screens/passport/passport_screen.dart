import 'dart:convert';
import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/app_drawer.dart';

/// Passeport Spirituel — carte de vérification de l'historique spirituel.
/// Connecté à l'API Backend : /api/v1/passports
class PassportScreen extends StatefulWidget {
  const PassportScreen({super.key});

  @override
  State<PassportScreen> createState() => _PassportScreenState();
}

class _PassportScreenState extends State<PassportScreen> {
  final _apiService = ApiService();
  Map<String, dynamic>? _passport;
  List<dynamic> _entries = [];
  bool _isLoading = true;
  bool _canManage = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _isLoading = true);
    try {
      final roleRes = await _apiService.get('/auth/me');
      final roles = roleRes.data['roles'];
      final roleList =
          roles is List ? roles.map((e) => e.toString()).toList() : <String>[];
      _canManage = roleList.any((r) =>
          r == 'ADMIN' ||
          r == 'PASTEUR' ||
          r == 'FAISEUR' ||
          r.endsWith('ROLE_ADMIN') ||
          r.endsWith('ROLE_PASTEUR') ||
          r.endsWith('ROLE_FAISEUR'));
    } catch (_) {
      _canManage = false;
    }
    try {
      final res = await _apiService.get('/passports/mine');
      if (mounted) {
        setState(() {
          _passport = res.data as Map<String, dynamic>?;
          if (_passport != null && _passport!['id'] != null) {
            _loadEntries(_passport!['id'] as String);
          } else {
            _isLoading = false;
          }
        });
      }
    } catch (e) {
      debugPrint('Error loading passport: $e');
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _loadEntries(String passportId) async {
    try {
      final res = await _apiService.get('/passports/$passportId/entries');
      if (mounted) {
        setState(() {
          _entries = (res.data is List ? res.data : []) as List<dynamic>;
          _isLoading = false;
        });
      }
    } catch (e) {
      debugPrint('Error loading entries: $e');
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Color? _statusColor(String? status) {
    switch (status?.toUpperCase()) {
      case 'ACTIVE':
        return const Color(0xFF4CAF50);
      case 'REVOKED':
        return const Color(0xFFC62828);
      case 'EXPIRED':
        return const Color(0xFFFF9800);
      default:
        return Colors.grey;
    }
  }

  IconData _entryIcon(dynamic type) {
    switch (type?.toString().toUpperCase()) {
      case 'BAPTÊME':
      case 'BAPTEME':
        return Icons.church;
      case 'FORMATION':
        return Icons.school;
      case 'SERVICE':
        return Icons.volunteer_activism;
      case 'LEADERSHIP':
        return Icons.leaderboard;
      default:
        return Icons.star;
    }
  }

  @override
  void setState(VoidCallback fn) {
    if (mounted) super.setState(fn);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF030712),
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        title: const Text('Passeport Spirituel'),
        actions: [
          if (_passport != null && _canManage)
            IconButton(
                icon: const Icon(Icons.qr_code_scanner),
                onPressed: _showQrDialog,
                tooltip: 'Code QR'),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const Center(
              child: CircularProgressIndicator(color: Color(0xFFFFB300)))
          : _passport == null
              ? ListView(padding: const EdgeInsets.all(24), children: const [
                  SizedBox(height: 100),
                  Center(
                      child: Column(children: [
                    Icon(Icons.auto_fix_high, size: 64, color: Colors.white24),
                    SizedBox(height: 16),
                    Text('Aucun passeport spirituel',
                        style: TextStyle(color: Colors.white70, fontSize: 16)),
                    SizedBox(height: 8),
                    Text('Votre passeport vous sera délivré par un pasteur.',
                        style: TextStyle(color: Colors.white38, fontSize: 13)),
                  ])),
                ])
              : _buildBody(),
    );
  }

  Widget _buildBody() {
    return ListView(padding: const EdgeInsets.all(16), children: [
      _buildPassportCard(),
      const SizedBox(height: 20),
      const Text('Historique',
          style: TextStyle(
              color: Colors.white, fontWeight: FontWeight.w600, fontSize: 16)),
      const SizedBox(height: 8),
      ListView.separated(
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        itemCount: _entries.length,
        separatorBuilder: (_, __) => const SizedBox(height: 8),
        itemBuilder: (context, i) {
          final entry = _entries[i] as Map<String, dynamic>;
          return Card(
            color: const Color(0xFF1E293B),
            elevation: 2,
            shape:
                RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            child: ListTile(
              leading: CircleAvatar(
                backgroundColor: const Color(0xFFFFB300).withValues(alpha: 0.2),
                child: Icon(_entryIcon(entry['type']),
                    color: const Color(0xFFFFB300), size: 18),
              ),
              title: Text(entry['type'] ?? '',
                  style: const TextStyle(color: Colors.white, fontSize: 13)),
              subtitle: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (entry['description'] != null)
                      Text(entry['description'],
                          style:
                              TextStyle(color: Colors.white54, fontSize: 11)),
                    if (entry['date'] != null)
                      Text(entry['date'],
                          style:
                              TextStyle(color: Colors.white38, fontSize: 10)),
                  ]),
            ),
          );
        },
      ),
    ]);
  }

  Widget _buildPassportCard() {
    final pc = _passport!['passportCode']?.toString() ?? '';
    final displayCode = pc.isNotEmpty ? '${pc.substring(0, (pc.length / 2).round())}...' : '';
    return Card(
      color: const Color(0xFF111827),
      elevation: 6,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(children: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
            decoration: BoxDecoration(
              color: _statusColor(_passport!['status'] as String?)
                  ?.withValues(alpha: 0.2),
              borderRadius: BorderRadius.circular(20),
            ),
            child: Text(_passport!['status'] ?? 'ACTIF',
                style: TextStyle(
                    color: _statusColor(_passport!['status'] as String?),
                    fontSize: 12,
                    fontWeight: FontWeight.bold)),
          ),
          const SizedBox(height: 12),
          Text('Passport #${(_passport!['id'] as String).substring(0, 8)}',
              style: const TextStyle(color: Colors.white54, fontSize: 12)),
          const SizedBox(height: 8),
          Text(displayCode,
              style: const TextStyle(color: Colors.white70, fontSize: 14)),
          const SizedBox(height: 4),
          Text('Émis : ${_passport!['issuedAt'] ?? '—'}',
              style: const TextStyle(color: Colors.white54, fontSize: 11)),
          Text('Expire : ${_passport!['expiresAt'] ?? '—'}',
              style: TextStyle(
                  color: _passport!['expiresAt'] != null
                      ? const Color(0xFFFFB300)
                      : Colors.white54,
                  fontSize: 11)),
        ]),
      ),
    );
  }

  void _showQrDialog() async {
    try {
      final res =
          await _apiService.get('/passports/${_passport!['id'] as String}/qr');
      final qrPngBase64 = res.data['qrPngBase64'] as String?;
      final verificationUrl = res.data['verificationUrl'] as String?;
      if (mounted) {
        showDialog(
            context: context,
            builder: (ctx) => AlertDialog(
                  backgroundColor: const Color(0xFF1E293B),
                  title: const Text('Code QR du passeport',
                      style: TextStyle(color: Colors.white)),
                  content: Column(mainAxisSize: MainAxisSize.min, children: [
                    if (qrPngBase64 != null && qrPngBase64.isNotEmpty)
                      Image.memory(base64Decode(qrPngBase64),
                          width: 200, height: 200, fit: BoxFit.contain)
                    else
                      const SizedBox.shrink(),
                    const SizedBox(height: 12),
                    SelectableText(verificationUrl ?? '',
                        style: TextStyle(color: Colors.white70, fontSize: 12)),
                  ]),
                  actions: [
                    TextButton(
                        onPressed: () => Navigator.pop(ctx),
                        child: const Text('Fermer'))
                  ],
                ));
      }
    } catch (e) {
      debugPrint('Error loading QR: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
            content: Text('Impossible de charger le QR'),
            backgroundColor: Color(0xFFC62828)));
      }
    }
  }
}
