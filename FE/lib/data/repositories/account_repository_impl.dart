import 'package:more_app/data/datasources/local/app_local_datasource.dart';
import 'package:more_app/data/datasources/remote/app_remote_datasource.dart';
import 'package:more_app/data/models/app_snapshot.dart';
import 'package:more_app/domain/entities/account.dart';
import 'package:more_app/domain/entities/account_dashboard.dart';
import 'package:more_app/domain/entities/transaction.dart';
import 'package:more_app/domain/repositories/account_repository.dart';

class AccountRepositoryImpl implements AccountRepository {
  AccountRepositoryImpl({
    required AppRemoteDataSource remoteDataSource,
    required AppLocalDataSource localDataSource,
  }) : _remoteDataSource = remoteDataSource,
       _localDataSource = localDataSource;

  final AppRemoteDataSource _remoteDataSource;
  final AppLocalDataSource _localDataSource;

  @override
  Future<AccountDashboard> fetchAccountDashboard() async {
    final snapshot = await _loadSnapshot();
    return _toDashboard(snapshot);
  }

  @override
  Future<Account> addAccount(Account account) async {
    try {
      final snapshot = await _remoteDataSource.addAccount(account);
      await _localDataSource.saveCachedSnapshot(snapshot);
      return snapshot.accounts.firstWhere((item) => item.id == account.id);
    } catch (_) {
      return _localDataSource.addAccount(account);
    }
  }

  @override
  Future<void> removeAccount(String id) async {
    try {
      final snapshot = await _remoteDataSource.removeAccount(id);
      await _localDataSource.saveCachedSnapshot(snapshot);
    } catch (_) {
      await _localDataSource.removeAccount(id);
    }
  }

  @override
  Future<Transaction?> findTransactionById(String id) async {
    final snapshot = await _loadSnapshot();
    for (final transaction in snapshot.transactions) {
      if (transaction.id == id) {
        return transaction;
      }
    }
    return null;
  }

  Future<AppSnapshot> _loadSnapshot() async {
    try {
      final snapshot = await _remoteDataSource.fetchSnapshot();
      await _localDataSource.saveCachedSnapshot(snapshot);
      return snapshot;
    } catch (_) {
      return _localDataSource.getCachedSnapshot();
    }
  }

  AccountDashboard _toDashboard(AppSnapshot snapshot) {
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
}
