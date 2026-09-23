import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../data/services/api_service.dart';

/// Super Admin — Flux de provisionnement guidé (mobile), 4 étapes cliquables :
///   1. Organisation (tenant) → 2. Église → 3. Département → 4. Famille → Récap
/// Même contrat d'API que le wizard web :
///   POST /platform/admin/tenants
///   POST /platform/admin/provisioning/{church,department,family}
class SuperAdminProvisioningScreen extends StatefulWidget {
  const SuperAdminProvisioningScreen({super.key, this.apiService});

  /// Permet d'injecter un ApiService mocké dans les tests widget.
  final ApiService? apiService;

  @override
  State<SuperAdminProvisioningScreen> createState() => _SuperAdminProvisioningScreenState();
}

class _SuperAdminProvisioningScreenState extends State<SuperAdminProvisioningScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();

  static const _stepTitles = ['Organisation', 'Église', 'Département', 'Famille'];

  int _step = 0; // 0..3 formulaires, 4 récapitulatif
  bool _submitting = false;

  // Résultats des étapes créées
  Map<String, dynamic>? _tenant;
  Map<String, dynamic>? _church;
  Map<String, dynamic>? _department;
  Map<String, dynamic>? _family;

  // Étape 1 — organisation
  final _orgName = TextEditingController();
  String _orgPlan = 'free';

  // Étape 2 — église
  final _churchName = TextEditingController();

  // Étape 3 — département
  final _deptNom = TextEditingController();
  final _deptDesc = TextEditingController();
  bool _deptNewMode = true;
  final _deptRespId = TextEditingController();
  final _deptFirstName = TextEditingController();
  final _deptLastName = TextEditingController();
  final _deptEmail = TextEditingController();
  final _deptPhone = TextEditingController();

  // Étape 4 — famille
  final _famNom = TextEditingController();
  bool _famNewMode = true;
  final _famChefId = TextEditingController();
  final _famFirstName = TextEditingController();
  final _famLastName = TextEditingController();
  final _famEmail = TextEditingController();
  final _famPhone = TextEditingController();

  @override
  void dispose() {
    _orgName.dispose(); _churchName.dispose();
    _deptNom.dispose(); _deptDesc.dispose(); _deptRespId.dispose();
    _deptFirstName.dispose(); _deptLastName.dispose(); _deptEmail.dispose(); _deptPhone.dispose();
    _famNom.dispose(); _famChefId.dispose();
    _famFirstName.dispose(); _famLastName.dispose(); _famEmail.dispose(); _famPhone.dispose();
    super.dispose();
  }

  String _slugify(String value) {
    var s = value.toLowerCase();
    const from = 'éèêëàâäùûüîïôöç';
    const to = 'eeeeaaauuuiiooc';
    for (var i = 0; i < from.length; i++) {
      s = s.replaceAll(from[i], to[i]);
    }
    s = s
        .replaceAll(RegExp('[^a-z0-9]+'), '-')
        .replaceAll(RegExp(r'-+'), '-')
        .replaceAll(RegExp(r'^-|-$'), '');
    return s.length > 50 ? s.substring(0, 50) : s;
  }

  String? _validateStep(int step) {
    if (step == 0) {
      if (_orgName.text.trim().isEmpty) return "Le nom de l'organisation est requis";
      if (_slugify(_orgName.text).isEmpty) return 'Nom invalide pour le slug';
      return null;
    }
    if (step == 1) {
      if (_churchName.text.trim().isEmpty) return "Le nom de l'église est requis";
      return null;
    }
    if (step == 2) {
      if (_deptNom.text.trim().isEmpty) return 'Le nom du département est requis';
      if (!_deptNewMode && _deptRespId.text.trim().isEmpty) {
        return "L'ID du responsable existant est requis";
      }
      if (_deptNewMode) {
        if (_deptFirstName.text.trim().isEmpty || _deptLastName.text.trim().isEmpty) {
          return 'Prénom et nom du responsable requis';
        }
        if (!RegExp(r'^\S+@\S+\.\S+$').hasMatch(_deptEmail.text.trim())) {
          return 'Email du responsable invalide';
        }
      }
      return null;
    }
    if (step == 3) {
      if (_famNom.text.trim().isEmpty) return 'Le nom de la famille est requis';
      if (!_famNewMode && _famChefId.text.trim().isEmpty) {
        return "L'ID du chef de famille existant est requis";
      }
      if (_famNewMode) {
        if (_famFirstName.text.trim().isEmpty || _famLastName.text.trim().isEmpty) {
          return 'Prénom et nom du chef requis';
        }
        if (!RegExp(r'^\S+@\S+\.\S+$').hasMatch(_famEmail.text.trim())) {
          return 'Email du chef invalide';
        }
      }
      return null;
    }
    return null;
  }

  void _snack(String message, {bool error = false}) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(message),
      backgroundColor: error ? Colors.red.shade700 : Colors.green.shade700,
    ));
  }

  Future<void> _submitCurrentStep() async {
    final validationError = _validateStep(_step);
    if (validationError != null) {
      _snack(validationError, error: true);
      return;
    }
    setState(() => _submitting = true);
    try {
      if (_step == 0) {
        final data = <String, dynamic>{
          'name': _orgName.text.trim(),
          'slug': _slugify(_orgName.text),
          'plan': _orgPlan,
          'country': 'CM',
          'currency': 'XAF',
          'timezone': 'Africa/Douala',
          'locale': 'fr',
        };
        final res = await _api.post('/platform/admin/tenants', data: data);
        _tenant = Map<String, dynamic>.from(res.data as Map);
        if (_churchName.text.trim().isEmpty) {
          _churchName.text = '${_orgName.text.trim()} — Église principale';
        }
        _snack('Organisation créée !');
      } else if (_step == 1) {
        final res = await _api.post('/platform/admin/provisioning/church', data: {
          'tenantId': _tenant!['id'],
          'name': _churchName.text.trim(),
        });
        _church = Map<String, dynamic>.from(res.data as Map);
        _snack('Église créée !');
      } else if (_step == 2) {
        final payload = <String, dynamic>{
          'tenantId': _tenant!['id'],
          'nom': _deptNom.text.trim(),
          'description': _deptDesc.text.trim().isEmpty ? null : _deptDesc.text.trim(),
        };
        if (_deptNewMode) {
          payload.addAll({
            'createNewResponsable': true,
            'newRespFirstName': _deptFirstName.text.trim(),
            'newRespLastName': _deptLastName.text.trim(),
            'newRespEmail': _deptEmail.text.trim(),
            'newRespPhone': _deptPhone.text.trim().isEmpty ? null : _deptPhone.text.trim(),
          });
        } else {
          payload['responsableId'] = _deptRespId.text.trim();
        }
        final res = await _api.post('/platform/admin/provisioning/department', data: payload);
        _department = Map<String, dynamic>.from(res.data as Map);
        if (_famNom.text.trim().isEmpty) _famNom.text = '${_orgName.text.trim()} — Famille modèle';
        _snack('Département créé !');
      } else if (_step == 3) {
        final payload = <String, dynamic>{
          'tenantId': _tenant!['id'],
          'nom': _famNom.text.trim(),
        };
        if (_famNewMode) {
          payload.addAll({
            'createNewChef': true,
            'newChefFirstName': _famFirstName.text.trim(),
            'newChefLastName': _famLastName.text.trim(),
            'newChefEmail': _famEmail.text.trim(),
            'newChefPhone': _famPhone.text.trim().isEmpty ? null : _famPhone.text.trim(),
          });
        } else {
          payload['chefFamilleId'] = _famChefId.text.trim();
        }
        final res = await _api.post('/platform/admin/provisioning/family', data: payload);
        _family = Map<String, dynamic>.from(res.data as Map);
        _snack('Famille créée — flux complet !');
      }
      if (mounted) setState(() => _step = (_step + 1).clamp(0, 4));
    } catch (e) {
      _snack('Erreur: $e', error: true);
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  void _resetAll() {
    setState(() {
      _step = 0;
      _tenant = null; _church = null; _department = null; _family = null;
      _orgName.clear(); _orgPlan = 'free'; _churchName.clear();
      _deptNom.clear(); _deptDesc.clear(); _deptNewMode = true;
      _deptRespId.clear(); _deptFirstName.clear(); _deptLastName.clear();
      _deptEmail.clear(); _deptPhone.clear();
      _famNom.clear(); _famNewMode = true; _famChefId.clear();
      _famFirstName.clear(); _famLastName.clear(); _famEmail.clear(); _famPhone.clear();
    });
  }


  bool _stepDone(int idx) =>
      idx == 0 ? _tenant != null : idx == 1 ? _church != null : idx == 2 ? _department != null : _family != null;

  bool _reachable(int idx) => idx == 0 || (idx == 1 && _tenant != null) ||
      (idx == 2 && _church != null) || (idx == 3 && _department != null);

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('Provisionnement guidé')),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            // ---------- Stepper horizontal (scrollable sur petit écran) ----------
            SizedBox(
              height: 64,
              child: ListView.separated(
                scrollDirection: Axis.horizontal,
                itemCount: _stepTitles.length,
                separatorBuilder: (_, __) => const SizedBox(width: 8),
                itemBuilder: (context, idx) {
                  final done = _stepDone(idx);
                  final active = _step == idx;
                  final reachable = _reachable(idx);
                  return InkWell(
                    onTap: reachable ? () => setState(() => _step = idx) : null,
                    borderRadius: BorderRadius.circular(12),
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                      decoration: BoxDecoration(
                        color: active
                            ? Colors.deepPurple
                            : done
                                ? Colors.green.withValues(alpha: 0.15)
                                : theme.chipTheme.backgroundColor,
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(
                          color: active
                              ? Colors.deepPurple
                              : done
                                  ? Colors.green
                                  : Colors.grey.shade400,
                        ),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(
                            done && !active ? Icons.check_circle_rounded : Icons.circle,
                            size: 18,
                            color: active ? Colors.white : done ? Colors.green : Colors.grey,
                          ),
                          const SizedBox(width: 6),
                          Text(
                            '${idx + 1}. ${_stepTitles[idx]}',
                            style: TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w600,
                              color: active ? Colors.white : reachable ? null : Colors.grey,
                            ),
                          ),
                        ],
                      ),
                    ),
                  );
                },
              ),
            ),
            const SizedBox(height: 16),

            // ---------- Étape 1 : organisation ----------
            if (_step == 0) ...[
              Text('1. Organisation (tenant)',
                  style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
              const SizedBox(height: 4),
              const Text("Identité de l'organisation sur la plateforme.",
                  style: TextStyle(fontSize: 13, color: Colors.grey)),
              const SizedBox(height: 16),
              TextField(
                controller: _orgName,
                decoration: const InputDecoration(
                  labelText: "Nom de l'organisation *",
                  hintText: 'Ex. : Église Évangélique Bethel',
                  border: OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 12),
              DropdownButtonFormField<String>(
                value: _orgPlan,
                decoration: const InputDecoration(labelText: 'Plan', border: OutlineInputBorder()),
                items: const [
                  DropdownMenuItem(value: 'free', child: Text('Free (free)')),
                  DropdownMenuItem(value: 'starter', child: Text('Starter (starter)')),
                  DropdownMenuItem(value: 'professional', child: Text('Professional (professional)')),
                ],
                onChanged: (v) => setState(() => _orgPlan = v ?? 'free'),
              ),
              const SizedBox(height: 8),
              Text('Slug : ${_slugify(_orgName.text)}',
                  style: const TextStyle(fontSize: 12, color: Colors.grey)),
            ],


            // ---------- Étape 2 : église ----------
            if (_step == 1) ...[
              Text('2. Église racine',
                  style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
              const SizedBox(height: 4),
              Text("Église racine de ${_tenant?['name'] ?? ''} .",
                  style: const TextStyle(fontSize: 13, color: Colors.grey)),
              const SizedBox(height: 16),
              TextField(
                controller: _churchName,
                decoration: const InputDecoration(
                  labelText: "Nom de l'église *",
                  hintText: 'Ex. : Bethel — Église principale',
                  border: OutlineInputBorder(),
                ),
              ),
            ],

            // ---------- Étape 3 : département ----------
            if (_step == 2) ...[
              Text('3. Département',
                  style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
              const SizedBox(height: 4),
              const Text('Première structure — un responsable est obligatoire.',
                  style: TextStyle(fontSize: 13, color: Colors.grey)),
              const SizedBox(height: 16),
              TextField(
                controller: _deptNom,
                decoration: const InputDecoration(
                  labelText: 'Nom du département *',
                  hintText: 'Ex. : Accueil & Louange',
                  border: OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _deptDesc,
                maxLines: 2,
                decoration: const InputDecoration(
                  labelText: 'Description',
                  border: OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 12),
              Row(children: [
                ChoiceChip(
                  label: const Text('Nouveau responsable'),
                  selected: _deptNewMode,
                  onSelected: (_) => setState(() => _deptNewMode = true),
                ),
                const SizedBox(width: 8),
                ChoiceChip(
                  label: const Text('Responsable existant'),
                  selected: !_deptNewMode,
                  onSelected: (_) => setState(() => _deptNewMode = false),
                ),
              ]),
              const SizedBox(height: 12),
              if (!_deptNewMode)
                TextField(
                  controller: _deptRespId,
                  decoration: const InputDecoration(
                    labelText: 'ID du responsable (UUID) *',
                    border: OutlineInputBorder(),
                  ),
                )
              else ...[
                TextField(
                  controller: _deptFirstName,
                  decoration: const InputDecoration(
                    labelText: 'Prénom du responsable *',
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: _deptLastName,
                  decoration: const InputDecoration(
                    labelText: 'Nom du responsable *',
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: _deptEmail,
                  keyboardType: TextInputType.emailAddress,
                  decoration: const InputDecoration(
                    labelText: 'Email du responsable *',
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: _deptPhone,
                  keyboardType: TextInputType.phone,
                  decoration: const InputDecoration(
                    labelText: 'Téléphone (optionnel)',
                    border: OutlineInputBorder(),
                  ),
                ),
              ],
            ],


            // ---------- Étape 4 : famille ----------
            if (_step == 3) ...[
              Text('4. Famille',
                  style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
              const SizedBox(height: 4),
              const Text('Première famille — un chef de famille est obligatoire.',
                  style: TextStyle(fontSize: 13, color: Colors.grey)),
              const SizedBox(height: 16),
              TextField(
                controller: _famNom,
                decoration: const InputDecoration(
                  labelText: 'Nom de la famille *',
                  hintText: 'Ex. : Famille Mbarga',
                  border: OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 12),
              Row(children: [
                ChoiceChip(
                  label: const Text('Nouveau chef'),
                  selected: _famNewMode,
                  onSelected: (_) => setState(() => _famNewMode = true),
                ),
                const SizedBox(width: 8),
                ChoiceChip(
                  label: const Text('Chef existant'),
                  selected: !_famNewMode,
                  onSelected: (_) => setState(() => _famNewMode = false),
                ),
              ]),
              const SizedBox(height: 12),
              if (!_famNewMode)
                TextField(
                  controller: _famChefId,
                  decoration: const InputDecoration(
                    labelText: 'ID du chef de famille (UUID) *',
                    border: OutlineInputBorder(),
                  ),
                )
              else ...[
                TextField(
                  controller: _famFirstName,
                  decoration: const InputDecoration(
                    labelText: 'Prénom du chef *',
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: _famLastName,
                  decoration: const InputDecoration(
                    labelText: 'Nom du chef *',
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: _famEmail,
                  keyboardType: TextInputType.emailAddress,
                  decoration: const InputDecoration(
                    labelText: 'Email du chef *',
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: _famPhone,
                  keyboardType: TextInputType.phone,
                  decoration: const InputDecoration(
                    labelText: 'Téléphone (optionnel)',
                    border: OutlineInputBorder(),
                  ),
                ),
              ],
            ],


            // ---------- Récapitulatif ----------
            if (_step == 4) ...[
              const Icon(Icons.check_circle_rounded, size: 56, color: Colors.green),
              const SizedBox(height: 8),
              Text('Organisation provisionnée !',
                  textAlign: TextAlign.center,
                  style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
              const SizedBox(height: 4),
              const Text('Les 4 étapes ont été créées — chaque action est journalisée en audit.',
                  textAlign: TextAlign.center, style: TextStyle(fontSize: 13, color: Colors.grey)),
              const SizedBox(height: 16),
              _RecapCard(title: 'Organisation', icon: Icons.rocket_launch_rounded,
                  name: '${_tenant?['name'] ?? ''}', id: '${_tenant?['id'] ?? ''}'),
              _RecapCard(title: 'Église', icon: Icons.church_rounded,
                  name: '${_church?['name'] ?? ''}', id: '${_church?['id'] ?? ''}'),
              _RecapCard(title: 'Département', icon: Icons.groups_rounded,
                  name: '${_department?['nom'] ?? ''}', id: '${_department?['id'] ?? ''}'),
              _RecapCard(title: 'Famille', icon: Icons.home_rounded,
                  name: '${_family?['nom'] ?? ''}', id: '${_family?['id'] ?? ''}'),
              const SizedBox(height: 8),
              Row(children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: _resetAll,
                    icon: const Icon(Icons.refresh_rounded),
                    label: const Text('Nouveau'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: () => context.go('/dashboard'),
                    icon: const Icon(Icons.dashboard_rounded),
                    label: const Text('Dashboard'),
                  ),
                ),
              ]),
            ],

            // ---------- Navigation (retour / création) ----------
            if (_step < 4) ...[
              const SizedBox(height: 24),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  if (_step > 0)
                    OutlinedButton.icon(
                      onPressed: _submitting ? null : () => setState(() => _step -= 1),
                      icon: const Icon(Icons.arrow_back_rounded),
                      label: const Text('Retour'),
                    )
                  else
                    const SizedBox.shrink(),
                  ElevatedButton.icon(
                    onPressed: _submitting ? null : _submitCurrentStep,
                    icon: _submitting
                        ? const SizedBox(
                            width: 16, height: 16,
                            child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                        : Icon(_step == 3 ? Icons.auto_awesome : Icons.arrow_forward_rounded),
                    label: Text([
                      "Créer l'organisation",
                      "Créer l'église",
                      'Créer le département',
                      'Créer la famille',
                    ][_step]),
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _RecapCard extends StatelessWidget {
  const _RecapCard({required this.title, required this.icon, required this.name, required this.id});

  final String title;
  final IconData icon;
  final String name;
  final String id;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        leading: Icon(icon, color: Colors.deepPurple),
        title: Text(name, style: const TextStyle(fontWeight: FontWeight.w600)),
        subtitle: Text('id : $id', style: const TextStyle(fontSize: 11)),
        trailing: Text(title, style: const TextStyle(fontSize: 11, color: Colors.grey)),
      ),
    );
  }
}

