import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:more_app/core/utils/mock_data.dart';
import 'package:more_app/domain/entities/account.dart';
import 'package:more_app/domain/entities/transaction.dart';

class AccountState {
  final List<Account> accounts;
  final List<Transaction> transactions;
  final int totalAsset;

  const AccountState({
    this.accounts = const [],
    this.transactions = const [],
    this.totalAsset = 0,
  });

  AccountState copyWith({
    List<Account>? accounts,
    List<Transaction>? transactions,
    int? totalAsset,
  }) {
    return AccountState(
      accounts: accounts ?? this.accounts,
      transactions: transactions ?? this.transactions,
      totalAsset: totalAsset ?? this.totalAsset,
    );
  }
}

class AccountViewModel extends StateNotifier<AccountState> {
  AccountViewModel() : super(const AccountState()) {
    _loadData();
  }

  void _loadData() {
    state = AccountState(
      accounts: MockData.accounts,
      transactions: MockData.transactions,
      totalAsset: MockData.totalAsset,
    );
  }

  void addAccount(Account account) {
    final updated = [...state.accounts, account];
    state = state.copyWith(
      accounts: updated,
      totalAsset: updated.fold<int>(0, (sum, a) => sum + a.balance),
    );
  }

  void removeAccount(String id) {
    final updated = state.accounts.where((a) => a.id != id).toList();
    state = state.copyWith(
      accounts: updated,
      totalAsset: updated.fold<int>(0, (sum, a) => sum + a.balance),
    );
  }
}

final accountViewModelProvider =
    StateNotifierProvider<AccountViewModel, AccountState>((ref) {
  return AccountViewModel();
});
