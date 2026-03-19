import 'package:more_app/domain/entities/account.dart';
import 'package:more_app/domain/entities/account_dashboard.dart';
import 'package:more_app/domain/entities/transaction.dart';

abstract class AccountRepository {
  Future<AccountDashboard> fetchAccountDashboard();
  Future<Account> addAccount(Account account);
  Future<void> removeAccount(String id);
  Future<Transaction?> findTransactionById(String id);
  Future<bool> confirmAccount(String accountNo);
  Future<Map<String, dynamic>> requestVerification(String accountNo);
  Future<bool> checkVerification({required String accountNo, required String authCode});
}