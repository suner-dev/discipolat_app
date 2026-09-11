import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

/// Écran de personnalisation du branding
class TenantBrandingScreen extends ConsumerStatefulWidget {
  const TenantBrandingScreen({super.key});

  @override
  ConsumerState<TenantBrandingScreen> createState() => _TenantBrandingScreenState();
}

class _TenantBrandingScreenState extends ConsumerState<TenantBrandingScreen> {
  bool _loading = true;
  bool _saving = false;
  Map<String, dynamic>? _branding;
  String? _message;
  String? _messageType;

  @override
  void initState() {
    super.initState();
    _loadBranding();
  }

  Future<void> _loadBranding() async {
    try {
      final response = await ApiService().get('/admin/branding');
      setState(() {
        _branding = Map<String, dynamic>.from(response['branding'] ?? {});
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

  Future<void> _saveBranding() async {
    if (_branding == null) return;
    
    setState(() => _saving = true);
    
    try {
      await ApiService().put('/admin/branding', _branding);
      setState(() {
        _saving = false;
        _message = 'Branding mis a jour avec succes';
        _messageType = 'success';
      });
      
      ref.read(tenantSessionProvider).init();
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
        title: const Text('Personnalisation'),
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
                  _buildColorsSection(),
                  const SizedBox(height: 24),
                  _buildIdentitySection(),
                  const SizedBox(height: 24),
                  _buildContactSection(),
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
        ),
      ),
    );
  }

  Widget _buildColorsSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Couleurs',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(child: _colorField('primaryColor', 'Couleur principale')),
                const SizedBox(width: 16),
                Expanded(child: _colorField('secondaryColor', 'Secondaire')),
                const SizedBox(width: 16),
                Expanded(child: _colorField('accentColor', 'Accent')),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _colorField(String key, String label) {
    final colorStr = _branding?[key] ?? '#6366F1';
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
        _branding?[key] = '#${picked.value.toRadixString(16).substring(2)}';
      });
    }
  }

  Widget _buildIdentitySection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Identite',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            TextField(
              decoration: const InputDecoration(
                labelText: 'Nom',
                border: OutlineInputBorder(),
              ),
              onChanged: (v) => setState(() => _branding['churchName'] = v),
            ),
            const SizedBox(height: 16),
            TextField(
              decoration: const InputDecoration(
                labelText: 'Slogan',
                border: OutlineInputBorder(),
              ),
              onChanged: (v) => setState(() => _branding['tagline'] = v),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildContactSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Coordonnees',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            TextField(
              decoration: const InputDecoration(
                labelText: 'Adresse',
                border: OutlineInputBorder(),
              ),
              onChanged: (v) => setState(() => _branding['address'] = v),
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(child: TextField(
                  decoration: const InputDecoration(
                    labelText: 'Tel',
                    border: OutlineInputBorder(),
                  ),
                  onChanged: (v) => setState(() => _branding['phone'] = v),
                )),
                const SizedBox(width: 16),
                Expanded(child: TextField(
                  decoration: const InputDecoration(
                    labelText: 'Email',
                    border: OutlineInputBorder(),
                  ),
                  keyboardType: TextInputType.emailAddress,
                  onChanged: (v) => setState(() => _branding['email'] = v),
                )),
              ],
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
        onPressed: _saving ? null : _saveBranding,
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
        width: 200,
        height: 200,
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
    // Simple mapping position -> couleur
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
