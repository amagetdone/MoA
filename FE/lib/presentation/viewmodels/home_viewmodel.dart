import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:more_app/core/utils/mock_data.dart';
import 'package:more_app/domain/entities/account.dart';
import 'package:more_app/domain/entities/goal.dart';

enum HomeStatus { noAccount, noGoal, ready }

class HomeState {
  final HomeStatus status;
  final List<Account> accounts;
  final List<Goal> activeGoals;
  final int totalAsset;
  final int weeklyBudget;
  final int weeklySpent;
  final int weeklySaved;

  const HomeState({
    this.status = HomeStatus.ready,
    this.accounts = const [],
    this.activeGoals = const [],
    this.totalAsset = 0,
    this.weeklyBudget = 0,
    this.weeklySpent = 0,
    this.weeklySaved = 0,
  });
}

class HomeViewModel extends StateNotifier<HomeState> {
  HomeViewModel() : super(const HomeState()) {
    _loadData();
  }

  void _loadData() {
    final accounts = MockData.accounts;
    final activeGoals = MockData.activeGoals;

    HomeStatus status;
    if (accounts.isEmpty) {
      status = HomeStatus.noAccount;
    } else if (activeGoals.isEmpty) {
      status = HomeStatus.noGoal;
    } else {
      status = HomeStatus.ready;
    }

    state = HomeState(
      status: status,
      accounts: accounts,
      activeGoals: activeGoals,
      totalAsset: MockData.totalAsset,
      weeklyBudget: MockData.weeklyBudget,
      weeklySpent: MockData.totalWeeklySpent,
      weeklySaved: 150000,
    );
  }
}

final homeViewModelProvider =
    StateNotifierProvider<HomeViewModel, HomeState>((ref) {
  return HomeViewModel();
});
