import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:more_app/domain/entities/account.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/repositories/home_repository.dart';
import 'package:more_app/presentation/providers/app_data_providers.dart';

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
  final int weeklySaved;

  const HomeState({
    this.isLoading = false,
    this.errorMessage,
    this.status = HomeStatus.ready,
    this.accounts = const [],
    this.activeGoals = const [],
    this.totalAsset = 0,
    this.weeklyBudget = 0,
    this.weeklySpent = 0,
    this.weeklySaved = 0,
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
    int? weeklySaved,
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
      weeklySaved: weeklySaved ?? this.weeklySaved,
    );
  }
}

class HomeViewModel extends StateNotifier<HomeState> {
  HomeViewModel(this._repository) : super(const HomeState(isLoading: true)) {
    loadData();
  }

  final HomeRepository _repository;

  Future<void> loadData() async {
    state = state.copyWith(isLoading: true, clearErrorMessage: true);

    try {
      final dashboard = await _repository.fetchHomeDashboard();

      HomeStatus status;
      if (dashboard.accounts.isEmpty) {
        status = HomeStatus.noAccount;
      } else if (dashboard.activeGoals.isEmpty) {
        status = HomeStatus.noGoal;
      } else {
        status = HomeStatus.ready;
      }

      state = state.copyWith(
        isLoading: false,
        status: status,
        accounts: dashboard.accounts,
        activeGoals: dashboard.activeGoals,
        totalAsset: dashboard.totalAsset,
        weeklyBudget: dashboard.weeklyBudget,
        weeklySpent: dashboard.weeklySpent,
        weeklySaved: dashboard.weeklySaved,
      );
    } catch (error) {
      state = state.copyWith(isLoading: false, errorMessage: error.toString());
    }
  }
}

final homeViewModelProvider = StateNotifierProvider<HomeViewModel, HomeState>((
  ref,
) {
  return HomeViewModel(ref.read(homeRepositoryProvider));
});
