import 'package:more_app/data/datasources/local/app_local_datasource.dart';
import 'package:more_app/data/datasources/remote/account_remote_datasource.dart';
import 'package:more_app/domain/entities/account.dart';
import 'package:more_app/domain/entities/account_dashboard.dart';
import 'package:more_app/domain/entities/transaction.dart';
import 'package:more_app/domain/repositories/account_repository.dart';

class AccountRepositoryImpl implements AccountRepository {
  AccountRepositoryImpl({
    required AccountRemoteDataSource remoteDataSource,
    required AppLocalDataSource localDataSource,
  }) : _remoteDataSource = remoteDataSource,
       _localDataSource = localDataSource;

  final AccountRemoteDataSource _remoteDataSource;
  final AppLocalDataSource _localDataSource;

  @override
  Future<AccountDashboard> fetchAccountDashboard() async {
    try {
      return await _remoteDataSource.fetchAccountDashboard();
    } catch (_) {
      final snapshot = await _localDataSource.getCachedSnapshot();
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

  @override
  Future<Account> addAccount(Account account) async {
    try {
      return await _remoteDataSource.addAccount(account);
    } catch (_) {
      return _localDataSource.addAccount(account);
    }
  }

  @override
  Future<void> removeAccount(String id) async {
    try {
      await _remoteDataSource.removeAccount(id);
    } catch (_) {
      await _localDataSource.removeAccount(id);
    }
  }

  @override
  Future<Transaction?> findTransactionById(String id) async {
    try {
      return await _remoteDataSource.findTransactionById(id);
    } catch (_) {
      return _localDataSource.getTransactionById(id);
    }
  }

  @override
  Future<bool> confirmAccount(String accountNo) async {
    return await _remoteDataSource.confirmAccount(accountNo);
  }

  @override
  Future<Map<String, dynamic>> requestVerification(String accountNo) async {
    return await _remoteDataSource.requestVerification(accountNo);
  }

  @override
  Future<bool> checkVerification({required String accountNo, required String authCode}) async {
    return await _remoteDataSource.checkVerification(accountNo: accountNo, authCode: authCode);
  }
}
