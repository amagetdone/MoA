import 'package:more_app/data/datasources/local/app_local_datasource.dart';
import 'package:more_app/domain/entities/account.dart';
import 'package:more_app/domain/entities/account_dashboard.dart';
import 'package:more_app/domain/entities/transaction.dart';
import 'package:more_app/domain/repositories/account_repository.dart';

class AccountRepositoryImpl implements AccountRepository {
  AccountRepositoryImpl({required AppLocalDataSource localDataSource})
    : _localDataSource = localDataSource;

  final AppLocalDataSource _localDataSource;

  @override
  Future<AccountDashboard> fetchAccountDashboard() async {
    final accounts = await _localDataSource.getAccounts();
    final transactions = await _localDataSource.getTransactions();
    final bankNames = await _localDataSource.getBankNames();
    final totalAsset = accounts.fold(
      0,
      (sum, account) => sum + account.balance,
    );

    return AccountDashboard(
      accounts: accounts,
      transactions: transactions,
      bankNames: bankNames,
      totalAsset: totalAsset,
    );
  }

  @override
  Future<Account> addAccount(Account account) =>
      _localDataSource.addAccount(account);

  @override
  Future<void> removeAccount(String id) => _localDataSource.removeAccount(id);

  @override
  Future<Transaction?> findTransactionById(String id) =>
      _localDataSource.getTransactionById(id);
}
