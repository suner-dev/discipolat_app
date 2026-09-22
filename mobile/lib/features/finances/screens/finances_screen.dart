import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:go_router/go_router.dart';

import 'package:discipolat_mobile/features/finances/models/finance_model.dart';
import 'package:discipolat_mobile/features/finances/services/finances_service.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';
import 'package:discipolat_mobile/features/finances/widgets/transaction_card.dart';
import 'package:discipolat_mobile/features/finances/widgets/finance_filter_chips.dart';

class FinancesScreen extends ConsumerStatefulWidget {
  const FinancesScreen({super.key});

  @override
  ConsumerState<FinancesScreen> createState() => _FinancesScreenState();
}

class _FinancesScreenState extends ConsumerState<FinancesScreen> with SingleTickerProviderStateMixin {
  TransactionType? _filterType;
  TransactionCategory? _filterCategory;
  TransactionStatus? _filterStatus;
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
    final transactionsAsync = ref.watch(_transactionsProvider(_filterType, _filterCategory, _filterStatus));
    final summaryAsync = ref.watch(_summaryProvider);
    final accountsAsync = ref.watch(_accountsProvider);
    final budgetsAsync = ref.watch(_budgetsProvider);
    final tontinesAsync = ref.watch(_tontinesProvider);

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
              ...TransactionType.values.map((t) => PopupMenuItem(value: t, child: Text(t.name))),
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
                selectedStatus: _filterStatus,
                onTypeChanged: (t) => setState(() => _filterType = t),
                onCategoryChanged: (c) => setState(() => _filterCategory = c),
                onStatusChanged: (s) => setState(() => _filterStatus = s),
              ),
              Expanded(
                child: RefreshIndicator(
                  onRefresh: () => ref.refresh(_transactionsProvider(_filterType, _filterCategory, _filterStatus).future),
                  child: transactionsAsync.when(
                    data: (transactions) {
                      if (transactions.isEmpty) {
                        return _buildEmptyState();
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

  Widget _buildAccountCard(Account account) {
    final typeColor = _getAccountTypeColor(account.type);
    final isPositive = account.balance >= 0;

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
                  backgroundColor: typeColor.withOpacity(0.2),
                  child: Icon(_getAccountTypeIcon(account.type), color: typeColor),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        account.name,
                        style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600),
                      ),
                      Text(
                        account.code,
                        style: TextStyle(fontSize: 12, color: AppColors.surface.withOpacity(0.7)),
                      ),
                    ],
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: typeColor.withOpacity(0.2),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Text(
                    account.type.name.toUpperCase(),
                    style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: typeColor),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Text(
              '${NumberFormat.currency(locale: 'fr_FR', symbol: account.currency).format(account.balance.abs())} ${account.currency}',
              style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                fontWeight: FontWeight.bold,
                color: isPositive ? Colors.green : Colors.red,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'Solde initial: ${NumberFormat.currency(locale: 'fr_FR', symbol: account.currency).format(account.initialBalance)} ${account.currency}',
              style: Theme.of(context).textTheme.bodySmall?.copyWith(color: AppColors.surface.withOpacity(0.7)),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildBudgetCard(Budget budget) {
    final progress = budget.allocatedAmount > 0 ? budget.spentAmount / budget.allocatedAmount : 0.0;
    final isOverBudget = budget.spentAmount > budget.allocatedAmount;
    final remaining = budget.allocatedAmount - budget.spentAmount;

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
                    budget.name,
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: budget.isActive ? Colors.green.withOpacity(0.2) : Colors.grey.withOpacity(0.2),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Text(
                    budget.isActive ? 'Actif' : 'Inactif',
                    style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: budget.isActive ? Colors.green : Colors.grey),
                  ),
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
                        'Alloué: ${NumberFormat.currency(locale: 'fr_FR', symbol: budget.currency).format(budget.allocatedAmount)} ${budget.currency}',
                        style: Theme.of(context).textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.w500),
                      ),
                      Text(
                        'Dépensé: ${NumberFormat.currency(locale: 'fr_FR', symbol: budget.currency).format(budget.spentAmount)} ${budget.currency}',
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(color: Colors.red),
                      ),
                    ],
                  ),
                ),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    Text(
                      'Restant: ${NumberFormat.currency(locale: 'fr_FR', symbol: budget.currency).format(remaining.abs())} ${budget.currency}',
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.bold,
                        color: isOverBudget ? Colors.red : Colors.green,
                      ),
                    ),
                    Text(
                      isOverBudget ? 'DÉPASSÉ' : 'Dans les limites',
                      style: TextStyle(fontSize: 10, color: isOverBudget ? Colors.red : Colors.green),
                    ),
                  ],
                ),
              ],
            ),
            const SizedBox(height: 12),
            LinearProgressIndicator(
              value: progress.clamp(0.0, 1.0),
              backgroundColor: AppColors.surfaceDark,
              valueColor: AlwaysStoppedAnimation<Color>(isOverBudget ? Colors.red : AppColors.primary),
              minHeight: 8,
              borderRadius: BorderRadius.circular(4),
            ),
            const SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  '${DateFormat('dd/MM/yyyy').format(budget.startDate)} - ${DateFormat('dd/MM/yyyy').format(budget.endDate)}',
                  style: TextStyle(fontSize: 11, color: AppColors.surface.withOpacity(0.7)),
                ),
                Text(
                  '${(progress * 100).clamp(0, 999).toStringAsFixed(1)}%',
                  style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: isOverBudget ? Colors.red : AppColors.primary),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTontineCard(Tontine tontine) {
    final statusColor = _getTontineStatusColor(tontine.status);

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
                    child: const Icon(Icons.group_rounded, color: AppColors.primary),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          tontine.name,
                          style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600),
                        ),
                        Text(
                          '${tontine.currentMembers}/${tontine.maxMembers} membres',
                          style: TextStyle(fontSize: 12, color: AppColors.surface.withOpacity(0.7)),
                        ),
                      ],
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: statusColor.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      tontine.status.displayName,
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
                  _buildInfoChip(Icons.attach_money_rounded, '${NumberFormat.currency(locale: 'fr_FR', symbol: tontine.currency).format(tontine.contributionAmount)} ${tontine.currency}', Colors.green),
                  const SizedBox(width: 12),
                  _buildInfoChip(Icons.calendar_today_rounded, tontine.frequency.displayName, AppColors.primary),
                  const SizedBox(width: 12),
                  _buildInfoChip(Icons.people_rounded, '${tontine.currentMembers}/${tontine.maxMembers}', Colors.orange),
                ],
              ),
              const SizedBox(height: 12),
              if (tontine.startDate != null)
                Text(
                  'Début: ${DateFormat('dd/MM/yyyy').format(tontine.startDate.toLocal())}',
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

  Color _getAccountTypeColor(AccountType type) {
    switch (type) {
      case AccountType.asset:
        return Colors.blue;
      case AccountType.liability:
        return Colors.red;
      case AccountType.equity:
        return Colors.purple;
      case AccountType.income:
        return Colors.green;
      case AccountType.expense:
        return Colors.orange;
    }
  }

  IconData _getAccountTypeIcon(AccountType type) {
    switch (type) {
      case AccountType.asset:
        return Icons.account_balance_rounded;
      case AccountType.liability:
        return Icons.credit_card_rounded;
      case AccountType.equity:
        return Icons.pie_chart_rounded;
      case AccountType.income:
        return Icons.trending_up_rounded;
      case AccountType.expense:
        return Icons.trending_down_rounded;
    }
  }

  Color _getTontineStatusColor(TontineStatus status) {
    switch (status) {
      case TontineStatus.draft:
        return Colors.grey;
      case TontineStatus.recruiting:
        return Colors.blue;
      case TontineStatus.active:
        return Colors.green;
      case TontineStatus.completed:
        return Colors.purple;
      case TontineStatus.cancelled:
        return Colors.red;
    }
  }
}

// Providers
final _transactionsProvider = FutureProvider.family<List<Transaction>, (TransactionType?, TransactionCategory?, TransactionStatus?)>((ref, params) async {
  final (type, category, status) = params;
  final service = ref.watch(financesServiceProvider);
  return service.getTransactions(type: type, category: category, status: status);
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

final _summaryProvider = FutureProvider<Map<String, dynamic>>((ref) async {
  final service = ref.watch(financesServiceProvider);
  return service.getFinancialSummary();
});