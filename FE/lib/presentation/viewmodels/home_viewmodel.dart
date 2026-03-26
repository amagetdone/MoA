

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:more_app/domain/entities/account.dart';
import 'package:more_app/domain/entities/account_dashboard.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/entities/home_dashboard.dart';
import 'package:more_app/domain/repositories/account_repository.dart';
import 'package:more_app/domain/repositories/home_repository.dart';
import 'package:more_app/presentation/providers/app_data_providers.dart';
import 'package:more_app/presentation/viewmodels/auth_viewmodel.dart';

enum HomeStatus { noAccount, noGoal, ready }

class HomeState {
  final bool isLoading;
  final String? errorMessage;
  final HomeStatus status;
  final List<Account> accounts;
  final List<Goal> activeGoals;
  final int totalAsset;
  final int weeklyBudget;
  final int weeklySpent;
  final int weeklyRemaining;
  final int daysRemaining;
  final int dailyAvailable;
  final int weeklySaved;
  final int savingTargetAmount;
  final int savingProgressRate;

  const HomeState({
    this.isLoading = false,
    this.errorMessage,
    this.status = HomeStatus.ready,
    this.accounts = const [],
    this.activeGoals = const [],
    this.totalAsset = 0,
    this.weeklyBudget = 0,
    this.weeklySpent = 0,
    this.weeklyRemaining = 0,
    this.daysRemaining = 0,
    this.dailyAvailable = 0,
    this.weeklySaved = 0,
    this.savingTargetAmount = 0,
    this.savingProgressRate = 0,
  });

  HomeState copyWith({
    bool? isLoading,
    String? errorMessage,
    bool clearErrorMessage = false,
    HomeStatus? status,
    List<Account>? accounts,
    List<Goal>? activeGoals,
    int? totalAsset,
    int? weeklyBudget,
    int? weeklySpent,
    int? weeklyRemaining,
    int? daysRemaining,
    int? dailyAvailable,
    int? weeklySaved,
    int? savingTargetAmount,
    int? savingProgressRate,
  }) {
    return HomeState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: clearErrorMessage
          ? null
          : errorMessage ?? this.errorMessage,
      status: status ?? this.status,
      accounts: accounts ?? this.accounts,
      activeGoals: activeGoals ?? this.activeGoals,
      totalAsset: totalAsset ?? this.totalAsset,
      weeklyBudget: weeklyBudget ?? this.weeklyBudget,
      weeklySpent: weeklySpent ?? this.weeklySpent,
      weeklyRemaining: weeklyRemaining ?? this.weeklyRemaining,
      daysRemaining: daysRemaining ?? this.daysRemaining,
      dailyAvailable: dailyAvailable ?? this.dailyAvailable,
      weeklySaved: weeklySaved ?? this.weeklySaved,
      savingTargetAmount: savingTargetAmount ?? this.savingTargetAmount,
      savingProgressRate: savingProgressRate ?? this.savingProgressRate,
    );
  }
}

class HomeViewModel extends StateNotifier<HomeState> {
  HomeViewModel(this._repository, this._accountRepository)
      : super(const HomeState(isLoading: true)) {
    loadData();
  }

  final HomeRepository _repository;
  final AccountRepository _accountRepository;

  Future<void> loadData() async {
    state = state.copyWith(isLoading: true, clearErrorMessage: true);

    try {
      final results = await Future.wait([
        _repository.fetchHomeDashboard(),
        _accountRepository.fetchAccountDashboard(),
      ]);
      if (!mounted) {
        return;
      }
      final dashboard = results[0] as HomeDashboard;
      final accountDashboard = results[1] as AccountDashboard;

      // 홈 API 계좌가 있으면 잔액 포함된 홈 API 것을 사용, 없으면 계좌 API 것 사용
      final accounts = dashboard.accounts.isNotEmpty
          ? dashboard.accounts
          : accountDashboard.accounts;

      HomeStatus status;
      if (accounts.isEmpty && dashboard.activeGoals.isEmpty) {
        status = HomeStatus.noAccount;
      } else if (dashboard.activeGoals.isEmpty) {
        status = HomeStatus.noGoal;
      } else {
        status = HomeStatus.ready;
      }

      state = state.copyWith(
        isLoading: false,
        status: status,
        accounts: accounts,
        activeGoals: dashboard.activeGoals,
        totalAsset: dashboard.totalAsset,
        weeklyBudget: dashboard.weeklyBudget,
        weeklySpent: dashboard.weeklySpent,
        weeklyRemaining: dashboard.weeklyRemaining,
        daysRemaining: dashboard.daysRemaining,
        dailyAvailable: dashboard.dailyAvailable,
        weeklySaved: dashboard.weeklySaved,
        savingTargetAmount: dashboard.savingTargetAmount,
        savingProgressRate: dashboard.savingProgressRate,
      );
    } catch (error) {
      if (!mounted) {
        return;
      }
      state = state.copyWith(isLoading: false, errorMessage: error.toString());
    }
  }
}

final homeViewModelProvider = StateNotifierProvider<HomeViewModel, HomeState>((
  ref,
) {
  ref.watch(authViewModelProvider);
  return HomeViewModel(
    ref.read(homeRepositoryProvider),
    ref.read(accountRepositoryProvider),
  );
});
