import 'package:riverpod_annotation/riverpod_annotation.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/data/services/providers.dart';
import 'package:discipolat_mobile/features/finances/models/finance_model.dart';

part 'finances_service.g.dart';

/// Service FINANCES — contrat exact de `FinanceController`
/// (monture `/api/v1/finances`). Tous les identifiants sont des UUID (`String`).
/// Les paramètres de requête sont exactement ceux que le contrôleur lit
/// (type, categorie, debut, fin, annee) — rien d'inventé.
@riverpod
FinancesService financesService(FinancesServiceRef ref) {
  final api = ref.watch(apiServiceProvider);
  return FinancesService(api);
}

class FinancesService {
  final ApiService _api;

  FinancesService(this._api);

  static String _day(DateTime d) => d.toIso8601String().substring(0, 10);

  // ---------- Transactions ----------

  Future<List<Transaction>> getTransactions({
    TransactionType? type,
    String? categorie,
    DateTime? debut,
    DateTime? fin,
  }) async {
    try {
      final queryParams = <String, dynamic>{
        if (type != null) 'type': type.wire,
        if (categorie != null && categorie.isNotEmpty) 'categorie': categorie,
        if (debut != null) 'debut': _day(debut),
        if (fin != null) 'fin': _day(fin),
      };
      final response = await _api.get('/finances/transactions', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => Transaction.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des transactions: $e');
    }
  }

  Future<Transaction> getTransaction(String id) async {
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

  Future<Transaction> updateTransaction(String id, Transaction transaction) async {
    try {
      final response = await _api.put('/finances/transactions/$id', data: transaction.toJson());
      return Transaction.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la mise à jour: $e');
    }
  }

  Future<void> deleteTransaction(String id) async {
    try {
      await _api.delete('/finances/transactions/$id');
    } catch (e) {
      throw Exception('Erreur lors de la suppression: $e');
    }
  }

  // ---------- Comptes (V236) ----------

  Future<List<Account>> getAccounts() async {
    try {
      final response = await _api.get('/finances/accounts');
      final data = response.data as List;
      return data.map((json) => Account.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des comptes: $e');
    }
  }

  Future<Account> getAccount(String id) async {
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

  // ---------- Budgets ----------

  Future<List<Budget>> getBudgets({int? annee}) async {
    try {
      final queryParams = <String, dynamic>{};
      if (annee != null) queryParams['annee'] = annee;
      final response = await _api.get('/finances/budgets', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => Budget.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des budgets: $e');
    }
  }

  Future<Budget> getBudget(String id) async {
    try {
      final response = await _api.get('/finances/budgets/$id');
      return Budget.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du chargement du budget: $e');
    }
  }

  /// POST /budgets = upsert serveur (annee + categorie + montant).
  Future<Budget> createBudget(Budget budget) async {
    try {
      final response = await _api.post('/finances/budgets', data: budget.toJson());
      return Budget.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création: $e');
    }
  }

  Future<void> deleteBudget(String id) async {
    try {
      await _api.delete('/finances/budgets/$id');
    } catch (e) {
      throw Exception('Erreur lors de la suppression: $e');
    }
  }

  // ---------- Tontines (V236) ----------

  Future<List<Tontine>> getTontines() async {
    try {
      final response = await _api.get('/finances/tontines');
      final data = response.data as List;
      return data.map((json) => Tontine.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des tontines: $e');
    }
  }

  Future<Tontine> getTontine(String id) async {
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

  Future<List<TontineMember>> getTontineMembers(String tontineId) async {
    try {
      final response = await _api.get('/finances/tontines/$tontineId/members');
      final data = response.data as List;
      return data.map((json) => TontineMember.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des membres: $e');
    }
  }

  Future<TontineMember> addTontineMember(String tontineId, TontineMember member) async {
    try {
      final response = await _api.post('/finances/tontines/$tontineId/members', data: member.toJson());
      return TontineMember.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de l\'ajout: $e');
    }
  }

  Future<List<TontinePayout>> getTontinePayouts(String tontineId) async {
    try {
      final response = await _api.get('/finances/tontines/$tontineId/payouts');
      final data = response.data as List;
      return data.map((json) => TontinePayout.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des versements: $e');
    }
  }

  // ---------- Dons (V236) ----------

  Future<List<Donation>> getDonations() async {
    try {
      final response = await _api.get('/finances/donations');
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

  // ---------- Rapports (V236) ----------

  Future<Map<String, dynamic>> getFinancialSummary() async {
    try {
      final response = await _api.get('/finances/reports/summary');
      return response.data as Map<String, dynamic>;
    } catch (e) {
      throw Exception('Erreur lors du chargement du résumé: $e');
    }
  }

  Future<List<Map<String, dynamic>>> getTransactionsByCategory() async {
    try {
      final response = await _api.get('/finances/reports/by-category');
      final data = response.data as List;
      return data.map((json) => json as Map<String, dynamic>).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement du rapport par catégorie: $e');
    }
  }

  Future<List<Map<String, dynamic>>> getCashFlow() async {
    try {
      final response = await _api.get('/finances/reports/cash-flow');
      final data = response.data as List;
      return data.map((json) => json as Map<String, dynamic>).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement du flux de trésorerie: $e');
    }
  }

  // ---------- Rapprochement (V236) ----------

  Future<void> reconcileTransaction(String id) async {
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

  /// Statistiques annuelles (parMois, recettesParCategorie, depensesParCategorie).
  Future<Map<String, dynamic>> getStats({int? annee}) async {
    try {
      final queryParams = <String, dynamic>{};
      if (annee != null) queryParams['annee'] = annee;
      final response = await _api.get('/finances/stats', queryParameters: queryParams);
      return response.data as Map<String, dynamic>;
    } catch (e) {
      throw Exception('Erreur lors du chargement des statistiques: $e');
    }
  }
}
