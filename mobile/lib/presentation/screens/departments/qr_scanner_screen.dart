import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';

/// Scanner QR code pour enregistrer la présence d'un membre.
/// Le responsable scan le QR code du membre → présence enregistrée.
///
/// En mode démo (sans caméra), l'utilisateur peut entrer l'ID manuellement.
class QrScannerScreen extends StatefulWidget {
  const QrScannerScreen({super.key});

  @override
  State<QrScannerScreen> createState() => _QrScannerScreenState();
}

class _QrScannerScreenState extends State<QrScannerScreen> {
  final _apiService = ApiService();
  final _manualCtrl = TextEditingController();
  bool _isProcessing = false;
  String? _lastResult;
  bool _lastSuccess = false;
  List<String> _scanHistory = [];

  @override
  void dispose() {
    _manualCtrl.dispose();
    super.dispose();
  }

  Future<void> _processQrData(String qrData) async {
    if (_isProcessing) return;
    setState(() { _isProcessing = true; _lastResult = null; });

    try {
      // Parse QR data format: "discipolat:soul:UUID"
      String soulId;
      if (qrData.startsWith('discipolat:soul:')) {
        soulId = qrData.replaceFirst('discipolat:soul:', '');
      } else {
        // Try as raw UUID
        soulId = qrData.trim();
      }

      // Validate UUID format
      UUID.parse(soulId);

      final res = await _apiService.post('/members/qr-checkin', data: {'soulId': soulId});
      final data = res.data as Map<String, dynamic>;

      HapticFeedback.heavyImpact();
      setState(() {
        _lastSuccess = data['success'] == true;
        _lastResult = data['message']?.toString() ?? (_lastSuccess ? 'Présence enregistrée' : 'Erreur');
        _scanHistory.insert(0, '${DateTime.now().hour}:${DateTime.now().minute.toString().padLeft(2, '0')} — ${soulId.substring(0, 8)}… ${_lastSuccess ? "✅" : "❌"}');
        if (_scanHistory.length > 20) _scanHistory = _scanHistory.sublist(0, 20);
      });
    } catch (e) {
      HapticFeedback.vibrate();
      setState(() {
        _lastSuccess = false;
        _lastResult = 'QR invalide: ${e.toString().length > 50 ? e.toString().substring(0, 50) : e}';
      });
    } finally {
      if (mounted) setState(() => _isProcessing = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Scanner QR Code'), actions: [
        if (_scanHistory.isNotEmpty)
          IconButton(icon: const Icon(Icons.history), onPressed: _showHistory),
      ]),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
            // Scanner icon
            Container(
              width: 120, height: 120,
              decoration: BoxDecoration(
                gradient: LinearGradient(colors: [AppColors.primary, AppColors.primary.withValues(alpha: 0.7)]),
                borderRadius: BorderRadius.circular(24),
                boxShadow: [BoxShadow(color: AppColors.primary.withValues(alpha: 0.3), blurRadius: 20)]),
              child: const Icon(Icons.qr_code_scanner, color: Colors.white, size: 56),
            ),
            const SizedBox(height: 24),
            const Text('Scanner un QR Code', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w700, fontSize: 20)),
            const SizedBox(height: 8),
            Text('Scannez le QR code du membre pour enregistrer sa présence',
                textAlign: TextAlign.center,
                style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 13)),
            const SizedBox(height: 32),

            // Manual entry (fallback)
            GlassCard(
              padding: const EdgeInsets.all(16),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Row(children: [
                  Icon(Icons.keyboard, color: AppColors.primaryLight, size: 18),
                  const SizedBox(width: 8),
                  Text('Saisie manuelle', style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 13, fontWeight: FontWeight.w600)),
                ]),
                const SizedBox(height: 12),
                TextField(
                  controller: _manualCtrl,
                  style: const TextStyle(color: Colors.white, fontSize: 13, fontFamily: 'monospace'),
                  decoration: InputDecoration(
                    hintText: 'Coller le contenu du QR code…',
                    hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.3)),
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.15))),
                    enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.15))),
                    suffixIcon: IconButton(
                      icon: const Icon(Icons.paste, size: 18),
                      onPressed: () async {
                        final data = await Clipboard.getData(Clipboard.kTextPlain);
                        if (data?.text != null) {
                          _manualCtrl.text = data!.text!;
                          _processQrData(data.text!);
                        }
                      }),
                  ),
                  onSubmitted: (v) { if (v.isNotEmpty) _processQrData(v); },
                ),
                const SizedBox(height: 12),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton.icon(
                    onPressed: _isProcessing || _manualCtrl.text.trim().isEmpty
                        ? null
                        : () => _processQrData(_manualCtrl.text.trim()),
                    icon: _isProcessing
                        ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                        : const Icon(Icons.check, size: 16),
                    label: const Text('Enregistrer la présence'),
                    style: FilledButton.styleFrom(backgroundColor: AppColors.primary),
                  ),
                ),
              ]),
            ),
            const SizedBox(height: 16),

            // Last result
            if (_lastResult != null)
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: (_lastSuccess ? Colors.green : Colors.red).withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: (_lastSuccess ? Colors.green : Colors.red).withValues(alpha: 0.3))),
                child: Row(children: [
                  Icon(_lastSuccess ? Icons.check_circle : Icons.error, color: _lastSuccess ? Colors.green : Colors.red, size: 20),
                  const SizedBox(width: 10),
                  Expanded(child: Text(_lastResult!, style: const TextStyle(color: Colors.white, fontSize: 13))),
                ]),
              ),

            // Scan count
            if (_scanHistory.isNotEmpty) ...[
              const SizedBox(height: 16),
              Text('${_scanHistory.length} scan${_scanHistory.length > 1 ? 's' : ''} cette session',
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
            ],
          ]),
        ),
      ),
    );
  }

  void _showHistory() {
    showModalBottomSheet(
      context: context,
      backgroundColor: AppColors.cardDark,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (ctx) => Container(
        padding: const EdgeInsets.all(16),
        child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
          const Text('Historique des scans', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w700, fontSize: 16)),
          const SizedBox(height: 12),
          SizedBox(
            height: 300,
            child: ListView.builder(
              itemCount: _scanHistory.length,
              itemBuilder: (_, i) => Padding(
                padding: const EdgeInsets.only(bottom: 6),
                child: Text(_scanHistory[i], style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 12, fontFamily: 'monospace')),
              ),
            ),
          ),
        ]),
      ),
    );
  }
}

/// Simple UUID parser helper.
class UUID {
  static void parse(String value) {
    final regex = RegExp(r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$');
    if (!regex.hasMatch(value)) {
      throw const FormatException('Invalid UUID format');
    }
  }
}
