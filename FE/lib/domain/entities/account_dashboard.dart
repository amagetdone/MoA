import 'package:more_app/domain/entities/account.dart';
import 'package:more_app/domain/entities/transaction.dart';

class AccountDashboard {
  final List<Account> accounts;
  final List<Transaction> transactions;
  final List<String> bankNames;
  final int totalAsset;

  const AccountDashboard({
    this.accounts = const [],
    this.transactions = const [],
    this.bankNames = const [],
    this.totalAsset = 0,
  });
}
