import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Pointage QR — branché sur POST /api/v1/members/qr-checkin.
class QrCheckinScreen extends StatefulWidget {
  const QrCheckinScreen({super.key});

  @override
  State<QrCheckinScreen> createState() => _QrCheckinScreenState();
}

class _QrCheckinScreenState extends State<QrCheckinScreen> {
  final _apiService = ApiService();
  bool _isLoading = false;
  String? _success;
  String? _error;
  final _codeCtrl = TextEditingController();

  @override
  void dispose() {
    _codeCtrl.dispose();
    super.dispose();
  }

  Future<void> _checkIn(String code) async {
    setState(() {
      _isLoading = true;
      _error = null;
      _success = null;
    });
    try {
      final res = await _apiService.post('/members/qr-checkin', data: {'code': code});
      if (mounted) {
        setState(() {
          _isLoading = false;
          _success = res.data is Map ? (res.data['message'] ?? 'Pointage enregistré') : 'Pointage enregistré';
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _error = 'Erreur lors du pointage. Vérifiez le code.';
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Pointage QR'),
        backgroundColor: Colors.indigo,
        foregroundColor: Colors.white,
      ),
      drawer: const AppDrawer(),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                Icons.qr_code_scanner,
                size: 80,
                color: Colors.white.withValues(alpha: 0.2),
              ),
              const SizedBox(height: 24),
              const Text(
                'Pointage par code QR',
                style: TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              Text(
                'Entrez ou scannez le code QR pour pointer votre présence',
                style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 14),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 32),
              GlassCard(
                padding: const EdgeInsets.all(16),
                child: TextField(
                  controller: _codeCtrl,
                  style: const TextStyle(color: Colors.white, fontSize: 18),
                  textAlign: TextAlign.center,
                  decoration: InputDecoration(
                    hintText: 'Entrez le code',
                    hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.3)),
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
                    filled: true,
                    fillColor: Colors.white.withValues(alpha: 0.06),
                  ),
                  onSubmitted: (v) {
                    if (v.isNotEmpty) _checkIn(v);
                  },
                ),
              ),
              const SizedBox(height: 16),
              SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  onPressed: _isLoading || _codeCtrl.text.isEmpty
                      ? null
                      : () => _checkIn(_codeCtrl.text),
                  icon: _isLoading
                      ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                      : const Icon(Icons.check, size: 18),
                  label: const Text('Pointer'),
                  style: FilledButton.styleFrom(backgroundColor: Colors.indigo),
                ),
              ),
              if (_success != null) ...[
                const SizedBox(height: 24),
                GlassCard(
                  padding: const EdgeInsets.all(16),
                  borderColor: Colors.green.withValues(alpha: 0.3),
                  child: Row(
                    children: [
                      const Icon(Icons.check_circle, color: Colors.green, size: 28),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(_success!, style: const TextStyle(color: Colors.green, fontSize: 14, fontWeight: FontWeight.w600)),
                      ),
                    ],
                  ),
                ),
              ],
              if (_error != null) ...[
                const SizedBox(height: 24),
                GlassCard(
                  padding: const EdgeInsets.all(16),
                  borderColor: Colors.red.withValues(alpha: 0.3),
                  child: Row(
                    children: [
                      const Icon(Icons.error, color: Colors.red, size: 28),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(_error!, style: const TextStyle(color: Colors.red, fontSize: 14)),
                      ),
                    ],
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
