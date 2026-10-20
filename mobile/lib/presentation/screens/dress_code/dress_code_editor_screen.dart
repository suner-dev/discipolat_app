import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/app_drawer.dart';

/// Dress Code Editor Screen — create/edit a dress code with rules.
/// Connected to API: POST /dress-codes, PUT /dress-codes/{id}, GET /dress-codes/{id}
class DressCodeEditorScreen extends StatefulWidget {
  final String? id;

  /// Injection de l'API — permet aux tests widget d'éviter le réseau.
  final ApiService? apiService;

  const DressCodeEditorScreen({super.key, this.id, this.apiService});

  @override
  State<DressCodeEditorScreen> createState() => _DressCodeEditorScreenState();
}

class _DressCodeEditorScreenState extends State<DressCodeEditorScreen> {
  final _formKey = GlobalKey<FormState>();
  late final ApiService _apiService = widget.apiService ?? ApiService();

  // Controllers du formulaire : la saisie utilisateur doit survivre à un
  // rechargement de l'entité, et les champs doivent refléter la valeur
  // chargée depuis l'API (un `initialValue` figé ne se met pas à jour).
  final _titleController = TextEditingController();
  final _serviceNameController = TextEditingController();
  final _spaceIdController = TextEditingController();
  final _eventIdController = TextEditingController();
  final _beginsAtController = TextEditingController();
  final _endsAtController = TextEditingController();

  String? _title;
  String? _serviceName;
  String? _status;
  String? _beginsAt;
  String? _endsAt;
  String? _spaceId;
  String? _eventId;
  final List<_RuleForm> _rules = [];
  bool _isSaving = false;
  bool _isLoading = false;
  String? _error;
  String? _success;

  @override
  void initState() {
    super.initState();
    _isLoading = widget.id != null;
    _loadIfEditing();
  }

  Future<void> _loadIfEditing() async {
    if (widget.id == null) return;
    try {
      final res = await _apiService.get('/dress-codes/${widget.id}');
      final data = res.data as Map<String, dynamic>;
      if (mounted) {
        setState(() {
          _title = data['title'] as String?;
          _serviceName = data['serviceName'] as String?;
          _status = data['status'] as String?;
          _beginsAt = data['beginsAt'] as String?;
          _endsAt = data['endsAt'] as String?;
          _spaceId = data['spaceId'] as String?;
          _eventId = data['eventId'] as String?;
          _rules.clear();
          final list = data['rules'] as List<dynamic>?;
          if (list != null) {
            for (final r in list) {
              _rules.add(_RuleForm(
                groupName: r['groupName'] as String? ?? '',
                description: r['description'] as String? ?? '',
                imageUrl: r['imageUrl'] as String? ?? '',
              ));
            }
          }
          _error = null;
          _isLoading = false;
          // Les champs du formulaire sont alimentés par des TextEditingController :
          // sans cette révocation, les controllers recyclés par le framework
          // conserveraient la valeur saisie AVANT le chargement (bug : le
          // formulaire affichait une valeur périmée au lieu de l'entité).
          _titleController.text = _title ?? '';
          _serviceNameController.text = _serviceName ?? '';
          _spaceIdController.text = _spaceId ?? '';
          _eventIdController.text = _eventId ?? '';
          _beginsAtController.text = _beginsAt ?? '';
          _endsAtController.text = _endsAt ?? '';
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = 'Impossible de charger le dress code';
          _isLoading = false;
        });
      }
    }
  }

  void _addRule() {
    setState(() {
      _rules.add(_RuleForm());
    });
  }

  void _removeRule(int index) {
    setState(() {
      final removed = _rules.removeAt(index);
      removed.dispose();
    });
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _isSaving = true);
    try {
      final body = {
        'title': _title,
        'serviceName': _serviceName,
        'spaceId': _spaceId,
        'eventId': _eventId,
        'beginsAt': _beginsAt,
        'endsAt': _endsAt,
        'status': _status ?? 'DRAFT',
        'rules': _rules.map((r) => {
              'groupName': r.groupName,
              'description': r.description,
              'imageUrl': r.imageUrl,
            }).toList(),
      };
      final successMessage = widget.id != null
          ? 'Dress code mis à jour'
          : 'Dress code créé';
      if (widget.id != null) {
        await _apiService.put('/dress-codes/${widget.id}', data: body);
      } else {
        await _apiService.post('/dress-codes', data: body);
      }
      if (mounted) {
        setState(() {
          _error = null;
          _success = successMessage;
          _isSaving = false;
        });
        // Pas de SnackBar ici : l'écran affiche déjà un écran de confirmation
        // complet (_success) — le double affichage était trompeur.
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = 'Erreur lors de l\'enregistrement';
          _isSaving = false;
        });
      }
    }
  }

  @override
  void dispose() {
    _titleController.dispose();
    _serviceNameController.dispose();
    _spaceIdController.dispose();
    _eventIdController.dispose();
    _beginsAtController.dispose();
    _endsAtController.dispose();
    for (final rule in _rules) {
      rule.dispose();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.id == null
            ? 'Créer une tenue'
            : 'Modifier la tenue'),
        backgroundColor: Colors.purple.shade600,
        foregroundColor: Colors.white,
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _error != null || _success != null
              ? _buildMessage()
              : Form(
                  key: _formKey,
                  child: _buildForm(),
                ),
    );
  }

  Widget _buildMessage() {
    if (_isSaving) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_error != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.error_outline, color: Colors.red.shade400, size: 48),
            const SizedBox(height: 12),
            Text(_error!, textAlign: TextAlign.center,
                style: TextStyle(color: Colors.red.shade300)),
            const SizedBox(height: 16),
            FilledButton.icon(
              // En création, « Réessayer » ne peut pas recharger une ressource
              // inexistante : on revient au formulaire (les champs saisis sont
              // conservés dans l'état). En édition, on recharge l'entité.
              onPressed: widget.id == null
                  ? () => setState(() => _error = null)
                  : _loadIfEditing,
              icon: const Icon(Icons.refresh),
              label: const Text('Réessayer'),
            ),
          ],
        ),
      );
    }
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.check_circle, color: Colors.green.shade400, size: 48),
          const SizedBox(height: 12),
          Text(_success!, textAlign: TextAlign.center,
              style: TextStyle(color: Colors.green.shade300)),
          const SizedBox(height: 16),
          FilledButton.icon(
            onPressed: () => Navigator.of(context).pop(),
            icon: const Icon(Icons.arrow_back),
            label: const Text('Retour'),
          ),
        ],
      ),
    );
  }

  Widget _buildForm() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          _buildSectionTitle('Informations générales'),
          const SizedBox(height: 8),
          _buildTextField('Titre *', _title, (v) => _title = v,
              controller: _titleController),
          const SizedBox(height: 8),
          _buildTextField('Service (événement/espace)', _serviceName,
              (v) => _serviceName = v, required: false,
              controller: _serviceNameController),
          const SizedBox(height: 8),
          _buildTextField('Espace (UUID, optionnel)', _spaceId, (v) => _spaceId = v,
              required: false, controller: _spaceIdController,
              validator: _uuidValidator),
          const SizedBox(height: 8),
          _buildTextField('Événement (UUID, optionnel)', _eventId,
              (v) => _eventId = v, required: false, controller: _eventIdController,
              validator: _uuidValidator),
          const SizedBox(height: 8),
          _buildDateTimePicker('Date de début', _beginsAt, (v) => _beginsAt = v,
              controller: _beginsAtController),
          const SizedBox(height: 8),
          _buildDateTimePicker('Date de fin', _endsAt, (v) => _endsAt = v,
              controller: _endsAtController),
          const SizedBox(height: 8),
          _buildStatusDropdown(),
          const SizedBox(height: 16),
          _buildSectionTitle('Règles de tenue'),
          const SizedBox(height: 8),
          _buildRuleForm(),
          const SizedBox(height: 8),
          OutlinedButton.icon(
            onPressed: _addRule,
            icon: const Icon(Icons.add),
            label: const Text('Ajouter une règle'),
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: FilledButton(
                  onPressed: _isSaving ? null : _save,
                  child: _isSaving
                      ? const SizedBox(
                          height: 20,
                          width: 20,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Text('Sauvegarder'),
                ),
              ),
              const SizedBox(width: 8),
              OutlinedButton(
                onPressed: () => Navigator.of(context).pop(),
                child: const Text('Annuler'),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildSectionTitle(String title) {
    return Text(title, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold));
  }

  Widget _buildTextField(
    String label,
    String? value,
    Function(String) onChanged, {
    bool required = true,
    TextEditingController? controller,
    String? Function(String?)? validator,
  }) {
    return TextFormField(
      initialValue: controller == null ? value : null,
      controller: controller,
      decoration: InputDecoration(
        labelText: label,
        border: const OutlineInputBorder(),
      ),
      // Seuls les champs marqués « * » sont obligatoires : « Espace » et
      // « Événement » sont des UUID optionnels (un dress code peut concerner
      // un service sans espace ni événement), et le nom de service est libre.
      validator: validator ??
          (required
              ? (v) => v == null || v.isEmpty ? 'Champ requis' : null
              : (v) => null),
      onChanged: onChanged,
    );
  }

  /// Validateur des UUID optionnels (Espace / Événement).
  static String? _uuidValidator(String? v) {
    if (v == null || v.isEmpty) return null;
    return _isUuid(v) ? null : 'UUID invalide';
  }

  /// UUID (avec ou sans tirets) — validation souple des champs optionnels.
  static final _uuidPattern = RegExp(
    r'^[0-9a-fA-F]{8}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{12}$',
  );

  static bool _isUuid(String value) => _uuidPattern.hasMatch(value.trim());

  Widget _buildDateTimePicker(
    String label,
    String? value,
    Function(String) onChanged, {
    TextEditingController? controller,
  }) {
    return TextFormField(
      initialValue: controller == null ? value : null,
      controller: controller,
      decoration: InputDecoration(
        labelText: label,
        hintText: 'YYYY-MM-DDTHH:MM:SS',
        border: const OutlineInputBorder(),
      ),
      validator: (v) {
        if (v == null || v.isEmpty) return null;
        final d = DateTime.tryParse(v);
        if (d == null) return 'Format invalide (YYYY-MM-DDTHH:MM:SS)';
        return null;
      },
      onChanged: onChanged,
    );
  }

  Widget _buildStatusDropdown() {
    return DropdownButtonFormField<String>(
      // `initialValue` (et non `value`) : la valeur doit suivre l'entité
      // chargée sans être réinitialisée à chaque rebuild.
      initialValue: _status,
      decoration: const InputDecoration(
        labelText: 'Statut',
        border: OutlineInputBorder(),
      ),
      items: ['DRAFT', 'PUBLISHED', 'ARCHIVED']
          .map((s) => DropdownMenuItem(value: s, child: Text(s)))
          .toList(),
      onChanged: (v) => setState(() => _status = v),
    );
  }

  Widget _buildRuleForm() {
    return Column(
      children: _rules.map((r) {
        return Padding(
          padding: const EdgeInsets.only(bottom: 8),
          child: Row(
            children: [
              Expanded(
                flex: 2,
                child: TextFormField(
                  controller: r.groupNameController,
                  decoration: const InputDecoration(labelText: 'Groupe'),
                  onChanged: (v) => r.groupName = v,
                ),
              ),
              Expanded(
                flex: 3,
                child: TextFormField(
                  controller: r.descriptionController,
                  decoration: const InputDecoration(labelText: 'Description'),
                  onChanged: (v) => r.description = v,
                ),
              ),
              IconButton(
                icon: const Icon(Icons.remove_circle_outline, color: Colors.red),
                onPressed: () => _removeRule(_rules.indexOf(r)),
              ),
            ],
          ),
        );
      }).toList(),
    );
  }
}

/// Une ligne de règle de tenue.
///
/// Les champs utilisent des `TextEditingController` : avec un `initialValue`,
/// Flutter recycle l'état du champ et peut réafficher la valeur d'une règle
/// supprimée (ou d'une autre règle) au lieu de la nouvelle.
class _RuleForm {
  String groupName;
  String description;
  String imageUrl;

  final TextEditingController groupNameController;
  final TextEditingController descriptionController;

  _RuleForm({
    this.groupName = '',
    this.description = '',
    this.imageUrl = '',
  })  : groupNameController = TextEditingController(text: groupName),
        descriptionController = TextEditingController(text: description);

  void dispose() {
    groupNameController.dispose();
    descriptionController.dispose();
  }
}
