import 'package:more_app/data/datasources/remote/account_remote_datasource.dart';
import 'package:more_app/data/datasources/remote/seed/seed_data_store.dart';
import 'package:more_app/domain/entities/account.dart';
import 'package:more_app/domain/entities/account_dashboard.dart';
import 'package:more_app/domain/entities/transaction.dart';

class SeedAccountRemoteDataSource implements AccountRemoteDataSource {
  SeedAccountRemoteDataSource({required SeedDataStore dataStore})
    : _dataStore = dataStore;

  final SeedDataStore _dataStore;

  @override
  Future<AccountDashboard> fetchAccountDashboard() async {
    await Future.delayed(const Duration(milliseconds: 220));
    final snapshot = _dataStore.clone(await _dataStore.ensureSessionSnapshot());

    final totalAsset = snapshot.accounts.fold(0, (sum, account) {
      return sum + account.balance;
    });

    return AccountDashboard(
      accounts: snapshot.accounts,
      transactions: List<Transaction>.from(snapshot.transactions)
        ..sort((a, b) => b.dateTime.compareTo(a.dateTime)),
      bankNames: List<String>.from(snapshot.bankNames),
      totalAsset: totalAsset,
    );
  }

  @override
  Future<Account> addAccount(Account account) async {
    final snapshot = await _dataStore.mutate((snapshot) {
      final updatedAccounts = List<Account>.from(snapshot.accounts);
      final existingIndex = updatedAccounts.indexWhere(
        (item) => item.id == account.id,
      );

      if (existingIndex == -1) {
        updatedAccounts.add(account);
      } else {
        updatedAccounts[existingIndex] = account;
      }

      return snapshot.copyWith(accounts: updatedAccounts);
    });

    return snapshot.accounts.firstWhere((item) => item.id == account.id);
  }

  @override
  Future<void> removeAccount(String id) async {
    await _dataStore.mutate((snapshot) {
      final remaining =
          snapshot.accounts.where((item) => item.id != id).toList();

      if (remaining.isNotEmpty && !remaining.any((item) => item.isPrimary)) {
        remaining[0] = remaining[0].copyWith(isPrimary: true);
      }

      final remainingAccountIds = remaining.map((item) => item.id).toSet();
      final updatedTransactions = snapshot.transactions
          .where(
            (transaction) =>
                transaction.accountId == null ||
                remainingAccountIds.contains(transaction.accountId),
          )
          .toList();

      return snapshot.copyWith(
        accounts: remaining,
        transactions: updatedTransactions,
      );
    });
  }

  @override
  Future<bool> confirmAccount(String accountNo) async {
    await Future.delayed(const Duration(milliseconds: 220));
    return true;
  }

  @override
  Future<Map<String, dynamic>> requestVerification(String accountNo) async {
    await Future.delayed(const Duration(milliseconds: 220));
    return {
      'verificationId': 1,
      'accountNo': accountNo,
      'authText': 'SEEDTEST',
      'transactionUniqueNo': 1,
      'requestedAt': DateTime.now().toIso8601String(),
      'status': 'REQUESTED',
    };
  }

  @override
  Future<bool> checkVerification({required String accountNo, required String authCode}) async {
    await Future.delayed(const Duration(milliseconds: 220));
    return true;
  }

  @override
  Future<Transaction?> findTransactionById(String id) async {
    await Future.delayed(const Duration(milliseconds: 220));
    final snapshot = await _dataStore.ensureSessionSnapshot();
    for (final transaction in snapshot.transactions) {
      if (transaction.id == id) {
        return transaction;
      }
    }
    return null;
  }

  @override
  Future<List<Transaction>> fetchTransactions({
    String? fromDate,
    String? toDate,
    String? analysisInclusion,
    int page = 0,
    int size = 50,
  }) async {
    await Future.delayed(const Duration(milliseconds: 220));
    final snapshot = await _dataStore.ensureSessionSnapshot();
    return snapshot.transactions;
  }
}
