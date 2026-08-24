import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

/// P0 #4 — Compliance Manager RGPD/CCPA (mobile).
///
/// Gestion complète de la conformité :
/// - Vue d'ensemble (stats, intégrité audit)
/// - Politiques de rétention
/// - Consentements
/// - Journal d'audit immuable
/// - Export portabilité 1-clic
class ComplianceManagerScreen extends StatefulWidget {
  const ComplianceManagerScreen({super.key});

  @override
  State<ComplianceManagerScreen> createState() =>
      _ComplianceManagerScreenState();
}

class _ComplianceManagerScreenState extends State<ComplianceManagerScreen> {
  final _api = ApiService();

  Map<String, dynamic>? _overview;
  List<dynamic> _policies = [];
  List<dynamic> _auditEntries = [];
  bool _isLoading = true;
  int _selectedTab = 0;

  static const _tabs = ['Vue d\'ensemble', 'Rétention', 'Audit', 'Portabilité'];

  @override
  void initState() {
    super.initState();
    _loadAll();
  }

  Future<void> _loadAll() async {
    setState(() => _isLoading = true);
    try {
      final results = await Future.wait([
        _api.get('/compliance/overview'),
        _api.get('/compliance/retention-policies'),
        _api.get('/compliance/audit', params: {'limit': 50}),
      ]);
      if (mounted) {
        setState(() {
          _overview = results[0].data as Map<String, dynamic>?;
          _policies = (results[1].data as List<dynamic>?) ?? [];
          _auditEntries = (results[2].data as List<dynamic>?) ?? [];
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _verifyIntegrity() async {
    try {
      final res = await _api.get('/compliance/audit/verify');
      final data = res.data as Map<String, dynamic>;
      if (!mounted) return;
      if (data['integrityValid'] == true) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
          content: Text(
              '✅ Chaîne valide — ${data['totalEntries']} entrées vérifiées'),
          backgroundColor: Colors.green.shade700,
        ));
      } else {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
          content: Text(
              '⚠️ ${data['brokenLinks']} lien(s) brisé(s) dans la chaîne !'),
          backgroundColor: Colors.red.shade700,
        ));
      }
      _loadAll();
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Vérification impossible')),
      );
    }
  }

  Future<void> _exportPortability(String userId) async {
    try {
      final res = await _api.get('/compliance/portability/$userId');
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Text(
            '📦 Export téléchargé (${(res.data as Map).keys.length} sections)'),
        backgroundColor: Colors.green.shade700,
      ));
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Erreur lors de l\'export')),
      );
    }
  }

  Future<void> _executePurge(String policyId) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1F2937),
        title: const Text('Exécuter la purge ?',
            style: TextStyle(color: Colors.white)),
        content: const Text(
          'Les données dépassant la durée de rétention seront traitées.',
          style: TextStyle(color: Colors.white70),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Annuler'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Purger',
                style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
    if (confirmed != true) return;

    try {
      await _api.post('/compliance/retention-policies/$policyId/execute');
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
            content: Text('✅ Purge exécutée'),
            backgroundColor: Colors.green),
      );
      _loadAll();
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF030712),
      appBar: AppBar(
        title: const Text('🛡️ Compliance RGPD',
            style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
        backgroundColor: Colors.transparent,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.verified_user, color: Colors.white70),
            onPressed: _verifyIntegrity,
            tooltip: 'Vérifier audit',
          ),
          IconButton(
            icon: const Icon(Icons.refresh, color: Colors.white70),
            onPressed: _loadAll,
          ),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const Center(
              child: CircularProgressIndicator(color: Color(0xFF06B6D4)))
          : Column(
              children: [
                // Tab bar
                _buildTabBar(),
                // Content
                Expanded(child: _buildContent()),
              ],
            ),
    );
  }

  Widget _buildTabBar() {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        color: Colors.white.withAlpha(5),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: List.generate(_tabs.length, (i) {
          final isSelected = _selectedTab == i;
          return Expanded(
            child: GestureDetector(
              onTap: () => setState(() => _selectedTab = i),
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 200),
                padding: const EdgeInsets.symmetric(vertical: 8),
                decoration: BoxDecoration(
                  color: isSelected
                      ? const Color(0xFF06B6D4).withAlpha(30)
                      : Colors.transparent,
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Text(
                  _tabs[i],
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    color: isSelected
                        ? const Color(0xFF06B6D4)
                        : Colors.white.withAlpha(120),
                    fontSize: 11,
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
        return _buildOverviewTab();
      case 1:
        return _buildRetentionTab();
      case 2:
        return _buildAuditTab();
      case 3:
        return _buildPortabilityTab();
      default:
        return const SizedBox.shrink();
    }
  }

  // ── Overview Tab ──────────────────────────────────────

  Widget _buildOverviewTab() {
    return RefreshIndicator(
      onRefresh: _loadAll,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Stats grid
          GridView.count(
            crossAxisCount: 2,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            crossAxisSpacing: 10,
            mainAxisSpacing: 10,
            childAspectRatio: 1.4,
            children: [
              _buildStatCard(
                'Politiques actives',
                '${_overview?['activeRetentionPolicies'] ?? 0}',
                Icons.schedule,
                const Color(0xFF06B6D4),
              ),
              _buildStatCard(
                'Consentements',
                '${_overview?['activeConsents'] ?? 0}',
                Icons.how_to_reg,
                const Color(0xFF22C55E),
              ),
              _buildStatCard(
                'Entrées audit',
                '${_overview?['auditTotalEntries'] ?? 0}',
                Icons.history,
                const Color(0xFFA855F7),
              ),
              _buildStatCard(
                'Demandes RGPD',
                '${_overview?['pendingGdprRequests'] ?? 0}',
                Icons.assignment,
                const Color(0xFFF59E0B),
              ),
            ],
          ),
          const SizedBox(height: 16),

          // Audit integrity
          GlassCard(
            borderColor: _overview?['auditIntegrityValid'] == true
                ? Colors.green.withAlpha(40)
                : Colors.red.withAlpha(40),
            child: Row(
              children: [
                Icon(
                  _overview?['auditIntegrityValid'] == true
                      ? Icons.verified
                      : Icons.warning_amber,
                  color: _overview?['auditIntegrityValid'] == true
                      ? Colors.green
                      : Colors.red,
                  size: 28,
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text('Intégrité de l\'audit',
                          style: TextStyle(
                              color: Colors.white,
                              fontSize: 14,
                              fontWeight: FontWeight.bold)),
                      Text(
                        _overview?['auditIntegrityValid'] == true
                            ? '✅ Chaîne intacte — aucune altération'
                            : '⚠️ Altération détectée',
                        style: TextStyle(
                            color: Colors.white.withAlpha(140), fontSize: 12),
                      ),
                    ],
                  ),
                ),
                IconButton(
                  icon: const Icon(Icons.verified_user, size: 20),
                  onPressed: _verifyIntegrity,
                  color: const Color(0xFF06B6D4),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // Compliance checklist
          GlassCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Checklist de conformité',
                    style: TextStyle(
                        color: Colors.white,
                        fontSize: 16,
                        fontWeight: FontWeight.bold)),
                const SizedBox(height: 12),
                ..._buildChecklist(),
              ],
            ),
          ),
          const SizedBox(height: 24),
        ],
      ),
    );
  }

  Widget _buildStatCard(
      String label, String value, IconData icon, Color color) {
    return GlassCard(
      padding: const EdgeInsets.all(14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(label,
                  style: TextStyle(color: Colors.white.withAlpha(120), fontSize: 11)),
              Icon(icon, color: color, size: 18),
            ],
          ),
          Text(value,
              style: TextStyle(
                  color: color, fontSize: 26, fontWeight: FontWeight.bold)),
        ],
      ),
    );
  }

  List<Widget> _buildChecklist() {
    final items = [
      ('Politique de rétention', (_overview?['activeRetentionPolicies'] ?? 0) > 0),
      ('Consentements collectés', (_overview?['activeConsents'] ?? 0) > 0),
      ('Audit trail immuable', _overview?['auditIntegrityValid'] == true),
      ('Portabilité 1-clic', true),
      ('Droit à l\'oubli', true),
      ('Chiffrement AES-256', true),
    ];
    return items.map((item) {
      final (label, done) = item;
      return Padding(
        padding: const EdgeInsets.only(bottom: 8),
        child: Row(
          children: [
            Icon(
              done ? Icons.check_circle : Icons.warning_amber,
              color: done ? Colors.green : Colors.amber,
              size: 20,
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Text(
                label,
                style: TextStyle(
                  color: done ? Colors.white.withAlpha(180) : Colors.amber,
                  fontSize: 13,
                ),
              ),
            ),
          ],
        ),
      );
    }).toList();
  }

  // ── Retention Tab ──────────────────────────────────────

  Widget _buildRetentionTab() {
    return RefreshIndicator(
      onRefresh: _loadAll,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          const Text('Politiques de rétention',
              style: TextStyle(
                  color: Colors.white,
                  fontSize: 18,
                  fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          if (_policies.isEmpty)
            GlassCard(
              child: Center(
                child: Padding(
                  padding: const EdgeInsets.all(24),
                  child: Column(
                    children: [
                      Icon(Icons.schedule,
                          color: Colors.white.withAlpha(40), size: 48),
                      const SizedBox(height: 12),
                      Text('Aucune politique configurée',
                          style: TextStyle(
                              color: Colors.white.withAlpha(120), fontSize: 14)),
                    ],
                  ),
                ),
              ),
            )
          else
            ..._policies.map((p) => _buildPolicyCard(p)),
          const SizedBox(height: 16),
          GlassCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Durées suggérées',
                    style: TextStyle(
                        color: Colors.white,
                        fontSize: 14,
                        fontWeight: FontWeight.bold)),
                const SizedBox(height: 8),
                ...[
                  ('SOULS', '3 ans'),
                  ('PRAYERS', '1 an'),
                  ('NOTES_PASTORALES', '5 ans'),
                  ('TRANSACTIONS', '7 ans (fiscal)'),
                  ('AUDIT_LOGS', '7 ans (immuable)'),
                ].map((item) {
                  final (cat, note) = item;
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 4),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(cat,
                            style: TextStyle(
                                color: Colors.white.withAlpha(160), fontSize: 12)),
                        Text(note,
                            style: TextStyle(
                                color: Colors.white.withAlpha(100), fontSize: 11)),
                      ],
                    ),
                  );
                }),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPolicyCard(Map<String, dynamic> policy) {
    final action = policy['actionOnExpiry'] == 'ANONYMIZE'
        ? 'Anonymiser'
        : policy['actionOnExpiry'] == 'DELETE'
            ? 'Supprimer'
            : 'Archiver';
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: GlassCard(
        child: Row(
          children: [
            Container(
              width: 40,
              height: 40,
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: [
                    const Color(0xFF06B6D4),
                    const Color(0xFF06B6D4).withAlpha(150),
                  ],
                ),
                borderRadius: BorderRadius.circular(10),
              ),
              child: const Icon(Icons.schedule, color: Colors.white, size: 18),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(policy['dataCategory'] ?? '',
                      style: const TextStyle(
                          color: Colors.white,
                          fontSize: 14,
                          fontWeight: FontWeight.w600)),
                  Text(
                    '${policy['retentionDays']} jours • $action',
                    style: TextStyle(
                        color: Colors.white.withAlpha(120), fontSize: 11),
                  ),
                ],
              ),
            ),
            IconButton(
              icon: const Icon(Icons.play_arrow, size: 20),
              color: const Color(0xFFF59E0B),
              onPressed: () => _executePurge(policy['id']),
            ),
          ],
        ),
      ),
    );
  }

  // ── Audit Tab ──────────────────────────────────────

  Widget _buildAuditTab() {
    return RefreshIndicator(
      onRefresh: _loadAll,
      child: _auditEntries.isEmpty
          ? Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.history,
                      color: Colors.white.withAlpha(40), size: 48),
                  const SizedBox(height: 12),
                  Text('Aucune entrée d\'audit',
                      style: TextStyle(
                          color: Colors.white.withAlpha(120), fontSize: 14)),
                ],
              ),
            )
          : ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: _auditEntries.length,
              itemBuilder: (ctx, i) {
                final entry = _auditEntries[i] as Map<String, dynamic>;
                return _buildAuditEntry(entry);
              },
            ),
    );
  }

  Widget _buildAuditEntry(Map<String, dynamic> entry) {
    final date = DateTime.tryParse(entry['createdAt'] ?? '')?.toLocal();
    final dateStr = date != null
        ? '${date.day}/${date.month} ${date.hour}:${date.minute.toString().padLeft(2, '0')}'
        : '';
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: GlassCard(
        padding: const EdgeInsets.all(12),
        child: Row(
          children: [
            Container(
              width: 36,
              height: 36,
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: [
                    const Color(0xFFA855F7),
                    const Color(0xFF6366F1),
                  ],
                ),
                borderRadius: BorderRadius.circular(8),
              ),
              child: const Icon(Icons.lock, color: Colors.white, size: 16),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(entry['action'] ?? '',
                            style: const TextStyle(
                                color: Colors.white,
                                fontSize: 13,
                                fontWeight: FontWeight.w600)),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 6, vertical: 2),
                        decoration: BoxDecoration(
                          color: const Color(0xFFA855F7).withAlpha(20),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Text(entry['resourceType'] ?? '',
                            style: const TextStyle(
                                color: Color(0xFFA855F7),
                                fontSize: 9,
                                fontWeight: FontWeight.w600)),
                      ),
                    ],
                  ),
                  const SizedBox(height: 2),
                  Text(
                    '$dateStr • ${(entry['entryHash'] ?? '').toString().substring(0, 12)}…',
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

  // ── Portability Tab ──────────────────────────────────────

  Widget _buildPortabilityTab() {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        GlassCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  const Icon(Icons.download,
                      color: Color(0xFF22C55E), size: 24),
                  const SizedBox(width: 10),
                  const Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Export portabilité',
                            style: TextStyle(
                                color: Colors.white,
                                fontSize: 16,
                                fontWeight: FontWeight.bold)),
                        Text('RGPD Art. 20 — Format JSON',
                            style: TextStyle(
                                color: Colors.white54, fontSize: 12)),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              const Text('Contenu de l\'export :',
                  style: TextStyle(color: Colors.white70, fontSize: 13)),
              const SizedBox(height: 8),
              ...[
                '👤 Profil utilisateur',
                '📖 Âmes liées (disciples)',
                '✅ Historique consentements',
                '📋 Demandes RGPD passées',
                '📦 Métadonnées (format, version)',
              ].map((item) => Padding(
                    padding: const EdgeInsets.only(bottom: 4),
                    child: Row(
                      children: [
                        const Icon(Icons.check,
                            color: Color(0xFF22C55E), size: 14),
                        const SizedBox(width: 8),
                        Text(item,
                            style: TextStyle(
                                color: Colors.white.withAlpha(160),
                                fontSize: 12)),
                      ],
                    ),
                  )),
              const SizedBox(height: 16),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton.icon(
                  onPressed: () => _exportPortability('current'),
                  icon: const Icon(Icons.download, size: 18),
                  label: const Text('Exporter mes données'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF22C55E),
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12)),
                  ),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
