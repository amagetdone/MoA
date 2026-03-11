import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:more_app/domain/entities/account.dart';
import 'package:more_app/domain/entities/transaction.dart';
import 'package:more_app/domain/repositories/account_repository.dart';
import 'package:more_app/presentation/providers/app_data_providers.dart';

class AccountState {
  final bool isLoading;
  final String? errorMessage;
  final List<String> bankNames;
  final List<Account> accounts;
  final List<Transaction> transactions;
  final int totalAsset;

  const AccountState({
    this.isLoading = false,
    this.errorMessage,
    this.bankNames = const [],
    this.accounts = const [],
    this.transactions = const [],
    this.totalAsset = 0,
  });

  AccountState copyWith({
    bool? isLoading,
    String? errorMessage,
    bool clearErrorMessage = false,
    List<String>? bankNames,
    List<Account>? accounts,
    List<Transaction>? transactions,
    int? totalAsset,
  }) {
    return AccountState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: clearErrorMessage
          ? null
          : errorMessage ?? this.errorMessage,
      bankNames: bankNames ?? this.bankNames,
      accounts: accounts ?? this.accounts,
      transactions: transactions ?? this.transactions,
      totalAsset: totalAsset ?? this.totalAsset,
    );
  }
}

class AccountViewModel extends StateNotifier<AccountState> {
  AccountViewModel(this._repository)
    : super(const AccountState(isLoading: true)) {
    loadData();
  }

  final AccountRepository _repository;

  Future<void> loadData() async {
    state = state.copyWith(isLoading: true, clearErrorMessage: true);

    try {
      final dashboard = await _repository.fetchAccountDashboard();
      state = state.copyWith(
        isLoading: false,
        bankNames: dashboard.bankNames,
        accounts: dashboard.accounts,
        transactions: dashboard.transactions,
        totalAsset: dashboard.totalAsset,
      );
    } catch (error) {
      state = state.copyWith(isLoading: false, errorMessage: error.toString());
    }
  }

  Future<void> addAccount(Account account) async {
    await _repository.addAccount(account);
    await loadData();
  }

  Future<void> addLinkedAccount({
    required String bankName,
    required String accountNumber,
  }) async {
    final account = Account(
      id: 'acc_${DateTime.now().millisecondsSinceEpoch}',
      bankName: bankName,
      accountNumber: accountNumber,
      accountName: '$bankName 연동 계좌',
      balance: 0,
      isPrimary: state.accounts.isEmpty,
    );
    await addAccount(account);
  }

  Future<void> removeAccount(String id) async {
    await _repository.removeAccount(id);
    await loadData();
  }
}

final accountViewModelProvider =
    StateNotifierProvider<AccountViewModel, AccountState>((ref) {
      return AccountViewModel(ref.read(accountRepositoryProvider));
    });
