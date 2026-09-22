import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:go_router/go_router.dart';

import 'package:discipolat_mobile/features/health/models/health_model.dart';
import 'package:discipolat_mobile/features/health/services/health_service.dart'
    as health_service;
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';
import 'package:discipolat_mobile/features/health/widgets/patient_card.dart';
import 'package:discipolat_mobile/features/health/widgets/consultation_card.dart';
import 'package:discipolat_mobile/features/health/widgets/stock_card.dart';
import 'package:discipolat_mobile/features/health/widgets/campaign_card.dart';

class HealthScreen extends ConsumerStatefulWidget {
  const HealthScreen({super.key});

  @override
  ConsumerState<HealthScreen> createState() => _HealthScreenState();
}

class _HealthScreenState extends ConsumerState<HealthScreen> with SingleTickerProviderStateMixin {
  late TabController _tabController;
  PatientStatus? _filterPatientStatus;
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
    final patientsAsync = ref.watch(_patientsProvider(_filterPatientStatus));
    final consultationsAsync = ref.watch(_consultationsProvider(_filterConsultationStatus));
    final stockAsync = ref.watch(_stockProvider);
    final campaignsAsync = ref.watch(_campaignsProvider(_filterCampaignStatus));
    final statsAsync = ref.watch(_statsProvider);

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
            onPressed: () {
              switch (_tabController.index) {
                case 0:
                  context.push('/health/patients/create');
                  break;
                case 1:
                  context.push('/health/consultations/create');
                  break;
                case 2:
                  context.push('/health/pharmacy/add');
                  break;
                case 3:
                  context.push('/health/campaigns/create');
                  break;
                case 4:
                  // TODO: Add kit/duty create
                  break;
              }
            },
            tooltip: 'Nouveau',
          ),
        ],
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          // Patients tab
          _buildPatientsTab(),
          // Consultations tab
          _buildConsultationsTab(),
          // Pharmacy tab
          _buildPharmacyTab(),
          // Campaigns tab
          _buildCampaignsTab(),
          // Kits & Duties tab
          _buildKitsDutiesTab(),
        ],
      ),
    );
  }

  Widget _buildPatientsTab() {
    final patientsAsync = ref.watch(_patientsProvider(_filterPatientStatus));

    return Column(
      children: [
        _buildPatientFilters(),
        Expanded(
          child: RefreshIndicator(
            onRefresh: () => ref.refresh(_patientsProvider(_filterPatientStatus).future),
            child: patientsAsync.when(
              data: (patients) {
                if (patients.isEmpty) {
                  return _buildEmptyState(
                    message: 'Aucun patient',
                    action: () => context.push('/health/patients/create'),
                  );
                }
                return RefreshIndicator(
                  onRefresh: () => ref.refresh(_patientsProvider(_filterPatientStatus).future),
                  child: ListView.builder(
                    padding: const EdgeInsets.all(16),
                    itemCount: patients.length,
                    itemBuilder: (context, index) {
                      final patient = patients[index];
                      return PatientCard(
                        patient: patient,
                        onTap: () => context.push('/health/patients/${patient.id}'),
                      );
                    },
                  ),
                );
              },
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (error, _) => Center(child: Text('Erreur: $error')),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildConsultationsTab() {
    final consultationsAsync = ref.watch(_consultationsProvider(_filterConsultationStatus));

    return Column(
      children: [
        _buildConsultationFilters(),
        Expanded(
          child: RefreshIndicator(
            onRefresh: () => ref.refresh(_consultationsProvider(_filterConsultationStatus).future),
            child: consultationsAsync.when(
              data: (consultations) {
                if (consultations.isEmpty) {
                  return _buildEmptyState(
                    message: 'Aucune consultation',
                    action: () => context.push('/health/consultations/create'),
                  );
                }
                return ListView.builder(
                  padding: const EdgeInsets.all(16),
                  itemCount: consultations.length,
                  itemBuilder: (context, index) {
                    final consultation = consultations[index];
                    return ConsultationCard(
                      consultation: consultation,
                      onTap: () => context.push('/health/consultations/${consultation.id}'),
                    );
                  },
                );
              },
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (error, _) => Center(child: Text('Erreur: $error')),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPharmacyTab() {
    final stockAsync = ref.watch(_stockProvider);

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Expanded(
                child: FilledButton.icon(
                  onPressed: () => context.push('/health/pharmacy/add'),
                  icon: const Icon(Icons.add_rounded),
                  label: const Text('Ajouter médicament'),
                  style: FilledButton.styleFrom(backgroundColor: Colors.red),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () => ref.refresh(_stockProvider),
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
                    message: 'Aucun médicament en stock',
                    action: () => context.push('/health/pharmacy/add'),
                  );
                }
                return ListView.builder(
                  padding: const EdgeInsets.all(16),
                  itemCount: stocks.length,
                  itemBuilder: (context, index) {
                    final stock = stocks[index];
                    return StockCard(
                      stock: stock,
                      onTap: () => context.push('/health/pharmacy/${stock.id}'),
                    );
                  },
                );
              },
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (error, _) => Center(child: Text('Erreur: $error')),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCampaignsTab() {
    final campaignsAsync = ref.watch(_campaignsProvider(_filterCampaignStatus));

    return Column(
      children: [
        _buildCampaignFilters(),
        Expanded(
          child: RefreshIndicator(
            onRefresh: () => ref.refresh(_campaignsProvider(_filterCampaignStatus).future),
            child: campaignsAsync.when(
              data: (campaigns) {
                if (campaigns.isEmpty) {
                  return _buildEmptyState(
                    message: 'Aucune campagne',
                    action: () => context.push('/health/campaigns/create'),
                  );
                }
                return ListView.builder(
                  padding: const EdgeInsets.all(16),
                  itemCount: campaigns.length,
                  itemBuilder: (context, index) {
                    final campaign = campaigns[index];
                    return CampaignCard(
                      campaign: campaign,
                      onTap: () => context.push('/health/campaigns/${campaign.id}'),
                    );
                  },
                );
              },
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (error, _) => Center(child: Text('Erreur: $error')),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildKitsDutiesTab() {
    // TODO: Implement kits and duties views
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.medical_services_rounded, size: 64, color: AppColors.surface.withOpacity(0.5)),
          const SizedBox(height: 16),
          Text('Kits médicaux & Gardes', style: TextStyle(color: AppColors.surface.withOpacity(0.7))),
          const SizedBox(height: 8),
          Text('À implémenter', style: TextStyle(color: AppColors.surface.withOpacity(0.5))),
        ],
      ),
    );
  }

  Widget _buildPatientFilters() {
    return Container(
      height: 50,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: ListView(
        scrollDirection: Axis.horizontal,
        children: [
          FilterChip(
            label: const Text('Tous'),
            selected: _filterPatientStatus == null,
            onSelected: (_) => setState(() => _filterPatientStatus = null),
            selectedColor: Colors.red.withOpacity(0.2),
            checkmarkColor: Colors.red,
          ),
          const SizedBox(width: 8),
          ...PatientStatus.values.map((status) {
            return Padding(
              padding: const EdgeInsets.only(right: 8),
              child: FilterChip(
                label: Text(status.displayName),
                selected: _filterPatientStatus == status,
                onSelected: (_) => setState(() => _filterPatientStatus = status),
                selectedColor: Colors.red.withOpacity(0.2),
                checkmarkColor: Colors.red,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildConsultationFilters() {
    return Container(
      height: 50,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: ListView(
        scrollDirection: Axis.horizontal,
        children: [
          FilterChip(
            label: const Text('Toutes'),
            selected: _filterConsultationStatus == null,
            onSelected: (_) => setState(() => _filterConsultationStatus = null),
            selectedColor: Colors.blue.withOpacity(0.2),
            checkmarkColor: Colors.blue,
          ),
          const SizedBox(width: 8),
          ...ConsultationStatus.values.map((status) {
            return Padding(
              padding: const EdgeInsets.only(right: 8),
              child: FilterChip(
                label: Text(status.displayName),
                selected: _filterConsultationStatus == status,
                onSelected: (_) => setState(() => _filterConsultationStatus = status),
                selectedColor: Colors.blue.withOpacity(0.2),
                checkmarkColor: Colors.blue,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCampaignFilters() {
    return Container(
      height: 50,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: ListView(
        scrollDirection: Axis.horizontal,
        children: [
          FilterChip(
            label: const Text('Toutes'),
            selected: _filterCampaignStatus == null,
            onSelected: (_) => setState(() => _filterCampaignStatus = null),
            selectedColor: Colors.green.withOpacity(0.2),
            checkmarkColor: Colors.green,
          ),
          const SizedBox(width: 8),
          ...CampaignStatus.values.map((status) {
            return Padding(
              padding: const EdgeInsets.only(right: 8),
              child: FilterChip(
                label: Text(status.displayName),
                selected: _filterCampaignStatus == status,
                onSelected: (_) => setState(() => _filterCampaignStatus = status),
                selectedColor: Colors.green.withOpacity(0.2),
                checkmarkColor: Colors.green,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildEmptyState({required String message, VoidCallback? action}) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.local_hospital_rounded, size: 64, color: AppColors.surface.withOpacity(0.5)),
          const SizedBox(height: 16),
          Text(message, style: TextStyle(color: AppColors.surface.withOpacity(0.7))),
          if (action != null) ...[
            const SizedBox(height: 24),
            FilledButton.icon(onPressed: action, icon: const Icon(Icons.add_rounded), label: const Text('Créer')),
          ],
        ],
      ),
    );
  }
}

// Providers
final _patientsProvider = FutureProvider.family<List<Patient>, PatientStatus?>((ref, status) async {
  final service = ref.watch(health_service.healthServiceProvider);
  return service.getPatients(status: status);
});

final _consultationsProvider = FutureProvider.family<List<Consultation>, ConsultationStatus?>((ref, status) async {
  final service = ref.watch(health_service.healthServiceProvider);
  return service.getConsultations(status: status);
});

final _stockProvider = FutureProvider<List<PharmacyStock>>((ref) async {
  final service = ref.watch(health_service.healthServiceProvider);
  return service.getPharmacyStock();
});

final _campaignsProvider = FutureProvider.family<List<HealthCampaign>, CampaignStatus?>((ref, status) async {
  final service = ref.watch(health_service.healthServiceProvider);
  return service.getHealthCampaigns(status: status);
});