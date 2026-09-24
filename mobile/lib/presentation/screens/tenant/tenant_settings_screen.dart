import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';
import '../../../data/services/api_service.dart';
import '../../../core/tenant_session.dart';

/// Écran de parametres du tenant (identité, branding, contact, avancé)
class TenantSettingsScreen extends ConsumerStatefulWidget {
  const TenantSettingsScreen({super.key});

  @override
  ConsumerState<TenantSettingsScreen> createState() =>
      _TenantSettingsScreenState();
}

class _TenantSettingsScreenState extends ConsumerState<TenantSettingsScreen> {
  bool _loading = true;
  bool _saving = false;
  Map<String, dynamic> _settings = {};
  String? _message;
  String? _messageType;
  String? _uploadingAsset;
  final ImagePicker _picker = ImagePicker();
  final ApiService _apiService = ApiService();

  @override
  void initState() {
    super.initState();
    _loadSettings();
    _listenForBrandingChanges();
  }

  Future<void> _listenForBrandingChanges() async {
    // TODO: Connect to WebSocket for real-time updates
    // ref.read(tenantSessionProvider).updateBranding will be called from WebSocket handler
  }

  Future<void> _loadSettings() async {
    try {
      final response = await _apiService.get('/admin/settings');
      final data = Map<String, dynamic>.from(response.data as Map);
      setState(() {
        _settings = data;
        _loading = false;
      });
    } catch (e) {
      setState(() {
        _loading = false;
        _message = 'Erreur lors du chargement: $e';
        _messageType = 'error';
      });
    }
  }

  Future<void> _saveSettings() async {
    setState(() => _saving = true);

    try {
      await _apiService.put('/admin/settings', data: _settings);
      setState(() {
        _saving = false;
        _message = 'Parametres mis a jour avec succes';
        _messageType = 'success';
      });
      ref.read(tenantSessionProvider).updateSettings(_settings);
    } catch (e) {
      setState(() {
        _saving = false;
        _message = 'Erreur lors de la mise a jour: $e';
        _messageType = 'error';
      });
    }
  }

  Future<void> _uploadAsset(String assetType) async {
    try {
      final XFile? image = await _picker.pickImage(
        source: ImageSource.gallery,
        maxWidth: 1920,
        maxHeight: 1080,
        imageQuality: 85,
      );
      if (image == null) return;

      setState(() => _uploadingAsset = assetType);

      final response = await _apiService.postImage(
        '/admin/settings/branding/assets',
        fieldName: 'file',
        fileBytes: await image.readAsBytes(),
        filename: image.name,
        data: {'assetType': assetType},
      );
      final data = Map<String, dynamic>.from(response.data as Map);
      final url = data['url'] as String;

      setState(() {
        _uploadingAsset = null;
        switch (assetType) {
          case 'logo':
            _settings['logoUrl'] = url;
            break;
          case 'logo-dark':
            _settings['logoDarkUrl'] = url;
            break;
          case 'cover':
            _settings['coverUrl'] = url;
            break;
          case 'favicon':
            _settings['faviconUrl'] = url;
            break;
        }
        _message = 'Asset $assetType mis a jour';
        _messageType = 'success';
      });

      ref.read(tenantSessionProvider).updateSettings(_settings);
    } catch (e) {
      setState(() {
        _uploadingAsset = null;
        _message = 'Erreur upload: $e';
        _messageType = 'error';
      });
    }
  }

  Widget _buildAssetField(String label, String assetType, String? currentUrl) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label,
            style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500)),
        const SizedBox(height: 8),
        Row(
          children: [
            Expanded(
              child: TextField(
                readOnly: true,
                decoration: InputDecoration(
                  hintText: currentUrl ?? 'Aucun fichier',
                  border: const OutlineInputBorder(),
                  contentPadding:
                      const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                ),
                controller: TextEditingController(text: currentUrl ?? ''),
              ),
            ),
            const SizedBox(width: 8),
            ElevatedButton.icon(
              onPressed: _uploadingAsset == assetType
                  ? null
                  : () => _uploadAsset(assetType),
              icon: _uploadingAsset == assetType
                  ? const SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(strokeWidth: 2))
                  : const Icon(Icons.cloud_upload, size: 18),
              label: const Text('Upload'),
            ),
          ],
        ),
        if (currentUrl != null && currentUrl.isNotEmpty) ...[
          const SizedBox(height: 8),
          Image.network(
            currentUrl,
            height: 80,
            fit: BoxFit.contain,
            errorBuilder: (_, __, ___) => const Text('Erreur chargement image'),
          ),
        ],
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Parametres'),
        actions: [
          if (_message != null && _messageType == 'success')
            const Padding(
              padding: EdgeInsets.only(right: 16),
              child: Icon(Icons.check_circle, color: Colors.green),
            ),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (_message != null) ...[
                    _buildMessage(),
                    const SizedBox(height: 16),
                  ],
                  _buildIdentitySection(),
                  const SizedBox(height: 24),
                  _buildBrandingAssetsSection(),
                  const SizedBox(height: 24),
                  _buildColorsSection(),
                  const SizedBox(height: 24),
                  _buildFontsSection(),
                  const SizedBox(height: 24),
                  _buildContactSection(),
                  const SizedBox(height: 24),
                  _buildAdvancedSection(),
                  const SizedBox(height: 24),
                  _buildSaveButton(),
                ],
              ),
            ),
    );
  }

  Widget _buildMessage() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: _messageType == 'success' ? Colors.green[100] : Colors.red[100],
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(
        _message!,
        style: TextStyle(
          color:
              _messageType == 'success' ? Colors.green[800] : Colors.red[800],
          fontSize: 14,
        ),
      ),
    );
  }

  Widget _buildSectionCard(String title, List<Widget> children) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            ...children,
          ],
        ),
      ),
    );
  }

  Widget _buildIdentitySection() {
    return _buildSectionCard('Identite', [
      _buildTextField('businessName', 'Nom commercial', 'Nom affiche partout'),
      const SizedBox(height: 16),
      _buildTextField('slogan', 'Slogan', 'Accroche'),
      const SizedBox(height: 16),
      _buildTextField('legalName', 'Nom legal', 'Nom juridique'),
      const SizedBox(height: 16),
      _buildTextField('description', 'Description', 'Description de l\'eglise'),
    ]);
  }

  Widget _buildBrandingAssetsSection() {
    return _buildSectionCard('Assets de branding', [
      _buildAssetField('Logo principal', 'logo', _settings['logoUrl']),
      const SizedBox(height: 16),
      _buildAssetField(
          'Logo mode sombre', 'logo-dark', _settings['logoDarkUrl']),
      const SizedBox(height: 16),
      _buildAssetField('Couverture', 'cover', _settings['coverUrl']),
      const SizedBox(height: 16),
      _buildAssetField('Favicon', 'favicon', _settings['faviconUrl']),
    ]);
  }

  Widget _buildColorsSection() {
    final colorFields = [
      ['primaryColor', 'Couleur principale'],
      ['secondaryColor', 'Secondaire'],
      ['accentColor', 'Accent'],
      ['surfaceColor', 'Surface'],
      ['backgroundColor', 'Arriere-plan'],
      ['textPrimaryColor', 'Texte principal'],
      ['textSecondaryColor', 'Texte secondaire'],
      ['successColor', 'Succes'],
      ['warningColor', 'Avertissement'],
      ['errorColor', 'Erreur'],
      ['infoColor', 'Info'],
    ];

    return _buildSectionCard('Couleurs', [
      Wrap(
        spacing: 16,
        runSpacing: 16,
        children: colorFields
            .map((field) => SizedBox(
                  width: 120,
                  child: _colorField(field[0], field[1]),
                ))
            .toList(),
      ),
    ]);
  }

  Widget _colorField(String key, String label) {
    final colorStr = _settings[key] ?? '#6366F1';
    final color = _parseColor(colorStr);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(fontSize: 12, color: Colors.grey)),
        const SizedBox(height: 8),
        GestureDetector(
          onTap: () => _selectColor(key, color),
          child: Container(
            height: 60,
            decoration: BoxDecoration(
              color: color,
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: Colors.grey[300]!),
            ),
          ),
        ),
      ],
    );
  }

  Color _parseColor(String hex) {
    try {
      return Color(int.parse(hex.replaceFirst('#', '0xFF'), radix: 16));
    } catch (e) {
      return const Color(0xFF6366F1);
    }
  }

  Future<void> _selectColor(String key, Color currentColor) async {
    final picked = await showDialog<Color>(
      context: context,
      builder: (context) => _ColorPickerDialog(initialColor: currentColor),
    );
    if (picked != null) {
      setState(() {
        _settings[key] = '#${picked.toARGB32().toRadixString(16).substring(2)}';
      });
    }
  }

  Widget _buildFontsSection() {
    return _buildSectionCard('Polices', [
      _buildTextField('primaryFont', 'Police principale', 'Ex: Inter'),
      const SizedBox(height: 16),
      _buildTextField('secondaryFont', 'Police secondaire', 'Ex: Inter'),
      const SizedBox(height: 16),
      _buildTextField('headingFont', 'Police titres', 'Ex: Inter'),
      const SizedBox(height: 16),
      _buildTextField('monoFont', 'Police monospace', 'Ex: JetBrains Mono'),
    ]);
  }

  Widget _buildContactSection() {
    return _buildSectionCard('Contact & Localisation', [
      _buildTextField('email', 'Email', 'contact@eglise.org',
          keyboardType: TextInputType.emailAddress),
      const SizedBox(height: 16),
      _buildTextField('phone', 'Telephone', '+225 07 07 07 07 07',
          keyboardType: TextInputType.phone),
      const SizedBox(height: 16),
      _buildTextField('website', 'Site web', 'https://eglise.org',
          keyboardType: TextInputType.url),
      const SizedBox(height: 16),
      _buildDropdown('currency', 'Devise', ['XAF', 'EUR', 'USD']),
      const SizedBox(height: 16),
      _buildDropdown('timezone', 'Fuseau horaire', [
        'Africa/Douala',
        'Africa/Lagos',
        'Africa/Kinshasa',
        'Europe/Paris',
        'UTC'
      ]),
      const SizedBox(height: 16),
      _buildTextField('address', 'Adresse', 'Abidjan, Cocody'),
      const SizedBox(height: 16),
      _buildTextField('city', 'Ville', 'Abidjan'),
      const SizedBox(height: 16),
      _buildDropdown('country', 'Pays', ['CM', 'FR', 'BE', 'CH', 'CD']),
    ]);
  }

  Widget _buildAdvancedSection() {
    return _buildSectionCard('Avance', [
      _buildTextField(
          'customCss', 'CSS personnalise', ':root { --brand-radius: 12px; }'),
      const SizedBox(height: 16),
      _buildTextField('customHeadHtml', 'HTML dans <head>',
          '<meta name="theme-color" content="#6366F1">'),
      const SizedBox(height: 16),
      _buildTextField(
          'footerText', 'Pied de page', '© {{year}} {{tenant_name}}'),
      const SizedBox(height: 16),
      _buildDropdown(
          'locale', 'Langue par defaut', ['fr', 'en', 'pt', 'es', 'ar']),
      const SizedBox(height: 16),
      _buildDropdown('dateFormat', 'Format de date',
          ['DD/MM/YYYY', 'MM/DD/YYYY', 'YYYY-MM-DD']),
      const SizedBox(height: 16),
      _buildDropdown('timeFormat', 'Format d\'heure', ['HH:mm', 'hh:mm a']),
    ]);
  }

  Widget _buildTextField(String key, String label, String hint,
      {TextInputType keyboardType = TextInputType.text}) {
    return TextField(
      decoration: InputDecoration(
        labelText: label,
        hintText: hint,
        border: const OutlineInputBorder(),
      ),
      keyboardType: keyboardType,
      controller: TextEditingController(text: _settings[key]?.toString() ?? ''),
      onChanged: (v) => setState(() => _settings[key] = v),
    );
  }

  Widget _buildDropdown(String key, String label, List<String> options) {
    final value = _settings[key]?.toString() ?? options.first;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label,
            style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500)),
        const SizedBox(height: 8),
        DropdownButtonFormField<String>(
          initialValue: options.contains(value) ? value : options.first,
          decoration: const InputDecoration(
            border: OutlineInputBorder(),
            contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          ),
          items: options
              .map((opt) => DropdownMenuItem(
                    value: opt,
                    child: Text(opt),
                  ))
              .toList(),
          onChanged: (v) => setState(() => _settings[key] = v),
        ),
      ],
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
            : const Text('Enregistrer', style: TextStyle(fontSize: 16)),
      ),
    );
  }
}

class _ColorPickerDialog extends StatefulWidget {
  final Color initialColor;
  const _ColorPickerDialog({required this.initialColor});

  @override
  State<_ColorPickerDialog> createState() => _ColorPickerDialogState();
}

class _ColorPickerDialogState extends State<_ColorPickerDialog> {
  late Color _selectedColor;

  @override
  void initState() {
    super.initState();
    _selectedColor = widget.initialColor;
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Choisir une couleur'),
      content: SizedBox(
        width: 280,
        height: 280,
        child: CustomPaint(
          painter: _ColorPickerPainter(_selectedColor),
          child: GestureDetector(
            onTapDown: (details) {
              setState(() {
                _selectedColor = _getColorFromPosition(
                  details.localPosition,
                  MediaQuery.of(context).size.width,
                );
              });
            },
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Annuler'),
        ),
        ElevatedButton(
          onPressed: () => Navigator.of(context).pop(_selectedColor),
          child: const Text('OK'),
        ),
      ],
    );
  }

  Color _getColorFromPosition(Offset position, double width) {
    final hue = (position.dx / width) * 360;
    return HSVColor.fromAHSV(1.0, hue, 1.0, 1.0).toColor();
  }
}

class _ColorPickerPainter extends CustomPainter {
  final Color selectedColor;
  _ColorPickerPainter(this.selectedColor);

  @override
  void paint(Canvas canvas, Size size) {
    final rect = Rect.fromLTWH(0, 0, size.width, size.height);
    final gradient = SweepGradient(
      colors: [
        Colors.red,
        Colors.yellow,
        Colors.green,
        Colors.blue,
        Colors.purple,
        Colors.red,
      ],
    );
    canvas.drawRect(rect, Paint()..shader = gradient.createShader(rect));
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => true;
}
