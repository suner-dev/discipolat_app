// B7 — Formulaires des 7 étapes du wizard (mobile).
//
// Chaque formulaire n'envoie QUE les champs acceptés par le contrat §3.1.
// Validation locale systématique : on n'envoie jamais de donnée invalide au
// backend (le backend répondrait 400 STEP_DATA_INVALID).
// Cibles tactiles >= 48 px, libellés reliés aux champs (accessibilité).

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../data/services/providers.dart';
import '../../widgets/glass_theme.dart';

typedef SubmitData = void Function(Map<String, dynamic> data);

class _Field extends StatelessWidget {
  const _Field({
    required this.label,
    required this.controller,
    this.enabled = true,
    this.hint,
    this.keyboardType,
  });

  final String label;
  final TextEditingController controller;
  final bool enabled;
  final String? hint;
  final TextInputType? keyboardType;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: TextField(
        controller: controller,
        enabled: enabled,
        keyboardType: keyboardType,
        decoration: InputDecoration(
          labelText: label,
          hintText: hint,
          border: const OutlineInputBorder(),
        ),
      ),
    );
  }
}

class _Submit extends StatelessWidget {
  const _Submit({required this.enabled, required this.onPressed, required this.label});
  final bool enabled;
  final VoidCallback onPressed;
  final String label;

  @override
  Widget build(BuildContext context) => FilledButton(
        onPressed: enabled ? onPressed : null,
        style: FilledButton.styleFrom(
          minimumSize: const Size.fromHeight(48),
          backgroundColor: AppColors.primary,
        ),
        child: Text(label),
      );
}

// ── 0. CHURCH_IDENTITY ────────────────────────────────────────────────────
class ChurchIdentityForm extends StatefulWidget {
  const ChurchIdentityForm({super.key, required this.enabled, required this.onSubmit});
  final bool enabled;
  final SubmitData onSubmit;

  @override
  State<ChurchIdentityForm> createState() => _ChurchIdentityFormState();
}

class _ChurchIdentityFormState extends State<ChurchIdentityForm> {
  final _name = TextEditingController();
  final _business = TextEditingController();
  final _city = TextEditingController();
  final _phone = TextEditingController();
  final _email = TextEditingController();
  final _timezone = TextEditingController();
  final _currency = TextEditingController();
  String? _error;

  @override
  void dispose() {
    for (final c in [_name, _business, _city, _phone, _email, _timezone, _currency]) {
      c.dispose();
    }
    super.dispose();
  }

  void _submit() {
    final name = _name.text.trim();
    if (name.length < 2 || name.length > 120) {
      setState(() => _error = 'Le nom doit contenir entre 2 et 120 caractères.');
      return;
    }
    final mail = _email.text.trim();
    if (mail.isNotEmpty && !RegExp(r'^[^\s@]+@[^\s@]+\.[^\s@]+$').hasMatch(mail)) {
      setState(() => _error = 'Adresse email invalide.');
      return;
    }
    setState(() => _error = null);
    widget.onSubmit({
      'churchName': name,
      if (_business.text.trim().isNotEmpty) 'businessName': _business.text.trim(),
      if (_city.text.trim().isNotEmpty) 'city': _city.text.trim(),
      if (_phone.text.trim().isNotEmpty) 'phone': _phone.text.trim(),
      if (mail.isNotEmpty) 'email': mail,
      if (_timezone.text.trim().isNotEmpty) 'timezone': _timezone.text.trim(),
      if (_currency.text.trim().isNotEmpty) 'currency': _currency.text.trim(),
    });
  }

  @override
  Widget build(BuildContext context) => GlassCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _Field(label: "Nom de l'église *", controller: _name, enabled: widget.enabled),
            _Field(label: 'Nom commercial', controller: _business, enabled: widget.enabled),
            _Field(label: 'Ville', controller: _city, enabled: widget.enabled),
            _Field(label: 'Téléphone', controller: _phone, enabled: widget.enabled, keyboardType: TextInputType.phone),
            _Field(label: 'Email', controller: _email, enabled: widget.enabled, keyboardType: TextInputType.emailAddress),
            _Field(label: 'Fuseau horaire (IANA)', controller: _timezone, enabled: widget.enabled, hint: 'Africa/Douala'),
            _Field(label: 'Devise (ISO-4217)', controller: _currency, enabled: widget.enabled, hint: 'XAF'),
            if (_error != null) Text(_error!, style: const TextStyle(color: Colors.redAccent)),
            const SizedBox(height: 8),
            _Submit(enabled: widget.enabled, onPressed: _submit, label: 'Enregistrer'),
          ],
        ),
      );
}

// ── 1. MEMBER_IMPORT ──────────────────────────────────────────────────────
class MemberImportForm extends StatefulWidget {
  const MemberImportForm({super.key, required this.enabled, required this.onSubmit});
  final bool enabled;
  final SubmitData onSubmit;

  @override
  State<MemberImportForm> createState() => _MemberImportFormState();
}

class _MemberImportFormState extends State<MemberImportForm> {
  final _count = TextEditingController();
  String? _error;

  @override
  void dispose() {
    _count.dispose();
    super.dispose();
  }

  void _submit() {
    final n = int.tryParse(_count.text.trim());
    if (n == null || n < 1) {
      setState(() => _error = 'Indiquez au moins un membre importé, ou ignorez cette étape.');
      return;
    }
    setState(() => _error = null);
    widget.onSubmit({'importedCount': n});
  }

  @override
  Widget build(BuildContext context) => GlassCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _Field(label: 'Nombre de membres importés *', controller: _count, enabled: widget.enabled, keyboardType: TextInputType.number),
            if (_error != null) Text(_error!, style: const TextStyle(color: Colors.redAccent)),
            const SizedBox(height: 8),
            _Submit(enabled: widget.enabled, onPressed: _submit, label: 'Enregistrer'),
          ],
        ),
      );
}

// ── 2. STRUCTURE ───────────────────────────────────────────────────────────
class StructureForm extends StatefulWidget {
  const StructureForm({super.key, required this.enabled, required this.onSubmit});
  final bool enabled;
  final SubmitData onSubmit;

  @override
  State<StructureForm> createState() => _StructureFormState();
}

class _StructureFormState extends State<StructureForm> {
  final List<TextEditingController> _departments = [TextEditingController()];
  final List<TextEditingController> _families = [TextEditingController()];
  String? _error;

  @override
  void dispose() {
    for (final c in [..._departments, ..._families]) {
      c.dispose();
    }
    super.dispose();
  }

  List<String> _clean(List<TextEditingController> list) =>
      list.map((c) => c.text.trim()).where((s) => s.isNotEmpty).toList();

  void _submit() {
    final d = _clean(_departments);
    final f = _clean(_families);
    if (d.isEmpty && f.isEmpty) {
      setState(() => _error = 'Ajoutez au moins un département ou une famille, ou ignorez avec un motif.');
      return;
    }
    setState(() => _error = null);
    widget.onSubmit({'departments': d, 'families': f});
  }

  Widget _list(String title, List<TextEditingController> controllers, String addLabel) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(title, style: const TextStyle(fontWeight: FontWeight.bold)),
        const SizedBox(height: 6),
        for (var i = 0; i < controllers.length; i++)
          Row(
            children: [
              Expanded(child: _Field(label: '$title ${i + 1}', controller: controllers[i], enabled: widget.enabled)),
              IconButton(
                onPressed: widget.enabled && controllers.length > 1
                    ? () {
                        setState(() => controllers.removeAt(i).dispose());
                      }
                    : null,
                icon: const Icon(Icons.close),
                tooltip: 'Retirer',
              ),
            ],
          ),
        Align(
          alignment: Alignment.centerLeft,
          child: TextButton.icon(
            onPressed: widget.enabled
                ? () => setState(() => controllers.add(TextEditingController()))
                : null,
            icon: const Icon(Icons.add),
            label: Text(addLabel),
            style: TextButton.styleFrom(minimumSize: const Size(120, 48)),
          ),
        ),
        const SizedBox(height: 12),
      ],
    );
  }

  @override
  Widget build(BuildContext context) => GlassCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _list('Départements', _departments, 'Ajouter un département'),
            _list('Familles', _families, 'Ajouter une famille'),
            if (_error != null) Text(_error!, style: const TextStyle(color: Colors.redAccent)),
            const SizedBox(height: 8),
            _Submit(enabled: widget.enabled, onPressed: _submit, label: 'Enregistrer'),
          ],
        ),
      );
}

// ── 3. ROLES ───────────────────────────────────────────────────────────────
class RolesForm extends StatefulWidget {
  const RolesForm({super.key, required this.enabled, required this.onSubmit});
  final bool enabled;
  final SubmitData onSubmit;

  @override
  State<RolesForm> createState() => _RolesFormState();
}

class _RolesFormState extends State<RolesForm> {
  final List<TextEditingController> _emails = [TextEditingController()];
  final List<TextEditingController> _roles = [TextEditingController(text: 'PASTEUR')];
  String? _error;

  @override
  void dispose() {
    for (final c in [..._emails, ..._roles]) {
      c.dispose();
    }
    super.dispose();
  }

  void _submit() {
    final rows = <Map<String, String>>[];
    for (var i = 0; i < _emails.length; i++) {
      final e = _emails[i].text.trim().toLowerCase();
      final r = _roles[i].text.trim().toUpperCase();
      if (e.isEmpty && r.isEmpty) continue;
      rows.add({'email': e, 'role': r});
    }
    if (rows.isEmpty) {
      setState(() => _error = 'Ajoutez au moins une invitation, ou ignorez cette étape.');
      return;
    }
    final bad = rows.any((r) =>
        !RegExp(r'^[^\s@]+@[^\s@]+\.[^\s@]+$').hasMatch(r['email']!) || r['role']!.isEmpty);
    if (bad) {
      setState(() => _error = 'Chaque ligne doit avoir un email valide et un rôle.');
      return;
    }
    setState(() => _error = null);
    widget.onSubmit({'invitations': rows});
  }

  @override
  Widget build(BuildContext context) => GlassCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            for (var i = 0; i < _emails.length; i++) ...[
              _Field(label: 'Email de l\'invité', controller: _emails[i], enabled: widget.enabled, keyboardType: TextInputType.emailAddress),
              _Field(label: 'Rôle', controller: _roles[i], enabled: widget.enabled),
              if (_emails.length > 1)
                Align(
                  alignment: Alignment.centerRight,
                  child: IconButton(
                    onPressed: widget.enabled
                        ? () => setState(() {
                              _emails.removeAt(i).dispose();
                              _roles.removeAt(i).dispose();
                            })
                        : null,
                    icon: const Icon(Icons.delete_outline),
                    tooltip: 'Retirer',
                  ),
                ),
            ],
            Align(
              alignment: Alignment.centerLeft,
              child: TextButton.icon(
                onPressed: widget.enabled
                    ? () => setState(() {
                          _emails.add(TextEditingController());
                          _roles.add(TextEditingController());
                        })
                    : null,
                icon: const Icon(Icons.add),
                label: const Text('Ajouter une invitation'),
                style: TextButton.styleFrom(minimumSize: const Size(160, 48)),
              ),
            ),
            if (_error != null) Text(_error!, style: const TextStyle(color: Colors.redAccent)),
            const SizedBox(height: 8),
            _Submit(enabled: widget.enabled, onPressed: _submit, label: 'Envoyer les invitations'),
          ],
        ),
      );
}

// ── 4. BRANDING ────────────────────────────────────────────────────────────
class BrandingForm extends StatefulWidget {
  const BrandingForm({super.key, required this.enabled, required this.onSubmit});
  final bool enabled;
  final SubmitData onSubmit;

  @override
  State<BrandingForm> createState() => _BrandingFormState();
}

class _BrandingFormState extends State<BrandingForm> {
  final _color = TextEditingController(text: '#1A7F5A');
  final _logo = TextEditingController();
  bool _darkMode = true;
  String? _error;

  @override
  void dispose() {
    _color.dispose();
    _logo.dispose();
    super.dispose();
  }

  void _submit() {
    final c = _color.text.trim();
    if (c.isNotEmpty && !RegExp(r'^#[0-9a-fA-F]{6}$').hasMatch(c)) {
      setState(() => _error = 'La couleur doit être au format #RRGGBB.');
      return;
    }
    final logo = _logo.text.trim();
    if (logo.isNotEmpty && !RegExp(r'^https?://', caseSensitive: false).hasMatch(logo)) {
      setState(() => _error = "L'URL du logo doit commencer par http:// ou https://");
      return;
    }
    setState(() => _error = null);
    widget.onSubmit({
      if (c.isNotEmpty) 'primaryColor': c,
      if (logo.isNotEmpty) 'logoUrl': logo,
      'allowDarkMode': _darkMode,
    });
  }

  @override
  Widget build(BuildContext context) => GlassCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _Field(label: 'Couleur principale', controller: _color, enabled: widget.enabled),
            _Field(label: 'URL du logo', controller: _logo, enabled: widget.enabled, keyboardType: TextInputType.url),
            SwitchListTile(
              value: _darkMode,
              onChanged: widget.enabled ? (v) => setState(() => _darkMode = v) : null,
              title: const Text('Autoriser le mode sombre'),
              contentPadding: EdgeInsets.zero,
            ),
            if (_error != null) Text(_error!, style: const TextStyle(color: Colors.redAccent)),
            const SizedBox(height: 8),
            _Submit(enabled: widget.enabled, onPressed: _submit, label: 'Enregistrer'),
          ],
        ),
      );
}

// ── 6. FIRST_EVENT ─────────────────────────────────────────────────────────
class FirstEventForm extends StatefulWidget {
  const FirstEventForm({super.key, required this.enabled, required this.onSubmit});
  final bool enabled;
  final SubmitData onSubmit;

  @override
  State<FirstEventForm> createState() => _FirstEventFormState();
}

class _FirstEventFormState extends State<FirstEventForm> {
  final _title = TextEditingController();
  final _location = TextEditingController();
  DateTime? _startAt;
  String? _error;

  @override
  void dispose() {
    _title.dispose();
    _location.dispose();
    super.dispose();
  }

  /// Utilise `State.context` (et non le `BuildContext` reçu) : après un `await`,
  /// le `context` du build est invalide. `State.context` reste valide tant que
  /// le State est monté.
  Future<void> _pickDate() async {
    final now = DateTime.now();
    final date = await showDatePicker(
      context: context,
      firstDate: now,
      lastDate: now.add(const Duration(days: 365 * 2)),
    );
    if (date == null || !mounted) return;
    final time = await showTimePicker(
      context: context,
      initialTime: const TimeOfDay(hour: 18, minute: 0),
    );
    if (time == null || !mounted) return;
    setState(() => _startAt = DateTime(date.year, date.month, date.day, time.hour, time.minute));
  }

  void _submit() {
    final t = _title.text.trim();
    if (t.length < 2 || t.length > 160) {
      setState(() => _error = 'Le titre doit contenir entre 2 et 160 caractères.');
      return;
    }
    if (_startAt == null) {
      setState(() => _error = 'La date de début est obligatoire.');
      return;
    }
    if (!_startAt!.isAfter(DateTime.now())) {
      setState(() => _error = 'La date doit être dans le futur.');
      return;
    }
    setState(() => _error = null);
    widget.onSubmit({
      'title': t,
      'startAt': _startAt!.toUtc().toIso8601String(),
      if (_location.text.trim().isNotEmpty) 'location': _location.text.trim(),
    });
  }

  @override
  Widget build(BuildContext context) => GlassCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _Field(label: "Titre de l'événement *", controller: _title, enabled: widget.enabled),
            ListTile(
              contentPadding: EdgeInsets.zero,
              title: const Text('Date et heure de début *'),
              subtitle: Text(
                _startAt == null
                    ? 'Non définie'
                    : _startAt!.toLocal().toString().substring(0, 16),
              ),
              trailing: const Icon(Icons.event),
              onTap: widget.enabled ? _pickDate : null,
            ),
            _Field(label: 'Lieu', controller: _location, enabled: widget.enabled),
            if (_error != null) Text(_error!, style: const TextStyle(color: Colors.redAccent)),
            const SizedBox(height: 8),
            _Submit(enabled: widget.enabled, onPressed: _submit, label: "Créer l'événement"),
          ],
        ),
      );
}

// ── 5. MODULES ─────────────────────────────────────────────────────────────
// La liste provient de GET /admin/tenant-features (endpoint réel, lecture seule :
// l'activation est faite par l'action serveur du wizard).
class ModulesForm extends ConsumerStatefulWidget {
  const ModulesForm({super.key, required this.enabled, required this.onSubmit});
  final bool enabled;
  final SubmitData onSubmit;

  @override
  ConsumerState<ModulesForm> createState() => _ModulesFormState();
}

class _ModulesFormState extends ConsumerState<ModulesForm> {
  final Set<String> _selected = <String>{};
  bool _loading = true;
  bool _loadFailed = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _loadFailed = false;
    });
    try {
      final api = ref.read(apiServiceProvider);
      final res = await api.get('/admin/tenant-features');
      final data = res.data;
      if (!mounted) return;
      if (data is! List) {
        setState(() {
          _loading = false;
          _loadFailed = true;
        });
        return;
      }
      setState(() => _loading = false);
    } on Object {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _loadFailed = true;
      });
    }
  }

  void _submit(List<String> codes) {
    if (_selected.isEmpty || _selected.length > 50) {
      setState(() => _error = 'Sélectionnez entre 1 et 50 modules.');
      return;
    }
    setState(() => _error = null);
    widget.onSubmit({'modules': codes});
  }

  @override
  Widget build(BuildContext context) {
    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (_loading)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 24),
              child: Center(child: CircularProgressIndicator()),
            )
          else if (_loadFailed)
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Impossible de charger la liste des modules.',
                    style: TextStyle(color: Colors.redAccent)),
                TextButton.icon(
                  onPressed: _load,
                  icon: const Icon(Icons.refresh),
                  label: const Text('Réessayer'),
                  style: TextButton.styleFrom(minimumSize: const Size(140, 48)),
                ),
              ],
            )
          else
            FutureBuilder<List<dynamic>>(
              future: ref.read(apiServiceProvider).get('/admin/tenant-features').then((r) => r.data is List ? r.data as List<dynamic> : <dynamic>[]),
              builder: (context, snapshot) {
                final items = snapshot.data ?? const <dynamic>[];
                if (items.isEmpty) {
                  return const Padding(
                    padding: EdgeInsets.symmetric(vertical: 12),
                    child: Text('Aucun module disponible.'),
                  );
                }
                return Column(
                  children: items.map((raw) {
                    final m = raw is Map ? raw.cast<String, dynamic>() : <String, dynamic>{};
                    final code = (m['key'] ?? m['code'] ?? '').toString();
                    if (code.isEmpty) return const SizedBox.shrink();
                    final label = (m['name'] ?? code).toString();
                    return CheckboxListTile(
                      value: _selected.contains(code),
                      onChanged: widget.enabled
                          ? (v) => setState(() => v == true ? _selected.add(code) : _selected.remove(code))
                          : null,
                      title: Text(label),
                      contentPadding: EdgeInsets.zero,
                    );
                  }).toList(),
                );
              },
            ),
          if (_error != null) Text(_error!, style: const TextStyle(color: Colors.redAccent)),
          const SizedBox(height: 8),
          _Submit(
            enabled: widget.enabled && !_loading,
            onPressed: () {
              final api = ref.read(apiServiceProvider);
              api.get('/admin/tenant-features').then((r) {
                final items = r.data is List ? r.data as List<dynamic> : <dynamic>[];
                final codes = items
                    .whereType<Map>()
                    .map((m) => (m['key'] ?? m['code'] ?? '').toString())
                    .where(_selected.contains)
                    .toList();
                _submit(codes);
              }).catchError((_) {
                if (mounted) setState(() => _error = 'Impossible de valider les modules.');
              });
            },
            label: 'Enregistrer',
          ),
        ],
      ),
    );
  }
}
