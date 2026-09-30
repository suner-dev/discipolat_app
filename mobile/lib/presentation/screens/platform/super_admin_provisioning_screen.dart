import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../app.dart';
import '../../../data/services/api_service.dart';
import '../../../data/services/currency_catalog_service.dart';

/// Super Admin — Flux de provisionnement guidé (mobile), 4 étapes cliquables :
///   1. Organisation (tenant) → 2. Église → 3. Département → 4. Famille → Récap
/// Même contrat d'API que le wizard web :
///   POST /platform/admin/provisioning
///   GET  /platform/admin/plans   (plans réels, jamais codés en dur — D12/B10)
///   GET  /platform/currencies    (catalogue ISO-4217)
///   GET  /currencies/timezones   (identifiants IANA)
///
/// B10 (constat MO4) : plans + géo + owner. Aucune valeur géographique/plan
/// n'est FORCÉE — elles ne servent que de sélection par défaut, modifiable.
/// L'owner (pasteur) est obligatoire (contrat §3.5) et son `activationEmailSent`
/// est affiché au récapitulatif (D10 : un SMTP absent reste non bloquant).
class SuperAdminProvisioningScreen extends StatefulWidget {
  const SuperAdminProvisioningScreen({super.key, this.apiService});

  /// Permet d'injecter un ApiService mocké dans les tests widget.
  final ApiService? apiService;

  @override
  State<SuperAdminProvisioningScreen> createState() =>
      _SuperAdminProvisioningScreenState();
}

/// Option générique `{value, label}` pour les sélecteurs alimentés par l'API.
class _Option {
  const _Option(this.value, this.label);
  final String value;
  final String label;
}

class _SuperAdminProvisioningScreenState
    extends State<SuperAdminProvisioningScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();

  static const _stepTitles = [
    'Organisation',
    'Église',
    'Département',
    'Famille'
  ];

  int _step = 0; // 0..3 formulaires, 4 récapitulatif
  bool _submitting = false;

  // Résultats des étapes créées
  Map<String, dynamic>? _tenant;
  Map<String, dynamic>? _church;
  Map<String, dynamic>? _department;
  Map<String, dynamic>? _family;
  Map<String, dynamic>? _owner;

  // ── B10 — référentiels chargés depuis l'API (D12 : rien de codé en dur) ──
  // Repli documentaire, uniquement si l'appel réseau échoue (avec message).
  static const List<_Option> _plansFallback = <_Option>[
    _Option('DISCOVERY', 'DISCOVERY'),
    _Option('STARTUP', 'STARTUP'),
    _Option('GROWTH', 'GROWTH'),
    _Option('NETWORK', 'NETWORK'),
  ];
  static const List<_Option> _timezoneFallback = <_Option>[
    _Option('Africa/Douala', 'Africa/Douala'),
    _Option('Africa/Abidjan', 'Africa/Abidjan'),
    _Option('Europe/Paris', 'Europe/Paris'),
    _Option('UTC', 'UTC'),
  ];

  List<_Option> _plans = _plansFallback;
  bool _plansFromApi = false;
  List<_Option> _currencies = const <_Option>[];
  bool _currenciesFromApi = false;
  List<_Option> _timezones = _timezoneFallback;
  bool _timezonesFromApi = false;

  // Étape 1 — organisation.
  //
  // G-B §5 : « une donnée en dur (plan/pays/devise/fuseau) utilisée comme valeur
  // forcée → REFUS ». Les valeurs ci-dessous ne sont plus des constantes
  // forcées : elles valent `null` tant que l'utilisateur n'a rien choisi, et
  // la clé est alors OMISE du payload pour que le serveur applique sa propre
  // valeur par défaut. Un `CM`/`XAF`/`Africa/Douala`/`DISCOVERY` envoyé sans
  // choix explicite de l'utilisateur affirmait une vérité non demandée.
  final _orgName = TextEditingController();
  final _countryController = TextEditingController();
  String? _orgPlan;
  String? _currency;
  String? _timezone;
  String _locale = 'fr';

  // B10 — owner obligatoire (contrat §3.5)
  final _ownerEmail = TextEditingController();
  final _ownerFirstName = TextEditingController();
  final _ownerLastName = TextEditingController();

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

  static final RegExp _emailRe = RegExp(r'^\S+@\S+\.\S+$');

  bool get _ownerValid =>
      _emailRe.hasMatch(_ownerEmail.text.trim()) &&
      _ownerFirstName.text.trim().isNotEmpty &&
      _ownerLastName.text.trim().isNotEmpty;

  @override
  void initState() {
    super.initState();
    _loadPlans();
    _loadCurrencies();
    _loadTimezones();
  }

  @override
  void dispose() {
    _orgName.dispose();
    _countryController.dispose();
    _ownerEmail.dispose();
    _ownerFirstName.dispose();
    _ownerLastName.dispose();
    _churchName.dispose();
    _deptNom.dispose();
    _deptDesc.dispose();
    _deptRespId.dispose();
    _deptFirstName.dispose();
    _deptLastName.dispose();
    _deptEmail.dispose();
    _deptPhone.dispose();
    _famNom.dispose();
    _famChefId.dispose();
    _famFirstName.dispose();
    _famLastName.dispose();
    _famEmail.dispose();
    _famPhone.dispose();
    super.dispose();
  }

  /// Plans réels via `GET /platform/admin/plans`. En cas d'échec, repli
  /// documentaire + message (l'utilisateur sait qu'il ne voit pas la liste
  /// officielle). Jamais de plan forcé.
  Future<void> _loadPlans() async {
    try {
      final res = await _api.get('/platform/admin/plans');
      final data = res.data;
      final list = <_Option>[];
      if (data is List) {
        for (final raw in data) {
          if (raw is Map && raw['key'] is String) {
            final key = raw['key'] as String;
            final name = raw['name'] is String ? raw['name'] as String : key;
            list.add(_Option(key, '$name ($key)'));
          }
        }
      }
      if (!mounted) return;
      if (list.isEmpty) {
        setState(() {
          _plans = _plansFallback;
          _plansFromApi = false;
        });
      } else {
        setState(() {
          _plans = list;
          _plansFromApi = true;
          // On ne présélectionne plus rien : tant que l'utilisateur n'a pas
          // choisi, `_orgPlan` reste null et la clé est omise du payload.
          if (_orgPlan != null && !_plans.any((p) => p.value == _orgPlan)) {
            _orgPlan = null;
          }
        });
      }
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _plans = _plansFallback;
        _plansFromApi = false;
      });
    }
  }

  Future<void> _loadCurrencies() async {
    final catalog = await CurrencyCatalogService(_api).fetch();
    if (!mounted) return;
    final options =
        catalog.currencies.map((c) => _Option(c.code, c.label)).toList();
    setState(() {
      _currencies = options;
      _currenciesFromApi = catalog.fromServer;
      // Un plan/devise déjà choisi qui disparaît du catalogue n'est pas
      // réécrit d'autorité : on le remet à « non choisi » (la clé sera omise).
      if (_currency != null && !_currencies.any((c) => c.value == _currency)) {
        _currency = null;
      }
    });
  }

  /// Fuseaux IANA via `GET /currencies/timezones`. Repli documentaire si absent.
  Future<void> _loadTimezones() async {
    try {
      final res = await _api.get('/currencies/timezones');
      final data = res.data;
      final list = <_Option>[];
      if (data is List) {
        for (final raw in data) {
          if (raw is Map && raw['id'] is String) {
            final id = raw['id'] as String;
            final name = raw['name'] is String ? raw['name'] as String : id;
            list.add(_Option(id, name));
          }
        }
      }
      if (!mounted) return;
      if (list.isEmpty) {
        setState(() {
          _timezones = _timezoneFallback;
          _timezonesFromApi = false;
        });
      } else {
        setState(() {
          _timezones = list;
          _timezonesFromApi = true;
          // Idem devise : une valeur choisie disparue n'est pas réécrite, elle
          // repasse à « non choisi » et la clé est omise du payload.
          if (_timezone != null && !_timezones.any((t) => t.value == _timezone)) {
            _timezone = null;
          }
        });
      }
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _timezones = _timezoneFallback;
        _timezonesFromApi = false;
      });
    }
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
      if (_orgName.text.trim().isEmpty) {
        return "Le nom de l'organisation est requis";
      }
      if (_slugify(_orgName.text).isEmpty) {
        return 'Nom invalide pour le slug';
      }
      // B10 — owner obligatoire (contrat §3.5, fail-closed côté serveur).
      if (_ownerEmail.text.trim().isEmpty) {
        return "L'email du propriétaire est requis";
      }
      if (!_emailRe.hasMatch(_ownerEmail.text.trim())) {
        return 'Email du propriétaire invalide';
      }
      if (_ownerFirstName.text.trim().isEmpty ||
          _ownerLastName.text.trim().isEmpty) {
        return 'Prénom et nom du propriétaire requis';
      }
      return null;
    }
    if (step == 1) {
      if (_churchName.text.trim().isEmpty) {
        return "Le nom de l'église est requis";
      }
      return null;
    }
    if (step == 2) {
      if (_deptNom.text.trim().isEmpty) {
        return 'Le nom du département est requis';
      }
      if (!_deptNewMode && _deptRespId.text.trim().isEmpty) {
        return "L'ID du responsable existant est requis";
      }
      if (_deptNewMode) {
        if (_deptFirstName.text.trim().isEmpty ||
            _deptLastName.text.trim().isEmpty) {
          return 'Prénom et nom du responsable requis';
        }
        if (!_emailRe.hasMatch(_deptEmail.text.trim())) {
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
        if (_famFirstName.text.trim().isEmpty ||
            _famLastName.text.trim().isEmpty) {
          return 'Prénom et nom du chef requis';
        }
        if (!_emailRe.hasMatch(_famEmail.text.trim())) {
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
      if (_step < 3) {
        if (_step == 0 && _churchName.text.trim().isEmpty) {
          _churchName.text = '${_orgName.text.trim()} — Église principale';
        }
        if (_step == 2 && _famNom.text.trim().isEmpty) {
          _famNom.text = '${_orgName.text.trim()} — Famille modèle';
        }
        if (mounted) setState(() => _step++);
        return;
      }

      // G-B §5 : seules les valeurs EXPLICITEMENT choisies sont envoyées. Le
      // reste est omis pour que le serveur applique sa valeur par défaut —
      // envoyer 'CM'/'XAF'/'Africa/Douala' sans choix serait une donnée forcée.
      final country = _countryController.text.trim();
      final payload = <String, dynamic>{
        'name': _orgName.text.trim(),
        'slug': _slugify(_orgName.text),
        if (_orgPlan != null) 'plan': _orgPlan,
        if (country.isNotEmpty) 'country': country,
        if (_currency != null) 'currency': _currency,
        if (_timezone != null) 'timezone': _timezone,
        'locale': _locale,
        'churchName': _churchName.text.trim(),
        'departmentName': _deptNom.text.trim(),
        'departmentDescription':
            _deptDesc.text.trim().isEmpty ? null : _deptDesc.text.trim(),
        'familyName': _famNom.text.trim(),
        // B10 — owner (contrat §3.5)
        'ownerEmail': _ownerEmail.text.trim(),
        'ownerFirstName': _ownerFirstName.text.trim(),
        'ownerLastName': _ownerLastName.text.trim(),
      };
      if (_deptNewMode) {
        payload.addAll({
          'createNewResponsable': true,
          'newResponsableFirstName': _deptFirstName.text.trim(),
          'newResponsableLastName': _deptLastName.text.trim(),
          'newResponsableEmail': _deptEmail.text.trim(),
          'newResponsablePhone':
              _deptPhone.text.trim().isEmpty ? null : _deptPhone.text.trim(),
        });
      } else {
        payload['responsableId'] = _deptRespId.text.trim();
      }
      if (_famNewMode) {
        payload.addAll({
          'createNewChef': true,
          'newChefFirstName': _famFirstName.text.trim(),
          'newChefLastName': _famLastName.text.trim(),
          'newChefEmail': _famEmail.text.trim(),
          'newChefPhone':
              _famPhone.text.trim().isEmpty ? null : _famPhone.text.trim(),
        });
      } else {
        payload['chefFamilleId'] = _famChefId.text.trim();
      }
      final res =
          await _api.post('/platform/admin/provisioning', data: payload);
      final result = Map<String, dynamic>.from(res.data as Map);
      _tenant = Map<String, dynamic>.from(result['tenant'] as Map);
      _church = Map<String, dynamic>.from(result['church'] as Map);
      _department = Map<String, dynamic>.from(result['department'] as Map);
      _family = Map<String, dynamic>.from(result['family'] as Map);
      _owner = result['owner'] is Map
          ? Map<String, dynamic>.from(result['owner'] as Map)
          : null;
      _snack('Provisionnement terminé !');
      if (mounted) setState(() => _step = 4);
    } catch (e) {
      _snack('Erreur: $e', error: true);
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  void _resetAll() {
    setState(() {
      _step = 0;
      _tenant = null;
      _church = null;
      _department = null;
      _family = null;
      _owner = null;
      _orgName.clear();
      _orgPlan = null;
      _countryController.clear();
      _currency = null;
      _timezone = null;
      _locale = 'fr';
      _ownerEmail.clear();
      _ownerFirstName.clear();
      _ownerLastName.clear();
      _churchName.clear();
      _deptNom.clear();
      _deptDesc.clear();
      _deptNewMode = true;
      _deptRespId.clear();
      _deptFirstName.clear();
      _deptLastName.clear();
      _deptEmail.clear();
      _deptPhone.clear();
      _famNom.clear();
      _famNewMode = true;
      _famChefId.clear();
      _famFirstName.clear();
      _famLastName.clear();
      _famEmail.clear();
      _famPhone.clear();
    });
  }

  bool _stepDone(int idx) => idx == 0
      ? _tenant != null
      : idx == 1
          ? _church != null
          : idx == 2
              ? _department != null
              : _family != null;

  bool _reachable(int idx) =>
      idx == 0 ||
      (idx == 1 && _tenant != null) ||
      (idx == 2 && _church != null) ||
      (idx == 3 && _department != null);

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
                      padding: const EdgeInsets.symmetric(
                          horizontal: 12, vertical: 8),
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
                            done && !active
                                ? Icons.check_circle_rounded
                                : Icons.circle,
                            size: 18,
                            color: active
                                ? Colors.white
                                : done
                                    ? Colors.green
                                    : Colors.grey,
                          ),
                          const SizedBox(width: 6),
                          Text(
                            '${idx + 1}. ${_stepTitles[idx]}',
                            style: TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w600,
                              color: active
                                  ? Colors.white
                                  : reachable
                                      ? null
                                      : Colors.grey,
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
                  style: theme.textTheme.titleMedium
                      ?.copyWith(fontWeight: FontWeight.bold)),
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
              _LabeledDropdown(
                label: 'Plan',
                value: _orgPlan,
                items: _plans,
                hint: _plansFromApi ? null : 'Liste minimale (API indisponible)',
                onChanged: (v) => setState(() => _orgPlan = v),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _countryController,
                decoration: const InputDecoration(
                  labelText: 'Pays (code)',
                  helperText: 'Aucune API pays — saisie libre (ex. CM)',
                  border: OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 12),
              if (_currencies.isNotEmpty)
                _LabeledDropdown(
                  label: 'Devise',
                  value: _currency,
                  items: _currencies,
                  hint: _currenciesFromApi
                      ? null
                      : 'Liste minimale (API indisponible)',
                  onChanged: (v) => setState(() => _currency = v),
                )
              else
                const LinearProgressIndicator(minHeight: 2),
              const SizedBox(height: 12),
              _LabeledDropdown(
                label: 'Fuseau horaire',
                value: _timezone,
                items: _timezones,
                hint: _timezonesFromApi ? null : 'Liste minimale (API indisponible)',
                onChanged: (v) => setState(() => _timezone = v),
              ),
              const SizedBox(height: 12),
              _LabeledDropdown(
                label: 'Langue',
                value: _locale,
                items: const <_Option>[
                  _Option('fr', 'Français'),
                  _Option('en', 'English'),
                  _Option('pt', 'Português'),
                  _Option('es', 'Español'),
                  _Option('sw', 'Kiswahili'),
                  _Option('ar', 'العربية'),
                ],
                onChanged: (v) => setState(() => _locale = v ?? _locale),
              ),
              const SizedBox(height: 8),
              Text('Slug : ${_slugify(_orgName.text)}',
                  style: const TextStyle(fontSize: 12, color: Colors.grey)),

              // ── B10 — Owner (pasteur) obligatoire, contrat §3.5 ──
              const SizedBox(height: 20),
              _OwnerSection(
                email: _ownerEmail,
                firstName: _ownerFirstName,
                lastName: _ownerLastName,
                valid: _ownerValid,
                onNotify: () => setState(() {}),
              ),
            ],

            // ---------- Étape 2 : église ----------
            if (_step == 1) ...[
              Text('2. Église racine',
                  style: theme.textTheme.titleMedium
                      ?.copyWith(fontWeight: FontWeight.bold)),
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
                  style: theme.textTheme.titleMedium
                      ?.copyWith(fontWeight: FontWeight.bold)),
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
                  style: theme.textTheme.titleMedium
                      ?.copyWith(fontWeight: FontWeight.bold)),
              const SizedBox(height: 4),
              const Text(
                  'Première famille — un chef de famille est obligatoire.',
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
              const Icon(Icons.check_circle_rounded,
                  size: 56, color: Colors.green),
              const SizedBox(height: 8),
              Text('Organisation provisionnée !',
                  textAlign: TextAlign.center,
                  style: theme.textTheme.titleMedium
                      ?.copyWith(fontWeight: FontWeight.bold)),
              const SizedBox(height: 4),
              const Text(
                  'Les 4 étapes ont été créées — chaque action est journalisée en audit.',
                  textAlign: TextAlign.center,
                  style: TextStyle(fontSize: 13, color: Colors.grey)),
              const SizedBox(height: 16),
              _RecapCard(
                  title: 'Organisation',
                  icon: Icons.rocket_launch_rounded,
                  name: '${_tenant?['name'] ?? ''}',
                  id: '${_tenant?['id'] ?? ''}'),
              _RecapCard(
                  title: 'Église',
                  icon: Icons.church_rounded,
                  name: '${_church?['name'] ?? ''}',
                  id: '${_church?['id'] ?? ''}'),
              _RecapCard(
                  title: 'Département',
                  icon: Icons.groups_rounded,
                  name: '${_department?['nom'] ?? ''}',
                  id: '${_department?['id'] ?? ''}'),
              _RecapCard(
                  title: 'Famille',
                  icon: Icons.home_rounded,
                  name: '${_family?['nom'] ?? ''}',
                  id: '${_family?['id'] ?? ''}'),
              if (_owner != null)
                _OwnerRecap(owner: _owner!),
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
                    onPressed: () => context.go(roleHome(AuthState().activeRole,
                        isPlatformSuperAdmin:
                            AuthState().isPlatformSuperAdmin)),
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
                      onPressed:
                          _submitting ? null : () => setState(() => _step -= 1),
                      icon: const Icon(Icons.arrow_back_rounded),
                      label: const Text('Retour'),
                    )
                  else
                    const SizedBox.shrink(),
                  ElevatedButton.icon(
                    onPressed: _submitting || (_step == 0 && !_ownerValid)
                        ? null
                        : _submitCurrentStep,
                    icon: _submitting
                        ? const SizedBox(
                            width: 16,
                            height: 16,
                            child: CircularProgressIndicator(
                                strokeWidth: 2, color: Colors.white))
                        : Icon(_step == 3
                            ? Icons.auto_awesome
                            : Icons.arrow_forward_rounded),
                    label: Text([
                      "Continuer",
                      "Continuer",
                      'Continuer',
                      "Provisionner l'organisation",
                    ][_step]),
                  ),
                ],
              ),
              if (_step == 0 && !_ownerValid)
                const Padding(
                  padding: EdgeInsets.only(top: 8),
                  child: Text(
                    "Renseignez un propriétaire (email + prénom + nom) valide "
                    "pour continuer.",
                    style: TextStyle(fontSize: 12, color: Colors.grey),
                  ),
                ),
            ],
          ],
        ),
      ),
    );
  }
}

/// Sélecteur étiqueté, piloté par l'état (items chargés depuis l'API).
class _LabeledDropdown extends StatelessWidget {
  const _LabeledDropdown({
    required this.label,
    required this.value,
    required this.items,
    required this.onChanged,
    this.hint,
  });

  final String label;

  /// Valeur choisie, ou `null` si l'utilisateur n'a rien sélectionné. Dans ce
  /// cas la clé correspondante est omise du payload (G-B §5) : on n'invente
  /// pas une valeur à sa place.
  final String? value;
  final List<_Option> items;
  final ValueChanged<String?> onChanged;
  final String? hint;

  /// Libellé de l'option « rien choisi ». Volontairement explicite pour que
  /// l'absence de valeur soit un choix visible, pas un oubli.
  static const String _unsetLabel = 'Non choisi (valeur par défaut du serveur)';

  @override
  Widget build(BuildContext context) {
    // La liste affichée est toujours préfixée de l'option « non choisi » : sans
    // elle, un `DropdownButton` à valeur nulle ne pourrait pas être representé
    // et le premier élément apparaîtrait coché par défaut.
    final options = <_Option>[const _Option('', _unsetLabel), ...items];
    final effective = (value != null && items.any((i) => i.value == value))
        ? value
        : '';
    return InputDecorator(
      decoration: InputDecoration(
        labelText: label,
        border: const OutlineInputBorder(),
        helperText: hint,
        helperStyle: TextStyle(color: Colors.orange.shade800, fontSize: 11),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<String>(
          isExpanded: true,
          value: effective,
          items: options
              .map((o) => DropdownMenuItem<String>(
                    value: o.value,
                    child: Text(o.label, overflow: TextOverflow.ellipsis),
                  ))
              .toList(),
          onChanged: (v) => onChanged((v == null || v.isEmpty) ? null : v),
        ),
      ),
    );
  }
}

/// Bloc owner obligatoire (contrat §3.5). Centralise les 3 champs + l'état de
/// validité affiché en temps réel.
class _OwnerSection extends StatelessWidget {
  const _OwnerSection({
    required this.email,
    required this.firstName,
    required this.lastName,
    required this.valid,
    required this.onNotify,
  });

  final TextEditingController email;
  final TextEditingController firstName;
  final TextEditingController lastName;
  final bool valid;
  final VoidCallback onNotify;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.deepPurple.withValues(alpha: 0.05),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.deepPurple.withValues(alpha: 0.3)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: const [
              Icon(Icons.person_pin_circle, size: 18, color: Colors.deepPurple),
              SizedBox(width: 6),
              Text("Propriétaire de l'organisation (obligatoire)",
                  style: TextStyle(fontWeight: FontWeight.bold)),
            ],
          ),
          const SizedBox(height: 4),
          const Text(
            "Le compte pasteur/owner est créé avec l'organisation ; un email "
            "d'activation lui est envoyé.",
            style: TextStyle(fontSize: 12, color: Colors.grey),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: email,
            keyboardType: TextInputType.emailAddress,
            onChanged: (_) => onNotify(),
            decoration: InputDecoration(
              labelText: 'Email du propriétaire *',
              border: const OutlineInputBorder(),
              errorText: email.text.trim().isNotEmpty &&
                      !RegExp(r'^\S+@\S+\.\S+$').hasMatch(email.text.trim())
                  ? 'Email invalide'
                  : null,
            ),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: firstName,
                  onChanged: (_) => onNotify(),
                  decoration: const InputDecoration(
                    labelText: 'Prénom *',
                    border: OutlineInputBorder(),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: TextField(
                  controller: lastName,
                  onChanged: (_) => onNotify(),
                  decoration: const InputDecoration(
                    labelText: 'Nom *',
                    border: OutlineInputBorder(),
                  ),
                ),
              ),
            ],
          ),
          if (valid)
            const Padding(
              padding: EdgeInsets.only(top: 8),
              child: Row(
                children: [
                  Icon(Icons.check_circle, size: 16, color: Colors.green),
                  SizedBox(width: 6),
                  Text('Owner prêt',
                      style: TextStyle(fontSize: 12, color: Colors.green)),
                ],
              ),
            ),
        ],
      ),
    );
  }
}

/// Récapitulatif owner + avertissement si l'email d'activation n'a pas pu être
/// envoyé (D10 : SMTP absent → `activationEmailSent == false`, non bloquant).
class _OwnerRecap extends StatelessWidget {
  const _OwnerRecap({required this.owner});

  final Map<String, dynamic> owner;

  @override
  Widget build(BuildContext context) {
    final sent = owner['activationEmailSent'] == true;
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        leading: const Icon(Icons.person_pin_circle, color: Colors.deepPurple),
        title: Text('${owner['email'] ?? ''}',
            style: const TextStyle(fontWeight: FontWeight.w600)),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Compte propriétaire',
                style: TextStyle(fontSize: 11, color: Colors.grey)),
            const SizedBox(height: 4),
            sent
                ? const Row(
                    children: [
                      Icon(Icons.mark_email_read, size: 14, color: Colors.green),
                      SizedBox(width: 4),
                      Text("Email d'activation envoyé",
                          style: TextStyle(fontSize: 12, color: Colors.green)),
                    ],
                  )
                : const Row(
                    children: [
                      Icon(Icons.warning_amber_rounded,
                          size: 14, color: Colors.orange),
                      SizedBox(width: 4),
                      Expanded(
                        child: Text(
                          "Email d'activation non envoyé — transmettez le lien "
                          "d'activation manuellement.",
                          style:
                              TextStyle(fontSize: 12, color: Colors.orange),
                        ),
                      ),
                    ],
                  ),
          ],
        ),
        isThreeLine: true,
      ),
    );
  }
}

class _RecapCard extends StatelessWidget {
  const _RecapCard(
      {required this.title,
      required this.icon,
      required this.name,
      required this.id});

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
        trailing: Text(title,
            style: const TextStyle(fontSize: 11, color: Colors.grey)),
      ),
    );
  }
}
