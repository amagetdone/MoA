import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:more_app/domain/entities/spending.dart';
import 'package:more_app/domain/repositories/spending_repository.dart';
import 'package:more_app/presentation/providers/app_data_providers.dart';

class SpendingState {
  final bool isLoading;
  final String? errorMessage;
  final bool isWeekly;
  final List<SpendingCategory> categories;
  final List<DailySpending> dailySpendings;
  final List<DailySpending> monthlySpendings;
  final int weeklyBudget;
  final int monthlyIncome;
  final int monthlySavingsGoal;
  final int monthlyFixedExpense;

  const SpendingState({
    this.isLoading = false,
    this.errorMessage,
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
    bool? isLoading,
    String? errorMessage,
    bool clearErrorMessage = false,
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
      isLoading: isLoading ?? this.isLoading,
      errorMessage: clearErrorMessage
          ? null
          : errorMessage ?? this.errorMessage,
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
  SpendingViewModel(this._repository)
    : super(const SpendingState(isLoading: true)) {
    loadData();
  }

  final SpendingRepository _repository;

  Future<void> loadData() async {
    state = state.copyWith(isLoading: true, clearErrorMessage: true);

    try {
      final overview = await _repository.fetchSpendingOverview();
      state = state.copyWith(
        isLoading: false,
        categories: overview.categories,
        dailySpendings: overview.dailySpendings,
        monthlySpendings: overview.monthlySpendings,
        weeklyBudget: overview.weeklyBudget,
        monthlyIncome: overview.monthlyIncome,
        monthlySavingsGoal: overview.monthlySavingsGoal,
        monthlyFixedExpense: overview.monthlyFixedExpense,
      );
    } catch (error) {
      state = state.copyWith(isLoading: false, errorMessage: error.toString());
    }
  }

  void togglePeriod(bool isWeekly) {
    state = state.copyWith(isWeekly: isWeekly);
  }

  Future<void> saveBudgetPlan({
    required int monthlyIncome,
    required int monthlySavingsGoal,
    required int monthlyFixedExpense,
    required List<SpendingCategory> categories,
  }) async {
    final overview = await _repository.updateBudgetPlan(
      monthlyIncome: monthlyIncome,
      monthlySavingsGoal: monthlySavingsGoal,
      monthlyFixedExpense: monthlyFixedExpense,
      categories: categories,
    );

    state = state.copyWith(
      monthlyIncome: overview.monthlyIncome,
      monthlySavingsGoal: overview.monthlySavingsGoal,
      monthlyFixedExpense: overview.monthlyFixedExpense,
      weeklyBudget: overview.weeklyBudget,
      categories: overview.categories,
    );
  }
}

final spendingViewModelProvider =
    StateNotifierProvider<SpendingViewModel, SpendingState>((ref) {
      return SpendingViewModel(ref.read(spendingRepositoryProvider));
    });
