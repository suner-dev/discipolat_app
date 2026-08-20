import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';

/// Affiche le QR code d'un membre pour le check-in rapide.
/// Le responsable scan ce QR pour enregistrer la présence.
class SoulQrScreen extends StatefulWidget {
  final String soulId;
  final String soulNom;

  const SoulQrScreen({super.key, required this.soulId, required this.soulNom});

  @override
  State<SoulQrScreen> createState() => _SoulQrScreenState();
}

class _SoulQrScreenState extends State<SoulQrScreen> {
  final _apiService = ApiService();
  String? _dataUrl;
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadQr();
  }

  Future<void> _loadQr() async {
    setState(() { _isLoading = true; _error = null; });
    try {
      final res = await _apiService.get('/souls/${widget.soulId}/qr-code');
      final data = res.data as Map<String, dynamic>;
      setState(() {
        _dataUrl = data['dataUrl']?.toString();
        _isLoading = false;
      });
    } catch (e) {
      setState(() { _error = 'Erreur de chargement du QR code'; _isLoading = false; });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('QR · ${widget.soulNom}')),
      body: Center(
        child: _isLoading
            ? const CircularProgressIndicator()
            : _error != null
                ? Column(mainAxisAlignment: MainAxisAlignment.center, children: [
                    Icon(Icons.error_outline, color: Colors.red, size: 48),
                    const SizedBox(height: 12),
                    Text(_error!, style: const TextStyle(color: Colors.white70)),
                    const SizedBox(height: 12),
                    FilledButton.icon(onPressed: _loadQr, icon: const Icon(Icons.refresh), label: const Text('Réessayer')),
                  ])
                : _dataUrl != null
                    ? GlassCard(
                        padding: const EdgeInsets.all(24),
                        child: Column(mainAxisSize: MainAxisSize.min, children: [
                          Text(widget.soulNom,
                              style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w700, fontSize: 18)),
                          const SizedBox(height: 8),
                          Text('Présentez ce QR code au responsable',
                              style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                          const SizedBox(height: 16),
                          // QR Code image from base64
                          Container(
                            width: 220, height: 220,
                            decoration: BoxDecoration(
                              color: Colors.white,
                              borderRadius: BorderRadius.circular(16),
                              boxShadow: [BoxShadow(color: AppColors.primary.withValues(alpha: 0.3), blurRadius: 20)]),
                            child: ClipRRect(
                              borderRadius: BorderRadius.circular(16),
                              child: Image.memory(
                                base64Decode(_dataUrl!.split(',').last),
                                fit: BoxFit.contain,
                              ),
                            ),
                          ),
                          const SizedBox(height: 16),
                          Text('ID: ${widget.soulId.substring(0, 8)}…',
                              style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 10, fontFamily: 'monospace')),
                          const SizedBox(height: 12),
                          OutlinedButton.icon(
                            onPressed: () {
                              Clipboard.setData(ClipboardData(text: _dataUrl!));
                              ScaffoldMessenger.of(context).showSnackBar(
                                const SnackBar(content: Text('QR code copié'), duration: Duration(seconds: 1)));
                            },
                            icon: const Icon(Icons.copy, size: 14),
                            label: const Text('Copier le QR', style: TextStyle(fontSize: 11)),
                            style: OutlinedButton.styleFrom(side: BorderSide(color: Colors.white.withValues(alpha: 0.2))),
                          ),
                        ]),
                      )
                    : const Text('QR code non disponible'),
      ),
    );
  }
}
