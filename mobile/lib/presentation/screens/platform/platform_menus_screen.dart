import 'package:flutter/material.dart';
import '../../../l10n/app_localizations.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import 'platform_icons.dart';

const List<String> kMenuRoles = ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'];

/// Administration des entrées de menu (ADMIN).
///
/// Personnalise la navigation : libellé, icône, section, ordre, rôles
/// visibles, rattachement à un module et activation. Permet de réordonner
/// les entrées d'une section (POST /platform/menus/reorder).
class PlatformMenusScreen extends StatefulWidget {
  const PlatformMenusScreen({super.key, this.apiService});

  /// Permet d'injecter un ApiService mocké dans les tests widget.
  final ApiService? apiService;

  @override
  State<PlatformMenusScreen> createState() => _PlatformMenusScreenState();
}

class _PlatformMenusScreenState extends State<PlatformMenusScreen> {
  late final ApiService _apiService = widget.apiService ?? ApiService();
  List<Map<String, dynamic>> _menus = [];
  List<Map<String, dynamic>> _modules = [];
  bool _isLoading = true;

  AppLocalizations get l10n => AppLocalizations.of(context);

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    try {
      final menusRes = await _apiService.get('/platform/admin/menus');
      List<Map<String, dynamic>> modules = [];
      try {
        final modulesRes = await _apiService.get('/platform/modules');
        modules = (modulesRes.data as List).map((e) => e as Map<String, dynamic>).toList();
      } catch (_) {/* les modules sont optionnels pour l'affichage */}
      if (mounted) {
        setState(() {
        _menus = (menusRes.data as List).map((e) => e as Map<String, dynamic>).toList();
        _modules = modules;
        _isLoading = false;
      });
      }
    } catch (_) { if (mounted) setState(() => _isLoading = false); }
  }

  Future<void> _toggle(Map<String, dynamic> menu, bool enabled) async {
    try {
      await _apiService.put('/platform/menus/${menu['id']}', data: {...menu, 'enabled': enabled});
      _load();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.platformMenusSaveError)),
        );
      }
    }
  }

  Future<void> _delete(String id, String label) async {
    try {
      await _apiService.delete('/platform/menus/$id');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Menu « $label » supprimé')),
        );
      }
      _load();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.platformMenusDeleteError)),
        );
      }
    }
  }

  Future<void> _reorder(String section, int from, int to) async {
    final items = _menus.where((m) => (m['section'] as String? ?? 'Général') == section).toList()
      ..sort((a, b) => ((a['ordre'] as num?)?.toInt() ?? 0).compareTo((b['ordre'] as num?)?.toInt() ?? 0));
    if (from < 0 || from >= items.length || to < 0 || to >= items.length) return;
    final moved = items.removeAt(from);
    items.insert(to, moved);
    try {
      await _apiService.post('/platform/menus/reorder', data: [
        for (int i = 0; i < items.length; i++) {'id': items[i]['id'], 'ordre': i, 'section': section},
      ]);
      _load();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.platformMenusSaveError)),
        );
      }
    }
  }

  Future<void> _openEditor([Map<String, dynamic>? menu]) async {
    final isEdit = menu != null;
    final keyCtrl = TextEditingController(text: isEdit ? (menu['key'] as String? ?? '') : '');
    final labelCtrl = TextEditingController(text: isEdit ? (menu['label'] as String? ?? '') : '');
    final hrefCtrl = TextEditingController(text: isEdit ? (menu['href'] as String? ?? '/') : '/');
    var icon = isEdit ? (menu['icon'] as String? ?? 'Menu') : 'Menu';
    final sectionCtrl = TextEditingController(text: isEdit ? (menu['section'] as String? ?? 'Général') : 'Général');
    final ordreCtrl = TextEditingController(text: '${(isEdit ? (menu['ordre'] as num?)?.toInt() : null) ?? 0}');
    var moduleKey = isEdit ? (menu['moduleKey'] as String? ?? '') : '';
    // Sécurité : si le module référencé n'existe plus (supprimé/désactivé),
    // on repart sur « Aucun » pour éviter un crash du DropdownButtonFormField.
    if (moduleKey.isNotEmpty && !_modules.any((m) => m['key'] == moduleKey)) {
      moduleKey = '';
    }
    var roles = List<String>.from(isEdit ? (menu['roles'] as List? ?? <String>[]) : <String>[]);
    var enabled = isEdit ? (menu['enabled'] as bool? ?? true) : true;

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
                Text(isEdit ? l10n.platformMenusEdit : l10n.platformMenusAdd,
                    style: const TextStyle(color: Colors.white, fontSize: 17, fontWeight: FontWeight.bold)),
                const SizedBox(height: 16),
                TextField(
                  controller: keyCtrl,
                  enabled: !isEdit,
                  style: const TextStyle(fontFamily: 'monospace'),
                  decoration: const InputDecoration(labelText: 'Clé', hintText: 'mon-menu'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: labelCtrl,
                  decoration: const InputDecoration(labelText: 'Libellé'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: hrefCtrl,
                  style: const TextStyle(fontFamily: 'monospace'),
                  decoration: const InputDecoration(labelText: 'URL'),
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
                    child: DropdownButtonFormField<String>(
                      initialValue: moduleKey,
                      decoration: const InputDecoration(labelText: 'Module'),
                      items: [
                        const DropdownMenuItem(value: '', child: Text('Aucun')),
                        for (final m in _modules) DropdownMenuItem(value: m['key'] as String, child: Text(m['label'] as String, overflow: TextOverflow.ellipsis)),
                      ],
                      onChanged: (v) => setSheetState(() => moduleKey = v ?? ''),
                    ),
                  ),
                ]),
                const SizedBox(height: 12),
                Row(children: [
                  Expanded(
                    child: TextField(
                      controller: sectionCtrl,
                      decoration: const InputDecoration(labelText: 'Section'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: TextField(
                      controller: ordreCtrl,
                      keyboardType: TextInputType.number,
                      decoration: const InputDecoration(labelText: 'Ordre'),
                    ),
                  ),
                ]),
                const SizedBox(height: 16),
                Text('Rôles visibles', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                const SizedBox(height: 6),
                Wrap(spacing: 6, runSpacing: 6, children: [
                  for (final r in kMenuRoles)
                    FilterChip(
                      label: Text(r, style: const TextStyle(fontSize: 11)),
                      selected: roles.contains(r),
                      onSelected: (sel) => setSheetState(() {
                        if (sel && !roles.contains(r)) { roles.add(r); }
                        if (!sel) { roles.remove(r); }
                      }),
                    ),
                ]),
                const SizedBox(height: 16),
                Row(children: [
                  Switch(
                    value: enabled,
                    activeThumbColor: Colors.green,
                    onChanged: (v) => setSheetState(() => enabled = v),
                  ),
                  const SizedBox(width: 4),
                  Text(enabled ? 'Visibilité active' : 'Masqué',
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                ]),
                const SizedBox(height: 24),
                FilledButton(
                  onPressed: () async {
                    final key = keyCtrl.text.trim();
                    final label = labelCtrl.text.trim();
                    final href = hrefCtrl.text.trim();
                    if (key.isEmpty || label.isEmpty || href.isEmpty) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(content: Text(l10n.platformMenusRequired)),
                      );
                      return;
                    }
                    final payload = {
                      'key': key, 'label': label, 'href': href,
                      'icon': icon, 'section': sectionCtrl.text.trim().isEmpty ? 'Général' : sectionCtrl.text.trim(),
                      'ordre': int.tryParse(ordreCtrl.text.trim()) ?? 0,
                      'roles': roles, 'moduleKey': moduleKey, 'enabled': enabled,
                    };
                    Navigator.pop(ctx);
                    try {
                      if (isEdit) {
                        await _apiService.put('/platform/menus/${menu['id']}', data: payload);
                      } else {
                        await _apiService.post('/platform/menus', data: payload);
                      }
                      if (mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(content: Text(isEdit ? l10n.platformMenusSaveSuccess : l10n.platformMenusSaveSuccess)),
                        );
                      }
                      _load();
                    } catch (_) {
                      if (mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(content: Text(l10n.platformMenusSaveError)),
                        );
                      }
                    }
                  },
                  child: Text(l10n.save),
                ),
                const SizedBox(height: 8),
                if (isEdit)
                  OutlinedButton(
                    style: OutlinedButton.styleFrom(foregroundColor: Colors.redAccent),
                    onPressed: () async {
                      final ok = await showDialog<bool>(
                        context: ctx,
                        builder: (c) => AlertDialog(
                          title: Text('Supprimer le menu « ${menu['label']} » ?'),
                          actions: [
                            TextButton(onPressed: () => Navigator.pop(c, false), child: Text(l10n.no)),
                            TextButton(onPressed: () => Navigator.pop(c, true), child: Text(l10n.yes)),
                          ],
                        ),
                      );
                      if (ok == true && ctx.mounted) {
                        Navigator.pop(ctx);
                        await _delete(menu['id'] as String, menu['label'] as String? ?? '');
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
      final sections = <String, List<Map<String, dynamic>>>{};
    for (final m in _menus) {
      final s = m['section'] as String? ?? 'Général';
      sections.putIfAbsent(s, () => []).add(m);
    }
    sections.forEach((_, items) => items.sort((a, b) => ((a['ordre'] as num?)?.toInt() ?? 0).compareTo((b['ordre'] as num?)?.toInt() ?? 0)));
    final sectionNames = sections.keys.toList();

    return Scaffold(
      appBar: AppBar(title: Text(l10n.platformMenusTitle)),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 4)
          : RefreshIndicator(
              onRefresh: _load,
              child: _menus.isEmpty
                  ? ListView(physics: const AlwaysScrollableScrollPhysics(), children: [
                      Padding(
                        padding: const EdgeInsets.only(top: 120),
                        child: Column(children: [
                          const Icon(Icons.menu_rounded, size: 56, color: Colors.white24),
                          const SizedBox(height: 12),
                          Text(l10n.platformMenusEmpty, style: const TextStyle(color: Colors.white54)),
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
                              Icon(Icons.menu_rounded, color: AppColors.primary, size: 20),
                              const SizedBox(width: 8),
                              Text(l10n.platformMenusSubtitle,
                                  style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
                            ]),
                            const SizedBox(height: 4),
                            Text('Personnalisez les entrées de navigation : ordre, libellé, icônes, rôles visibles et activation.',
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
                              Text('${sections[section]!.length} entrée(s)',
                                  style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 10)),
                            ]),
                          ),
                          for (int idx = 0; idx < sections[section]!.length; idx++)
                            _menuCard(sections[section]![idx], idx, sections[section]!.length, section),
                          const SizedBox(height: 8),
                        ],
                        const SizedBox(height: 24),
                        FilledButton.icon(
                          onPressed: () => _openEditor(),
                          icon: const Icon(Icons.add),
                          label: Text(l10n.platformMenusAdd),
                        ),
                        const SizedBox(height: 24),
                      ],
                    ),
            ),
    );
  }

  Widget _menuCard(Map<String, dynamic> m, int idx, int length, String section) {
    final enabled = m['enabled'] == true;
    final label = m['label'] as String? ?? '';
    final roles = (m['roles'] as List? ?? <dynamic>[]).cast<String>();
    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      borderColor: enabled ? AppColors.primary.withValues(alpha: 0.2) : Colors.white.withValues(alpha: 0.05),
      child: Row(children: [
        // Réordonnancement
        Column(mainAxisSize: MainAxisSize.min, children: [
          IconButton(
            visualDensity: VisualDensity.compact,
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(minWidth: 28, minHeight: 20),
            icon: const Icon(Icons.arrow_upward, color: Colors.white38, size: 14),
            onPressed: idx == 0 ? null : () => _reorder(section, idx, idx - 1),
          ),
          IconButton(
            visualDensity: VisualDensity.compact,
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(minWidth: 28, minHeight: 20),
            icon: const Icon(Icons.arrow_downward, color: Colors.white38, size: 14),
            onPressed: idx >= length - 1 ? null : () => _reorder(section, idx, idx + 1),
          ),
        ]),
        const SizedBox(width: 6),
        Container(
          width: 38,
          height: 38,
          decoration: BoxDecoration(
            gradient: enabled
                ? const LinearGradient(colors: [Color(0xFF16A34A), Color(0xFF15803D)], begin: Alignment.topLeft, end: Alignment.bottomRight)
                : LinearGradient(colors: [Colors.white.withValues(alpha: 0.08), Colors.white.withValues(alpha: 0.04)]),
            borderRadius: BorderRadius.circular(10),
          ),
          child: Icon(platformIcon(m['icon'] as String?), color: enabled ? Colors.white : Colors.white38, size: 18),
        ),
        const SizedBox(width: 10),
        Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            Flexible(child: Text(label,
                style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w600),
                overflow: TextOverflow.ellipsis)),
            const SizedBox(width: 6),
            if ((m['moduleKey'] as String? ?? '').isNotEmpty)
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                decoration: BoxDecoration(
                  color: AppColors.primary.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Text(m['moduleKey'] as String,
                    style: TextStyle(color: AppColors.primaryLight, fontSize: 8, fontWeight: FontWeight.bold)),
              ),
          ]),
          const SizedBox(height: 2),
          Text(m['href'] as String? ?? '',
              style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 10, fontFamily: 'monospace')),
          if (roles.isNotEmpty) ...[
            const SizedBox(height: 4),
            Wrap(spacing: 4, runSpacing: 4, children: [
              for (final r in roles)
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.05),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: AppColors.primary.withValues(alpha: 0.2)),
                  ),
                  child: Text(r, style: TextStyle(color: AppColors.primaryLight, fontSize: 8, fontWeight: FontWeight.w500)),
                ),
            ]),
          ],
        ])),
        const SizedBox(width: 6),
        Column(crossAxisAlignment: CrossAxisAlignment.end, children: [
          Switch(
            value: enabled,
            activeThumbColor: Colors.green,
            onChanged: (v) => _toggle(m, v),
          ),
          Row(mainAxisSize: MainAxisSize.min, children: [
            IconButton(
              visualDensity: VisualDensity.compact,
              icon: const Icon(Icons.edit_rounded, color: Colors.white54, size: 16),
              onPressed: () => _openEditor(m),
            ),
            IconButton(
              visualDensity: VisualDensity.compact,
              icon: const Icon(Icons.delete_rounded, color: Colors.redAccent, size: 16),
              onPressed: () => _confirmDelete(m),
            ),
          ]),
        ]),
      ]),
    );
  }

  /// Confirmation avant suppression depuis la carte.
  Future<void> _confirmDelete(Map<String, dynamic> menu) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        title: Text('Supprimer le menu « ${menu['label']} » ?'),
        content: Text(l10n.irreversibleAction),
        actions: [
          TextButton(onPressed: () => Navigator.pop(c, false), child: Text(l10n.no)),
          TextButton(onPressed: () => Navigator.pop(c, true), child: Text(l10n.yes)),
        ],
      ),
    );
    if (ok == true) {
      await _delete(menu['id'] as String, menu['label'] as String? ?? '');
    }
  }
}
