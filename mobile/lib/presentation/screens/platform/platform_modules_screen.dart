import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import 'platform_icons.dart';

/// Administration des modules de la plateforme (ADMIN).
///
/// Liste les modules regroupés par section, permet d'activer/désactiver
/// (un module désactivé est masqué des menus ET son API est bloquée côté
/// serveur), de créer, modifier et supprimer un module.
class PlatformModulesScreen extends StatefulWidget {
  const PlatformModulesScreen({super.key, this.apiService});

  /// Permet d'injecter un ApiService mocké dans les tests widget.
  final ApiService? apiService;

  @override
  State<PlatformModulesScreen> createState() => _PlatformModulesScreenState();
}

class _PlatformModulesScreenState extends State<PlatformModulesScreen> {
  late final ApiService _apiService = widget.apiService ?? ApiService();
  List<Map<String, dynamic>> _modules = [];
  bool _isLoading = true;

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    try {
      final res = await _apiService.get('/platform/modules');
      if (mounted) setState(() {
        _modules = (res.data as List).map((e) => e as Map<String, dynamic>).toList();
        _isLoading = false;
      });
    } catch (_) { if (mounted) setState(() => _isLoading = false); }
  }

  Future<void> _toggle(Map<String, dynamic> module, bool enabled) async {
    try {
      await _apiService.put('/platform/modules/${module['key']}', data: {'enabled': enabled});
      _load();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erreur lors de la mise à jour')),
        );
      }
    }
  }

  /// Confirmation avant suppression depuis la carte (pas l'éditeur).
  Future<void> _confirmDelete(Map<String, dynamic> module) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        title: Text('Supprimer le module « ${module['label']} » ?'),
        content: const Text('Cette action est irréversible.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(c, false), child: const Text('Non')),
          TextButton(onPressed: () => Navigator.pop(c, true), child: const Text('Oui')),
        ],
      ),
    );
    if (ok == true) {
      await _delete(module['key'] as String, module['label'] as String? ?? '');
    }
  }

  Future<void> _delete(String key, String label) async {
    try {
      await _apiService.delete('/platform/modules/$key');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Module « $label » supprimé')),
        );
      }
      _load();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Impossible de supprimer ce module')),
        );
      }
    }
  }

  Future<void> _openEditor([Map<String, dynamic>? module]) async {
    final isEdit = module != null;
    final keyCtrl = TextEditingController(text: isEdit ? (module['key'] as String? ?? '') : '');
    final labelCtrl = TextEditingController(text: isEdit ? (module['label'] as String? ?? '') : '');
    final descCtrl = TextEditingController(text: isEdit ? (module['description'] as String? ?? '') : '');
    var icon = isEdit ? (module['icon'] as String? ?? 'Boxes') : 'Boxes';
    final sectionCtrl = TextEditingController(text: isEdit ? (module['section'] as String? ?? 'Général') : 'Général');
    final ordreCtrl = TextEditingController(text: '${(isEdit ? (module['ordre'] as num?)?.toInt() : null) ?? 0}');
    var enabled = isEdit ? (module['enabled'] as bool? ?? true) : true;

    await showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: const Color(0xFF0F172A),
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setSheetState) => Padding(
          padding: EdgeInsets.only(bottom: MediaQuery.of(ctx).viewInsets.bottom),
          child: DraggableScrollableSheet(
            expand: false,
            initialChildSize: 0.9,
            builder: (ctx, scrollController) => ListView(
              controller: scrollController,
              padding: const EdgeInsets.all(20),
              children: [
                Text(isEdit ? 'Modifier le module' : 'Nouveau module',
                    style: const TextStyle(color: Colors.white, fontSize: 17, fontWeight: FontWeight.bold)),
                const SizedBox(height: 16),
                TextField(
                  controller: keyCtrl,
                  enabled: !isEdit,
                  style: const TextStyle(fontFamily: 'monospace'),
                  decoration: const InputDecoration(labelText: 'Clé (unique)', hintText: 'EXEMPLE'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: labelCtrl,
                  decoration: const InputDecoration(labelText: 'Libellé'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: descCtrl,
                  decoration: const InputDecoration(labelText: 'Description'),
                ),
                const SizedBox(height: 16),
                Row(children: [
                  Expanded(
                    child: DropdownButtonFormField<String>(
                      initialValue: icon,
                      decoration: const InputDecoration(labelText: 'Icône'),
                      items: [for (final k in kPlatformIconKeys) DropdownMenuItem(value: k, child: Text(k, style: const TextStyle(fontSize: 13)))],
                      onChanged: (v) => setSheetState(() => icon = v ?? icon),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: TextField(
                      controller: sectionCtrl,
                      decoration: const InputDecoration(labelText: 'Section'),
                    ),
                  ),
                ]),
                const SizedBox(height: 12),
                Row(children: [
                  Expanded(
                    child: TextField(
                      controller: ordreCtrl,
                      keyboardType: TextInputType.number,
                      decoration: const InputDecoration(labelText: 'Ordre'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Row(children: [
                      Switch(
                        value: enabled,
                        activeColor: Colors.green,
                        onChanged: (v) => setSheetState(() => enabled = v),
                      ),
                      const SizedBox(width: 4),
                      Expanded(child: Text(enabled ? 'Actif' : 'Désactivé',
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12))),
                    ]),
                  ),
                ]),
                const SizedBox(height: 24),
                FilledButton(
                  onPressed: () async {
                    final key = keyCtrl.text.trim().toUpperCase();
                    final label = labelCtrl.text.trim();
                    if (key.isEmpty || label.isEmpty) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('Clé et libellé sont obligatoires')),
                      );
                      return;
                    }
                    final payload = {
                      'key': key, 'label': label, 'description': descCtrl.text.trim(),
                      'icon': icon, 'section': sectionCtrl.text.trim().isEmpty ? 'Général' : sectionCtrl.text.trim(),
                      'ordre': int.tryParse(ordreCtrl.text.trim()) ?? 0,
                      'enabled': enabled,
                    };
                    Navigator.pop(ctx);
                    try {
                      if (isEdit) {
                        await _apiService.put('/platform/modules/$key/edit', data: payload);
                      } else {
                        await _apiService.post('/platform/modules', data: payload);
                      }
                      if (mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(content: Text(isEdit ? 'Module modifié' : 'Module créé')),
                        );
                      }
                      _load();
                    } catch (_) {
                      if (mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('Erreur lors de l\u2019enregistrement')),
                        );
                      }
                    }
                  },
                  child: const Text('Enregistrer'),
                ),
                const SizedBox(height: 8),
                if (isEdit)
                  OutlinedButton(
                    style: OutlinedButton.styleFrom(foregroundColor: Colors.redAccent),
                    onPressed: () async {
                      final ok = await showDialog<bool>(
                        context: ctx,
                        builder: (c) => AlertDialog(
                          title: Text('Supprimer le module « ${module['label']} » ?'),
                          actions: [
                            TextButton(onPressed: () => Navigator.pop(c, false), child: const Text('Non')),
                            TextButton(onPressed: () => Navigator.pop(c, true), child: const Text('Oui')),
                          ],
                        ),
                      );
                      if (ok == true) {
                        Navigator.pop(ctx);
                        await _delete(module['key'] as String, module['label'] as String? ?? '');
                      }
                    },
                    child: const Text('Supprimer'),
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    // Regroupement par section, tri par ordre à l'intérieur de chaque section.
    final sections = <String, List<Map<String, dynamic>>>{};
    for (final m in _modules) {
      final s = m['section'] as String? ?? 'Général';
      sections.putIfAbsent(s, () => []).add(m);
    }
    sections.forEach((_, items) => items.sort((a, b) => ((a['ordre'] as num?)?.toInt() ?? 0).compareTo((b['ordre'] as num?)?.toInt() ?? 0)));
    final sectionNames = sections.keys.toList();

    return Scaffold(
      appBar: AppBar(title: const Text('Modules')),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 4)
          : RefreshIndicator(
              onRefresh: _load,
              child: _modules.isEmpty
                  ? ListView(physics: const AlwaysScrollableScrollPhysics(), children: const [
                      Padding(
                        padding: EdgeInsets.only(top: 120),
                        child: Column(children: [
                          Icon(Icons.inventory_2_outlined, size: 56, color: Colors.white24),
                          SizedBox(height: 12),
                          Text('Aucun module', style: TextStyle(color: Colors.white54)),
                        ]),
                      ),
                    ])
                  : ListView(
                      padding: const EdgeInsets.all(16),
                      children: [
                        GlassCard(
                          padding: const EdgeInsets.all(16),
                          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                            Row(children: [
                              Icon(Icons.inventory_2_rounded, color: AppColors.primary, size: 20),
                              const SizedBox(width: 8),
                              const Text('Modules de la plateforme',
                                  style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
                            ]),
                            const SizedBox(height: 4),
                            Text('Activez ou désactivez les grands modules. Un module désactivé est masqué des menus et son API est bloquée côté serveur.',
                                style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                          ]),
                        ),
                        const SizedBox(height: 16),
                        for (final section in sectionNames) ...[
                          Padding(
                            padding: const EdgeInsets.only(bottom: 8, left: 4),
                            child: Row(children: [
                              Text(section.toUpperCase(),
                                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11, fontWeight: FontWeight.bold, letterSpacing: 0.8)),
                              const SizedBox(width: 8),
                              Text('${sections[section]!.length} module(s)',
                                  style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 10)),
                            ]),
                          ),
                          for (final m in sections[section]!) ...[
                            _moduleCard(m),
                            const SizedBox(height: 8),
                          ],
                        ],
                        const SizedBox(height: 24),
                        FilledButton.icon(
                          onPressed: () => _openEditor(),
                          icon: const Icon(Icons.add),
                          label: const Text('Nouveau module'),
                        ),
                        const SizedBox(height: 24),
                      ],
                    ),
            ),
    );
  }

  Widget _moduleCard(Map<String, dynamic> m) {
    final enabled = m['enabled'] == true;
    final label = m['label'] as String? ?? '';
    return GlassCard(
      padding: const EdgeInsets.all(14),
      borderColor: enabled ? AppColors.primary.withValues(alpha: 0.25) : Colors.white.withValues(alpha: 0.05),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          Container(
            width: 42,
            height: 42,
            decoration: BoxDecoration(
              gradient: enabled
                  ? const LinearGradient(colors: [Color(0xFF16A34A), Color(0xFF15803D)], begin: Alignment.topLeft, end: Alignment.bottomRight)
                  : LinearGradient(colors: [Colors.white.withValues(alpha: 0.08), Colors.white.withValues(alpha: 0.04)]),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(platformIcon(m['icon'] as String?),
                color: enabled ? Colors.white : Colors.white38, size: 20),
          ),
          const SizedBox(width: 12),
          Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Row(children: [
              Flexible(child: Text(label,
                  style: const TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w600),
                  overflow: TextOverflow.ellipsis)),
              const SizedBox(width: 6),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.06),
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Text(m['key'] as String? ?? '',
                    style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 9, fontFamily: 'monospace')),
              ),
            ]),
            if ((m['description'] as String? ?? '').isNotEmpty) ...[
              const SizedBox(height: 3),
              Text(m['description'] as String,
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11),
                  maxLines: 2, overflow: TextOverflow.ellipsis),
            ],
          ])),
          const SizedBox(width: 8),
          Switch(
            value: enabled,
            activeColor: Colors.green,
            onChanged: (v) => _toggle(m, v),
          ),
        ]),
        const SizedBox(height: 8),
        Row(children: [
          StatusBadge(label: enabled ? 'Actif' : 'Inactif', color: enabled ? Colors.green : Colors.blueGrey),
          const Spacer(),
          IconButton(
            visualDensity: VisualDensity.compact,
            icon: const Icon(Icons.edit_rounded, color: Colors.white54, size: 18),
            onPressed: () => _openEditor(m),
          ),
          IconButton(
            visualDensity: VisualDensity.compact,
            icon: const Icon(Icons.delete_rounded, color: Colors.redAccent, size: 18),
            onPressed: () => _confirmDelete(m),
          ),
        ]),
      ]),
    );
  }
}
