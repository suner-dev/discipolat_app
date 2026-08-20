import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';

/// Workflows Configuration screen — admins can toggle and configure
/// each workflow automation rule for their church.
class WorkflowsConfigScreen extends StatefulWidget {
  final ApiService? apiService;
  const WorkflowsConfigScreen({super.key, this.apiService});

  @override
  State<WorkflowsConfigScreen> createState() => _WorkflowsConfigScreenState();
}

class _WorkflowsConfigScreenState extends State<WorkflowsConfigScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _workflows = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadWorkflows();
  }

  Future<void> _loadWorkflows() async {
    setState(() => _isLoading = true);
    try {
      final res = await _api.get('/workflows');
      final data = res.data;
      List<dynamic> workflows = [];
      if (data is List) {
        workflows = data;
      }
      if (mounted) setState(() { _workflows = workflows; _isLoading = false; });
    } catch (e) {
      if (mounted) setState(() { _isLoading = false; });
    }
  }

  Future<void> _toggleWorkflow(String key) async {
    try {
      await _api.post('/workflows/$key/toggle');
      _loadWorkflows();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Erreur: $e'), backgroundColor: Colors.red),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0A0A1A),
      appBar: AppBar(
        title: const Text('⚙️ Workflows', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
        backgroundColor: Colors.transparent,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh, color: Colors.white),
            onPressed: _loadWorkflows,
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Colors.cyanAccent))
          : RefreshIndicator(
              onRefresh: _loadWorkflows,
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  _buildHeader(),
                  const SizedBox(height: 16),
                  ...List.generate(_workflows.length, (i) => _buildWorkflowCard(_workflows[i])),
                ],
              ),
            ),
    );
  }

  Widget _buildHeader() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.cyanAccent.withAlpha(10),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.cyanAccent.withAlpha(30)),
      ),
      child: Row(
        children: [
          Icon(Icons.auto_fix_high, color: Colors.cyanAccent.withAlpha(200), size: 24),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Automatisations configurables',
                    style: TextStyle(color: Colors.white, fontSize: 15, fontWeight: FontWeight.bold)),
                const SizedBox(height: 4),
                Text('Activez, désactivez et configurez les règles métier de votre église',
                    style: TextStyle(color: Colors.white.withAlpha(150), fontSize: 12)),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildWorkflowCard(dynamic workflow) {
    final key = workflow['key']?.toString() ?? '';
    final label = workflow['label']?.toString() ?? key;
    final description = workflow['description']?.toString() ?? '';
    final enabled = workflow['enabled'] == true;
    final rules = workflow['rules'] as Map<String, dynamic>? ?? {};

    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white.withAlpha(6),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: enabled ? Colors.green.withAlpha(40) : Colors.white.withAlpha(10),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(
                enabled ? Icons.check_circle : Icons.pause_circle,
                color: enabled ? Colors.green : Colors.white.withAlpha(80),
                size: 22,
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(label, style: const TextStyle(color: Colors.white, fontSize: 15, fontWeight: FontWeight.bold)),
                    const SizedBox(height: 2),
                    Text(description, style: TextStyle(color: Colors.white.withAlpha(130), fontSize: 12)),
                  ],
                ),
              ),
              Switch(
                value: enabled,
                onChanged: (_) => _toggleWorkflow(key),
                activeThumbColor: Colors.green,
              ),
            ],
          ),
          if (rules.isNotEmpty) ...[
            const Divider(color: Colors.white10, height: 20),
            Wrap(
              spacing: 8,
              runSpacing: 6,
              children: rules.entries.map((e) {
                return Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: Colors.white.withAlpha(8),
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: Text('${_formatRuleKey(e.key)}: ${e.value}',
                      style: TextStyle(color: Colors.white.withAlpha(180), fontSize: 11)),
                );
              }).toList(),
            ),
          ],
        ],
      ),
    );
  }

  String _formatRuleKey(String key) {
    return key.split('_').map((w) => w[0].toUpperCase() + w.substring(1).toLowerCase()).join(' ');
  }
}
