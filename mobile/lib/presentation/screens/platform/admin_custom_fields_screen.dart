import 'package:flutter/material.dart';
import '../../../l10n/app_localizations.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

/// Administration des champs personnalisés (ADMIN).
class AdminCustomFieldsScreen extends StatefulWidget {
  const AdminCustomFieldsScreen({super.key});

  @override
  State<AdminCustomFieldsScreen> createState() => _AdminCustomFieldsScreenState();
}

class _AdminCustomFieldsScreenState extends State<AdminCustomFieldsScreen> {
  final _apiService = ApiService();
  List<dynamic> _fields = [];
  bool _isLoading = true;

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    setState(() => _isLoading = true);
    try {
      final res = await _apiService.get('/custom-fields');
      _fields = (res.data is List ? res.data : []) as List<dynamic>;
    } catch (_) {}
    if (mounted) setState(() => _isLoading = false);
  }

  Future<void> _create() async {
    final nameCtrl = TextEditingController();
    final entityCtrl = TextEditingController(text: 'SOUL');
    final typeCtrl = TextEditingController(text: 'TEXT');
    final l10n = AppLocalizations.of(context);
    await showDialog(context: context, builder: (ctx) => AlertDialog(
      backgroundColor: AppColors.cardDark,
      title: Text(l10n.newField, style: const TextStyle(color: Colors.white)),
      content: Column(mainAxisSize: MainAxisSize.min, children: [
        TextField(controller: nameCtrl, style: const TextStyle(color: Colors.white), decoration: InputDecoration(labelText: l10n.fieldNameLabel)),
        const SizedBox(height: 8),
        TextField(controller: entityCtrl, style: const TextStyle(color: Colors.white), decoration: InputDecoration(labelText: l10n.fieldEntityLabel)),
        const SizedBox(height: 8),
        TextField(controller: typeCtrl, style: const TextStyle(color: Colors.white), decoration: InputDecoration(labelText: l10n.fieldTypeLabel)),
      ]),
      actions: [
        TextButton(onPressed: () => Navigator.pop(ctx), child: Text(l10n.cancel)),
        FilledButton(onPressed: () async {
          if (nameCtrl.text.trim().isEmpty) return;
          try {
            await _apiService.post('/custom-fields', data: {
              'name': nameCtrl.text.trim(), 'entityType': entityCtrl.text.trim(), 'fieldType': typeCtrl.text.trim(),
            });
            if (ctx.mounted) Navigator.pop(ctx);
            _load();
          } catch (_) {}
        }, child: Text(l10n.create)),
      ],
    ));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(AppLocalizations.of(context).customFields), actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _load)]),
      drawer: const AppDrawer(),
      floatingActionButton: FloatingActionButton(onPressed: _create, child: const Icon(Icons.add)),
      body: _isLoading ? const ShimmerLoading(itemCount: 5) : _fields.isEmpty
          ? Center(child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
              Icon(Icons.text_fields, color: Colors.white.withValues(alpha: 0.15), size: 48),
              const SizedBox(height: 12),
              Text(AppLocalizations.of(context).noCustomFields, style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
            ]))
          : ListView.builder(padding: const EdgeInsets.all(12), itemCount: _fields.length, itemBuilder: (_, i) {
              final f = _fields[i] as Map<String, dynamic>;
              return GlassCard(margin: const EdgeInsets.only(bottom: 8), padding: const EdgeInsets.all(12), child: Row(children: [
                Container(width: 36, height: 36, decoration: BoxDecoration(color: AppColors.primary.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(8)),
                  child: Icon(Icons.text_fields, color: AppColors.primaryLight, size: 18)),
                const SizedBox(width: 10),
                Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text(f['name']?.toString() ?? '', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13)),
                  Text('${f['entityType'] ?? ''} · ${f['fieldType'] ?? ''}', style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                ])),
                IconButton(icon: Icon(Icons.delete_outline, color: Colors.red.withValues(alpha: 0.6), size: 18), onPressed: () async {
                  final ok = await showDialog<bool>(context: context, builder: (c) => AlertDialog(backgroundColor: AppColors.cardDark,
                    title: Text(AppLocalizations.of(context).deleteQuestion, style: const TextStyle(color: Colors.white)),
                    actions: [TextButton(onPressed: () => Navigator.pop(c, false), child: Text(AppLocalizations.of(context).no)), TextButton(onPressed: () => Navigator.pop(c, true), child: Text(AppLocalizations.of(context).yes))]));
                  if (ok == true) { try { await _apiService.delete('/custom-fields/${f['id']}'); _load(); } catch (_) {} }
                }),
              ]));
            }),
    );
  }
}
