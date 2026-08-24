import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

/// P0 #1 — Pont WhatsApp : rappels automatiques et gestion des abonnements famille.
///
/// - Liste des rappels programmés / envoyés / échoués
/// - Commandes WhatsApp (#rejoindre famille, #quitter, #afamille)
/// - Statistiques d'envoi
/// - Programmation de rappels d'événements
class WhatsAppRemindersScreen extends StatefulWidget {
  const WhatsAppRemindersScreen({super.key});

  @override
  State<WhatsAppRemindersScreen> createState() =>
      _WhatsAppRemindersScreenState();
}

class _WhatsAppRemindersScreenState extends State<WhatsAppRemindersScreen> {
  final _api = ApiService();

  List<dynamic> _reminders = [];
  Map<String, dynamic>? _stats;
  Map<String, dynamic>? _config;
  bool _isLoading = true;
  int _selectedTab = 0;

  static const _commandDescriptions = [
    ('#rejoindre', 'S\'inscrire aux annonces générales'),
    ('#rejoindre famille <nom>', 'Recevoir les annonces d\'une famille'),
    ('#quitter famille <nom>', 'Se désabonner d\'une famille'),
    ('#afamille', 'Voir ses familles abonnées'),
    ('#stop', 'Se désabonner de tout'),
    ('#aide', 'Afficher les commandes disponibles'),
  ];

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final results = await Future.wait([
        _api.get('/whatsapp/reminders'),
        _api.get('/whatsapp/stats'),
        _api.get('/whatsapp/config'),
      ]);
      if (mounted) {
        setState(() {
          _reminders = (results[0].data as List<dynamic>?) ?? [];
          _stats = results[1].data as Map<String, dynamic>?;
          _config = results[2].data as Map<String, dynamic>?;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF030712),
      appBar: AppBar(
        title: const Text('📱 WhatsApp',
            style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
        backgroundColor: Colors.transparent,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh, color: Colors.white70),
            onPressed: _loadData,
          ),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const Center(
              child: CircularProgressIndicator(color: Color(0xFF22C55E)))
          : Column(
              children: [
                // Connection status
                _buildStatusBanner(),
                // Tab bar
                _buildTabBar(),
                // Content
                Expanded(child: _buildContent()),
              ],
            ),
    );
  }

  Widget _buildStatusBanner() {
    final isConfigured = _config?['configured'] == true;
    final isEnabled = _config?['enabled'] == true;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
      color: isEnabled
          ? const Color(0xFF22C55E).withAlpha(15)
          : const Color(0xFFF59E0B).withAlpha(15),
      child: Row(
        children: [
          Icon(
            isConfigured
                ? (isEnabled ? Icons.check_circle : Icons.pause_circle)
                : Icons.error_outline,
            color: isEnabled
                ? const Color(0xFF22C55E)
                : const Color(0xFFF59E0B),
            size: 20,
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              isConfigured
                  ? (isEnabled
                      ? 'WhatsApp actif — ${_config?['displayPhoneNumber'] ?? ''}'
                      : 'WhatsApp configuré mais désactivé')
                  : 'WhatsApp non configuré — allez dans Admin > WhatsApp',
              style: TextStyle(
                color: isEnabled
                    ? const Color(0xFF22C55E)
                    : const Color(0xFFF59E0B),
                fontSize: 12,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTabBar() {
    final tabs = ['Rappels', 'Stats', 'Commandes'];
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        color: Colors.white.withAlpha(5),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: List.generate(tabs.length, (i) {
          final isSelected = _selectedTab == i;
          return Expanded(
            child: GestureDetector(
              onTap: () => setState(() => _selectedTab = i),
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 200),
                padding: const EdgeInsets.symmetric(vertical: 8),
                decoration: BoxDecoration(
                  color: isSelected
                      ? const Color(0xFF22C55E).withAlpha(30)
                      : Colors.transparent,
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Text(
                  tabs[i],
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    color: isSelected
                        ? const Color(0xFF22C55E)
                        : Colors.white.withAlpha(120),
                    fontSize: 12,
                    fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                  ),
                ),
              ),
            ),
          );
        }),
      ),
    );
  }

  Widget _buildContent() {
    switch (_selectedTab) {
      case 0:
        return _buildRemindersTab();
      case 1:
        return _buildStatsTab();
      case 2:
        return _buildCommandsTab();
      default:
        return const SizedBox.shrink();
    }
  }

  // ── Reminders Tab ──────────────────────────────────────

  Widget _buildRemindersTab() {
    if (_reminders.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.notifications_none,
                color: Colors.white.withAlpha(40), size: 56),
            const SizedBox(height: 12),
            Text('Aucun rappel programmé',
                style: TextStyle(
                    color: Colors.white.withAlpha(120), fontSize: 14)),
            const SizedBox(height: 4),
            Text(
              'Les rappels d\'événement sont envoyés\n24h avant automatiquement',
              textAlign: TextAlign.center,
              style: TextStyle(
                  color: Colors.white.withAlpha(80), fontSize: 12),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _loadData,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: _reminders.length,
        itemBuilder: (ctx, i) {
          final reminder = _reminders[i] as Map<String, dynamic>;
          return _buildReminderCard(reminder);
        },
      ),
    );
  }

  Widget _buildReminderCard(Map<String, dynamic> reminder) {
    final status = reminder['status'] ?? 'PENDING';
    final statusColor = switch (status) {
      'SENT' => Colors.green,
      'FAILED' => Colors.red,
      'CANCELLED' => Colors.grey,
      _ => const Color(0xFFF59E0B),
    };
    final statusIcon = switch (status) {
      'SENT' => Icons.check_circle,
      'FAILED' => Icons.error,
      'CANCELLED' => Icons.cancel,
      _ => Icons.schedule,
    };
    final scheduledAt = DateTime.tryParse(reminder['scheduledAt'] ?? '');

    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: GlassCard(
        padding: const EdgeInsets.all(14),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              width: 40,
              height: 40,
              decoration: BoxDecoration(
                color: statusColor.withAlpha(20),
                borderRadius: BorderRadius.circular(10),
              ),
              child: Icon(statusIcon, color: statusColor, size: 20),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          reminder['referenceType'] ?? '',
                          style: const TextStyle(
                              color: Colors.white,
                              fontSize: 14,
                              fontWeight: FontWeight.w600),
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 6, vertical: 2),
                        decoration: BoxDecoration(
                          color: statusColor.withAlpha(20),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Text(
                          status,
                          style: TextStyle(
                              color: statusColor,
                              fontSize: 10,
                              fontWeight: FontWeight.bold),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    reminder['message'] ?? '',
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                        color: Colors.white.withAlpha(140), fontSize: 12),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    '📞 ${reminder['phoneNumber'] ?? ''} • ${scheduledAt != null ? _formatDate(scheduledAt) : ''}',
                    style: TextStyle(
                        color: Colors.white.withAlpha(80), fontSize: 10),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  // ── Stats Tab ──────────────────────────────────────

  Widget _buildStatsTab() {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        GridView.count(
          crossAxisCount: 3,
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          crossAxisSpacing: 10,
          mainAxisSpacing: 10,
          childAspectRatio: 1.2,
          children: [
            _buildMiniStat(
              'Entrants',
              '${_stats?['inbound'] ?? 0}',
              Icons.input,
              const Color(0xFF3B82F6),
            ),
            _buildMiniStat(
              'Sortants',
              '${_stats?['outbound'] ?? 0}',
              Icons.output,
              const Color(0xFF22C55E),
            ),
            _buildMiniStat(
              'Livrés',
              '${_stats?['deliveredOrRead'] ?? 0}',
              Icons.check_circle_outline,
              const Color(0xFF06B6D4),
            ),
          ],
        ),
        const SizedBox(height: 16),
        GlassCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Commandes WhatsApp disponibles',
                  style: TextStyle(
                      color: Colors.white,
                      fontSize: 16,
                      fontWeight: FontWeight.bold)),
              const SizedBox(height: 4),
              Text(
                'Envoyez ces commandes par WhatsApp à votre numéro d\'église :',
                style: TextStyle(
                    color: Colors.white.withAlpha(100), fontSize: 12),
              ),
              const SizedBox(height: 12),
              ..._commandDescriptions.map((cmd) {
                final (command, desc) = cmd;
                return Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: Colors.white.withAlpha(4),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Row(
                      children: [
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(command,
                                  style: const TextStyle(
                                      color: Color(0xFF22C55E),
                                      fontSize: 13,
                                      fontWeight: FontWeight.bold,
                                      fontFamily: 'monospace')),
                              Text(desc,
                                  style: TextStyle(
                                      color: Colors.white.withAlpha(120),
                                      fontSize: 11)),
                            ],
                          ),
                        ),
                        Icon(Icons.chevron_right,
                            color: Colors.white.withAlpha(40), size: 18),
                      ],
                    ),
                  ),
                );
              }),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildMiniStat(
      String label, String value, IconData icon, Color color) {
    return GlassCard(
      padding: const EdgeInsets.all(12),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(icon, color: color, size: 22),
          const SizedBox(height: 6),
          Text(value,
              style: TextStyle(
                  color: color, fontSize: 22, fontWeight: FontWeight.bold)),
          Text(label,
              style: TextStyle(color: Colors.white.withAlpha(100), fontSize: 10)),
        ],
      ),
    );
  }

  // ── Commands Tab ──────────────────────────────────────

  Widget _buildCommandsTab() {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        GlassCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Row(
                children: [
                  Text('📱', style: TextStyle(fontSize: 20)),
                  SizedBox(width: 8),
                  Text('Guide des commandes WhatsApp',
                      style: TextStyle(
                          color: Colors.white,
                          fontSize: 16,
                          fontWeight: FontWeight.bold)),
                ],
              ),
              const SizedBox(height: 12),
              Text(
                'Envoyez ces commandes en message WhatsApp au numéro de votre église.',
                style:
                    TextStyle(color: Colors.white.withAlpha(120), fontSize: 12),
              ),
              const SizedBox(height: 16),
              ..._commandDescriptions.map((cmd) {
                final (command, desc) = cmd;
                return Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 10, vertical: 6),
                        decoration: BoxDecoration(
                          color: const Color(0xFF22C55E).withAlpha(15),
                          borderRadius: BorderRadius.circular(8),
                          border: Border.all(
                              color: const Color(0xFF22C55E).withAlpha(30)),
                        ),
                        child: Text(command,
                            style: const TextStyle(
                                color: Color(0xFF22C55E),
                                fontSize: 13,
                                fontWeight: FontWeight.bold,
                                fontFamily: 'monospace')),
                      ),
                      const SizedBox(height: 4),
                      Padding(
                        padding: const EdgeInsets.only(left: 4),
                        child: Text(desc,
                            style: TextStyle(
                                color: Colors.white.withAlpha(140),
                                fontSize: 12)),
                      ),
                    ],
                  ),
                );
              }),
            ],
          ),
        ),
        const SizedBox(height: 12),
        GlassCard(
          borderColor: const Color(0xFFF59E0B).withAlpha(30),
          child: Row(
            children: [
              const Icon(Icons.info_outline,
                  color: Color(0xFFF59E0B), size: 20),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  'Les rappels d\'événement sont envoyés automatiquement 24h avant.',
                  style: TextStyle(
                      color: Colors.white.withAlpha(140), fontSize: 12),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  String _formatDate(DateTime date) {
    return '${date.day}/${date.month}/${date.year} ${date.hour}:${date.minute.toString().padLeft(2, '0')}';
  }
}
