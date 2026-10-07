import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:discipolat_mobile/app.dart';
import 'package:discipolat_mobile/features/health/models/health_model.dart';
import 'package:discipolat_mobile/features/health/services/health_service.dart'
    as health_service;
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';
import 'package:discipolat_mobile/features/health/widgets/patient_card.dart';
import 'package:discipolat_mobile/features/health/widgets/consultation_card.dart';
import 'package:discipolat_mobile/features/health/widgets/stock_card.dart';
import 'package:discipolat_mobile/features/health/widgets/campaign_card.dart';

/// UUID canonique — seul format accepté par le serveur (`@PathVariable UUID`
/// et désérialisation Jackson des associations imbriquées).
final RegExp _uuidPattern = RegExp(
  r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$',
);

/// Message lisible d'une erreur API — préserve le statut HTTP au lieu de
/// l'écraser dans une `Exception` générique.
String _errorText(Object e) {
  if (e is DioException) {
    final status = e.response?.statusCode;
    final data = e.response?.data;
    String? message;
    if (data is Map) {
      message = data['message']?.toString() ?? data['error']?.toString();
    }
    if (message != null && message.isNotEmpty) {
      return 'Erreur ${status ?? 'réseau'} : $message';
    }
    return status != null ? 'Erreur serveur ($status)' : 'Erreur réseau';
  }
  return 'Erreur : $e';
}

Future<DateTime?> _pickDate(BuildContext context, {DateTime? initial}) =>
    showDatePicker(
      context: context,
      initialDate: initial ?? DateTime.now(),
      firstDate: DateTime(2000),
      lastDate: DateTime(2100),
    );

/// `yyyy-MM-dd` — format `LocalDate` attendu par le serveur (Jackson ISO).
String _isoDate(DateTime d) =>
    '${d.year.toString().padLeft(4, '0')}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

void _snack(BuildContext context, String message, {bool error = false}) {
  ScaffoldMessenger.of(context).showSnackBar(SnackBar(
    content: Text(message),
    backgroundColor: error ? Colors.red.shade700 : Colors.green.shade700,
  ));
}

// ---------------------------------------------------------------------------
// Providers — page unique de 50 entrées par onglet (le serveur borne size
// à 50 ; aucun filtre inventé n'est envoyé).
// ---------------------------------------------------------------------------

final _patientsProvider = FutureProvider.autoDispose<List<PatientRecord>>((ref) async {
  final service = ref.watch(health_service.healthServiceProvider);
  return service.getPatients(size: 50);
});

/// Le serveur n'expose PAS de paramètre `status` sur `GET /consultations`
/// (contrat V240 vérifié) : le filtre s'applique côté client sur la page
/// chargée — rien n'est simulé côté serveur.
final _consultationsProvider =
    FutureProvider.autoDispose.family<List<MedicalConsultation>, ConsultationStatus?>((
        ref, status) async {
  final service = ref.watch(health_service.healthServiceProvider);
  final all = await service.getConsultations(size: 50);
  if (status == null) return all;
  return all.where((c) => c.status == status).toList(growable: false);
});

final _stockProvider = FutureProvider.autoDispose<List<PharmacyStock>>((ref) async {
  final service = ref.watch(health_service.healthServiceProvider);
  return service.getPharmacyStock(size: 50);
});

/// `GET /campaigns` accepte un `status` serveur : filtre réellement
/// appliquée côté serveur.
final _campaignsProvider =
    FutureProvider.autoDispose.family<List<HealthCampaign>, CampaignStatus?>((
        ref, status) async {
  final service = ref.watch(health_service.healthServiceProvider);
  return service.getCampaigns(status: status, size: 20);
});

final _kitsProvider = FutureProvider.autoDispose<List<HealthKit>>((ref) async {
  final service = ref.watch(health_service.healthServiceProvider);
  return service.getKits();
});

final _dutiesProvider = FutureProvider.autoDispose<List<HealthDuty>>((ref) async {
  final service = ref.watch(health_service.healthServiceProvider);
  return service.getDuties();
});

class HealthScreen extends ConsumerStatefulWidget {
  const HealthScreen({super.key});

  @override
  ConsumerState<HealthScreen> createState() => _HealthScreenState();
}

class _HealthScreenState extends ConsumerState<HealthScreen>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;
  ConsultationStatus? _filterConsultationStatus;
  CampaignStatus? _filterCampaignStatus;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 5, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.surfaceDark,
      appBar: AppBar(
        title: const Text('Santé & Infirmerie'),
        backgroundColor: AppColors.cardDark,
        elevation: 0,
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: Colors.red,
          labelColor: Colors.red,
          unselectedLabelColor: AppColors.surface.withOpacity(0.7),
          tabs: const [
            Tab(text: 'Patients'),
            Tab(text: 'Consultations'),
            Tab(text: 'Pharmacie'),
            Tab(text: 'Campagnes'),
            Tab(text: 'Kits/Gardes'),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.add_rounded),
            tooltip: 'Nouveau',
            onPressed: () => switch (_tabController.index) {
              0 => _showPatientCreateSheet(),
              1 => _showConsultationCreateSheet(),
              2 => _showPharmacyItemCreateSheet(),
              3 => _showCampaignCreateSheet(),
              // Kits & gardes : le serveur n'expose que des GET (contrat
              // V235/V240 vérifié) — la création passe par le back-office.
              _ => _snack(context, 'Création de kits/gardes via le back-office web.',
                  error: true),
            },
          ),
        ],
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          _buildPatientsTab(),
          _buildConsultationsTab(),
          _buildPharmacyTab(),
          _buildCampaignsTab(),
          _buildKitsDutiesTab(),
        ],
      ),
    );
  }

  // --------------------------------------------------------------------------
  // Onglets
  // --------------------------------------------------------------------------

  Widget _buildPatientsTab() {
    final patientsAsync = ref.watch(_patientsProvider);
    return RefreshIndicator(
      onRefresh: () => ref.refresh(_patientsProvider.future),
      child: patientsAsync.when(
        data: (patients) {
          if (patients.isEmpty) {
            return _buildEmptyState(
              message: 'Aucun patient',
              action: _showPatientCreateSheet,
            );
          }
          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: patients.length,
            itemBuilder: (context, index) {
              final patient = patients[index];
              return PatientCard(
                patient: patient,
                onTap: () => _showPatientDetails(patient),
              );
            },
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => _buildErrorState(error),
      ),
    );
  }

  Widget _buildConsultationsTab() {
    final consultationsAsync = ref.watch(_consultationsProvider(_filterConsultationStatus));
    return Column(
      children: [
        _buildChipFilter(
          colors: (Colors.blue, Colors.blue),
          allLabel: 'Toutes',
          values: ConsultationStatus.values,
          selected: _filterConsultationStatus,
          labelOf: (s) => s.displayName,
          onChanged: (s) => setState(() => _filterConsultationStatus = s),
        ),
        Expanded(
          child: RefreshIndicator(
            onRefresh: () => ref.refresh(_consultationsProvider(_filterConsultationStatus).future),
            child: consultationsAsync.when(
              data: (consultations) {
                if (consultations.isEmpty) {
                  return _buildEmptyState(
                    message: 'Aucune consultation',
                    action: _showConsultationCreateSheet,
                  );
                }
                return ListView.builder(
                  padding: const EdgeInsets.all(16),
                  itemCount: consultations.length,
                  itemBuilder: (context, index) {
                    final consultation = consultations[index];
                    return ConsultationCard(
                      consultation: consultation,
                      onTap: () => _showConsultationDetails(consultation),
                    );
                  },
                );
              },
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (error, _) => _buildErrorState(error),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildPharmacyTab() {
    final stockAsync = ref.watch(_stockProvider);
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
          child: Row(
            children: [
              Expanded(
                child: FilledButton.icon(
                  onPressed: _showPharmacyItemCreateSheet,
                  icon: const Icon(Icons.add_rounded),
                  label: const Text('Nouvel article'),
                  style: FilledButton.styleFrom(backgroundColor: Colors.red),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () => ref.refresh(_stockProvider.future),
                  icon: const Icon(Icons.refresh_rounded),
                  label: const Text('Actualiser'),
                ),
              ),
            ],
          ),
        ),
        Expanded(
          child: RefreshIndicator(
            onRefresh: () => ref.refresh(_stockProvider.future),
            child: stockAsync.when(
              data: (stocks) {
                if (stocks.isEmpty) {
                  return _buildEmptyState(
                    message: 'Aucun lot en stock',
                    action: _showPharmacyItemCreateSheet,
                  );
                }
                return ListView.builder(
                  padding: const EdgeInsets.all(16),
                  itemCount: stocks.length,
                  itemBuilder: (context, index) {
                    final stock = stocks[index];
                    return StockCard(
                      stock: stock,
                      onTap: () => _showStockDetails(stock),
                    );
                  },
                );
              },
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (error, _) => _buildErrorState(error),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildCampaignsTab() {
    final campaignsAsync = ref.watch(_campaignsProvider(_filterCampaignStatus));
    return Column(
      children: [
        _buildChipFilter(
          colors: (Colors.green, Colors.green),
          allLabel: 'Toutes',
          values: CampaignStatus.values,
          selected: _filterCampaignStatus,
          labelOf: (s) => s.displayName,
          onChanged: (s) => setState(() => _filterCampaignStatus = s),
        ),
        Expanded(
          child: RefreshIndicator(
            onRefresh: () => ref.refresh(_campaignsProvider(_filterCampaignStatus).future),
            child: campaignsAsync.when(
              data: (campaigns) {
                if (campaigns.isEmpty) {
                  return _buildEmptyState(
                    message: 'Aucune campagne',
                    action: _showCampaignCreateSheet,
                  );
                }
                return ListView.builder(
                  padding: const EdgeInsets.all(16),
                  itemCount: campaigns.length,
                  itemBuilder: (context, index) {
                    final campaign = campaigns[index];
                    return CampaignCard(
                      campaign: campaign,
                      onTap: () => _showCampaignDetails(campaign),
                    );
                  },
                );
              },
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (error, _) => _buildErrorState(error),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildKitsDutiesTab() {
    final kitsAsync = ref.watch(_kitsProvider);
    final dutiesAsync = ref.watch(_dutiesProvider);
    return RefreshIndicator(
      onRefresh: () async {
        ref.invalidate(_kitsProvider);
        ref.invalidate(_dutiesProvider);
        await Future.wait([
          ref.refresh(_kitsProvider.future),
          ref.refresh(_dutiesProvider.future),
        ]);
      },
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text('Kits médicaux',
              style: Theme.of(context)
                  .textTheme
                  .titleMedium
                  ?.copyWith(fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          ...kitsAsync.maybeWhen(
            data: (kits) => kits.isEmpty
                ? [const _MiniEmpty('Aucun kit')]
                : kits
                    .map((k) => _kitTile(k))
                    .toList(growable: false),
            orElse: () => const [SizedBox(height: 40, child: Center(child: CircularProgressIndicator()))],
          ),
          const SizedBox(height: 24),
          Text('Gardes',
              style: Theme.of(context)
                  .textTheme
                  .titleMedium
                  ?.copyWith(fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          ...dutiesAsync.maybeWhen(
            data: (duties) => duties.isEmpty
                ? [const _MiniEmpty('Aucune garde planifiée')]
                : duties
                    .map((d) => _dutyTile(d))
                    .toList(growable: false),
            orElse: () => const [SizedBox(height: 40, child: Center(child: CircularProgressIndicator()))],
          ),
        ],
      ),
    );
  }

  Widget _kitTile(HealthKit kit) {
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      color: AppColors.cardDark,
      child: ListTile(
        leading: Icon(Icons.medical_services_rounded,
            color: kit.isActive ? Colors.teal : Colors.grey),
        title: Text(kit.name ?? 'Kit sans nom'),
        subtitle: Text(
          [
            if (kit.category != null) kit.category!,
            '${kit.quantity} unité(s)',
          ].join(' • '),
        ),
      ),
    );
  }

  Widget _dutyTile(HealthDuty duty) {
    final scheduled = duty.scheduledAt;
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      color: AppColors.cardDark,
      child: ListTile(
        leading: Icon(
          duty.completedAt != null
              ? Icons.task_alt_rounded
              : Icons.security_rounded,
          color: duty.completedAt != null
              ? Colors.green
              : (duty.isActive ? Colors.blue : Colors.grey),
        ),
        title: Text(duty.name ?? 'Garde'),
        subtitle: Text([
          if (duty.dutyType != null) duty.dutyType!,
          if (scheduled != null)
            '${scheduled.day.toString().padLeft(2, '0')}/${scheduled.month.toString().padLeft(2, '0')}/${scheduled.year} ${scheduled.hour.toString().padLeft(2, '0')}:${scheduled.minute.toString().padLeft(2, '0')}',
        ].join(' • ')),
      ),
    );
  }

  // --------------------------------------------------------------------------
  // Filtres à chips (réutilisable)
  // --------------------------------------------------------------------------

  Widget _buildChipFilter<T>({
    required (Color, Color) colors,
    required String allLabel,
    required List<T> values,
    required T? selected,
    required String Function(T) labelOf,
    required void Function(T?) onChanged,
  }) {
    return SizedBox(
      height: 50,
      child: ListView(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        children: [
          FilterChip(
            label: Text(allLabel),
            selected: selected == null,
            onSelected: (_) => onChanged(null),
            selectedColor: colors.$1.withOpacity(0.2),
            checkmarkColor: colors.$2,
          ),
          const SizedBox(width: 8),
          ...values.map((value) {
            return Padding(
              padding: const EdgeInsets.only(right: 8),
              child: FilterChip(
                label: Text(labelOf(value)),
                selected: selected == value,
                onSelected: (_) => onChanged(value),
                selectedColor: colors.$1.withOpacity(0.2),
                checkmarkColor: colors.$2,
              ),
            );
          }),
        ],
      ),
    );
  }

  // --------------------------------------------------------------------------
  // États vides / erreur
  // --------------------------------------------------------------------------

  Widget _buildEmptyState({required String message, VoidCallback? action}) {
    return ListView(
      children: [
        SizedBox(
          height: 320,
          child: Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.local_hospital_rounded,
                    size: 64, color: AppColors.surface.withOpacity(0.5)),
                const SizedBox(height: 16),
                Text(message,
                    style: TextStyle(color: AppColors.surface.withOpacity(0.7))),
                if (action != null) ...[
                  const SizedBox(height: 24),
                  FilledButton.icon(
                    onPressed: action,
                    icon: const Icon(Icons.add_rounded),
                    label: const Text('Créer'),
                  ),
                ],
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildErrorState(Object error) {
    return ListView(
      children: [
        SizedBox(
          height: 320,
          child: Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(Icons.cloud_off_rounded, size: 48, color: Colors.red),
                const SizedBox(height: 12),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 32),
                  child: Text(
                    _errorText(error),
                    textAlign: TextAlign.center,
                    style: TextStyle(color: Colors.red.shade300),
                  ),
                ),
                const SizedBox(height: 16),
                OutlinedButton.icon(
                  onPressed: () => setState(() {}),
                  icon: const Icon(Icons.refresh_rounded),
                  label: const Text('Réessayer'),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  // --------------------------------------------------------------------------
  // Feuilles de création (bottom sheets) — plus de poussées vers des routes
  // mortes ; corps envoyés = `createBody` du modèle (contrat serveur exact).
  // --------------------------------------------------------------------------

  Future<void> _showFormSheet({required Widget Function(BuildContext) builder}) {
    return showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.cardDark,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: builder,
    );
  }

  void _showPatientCreateSheet() {
    _showFormSheet(
      builder: (_) => _PatientCreateSheet(
        onCreated: () {
          ref.invalidate(_patientsProvider);
          _snack(context, 'Patient créé.');
        },
      ),
    );
  }

  void _showConsultationCreateSheet() {
    final practitionerId = AuthState().userId;
    if (practitionerId == null) {
      _snack(context, 'Session invalide : identifiant utilisateur manquant.',
          error: true);
      return;
    }
    _showFormSheet(
      builder: (_) => _ConsultationCreateSheet(
        practitionerId: practitionerId,
        onCreated: () {
          ref.invalidate(_consultationsProvider);
          _snack(context, 'Consultation créée.');
        },
      ),
    );
  }

  void _showPharmacyItemCreateSheet() {
    _showFormSheet(
      builder: (_) => _PharmacyItemCreateSheet(
        onCreated: () {
          ref.invalidate(_stockProvider);
          _snack(context, 'Article créé. Les lots se pilotent via le back-office.');
        },
      ),
    );
  }

  void _showCampaignCreateSheet() {
    _showFormSheet(
      builder: (_) => _CampaignCreateSheet(
        onCreated: () {
          ref.invalidate(_campaignsProvider);
          _snack(context, 'Campagne créée.');
        },
      ),
    );
  }

  // --------------------------------------------------------------------------
  // Feuilles de détail (tap sur carte) — données déjà chargées, aucune route
  // morte.
  // --------------------------------------------------------------------------

  void _showPatientDetails(PatientRecord patient) {
    _showDetailsSheet('Patient', [
      ('Nom', patient.personName),
      ('Famille', patient.familyName),
      ('Groupe sanguin', patient.groupeSanguin),
      ('Allergies', patient.allergies),
      ('Antécédents', patient.antecedents),
      ('Médecin traitant', patient.medecinTraitant),
      ('Tél. médecin', patient.medecinTel),
      ('Poids', patient.poidsKg != null ? '${patient.poidsKg} kg' : null),
      ('Taille', patient.tailleCm != null ? '${patient.tailleCm} cm' : null),
      ('Tension', patient.tensionArterielle),
      ('Glycémie', patient.glycemie),
      ('N° assurance', patient.numeroAssurance),
      ('Confidentialité', patient.confidentialityLevel.displayName),
      ('Mesures', patient.mesures),
    ]);
  }

  void _showConsultationDetails(MedicalConsultation consultation) {
    _showDetailsSheet('Consultation', [
      ('Patient', consultation.patientName),
      ('Praticien', consultation.practitionerName),
      ('Date', consultation.consultationDate),
      ('Type', consultation.typeConsultation),
      ('Motif', consultation.motif),
      ('Constantes', consultation.constantes),
      ('Diagnostic', consultation.diagnostic),
      ('Traitement', consultation.traitement),
      ('Résultat', consultation.resultat),
      ('Orientation', consultation.orientation),
      ('Statut', consultation.status.displayName),
    ]);
  }

  void _showStockDetails(PharmacyStock stock) {
    _showDetailsSheet('Lot de pharmacie', [
      ('Article', stock.itemName),
      ('Lot', stock.lotNumber),
      ('Quantité', '${stock.quantite ?? 0}'),
      ('Seuil alerte', '${stock.seuilAlerte ?? 0}'),
      ('Expiration', stock.dateExpiration),
      ('Prix unitaire', stock.prixUnitaire?.toString()),
      ('Statut', stock.status.displayName),
      ('Expiré (serveur)', stock.isExpired ? 'oui' : 'non'),
    ]);
  }

  void _showCampaignDetails(HealthCampaign campaign) {
    _showDetailsSheet('Campagne', [
      ('Titre', campaign.title),
      ('Type', campaign.campaignType.displayName),
      ('Description', campaign.description),
      ('Début', campaign.startDate),
      ('Fin', campaign.endDate),
      ('Lieu', campaign.lieu),
      ('Responsable', campaign.responsibleName),
      ('Objectif', campaign.objectif),
      ('Cibles', campaign.cibles),
      ('Statut', campaign.status.displayName),
      ('Inscrits', '${campaign.participantsCount}'),
    ]);
  }

  void _showDetailsSheet(String title, List<(String, String?)> fields) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.cardDark,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (sheetContext) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title,
                  style: Theme.of(sheetContext)
                      .textTheme
                      .titleLarge
                      ?.copyWith(fontWeight: FontWeight.bold)),
              const SizedBox(height: 12),
              Flexible(
                child: SingleChildScrollView(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      for (final (label, value) in fields)
                        if (value != null && value.isNotEmpty)
                          Padding(
                            padding: const EdgeInsets.only(bottom: 8),
                            child: RichText(
                              text: TextSpan(
                                style: const TextStyle(fontSize: 14),
                                children: [
                                  TextSpan(
                                    text: '$label : ',
                                    style: TextStyle(
                                        fontWeight: FontWeight.w600,
                                        color: AppColors.surface
                                            .withOpacity(0.9)),
                                  ),
                                  TextSpan(
                                    text: value,
                                    style: TextStyle(
                                        color:
                                            AppColors.surface.withOpacity(0.75)),
                                  ),
                                ],
                              ),
                            ),
                          ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _MiniEmpty extends StatelessWidget {
  const _MiniEmpty(this.message);
  final String message;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Text(message,
          style: TextStyle(color: AppColors.surface.withOpacity(0.6))),
    );
  }
}

// ---------------------------------------------------------------------------
// Feuilles de formulaire — chaque corps est construit par le `createBody`
// du modèle (formes exactes vérifiées contre le serveur V240).
// ---------------------------------------------------------------------------

class _FormCommons {
  _FormCommons();

  static InputDecoration input(BuildContext context, String label,
          {String? hint, String? error}) {
    return InputDecoration(
      labelText: label,
      hintText: hint,
      errorText: error,
      labelStyle: TextStyle(color: AppColors.surface.withOpacity(0.8)),
      hintStyle: TextStyle(color: AppColors.surface.withOpacity(0.4)),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: BorderSide(color: AppColors.surface.withOpacity(0.3)),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: Colors.red),
      ),
      errorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: Colors.red),
      ),
      focusedErrorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: Colors.red),
      ),
    );
  }
}

/// Création de patient — le serveur exige l'association `person: {id}` ;
/// aucun endpoint de recherche de personnes n'expose de picker mobile
/// (contrat vérifié) : le champ UUID est donc direct, validé côté client.
class _PatientCreateSheet extends ConsumerStatefulWidget {
  const _PatientCreateSheet({required this.onCreated});
  final VoidCallback onCreated;

  @override
  ConsumerState<_PatientCreateSheet> createState() => _PatientCreateSheetState();
}

class _PatientCreateSheetState extends ConsumerState<_PatientCreateSheet> {
  final _formKey = GlobalKey<FormState>();
  final _personIdController = TextEditingController();
  final _groupeSanguinController = TextEditingController();
  final _allergiesController = TextEditingController();
  final _antecedentsController = TextEditingController();
  final _medecinController = TextEditingController();
  bool _submitting = false;

  @override
  void dispose() {
    _personIdController.dispose();
    _groupeSanguinController.dispose();
    _allergiesController.dispose();
    _antecedentsController.dispose();
    _medecinController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _submitting = true);
    try {
      final service = ref.read(health_service.healthServiceProvider);
      await service.createPatient(PatientRecord.createBody(
        personId: _personIdController.text.trim(),
        groupeSanguin: _emptyToNull(_groupeSanguinController.text),
        allergies: _emptyToNull(_allergiesController.text),
        antecedents: _emptyToNull(_antecedentsController.text),
        medecinTraitant: _emptyToNull(_medecinController.text),
      ));
      if (!mounted) return;
      Navigator.of(context).pop();
      widget.onCreated();
    } catch (e) {
      if (!mounted) return;
      setState(() => _submitting = false);
      _snack(context, _errorText(e), error: true);
    }
  }

  static String? _emptyToNull(String v) => v.trim().isEmpty ? null : v.trim();

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: EdgeInsets.only(
          left: 20,
          right: 20,
          top: 20,
          bottom: MediaQuery.viewInsetsOf(context).bottom + 20,
        ),
        child: Form(
          key: _formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text('Nouveau patient',
                  style: Theme.of(context)
                      .textTheme
                      .titleLarge
                      ?.copyWith(fontWeight: FontWeight.bold)),
              const SizedBox(height: 16),
              TextFormField(
                controller: _personIdController,
                decoration: _FormCommons.input(
                  context,
                  'UUID de la personne *',
                  hint: 'ex. 3f2b6c1e-… (peoples/id)',
                ),
                validator: (v) {
                  final s = v?.trim() ?? '';
                  if (s.isEmpty) return 'Identifiant personne requis';
                  if (!_uuidPattern.hasMatch(s)) return 'UUID invalide';
                  return null;
                },
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _groupeSanguinController,
                decoration:
                    _FormCommons.input(context, 'Groupe sanguin (optionnel)'),
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _allergiesController,
                decoration: _FormCommons.input(context, 'Allergies (optionnel)'),
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _antecedentsController,
                decoration:
                    _FormCommons.input(context, 'Antécédents (optionnel)'),
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _medecinController,
                decoration:
                    _FormCommons.input(context, 'Médecin traitant (optionnel)'),
              ),
              const SizedBox(height: 20),
              FilledButton.icon(
                onPressed: _submitting ? null : _submit,
                icon: _submitting
                    ? const SizedBox(
                        width: 16,
                        height: 16,
                        child: CircularProgressIndicator(strokeWidth: 2))
                    : const Icon(Icons.check_rounded),
                label: Text(_submitting ? 'Enregistrement…' : 'Créer le patient'),
                style: FilledButton.styleFrom(backgroundColor: Colors.red),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ConsultationCreateSheet extends ConsumerStatefulWidget {
  const _ConsultationCreateSheet({
    required this.practitionerId,
    required this.onCreated,
  });
  final String practitionerId;
  final VoidCallback onCreated;

  @override
  ConsumerState<_ConsultationCreateSheet> createState() =>
      _ConsultationCreateSheetState();
}

class _ConsultationCreateSheetState
    extends ConsumerState<_ConsultationCreateSheet> {
  static const _types = ['TRIAGE', 'CONSULTATION', 'SUIVI'];

  final _formKey = GlobalKey<FormState>();
  final _motifController = TextEditingController();
  final _diagnosticController = TextEditingController();
  PatientRecord? _patient;
  DateTime? _date;
  String _type = 'CONSULTATION';
  bool _submitting = false;

  @override
  void dispose() {
    _motifController.dispose();
    _diagnosticController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    final patient = _patient;
    if (patient == null) {
      _snack(context, 'Sélectionnez un patient.', error: true);
      return;
    }
    setState(() => _submitting = true);
    try {
      final service = ref.read(health_service.healthServiceProvider);
      await service.createConsultation(MedicalConsultation.createBody(
        patientId: patient.id,
        practitionerId: widget.practitionerId,
        consultationDate: _isoDate(_date ?? DateTime.now()),
        typeConsultation: _type,
        motif: _motifController.text.trim().isEmpty
            ? null
            : _motifController.text.trim(),
        diagnostic: _diagnosticController.text.trim().isEmpty
            ? null
            : _diagnosticController.text.trim(),
      ));
      if (!mounted) return;
      Navigator.of(context).pop();
      widget.onCreated();
    } catch (e) {
      if (!mounted) return;
      setState(() => _submitting = false);
      _snack(context, _errorText(e), error: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final patientsAsync = ref.watch(_patientsProvider);
    return SafeArea(
      child: Padding(
        padding: EdgeInsets.only(
          left: 20,
          right: 20,
          top: 20,
          bottom: MediaQuery.viewInsetsOf(context).bottom + 20,
        ),
        child: Form(
          key: _formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text('Nouvelle consultation',
                  style: Theme.of(context)
                      .textTheme
                      .titleLarge
                      ?.copyWith(fontWeight: FontWeight.bold)),
              const SizedBox(height: 16),
              patientsAsync.maybeWhen(
                data: (patients) => DropdownButtonFormField<PatientRecord>(
                  initialValue: _patient,
                  decoration: _FormCommons.input(context, 'Patient *'),
                  items: [
                    for (final p in patients)
                      DropdownMenuItem(
                        value: p,
                        child: Text(p.personName ?? p.id,
                            overflow: TextOverflow.ellipsis),
                      ),
                  ],
                  onChanged: (p) => setState(() => _patient = p),
                  validator: (_) =>
                      _patient == null ? 'Patient requis' : null,
                ),
                orElse: () => const SizedBox(
                    height: 48,
                    child: Center(
                        child: CircularProgressIndicator(strokeWidth: 2))),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: () async {
                        final picked = await _pickDate(context, initial: _date);
                        if (picked != null) setState(() => _date = picked);
                      },
                      icon: const Icon(Icons.event_rounded, size: 16),
                      label: Text(_date != null ? _isoDate(_date!) : 'Date *'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: DropdownButtonFormField<String>(
                      initialValue: _type,
                      decoration: _FormCommons.input(context, 'Type'),
                      items: _types
                          .map((t) => DropdownMenuItem(value: t, child: Text(t)))
                          .toList(),
                      onChanged: (t) =>
                          setState(() => _type = t ?? 'CONSULTATION'),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _motifController,
                decoration: _FormCommons.input(context, 'Motif (optionnel)'),
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _diagnosticController,
                decoration:
                    _FormCommons.input(context, 'Diagnostic (optionnel)'),
              ),
              const SizedBox(height: 20),
              FilledButton.icon(
                onPressed: _submitting ? null : _submit,
                icon: _submitting
                    ? const SizedBox(
                        width: 16,
                        height: 16,
                        child: CircularProgressIndicator(strokeWidth: 2))
                    : const Icon(Icons.check_rounded),
                label: Text(
                    _submitting ? 'Enregistrement…' : 'Créer la consultation'),
                style: FilledButton.styleFrom(backgroundColor: Colors.blue),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _PharmacyItemCreateSheet extends ConsumerStatefulWidget {
  const _PharmacyItemCreateSheet({required this.onCreated});
  final VoidCallback onCreated;

  @override
  ConsumerState<_PharmacyItemCreateSheet> createState() =>
      _PharmacyItemCreateSheetState();
}

class _PharmacyItemCreateSheetState
    extends ConsumerState<_PharmacyItemCreateSheet> {
  final _formKey = GlobalKey<FormState>();
  final _nomController = TextEditingController();
  final _codeController = TextEditingController();
  final _categorieController = TextEditingController();
  final _uniteController = TextEditingController();
  final _fournisseurController = TextEditingController();
  final _prixController = TextEditingController();
  bool _submitting = false;

  @override
  void dispose() {
    _nomController.dispose();
    _codeController.dispose();
    _categorieController.dispose();
    _uniteController.dispose();
    _fournisseurController.dispose();
    _prixController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _submitting = true);
    try {
      final service = ref.read(health_service.healthServiceProvider);
      await service.createPharmacyItem(PharmacyItem.createBody(
        nom: _nomController.text.trim(),
        code: _optional(_codeController.text),
        categorie: _optional(_categorieController.text),
        unite: _optional(_uniteController.text),
        fournisseur: _optional(_fournisseurController.text),
        prixAchat: _optionalDouble(_prixController.text),
      ));
      if (!mounted) return;
      Navigator.of(context).pop();
      widget.onCreated();
    } catch (e) {
      if (!mounted) return;
      setState(() => _submitting = false);
      _snack(context, _errorText(e), error: true);
    }
  }

  static String? _optional(String v) =>
      v.trim().isEmpty ? null : v.trim();

  static double? _optionalDouble(String v) =>
      v.trim().isEmpty ? null : double.tryParse(v.trim().replaceAll(',', '.'));

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: EdgeInsets.only(
          left: 20,
          right: 20,
          top: 20,
          bottom: MediaQuery.viewInsetsOf(context).bottom + 20,
        ),
        child: Form(
          key: _formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text('Nouvel article pharmaceutique',
                  style: Theme.of(context)
                      .textTheme
                      .titleLarge
                      ?.copyWith(fontWeight: FontWeight.bold)),
              const SizedBox(height: 16),
              TextFormField(
                controller: _nomController,
                decoration: _FormCommons.input(context, 'Nom *'),
                validator: (v) =>
                    (v == null || v.trim().isEmpty) ? 'Nom requis' : null,
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: TextFormField(
                      controller: _codeController,
                      decoration: _FormCommons.input(context, 'Code'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: TextFormField(
                      controller: _categorieController,
                      decoration: _FormCommons.input(context, 'Catégorie'),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: TextFormField(
                      controller: _uniteController,
                      decoration: _FormCommons.input(context, 'Unité'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: TextFormField(
                      controller: _prixController,
                      keyboardType:
                          const TextInputType.numberWithOptions(decimal: true),
                      decoration: _FormCommons.input(context, 'Prix d\'achat'),
                      validator: (v) {
                        if (v == null || v.trim().isEmpty) return null;
                        return _optionalDouble(v) == null
                            ? 'Nombre attendu'
                            : null;
                      },
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _fournisseurController,
                decoration: _FormCommons.input(context, 'Fournisseur'),
              ),
              const SizedBox(height: 20),
              FilledButton.icon(
                onPressed: _submitting ? null : _submit,
                icon: _submitting
                    ? const SizedBox(
                        width: 16,
                        height: 16,
                        child: CircularProgressIndicator(strokeWidth: 2))
                    : const Icon(Icons.check_rounded),
                label: Text(_submitting ? 'Enregistrement…' : 'Créer l\'article'),
                style: FilledButton.styleFrom(backgroundColor: Colors.red),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _CampaignCreateSheet extends ConsumerStatefulWidget {
  const _CampaignCreateSheet({required this.onCreated});
  final VoidCallback onCreated;

  @override
  ConsumerState<_CampaignCreateSheet> createState() =>
      _CampaignCreateSheetState();
}

class _CampaignCreateSheetState extends ConsumerState<_CampaignCreateSheet> {
  final _formKey = GlobalKey<FormState>();
  final _titleController = TextEditingController();
  final _lieuController = TextEditingController();
  final _descriptionController = TextEditingController();
  CampaignType _type = CampaignType.vaccination;
  DateTime? _startDate;
  DateTime? _endDate;
  bool _submitting = false;

  @override
  void dispose() {
    _titleController.dispose();
    _lieuController.dispose();
    _descriptionController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    // nullable=false côté serveur (HealthCampaign.startDate) : validation
    // hors Form (le champ est un bouton date, pas un TextFormField).
    if (_startDate == null) {
      _snack(context, 'Date de début requise.', error: true);
      return;
    }
    setState(() => _submitting = true);
    try {
      final service = ref.read(health_service.healthServiceProvider);
      // `responsible` N'EST PAS envoyé : le serveur le force à l'acteur
      // courant (createCampaign V240 vérifié).
      await service.createCampaign(HealthCampaign.createBody(
        title: _titleController.text.trim(),
        startDate: _isoDate(_startDate!),
        lieu: _lieuController.text.trim(),
        campaignType: _type,
        description: _descriptionController.text.trim().isEmpty
            ? null
            : _descriptionController.text.trim(),
        endDate: _endDate != null ? _isoDate(_endDate!) : null,
      ));
      if (!mounted) return;
      Navigator.of(context).pop();
      widget.onCreated();
    } catch (e) {
      if (!mounted) return;
      setState(() => _submitting = false);
      _snack(context, _errorText(e), error: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: EdgeInsets.only(
          left: 20,
          right: 20,
          top: 20,
          bottom: MediaQuery.viewInsetsOf(context).bottom + 20,
        ),
        child: Form(
          key: _formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text('Nouvelle campagne',
                  style: Theme.of(context)
                      .textTheme
                      .titleLarge
                      ?.copyWith(fontWeight: FontWeight.bold)),
              const SizedBox(height: 16),
              TextFormField(
                controller: _titleController,
                decoration: _FormCommons.input(context, 'Titre *'),
                validator: (v) =>
                    (v == null || v.trim().isEmpty) ? 'Titre requis' : null,
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _lieuController,
                decoration: _FormCommons.input(context, 'Lieu *'),
                validator: (v) =>
                    (v == null || v.trim().isEmpty) ? 'Lieu requis' : null,
              ),
              const SizedBox(height: 12),
              DropdownButtonFormField<CampaignType>(
                initialValue: _type,
                decoration: _FormCommons.input(context, 'Type'),
                items: CampaignType.values
                    .map((t) =>
                        DropdownMenuItem(value: t, child: Text(t.displayName)))
                    .toList(),
                onChanged: (t) => setState(() => _type = t ?? _type),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: () async {
                        final picked =
                            await _pickDate(context, initial: _startDate);
                        if (picked != null) setState(() => _startDate = picked);
                      },
                      icon: const Icon(Icons.event_rounded, size: 16),
                      label: Text(_startDate != null
                          ? _isoDate(_startDate!)
                          : 'Début *'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: () async {
                        final picked =
                            await _pickDate(context, initial: _endDate);
                        if (picked != null) setState(() => _endDate = picked);
                      },
                      icon: const Icon(Icons.date_range_rounded, size: 16),
                      label: Text(_endDate != null ? _isoDate(_endDate!) : 'Fin'),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _descriptionController,
                maxLines: 2,
                decoration:
                    _FormCommons.input(context, 'Description (optionnel)'),
              ),
              const SizedBox(height: 20),
              FilledButton.icon(
                onPressed: _submitting ? null : _submit,
                icon: _submitting
                    ? const SizedBox(
                        width: 16,
                        height: 16,
                        child: CircularProgressIndicator(strokeWidth: 2))
                    : const Icon(Icons.check_rounded),
                label: Text(_submitting ? 'Enregistrement…' : 'Créer la campagne'),
                style: FilledButton.styleFrom(backgroundColor: Colors.green),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
