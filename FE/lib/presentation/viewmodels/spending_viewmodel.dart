import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:more_app/core/utils/mock_data.dart';
import 'package:more_app/domain/entities/spending.dart';

class SpendingState {
  final bool isWeekly;
  final List<SpendingCategory> categories;
  final List<DailySpending> dailySpendings;
  final List<DailySpending> monthlySpendings;
  final int weeklyBudget;
  final int monthlyIncome;
  final int monthlySavingsGoal;
  final int monthlyFixedExpense;

  const SpendingState({
    this.isWeekly = true,
    this.categories = const [],
    this.dailySpendings = const [],
    this.monthlySpendings = const [],
    this.weeklyBudget = 0,
    this.monthlyIncome = 0,
    this.monthlySavingsGoal = 0,
    this.monthlyFixedExpense = 0,
  });

  int get totalSpent => categories.fold(0, (sum, c) => sum + c.amount);

  SpendingState copyWith({
    bool? isWeekly,
    List<SpendingCategory>? categories,
    List<DailySpending>? dailySpendings,
    List<DailySpending>? monthlySpendings,
    int? weeklyBudget,
    int? monthlyIncome,
    int? monthlySavingsGoal,
    int? monthlyFixedExpense,
  }) {
    return SpendingState(
      isWeekly: isWeekly ?? this.isWeekly,
      categories: categories ?? this.categories,
      dailySpendings: dailySpendings ?? this.dailySpendings,
      monthlySpendings: monthlySpendings ?? this.monthlySpendings,
      weeklyBudget: weeklyBudget ?? this.weeklyBudget,
      monthlyIncome: monthlyIncome ?? this.monthlyIncome,
      monthlySavingsGoal: monthlySavingsGoal ?? this.monthlySavingsGoal,
      monthlyFixedExpense: monthlyFixedExpense ?? this.monthlyFixedExpense,
    );
  }
}

class SpendingViewModel extends StateNotifier<SpendingState> {
  SpendingViewModel() : super(const SpendingState()) {
    _loadData();
  }

  void _loadData() {
    state = SpendingState(
      categories: MockData.spendingCategories,
      dailySpendings: MockData.dailySpendings,
      monthlySpendings: MockData.monthlySpendings,
      weeklyBudget: MockData.weeklyBudget,
      monthlyIncome: MockData.monthlyIncome,
      monthlySavingsGoal: MockData.monthlySavingsGoal,
      monthlyFixedExpense: MockData.monthlyFixedExpense,
    );
  }

  void togglePeriod(bool isWeekly) {
    state = state.copyWith(isWeekly: isWeekly);
  }
}

final spendingViewModelProvider =
    StateNotifierProvider<SpendingViewModel, SpendingState>((ref) {
  return SpendingViewModel();
});
