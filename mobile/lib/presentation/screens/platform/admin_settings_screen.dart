import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';
import '../../../../l10n/app_localizations.dart';

/// Administration des paramètres de l'église (nom, logo, couleurs).
class AdminSettingsScreen extends StatefulWidget {
  const AdminSettingsScreen({super.key});

  @override
  State<AdminSettingsScreen> createState() => _AdminSettingsScreenState();
}

class _AdminSettingsScreenState extends State<AdminSettingsScreen> {
  final _apiService = ApiService();
  Map<String, dynamic>? _settings;
  bool _isLoading = true;
  bool _isSaving = false;

  final _nameCtrl = TextEditingController();
  final _sloganCtrl = TextEditingController();
  final _primaryColorCtrl = TextEditingController();
  final _accentColorCtrl = TextEditingController();

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _nameCtrl.dispose();
    _sloganCtrl.dispose();
    _primaryColorCtrl.dispose();
    _accentColorCtrl.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() => _isLoading = true);
    try {
      final res = await _apiService.get('/admin/settings');
      _settings = res.data as Map<String, dynamic>?;
      _nameCtrl.text = _settings?['churchName']?.toString() ?? '';
      _sloganCtrl.text = _settings?['slogan']?.toString() ?? '';
      _primaryColorCtrl.text = _settings?['primaryColor']?.toString() ?? '#6366f1';
      _accentColorCtrl.text = _settings?['accentColor']?.toString() ?? '#f59e0b';
    } catch (_) {}
    if (mounted) setState(() => _isLoading = false);
  }

  Future<void> _save() async {
    setState(() => _isSaving = true);
    try {
      await _apiService.put('/admin/settings', data: {
        'churchName': _nameCtrl.text.trim(),
        'slogan': _sloganCtrl.text.trim(),
        'primaryColor': _primaryColorCtrl.text.trim(),
        'accentColor': _accentColorCtrl.text.trim(),
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(AppLocalizations.of(context).settingsSaved), backgroundColor: Colors.green),
        );
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(AppLocalizations.of(context).saveFailed), backgroundColor: Colors.red),
        );
      }
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context).churchSettings),
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _load)],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 5)
          : SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  SectionTitle(title: AppLocalizations.of(context).identity, icon: Icons.church),
                  GlassCard(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      children: [
                        _field(AppLocalizations.of(context).churchName, _nameCtrl, Icons.church),
                        const SizedBox(height: 12),
                        _field(AppLocalizations.of(context).slogan, _sloganCtrl, Icons.format_quote),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                  SectionTitle(title: AppLocalizations.of(context).colorsSection, icon: Icons.palette),
                  GlassCard(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      children: [
                        _field(AppLocalizations.of(context).primaryColorLabel, _primaryColorCtrl, Icons.circle, hint: '#6366f1'),
                        const SizedBox(height: 12),
                        _field(AppLocalizations.of(context).accentColorLabel, _accentColorCtrl, Icons.circle, hint: '#f59e0b'),
                      ],
                    ),
                  ),
                  const SizedBox(height: 24),
                  SizedBox(
                    width: double.infinity,
                    height: 48,
                    child: FilledButton.icon(
                      onPressed: _isSaving ? null : _save,
                      icon: _isSaving
                          ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                          : const Icon(Icons.save, size: 16),
                      label: Text(AppLocalizations.of(context).save),
                      style: FilledButton.styleFrom(backgroundColor: AppColors.primary),
                    ),
                  ),
                ],
              ),
            ),
    );
  }

  Widget _field(String label, TextEditingController ctrl, IconData icon, {String? hint}) {
    return TextField(
      controller: ctrl,
      style: const TextStyle(color: Colors.white, fontSize: 14),
      decoration: InputDecoration(
        labelText: label,
        labelStyle: TextStyle(color: Colors.white.withValues(alpha: 0.5)),
        hintText: hint,
        hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.3)),
        prefixIcon: Icon(icon, color: AppColors.primaryLight, size: 18),
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.15))),
        enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.15))),
      ),
    );
  }
}
