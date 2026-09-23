import 'package:dio/dio.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/data/services/providers.dart';
import 'package:discipolat_mobile/features/finances/models/finance_model.dart';

part 'finances_service.g.dart';

@riverpod
FinancesService financesService(FinancesServiceRef ref) {
  final api = ref.watch(apiServiceProvider);
  return FinancesService(api);
}

class FinancesService {
  final ApiService _api;

  FinancesService(this._api);

  // Transactions
  Future<List<Transaction>> getTransactions({
    int page = 0,
    int size = 20,
    TransactionType? type,
    TransactionCategory? category,
    TransactionStatus? status,
    DateTime? fromDate,
    DateTime? toDate,
    int? accountId,
    int? budgetId,
    String? search,
  }) async {
    try {
      final queryParams = <String, dynamic>{
        'page': page,
        'size': size,
        if (type != null) 'type': type.name,
        if (category != null) 'category': category.name,
        if (status != null) 'status': status.name,
        if (fromDate != null) 'fromDate': fromDate.toIso8601String(),
        if (toDate != null) 'toDate': toDate.toIso8601String(),
        if (accountId != null) 'accountId': accountId,
        if (budgetId != null) 'budgetId': budgetId,
        if (search != null && search.isNotEmpty) 'search': search,
      };
      final response = await _api.get('/finances/transactions', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => Transaction.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des transactions: $e');
    }
  }

  Future<Transaction> getTransaction(int id) async {
    try {
      final response = await _api.get('/finances/transactions/$id');
      return Transaction.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du chargement de la transaction: $e');
    }
  }

  Future<Transaction> createTransaction(Transaction transaction) async {
    try {
      final response = await _api.post('/finances/transactions', data: transaction.toJson());
      return Transaction.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création: $e');
    }
  }

  Future<Transaction> updateTransaction(int id, Transaction transaction) async {
    try {
      final response = await _api.put('/finances/transactions/$id', data: transaction.toJson());
      return Transaction.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la mise à jour: $e');
    }
  }

  Future<void> deleteTransaction(int id) async {
    try {
      await _api.delete('/finances/transactions/$id');
    } catch (e) {
      throw Exception('Erreur lors de la suppression: $e');
    }
  }

  // Accounts
  Future<List<Account>> getAccounts({bool? isActive, AccountType? type}) async {
    try {
      final queryParams = <String, dynamic>{};
      if (isActive != null) queryParams['isActive'] = isActive.toString();
      if (type != null) queryParams['type'] = type.name;
      final response = await _api.get('/finances/accounts', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => Account.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des comptes: $e');
    }
  }

  Future<Account> getAccount(int id) async {
    try {
      final response = await _api.get('/finances/accounts/$id');
      return Account.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du chargement du compte: $e');
    }
  }

  Future<Account> createAccount(Account account) async {
    try {
      final response = await _api.post('/finances/accounts', data: account.toJson());
      return Account.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création: $e');
    }
  }

  // Budgets
  Future<List<Budget>> getBudgets({bool? isActive, int? departmentId}) async {
    try {
      final queryParams = <String, dynamic>{};
      if (isActive != null) queryParams['isActive'] = isActive.toString();
      if (departmentId != null) queryParams['departmentId'] = departmentId;
      final response = await _api.get('/finances/budgets', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => Budget.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des budgets: $e');
    }
  }

  Future<Budget> getBudget(int id) async {
    try {
      final response = await _api.get('/finances/budgets/$id');
      return Budget.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du chargement du budget: $e');
    }
  }

  Future<Budget> createBudget(Budget budget) async {
    try {
      final response = await _api.post('/finances/budgets', data: budget.toJson());
      return Budget.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création: $e');
    }
  }

  // Tontines
  Future<List<Tontine>> getTontines({TontineStatus? status}) async {
    try {
      final queryParams = <String, dynamic>{};
      if (status != null) queryParams['status'] = status.name;
      final response = await _api.get('/finances/tontines', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => Tontine.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des tontines: $e');
    }
  }

  Future<Tontine> getTontine(int id) async {
    try {
      final response = await _api.get('/finances/tontines/$id');
      return Tontine.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du chargement de la tontine: $e');
    }
  }

  Future<Tontine> createTontine(Tontine tontine) async {
    try {
      final response = await _api.post('/finances/tontines', data: tontine.toJson());
      return Tontine.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création: $e');
    }
  }

  Future<List<TontineMember>> getTontineMembers(int tontineId) async {
    try {
      final response = await _api.get('/finances/tontines/$tontineId/members');
      final data = response.data as List;
      return data.map((json) => TontineMember.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des membres: $e');
    }
  }

  Future<TontineMember> addTontineMember(int tontineId, TontineMember member) async {
    try {
      final response = await _api.post('/finances/tontines/$tontineId/members', data: member.toJson());
      return TontineMember.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de l\'ajout: $e');
    }
  }

  Future<List<TontinePayout>> getTontinePayouts(int tontineId) async {
    try {
      final response = await _api.get('/finances/tontines/$tontineId/payouts');
      final data = response.data as List;
      return data.map((json) => TontinePayout.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des versements: $e');
    }
  }

  // Donations
  Future<List<Donation>> getDonations({
    int page = 0,
    int size = 20,
    DonationStatus? status,
    DonationSource? source,
    DateTime? fromDate,
    DateTime? toDate,
    String? search,
  }) async {
    try {
      final queryParams = <String, dynamic>{
        'page': page,
        'size': size,
        if (status != null) 'status': status.name,
        if (source != null) 'source': source.name,
        if (fromDate != null) 'fromDate': fromDate.toIso8601String(),
        if (toDate != null) 'toDate': toDate.toIso8601String(),
        if (search != null && search.isNotEmpty) 'search': search,
      };
      final response = await _api.get('/finances/donations', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => Donation.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des dons: $e');
    }
  }

  Future<Donation> createDonation(Donation donation) async {
    try {
      final response = await _api.post('/finances/donations', data: donation.toJson());
      return Donation.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création: $e');
    }
  }

  // Reports
  Future<Map<String, dynamic>> getFinancialSummary({DateTime? fromDate, DateTime? toDate}) async {
    try {
      final queryParams = <String, dynamic>{};
      if (fromDate != null) queryParams['fromDate'] = fromDate.toIso8601String();
      if (toDate != null) queryParams['toDate'] = toDate.toIso8601String();
      final response = await _api.get('/finances/reports/summary', queryParameters: queryParams);
      return response.data as Map<String, dynamic>;
    } catch (e) {
      throw Exception('Erreur lors du chargement du résumé: $e');
    }
  }

  Future<List<Map<String, dynamic>>> getTransactionsByCategory({DateTime? fromDate, DateTime? toDate}) async {
    try {
      final queryParams = <String, dynamic>{};
      if (fromDate != null) queryParams['fromDate'] = fromDate.toIso8601String();
      if (toDate != null) queryParams['toDate'] = toDate.toIso8601String();
      final response = await _api.get('/finances/reports/by-category', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => json as Map<String, dynamic>).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement du rapport par catégorie: $e');
    }
  }

  Future<List<Map<String, dynamic>>> getCashFlow({DateTime? fromDate, DateTime? toDate}) async {
    try {
      final queryParams = <String, dynamic>{};
      if (fromDate != null) queryParams['fromDate'] = fromDate.toIso8601String();
      if (toDate != null) queryParams['toDate'] = toDate.toIso8601String();
      final response = await _api.get('/finances/reports/cash-flow', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => json as Map<String, dynamic>).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement du flux de trésorerie: $e');
    }
  }

  // Reconciliation
  Future<void> reconcileTransaction(int id) async {
    try {
      await _api.post('/finances/transactions/$id/reconcile');
    } catch (e) {
      throw Exception('Erreur lors de la réconciliation: $e');
    }
  }

  Future<List<Transaction>> getUnreconciledTransactions() async {
    try {
      final response = await _api.get('/finances/transactions/unreconciled');
      final data = response.data as List;
      return data.map((json) => Transaction.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement: $e');
    }
  }
}