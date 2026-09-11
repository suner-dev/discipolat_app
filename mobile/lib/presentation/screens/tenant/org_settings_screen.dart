import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

/// Écran de configuration de l'organisation
class OrgSettingsScreen extends ConsumerStatefulWidget {
  const OrgSettingsScreen({super.key});

  @override
  ConsumerState<OrgSettingsScreen> createState() => _OrgSettingsScreenState();
}

class _OrgSettingsScreenState extends ConsumerState<OrgSettingsScreen> {
  bool _loading = true;
  bool _saving = false;
  Map<String, dynamic>? _settings;
  String? _message;
  String? _messageType;

  @override
  void initState() {
    super.initState();
    _loadSettings();
  }

  Future<void> _loadSettings() async {
    try {
      final response = await ApiService().get('/admin/settings');
      setState(() {
        _settings = Map<String, dynamic>.from(response['settings'] ?? {});
        _loading = false;
      });
    } catch (e) {
      setState(() {
        _loading = false;
        _message = 'Erreur lors du chargement';
        _messageType = 'error';
      });
    }
  }

  Future<void> _saveSettings() async {
    if (_settings == null) return;
    
    setState(() => _saving = true);
    
    try {
      await ApiService().put('/admin/settings', _settings);
      setState(() {
        _saving = false;
        _message = 'Parametres mis a jour avec succes';
        _messageType = 'success';
      });
    } catch (e) {
      setState(() {
        _saving = false;
        _message = 'Erreur lors de la mise a jour';
        _messageType = 'error';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Parametres'),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (_message != null)
                    _buildMessage(),
                  const SizedBox(height: 16),
                  _buildGeneralSection(),
                  const SizedBox(height: 24),
                  _buildContactsSection(),
                  const SizedBox(height: 24),
                  _buildSaveButton(),
                ],
              ),
            ),
    );
  }

  Widget _buildMessage() {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: _messageType == 'success' 
            ? Colors.green[100] 
            : Colors.red[100],
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(
        _message!,
        style: TextStyle(
          color: _messageType == 'success' 
              ? Colors.green[800] 
              : Colors.red[800],
          fontSize: 14,
        ),
      ),
    );
  }

  Widget _buildGeneralSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'General',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            _buildDropdown('language', 'Langue', ['fr', 'en', 'pt', 'es', 'ar']),
            const SizedBox(height: 16),
            _buildDropdown('country', 'Pays', ['CM', 'FR', 'BE', 'CH', 'CD']),
            const SizedBox(height: 16),
            _buildDropdown('currency', 'Devise', ['XAF', 'EUR', 'USD']),
            const SizedBox(height: 16),
            _buildDropdown('timezone', 'Fuseau horaire', [
              'Africa/Douala', 'Africa/Lagos', 'Africa/Kinshasa',
              'Europe/Paris', 'UTC'
            ]),
            const SizedBox(height: 16),
            TextField(
              decoration: const InputDecoration(
                labelText: 'Format de date',
                border: OutlineInputBorder(),
              ),
              onChanged: (v) => setState(() => _settings['dateFormat'] = v),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDropdown(String key, String label, List<String> options) {
    final value = _settings?[key] ?? options.first;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(fontSize: 12, color: Colors.grey)),
        const SizedBox(height: 8),
        DropdownButtonFormField<String>(
          value: options.contains(value) ? value : options.first,
          decoration: const InputDecoration(
            border: OutlineInputBorder(),
          ),
          items: options.map((opt) => DropdownMenuItem(
            value: opt,
            child: Text(opt),
          )).toList(),
          onChanged: (v) => setState(() => _settings?[key] = v),
        ),
      ],
    );
  }

  Widget _buildContactsSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Contacts',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            TextField(
              decoration: const InputDecoration(
                labelText: 'Email',
                border: OutlineInputBorder(),
              ),
              keyboardType: TextInputType.emailAddress,
              onChanged: (v) => setState(() => _settings['email'] = v),
            ),
            const SizedBox(height: 16),
            TextField(
              decoration: const InputDecoration(
                labelText: 'Telephone',
                border: OutlineInputBorder(),
              ),
              keyboardType: TextInputType.phone,
              onChanged: (v) => setState(() => _settings['phone'] = v),
            ),
            const SizedBox(height: 16),
            TextField(
              decoration: const InputDecoration(
                labelText: 'Site web',
                border: OutlineInputBorder(),
              ),
              onChanged: (v) => setState(() => _settings['website'] = v),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSaveButton() {
    return SizedBox(
      width: double.infinity,
      child: ElevatedButton(
        onPressed: _saving ? null : _saveSettings,
        style: ElevatedButton.styleFrom(
          backgroundColor: Theme.of(context).primaryColor,
          padding: const EdgeInsets.all(16),
        ),
        child: _saving
            ? const CircularProgressIndicator(color: Colors.white)
            : const Text('Enregistrer'),
      ),
    );
  }
}
