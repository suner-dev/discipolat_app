import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

/// USSD Discipolat — accès basique via feature phone.
class UssdScreen extends StatefulWidget {
  const UssdScreen({super.key});

  @override
  State<UssdScreen> createState() => _UssdScreenState();
}

class _UssdScreenState extends State<UssdScreen> {
  final _apiService = ApiService();
  bool _isLoading = true;
  bool _ussdConfigured = false;
  String _serviceCode = '';

  @override
  void initState() {
    super.initState();
    _loadStatus();
  }

  Future<void> _loadStatus() async {
    try {
      final res = await _apiService.get('/ussd/status');
      final data = res.data as Map<String, dynamic>;
      if (!mounted) return;
      setState(() {
        _ussdConfigured = data['configured'] as bool? ?? false;
        _serviceCode = data['serviceCode'] as String? ?? '';
        _isLoading = false;
      });
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _dialUssd() async {
    if (_serviceCode.isEmpty) return;
    final ussdCode = '$_serviceCode#';
    final uri = Uri(scheme: 'tel', path: ussdCode);
    try {
      if (await canLaunchUrl(uri)) {
        await launchUrl(uri);
      } else {
        _showSnackBar('Impossible de composer le code USSD');
      }
    } catch (e) {
      _showSnackBar('Erreur: $e');
    }
  }

  void _showSnackBar(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), backgroundColor: Colors.orange),
    );
  }
}

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('USSD — Accès Feature Phone'),
        backgroundColor: Colors.transparent,
        elevation: 0,
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  GlassCard(
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Row(
                        children: [
                          Icon(
                            _ussdConfigured ? Icons.check_circle : Icons.info_outline,
                            color: _ussdConfigured ? Colors.green : Colors.orange,
                            size: 32,
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  _ussdConfigured ? 'Service USSD Actif' : 'Service USSD Non Configuré',
                                  style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16),
                                ),
                                const SizedBox(height: 4),
                                Text(
                                  _ussdConfigured ? 'Composez le code ci-dessous' : 'Contactez votre administrateur',
                                  style: const TextStyle(color: Colors.white54, fontSize: 12),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 20),
                  if (_ussdConfigured && _serviceCode.isNotEmpty) ...[
                    const Text('Code USSD', style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold)),
                    const SizedBox(height: 10),
                    GlassCard(
                      child: InkWell(
                        onTap: _dialUssd,
                        child: Padding(
                          padding: const EdgeInsets.all(20),
                          child: Column(
                            children: [
                              const Icon(Icons.phone, color: Color(0xFF06B6D4), size: 40),
                              const SizedBox(height: 12),
                              Text('$_serviceCode#', style: const TextStyle(color: Colors.white, fontSize: 28, fontWeight: FontWeight.bold, letterSpacing: 2)),
                              const SizedBox(height: 8),
                              const Text('Appuyez pour composer', style: TextStyle(color: Colors.white54, fontSize: 12)),
                            ],
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: 20),
                  ],
                  const Text('Services disponibles', style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 10),
                  _buildServiceItem(Icons.monetization_on, 'Dîme & Offrande', 'Payez via Mobile Money'),
                  _buildServiceItem(Icons.favorite, 'Demande de prière', 'Soumettez un besoin'),
                  _buildServiceItem(Icons.event, 'Événements', 'Prochains événements'),
                  _buildServiceItem(Icons.person, 'Mon compte', 'Historique dons'),
                  _buildServiceItem(Icons.contact_phone, 'Contact église', 'Téléphone et email'),
                ],
              ),
            ),
    );
  }

  Widget _buildServiceItem(IconData icon, String title, String description) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: GlassCard(
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Row(
            children: [
              Icon(icon, color: const Color(0xFF06B6D4), size: 24),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                    Text(description, style: const TextStyle(color: Colors.white54, fontSize: 11)),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
