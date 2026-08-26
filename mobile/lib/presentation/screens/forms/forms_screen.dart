import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';
import '../../../l10n/app_localizations.dart';
import 'form_fill_screen.dart';
import 'form_builder_screen.dart';

/// P1 #13 — Formulaires drag & drop: vue des formulaires et soumission — branché API.
class FormsScreen extends StatefulWidget {
  const FormsScreen({super.key, this.apiService});

  final ApiService? apiService;

  @override
  State<FormsScreen> createState() => _FormsScreenState();
}

class _FormsScreenState extends State<FormsScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _forms = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() { _isLoading = true; _error = null; });
    try {
      final res = await _api.get('/api/v1/forms/published');
      if (mounted) {
        setState(() {
          _forms = (res.data is List ? res.data : []) as List<dynamic>;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) setState(() { _error = e.toString(); _isLoading = false; });
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.formsTitle),
        backgroundColor: Colors.teal.shade600,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _load)],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 5)
          : _error != null
              ? Center(child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(Icons.error_outline, color: Colors.white.withValues(alpha: 0.3), size: 48),
                    const SizedBox(height: 12),
                    Text(l10n.formsError, style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                    const SizedBox(height: 12),
                    FilledButton.icon(onPressed: _load, icon: const Icon(Icons.refresh, size: 16), label: Text(l10n.retry)),
                  ],
                ))
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      // Builder toggle
                      GlassCard(
                        padding: const EdgeInsets.all(0),
                        child: ListTile(
                          leading: const Icon(Icons.add_chart, color: Colors.teal),
                          title: Text(l10n.createForm),
                          subtitle: Text(l10n.createFormSubtitle),
                          trailing: const Icon(Icons.chevron_right),
                          onTap: () {
                            Navigator.push(context, MaterialPageRoute(
                              builder: (_) => const FormBuilderScreen(),
                            ));
                          },
                        ),
                      ),
                      const SizedBox(height: 16),
                      // Published forms list
                      Text(l10n.publishedForms, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                      const SizedBox(height: 8),
                      if (_forms.isEmpty)
                        Padding(
                          padding: const EdgeInsets.all(24),
                          child: Center(child: Text(l10n.formsEmpty, style: TextStyle(color: Colors.white.withValues(alpha: 0.5)))),
                        )
                      else
                        ..._forms.map((f) {
                          final form = f as Map<String, dynamic>;
                          final title = form['titre']?.toString() ?? form['title']?.toString() ?? '';
                          final desc = form['description']?.toString() ?? '';
                          final responses = (form['responseCount'] as num?)?.toInt() ?? 0;
                          return GlassCard(
                            margin: const EdgeInsets.only(bottom: 8),
                            padding: const EdgeInsets.all(0),
                            child: ListTile(
                              leading: CircleAvatar(
                                backgroundColor: Colors.teal.withValues(alpha: 0.15),
                                child: const Icon(Icons.description, color: Colors.teal),
                              ),
                              title: Text(title),
                              subtitle: Text(desc.isNotEmpty ? desc : l10n.responsesCount(responses)),
                              trailing: Chip(label: Text(l10n.responsesCount(responses))),
                              onTap: () {
                                Navigator.push(context, MaterialPageRoute(
                                  builder: (_) => FormFillScreen(apiService: _api, form: form),
                                ));
                              },
                            ),
                          );
                        }),
                    ],
                  ),
                ),
    );
  }
}
