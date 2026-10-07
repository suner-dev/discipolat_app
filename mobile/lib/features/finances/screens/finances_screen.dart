import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:go_router/go_router.dart';

import 'package:discipolat_mobile/features/finances/models/finance_model.dart';
import 'package:discipolat_mobile/features/finances/services/finances_service.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';
import 'package:discipolat_mobile/features/finances/widgets/transaction_card.dart';
import 'package:discipolat_mobile/features/finances/widgets/finance_filter_chips.dart';

/// Écran FINANCES — Branché sur `FinancesService`, contrat `FinanceController`
/// (identifiants UUID, vues serveur uniquement : pas de statut de transaction,
/// pas de type de compte, pas de solde initial — ces champs n'existent pas).
class FinancesScreen extends ConsumerStatefulWidget {
  const FinancesScreen({super.key});

  @override
  ConsumerState<FinancesScreen> createState() => _FinancesScreenState();
}

class _FinancesScreenState extends ConsumerState<FinancesScreen> with SingleTickerProviderStateMixin {
  TransactionType? _filterType;
  String? _filterCategory;
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 4, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final transactionsAsync = ref.watch(_transactionsProvider((_filterType, _filterCategory)));
    final accountsAsync = ref.watch(_accountsProvider);
    final budgetsAsync = ref.watch(_budgetsProvider);
    final tontinesAsync = ref.watch(_tontinesProvider);

    // Catégories proposées aux filtres = catégories réellement présentes dans
    // les transactions chargées (le serveur stocke une catégorie texte libre).
    final availableCategories = transactionsAsync.maybeWhen(
      data: (txs) => (txs.map((t) => t.categorie).whereType<String>().toSet().toList())..sort(),
      orElse: () => <String>[],
    );

    return Scaffold(
      backgroundColor: AppColors.surfaceDark,
      appBar: AppBar(
        title: const Text('Finances & Comptabilité'),
        backgroundColor: AppColors.cardDark,
        elevation: 0,
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: AppColors.primary,
          labelColor: AppColors.primary,
          unselectedLabelColor: AppColors.surface.withOpacity(0.7),
          tabs: const [
            Tab(text: 'Transactions'),
            Tab(text: 'Comptes'),
            Tab(text: 'Budgets'),
            Tab(text: 'Tontines'),
          ],
        ),
        actions: [
          PopupMenuButton<TransactionType?>(
            icon: const Icon(Icons.filter_list_rounded),
            onSelected: (value) => setState(() => _filterType = value),
            itemBuilder: (context) => [
              const PopupMenuItem(value: null, child: Text('Tous types')),
              ...TransactionType.values.map((t) => PopupMenuItem(value: t, child: Text(t.label))),
            ],
          ),
          IconButton(
            icon: const Icon(Icons.add_rounded),
            onPressed: () => context.push('/finances/transactions/create'),
            tooltip: 'Nouvelle transaction',
          ),
        ],
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          // Transactions tab
          Column(
            children: [
              FinanceFilterChips(
                selectedType: _filterType,
                selectedCategory: _filterCategory,
                availableCategories: availableCategories,
                onTypeChanged: (t) => setState(() => _filterType = t),
                onCategoryChanged: (c) => setState(() => _filterCategory = c),
              ),
              Expanded(
                child: RefreshIndicator(
                  onRefresh: () => ref.refresh(_transactionsProvider((_filterType, _filterCategory)).future),
                  child: transactionsAsync.when(
                    data: (transactions) {
                      if (transactions.isEmpty) {
                        return _buildEmptyState(message: 'Aucune transaction');
                      }
                      return ListView.builder(
                        padding: const EdgeInsets.all(16),
                        itemCount: transactions.length,
                        itemBuilder: (context, index) {
                          final tx = transactions[index];
                          return TransactionCard(
                            transaction: tx,
                            onTap: () => context.push('/finances/transactions/${tx.id}'),
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
          ),
          // Accounts tab
          _buildAccountsTab(accountsAsync),
          // Budgets tab
          _buildBudgetsTab(budgetsAsync),
          // Tontines tab
          _buildTontinesTab(tontinesAsync),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.push('/finances/transactions/create'),
        icon: const Icon(Icons.add_rounded),
        label: const Text('Nouvelle transaction'),
        backgroundColor: AppColors.primary,
      ),
    );
  }

  Widget _buildAccountsTab(AsyncValue<List<Account>> accountsAsync) {
    return accountsAsync.when(
      data: (accounts) {
        if (accounts.isEmpty) {
          return _buildEmptyState(message: 'Aucun compte', action: () => context.push('/finances/accounts/create'));
        }
        return RefreshIndicator(
          onRefresh: () => ref.refresh(_accountsProvider.future),
          child: ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: accounts.length,
            itemBuilder: (context, index) {
              final account = accounts[index];
              return _buildAccountCard(account);
            },
          ),
        );
      },
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (error, _) => Center(child: Text('Erreur: $error')),
    );
  }

  Widget _buildBudgetsTab(AsyncValue<List<Budget>> budgetsAsync) {
    return budgetsAsync.when(
      data: (budgets) {
        if (budgets.isEmpty) {
          return _buildEmptyState(message: 'Aucun budget', action: () => context.push('/finances/budgets/create'));
        }
        return RefreshIndicator(
          onRefresh: () => ref.refresh(_budgetsProvider.future),
          child: ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: budgets.length,
            itemBuilder: (context, index) {
              final budget = budgets[index];
              return _buildBudgetCard(budget);
            },
          ),
        );
      },
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (error, _) => Center(child: Text('Erreur: $error')),
    );
  }

  Widget _buildTontinesTab(AsyncValue<List<Tontine>> tontinesAsync) {
    return tontinesAsync.when(
      data: (tontines) {
        if (tontines.isEmpty) {
          return _buildEmptyState(message: 'Aucune tontine', action: () => context.push('/finances/tontines/create'));
        }
        return RefreshIndicator(
          onRefresh: () => ref.refresh(_tontinesProvider.future),
          child: ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: tontines.length,
            itemBuilder: (context, index) {
              final tontine = tontines[index];
              return _buildTontineCard(tontine);
            },
          ),
        );
      },
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (error, _) => Center(child: Text('Erreur: $error')),
    );
  }

  String _money(num amount) => NumberFormat('#,##0.00', 'fr_FR').format(amount);

  Widget _buildAccountCard(Account account) {
    final isPositive = account.balance >= 0;
    final devise = account.devise ?? '';

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      color: AppColors.cardDark,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                CircleAvatar(
                  backgroundColor: AppColors.primary.withOpacity(0.2),
                  child: Icon(Icons.account_balance_rounded, color: AppColors.primary),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        account.name ?? 'Compte',
                        style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600),
                      ),
                      if (account.bankName != null || account.accountNumber != null)
                        Text(
                          [account.bankName, account.accountNumber].whereType<String>().join(' · '),
                          style: TextStyle(fontSize: 12, color: AppColors.surface.withOpacity(0.7)),
                        ),
                    ],
                  ),
                ),
                if (!account.isActive)
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: Colors.grey.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: const Text(
                      'INACTIF',
                      style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: Colors.grey),
                    ),
                  ),
              ],
            ),
            const SizedBox(height: 12),
            Text(
              '${_money(account.balance)} $devise',
              style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                fontWeight: FontWeight.bold,
                color: isPositive ? Colors.green : Colors.red,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildBudgetCard(Budget budget) {
    // Consommation et statut calculés côté serveur (OK / ALERTE / DEPASSE).
    final progress = budget.consommationPct / 100.0;
    final statutColor = budget.estDepasse
        ? Colors.red
        : budget.estAlerte
            ? Colors.orange
            : Colors.green;

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      color: AppColors.cardDark,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    budget.categorie ?? 'Budget',
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                if (budget.annee != null)
                  Text(
                    '${budget.annee}',
                    style: TextStyle(fontSize: 12, color: AppColors.surface.withOpacity(0.7)),
                  ),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Prévu: ${_money(budget.montant)}',
                        style: Theme.of(context).textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.w500),
                      ),
                      Text(
                        'Dépensé: ${_money(budget.depenseReelle)}',
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(color: Colors.red),
                      ),
                    ],
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: statutColor.withOpacity(0.2),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Text(
                    budget.statut.isEmpty ? 'OK' : budget.statut,
                    style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: statutColor),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            LinearProgressIndicator(
              value: progress.clamp(0.0, 1.0),
              backgroundColor: AppColors.surfaceDark,
              valueColor: AlwaysStoppedAnimation<Color>(statutColor),
              minHeight: 8,
              borderRadius: BorderRadius.circular(4),
            ),
            const SizedBox(height: 8),
            Text(
              '${budget.consommationPct.toStringAsFixed(1)}%',
              style: TextStyle(
                fontSize: 11,
                fontWeight: FontWeight.w600,
                color: statutColor,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTontineCard(Tontine tontine) {
    final statusColor = tontine.isActive ? Colors.green : Colors.grey;
    final debut = tontine.debut;

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      color: AppColors.cardDark,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: InkWell(
        onTap: () => context.push('/finances/tontines/${tontine.id}'),
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  CircleAvatar(
                    backgroundColor: AppColors.primary.withOpacity(0.2),
                    child: Icon(Icons.group_rounded, color: AppColors.primary),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      tontine.name ?? 'Tontine',
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600),
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: statusColor.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      tontine.isActive ? 'ACTIVE' : 'INACTIVE',
                      style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: statusColor),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              if (tontine.description != null) ...[
                Text(tontine.description!, style: Theme.of(context).textTheme.bodySmall?.copyWith(color: AppColors.surface.withOpacity(0.7))),
                const SizedBox(height: 12),
              ],
              Row(
                children: [
                  _buildInfoChip(Icons.attach_money_rounded, _money(tontine.amountPerTurn), Colors.green),
                  const SizedBox(width: 12),
                  if (tontine.frequency != null)
                    _buildInfoChip(Icons.calendar_today_rounded, tontine.frequency!.label, AppColors.primary),
                ],
              ),
              const SizedBox(height: 12),
              if (debut != null)
                Text(
                  'Début: ${DateFormat('dd/MM/yyyy').format(debut.toLocal())}',
                  style: TextStyle(fontSize: 11, color: AppColors.surface.withOpacity(0.7)),
                ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildInfoChip(IconData icon, String label, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: color.withOpacity(0.3)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 12, color: color),
          const SizedBox(width: 4),
          Text(label, style: TextStyle(fontSize: 10, fontWeight: FontWeight.w500, color: color)),
        ],
      ),
    );
  }

  Widget _buildEmptyState({required String message, VoidCallback? action}) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.account_balance_wallet_rounded, size: 64, color: AppColors.surface.withOpacity(0.5)),
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
final _transactionsProvider = FutureProvider.family<List<Transaction>, (TransactionType?, String?)>((ref, params) async {
  final (type, category) = params;
  final service = ref.watch(financesServiceProvider);
  return service.getTransactions(type: type, categorie: category);
});

final _accountsProvider = FutureProvider<List<Account>>((ref) async {
  final service = ref.watch(financesServiceProvider);
  return service.getAccounts();
});

final _budgetsProvider = FutureProvider<List<Budget>>((ref) async {
  final service = ref.watch(financesServiceProvider);
  return service.getBudgets();
});

final _tontinesProvider = FutureProvider<List<Tontine>>((ref) async {
  final service = ref.watch(financesServiceProvider);
  return service.getTontines();
});
