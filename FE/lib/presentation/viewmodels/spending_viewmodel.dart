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
  final List<DailySpending> monthlyComparisons;
  final int weeklyBudget;
  final int monthlyIncome;
  final int monthlySavingsGoal;
  final int monthlyFixedExpense;
  final DateTime selectedWeekStart;
  final DateTime selectedMonthStart;
  final int? touchedPieIndex;
  final int weeklyExpenseAmount;
  final int monthlyExpenseAmount;

  SpendingState({
    this.isLoading = false,
    this.errorMessage,
    this.isWeekly = true,
    this.categories = const [],
    this.dailySpendings = const [],
    this.monthlySpendings = const [],
    this.monthlyComparisons = const [],
    this.weeklyBudget = 0,
    this.monthlyIncome = 0,
    this.monthlySavingsGoal = 0,
    this.monthlyFixedExpense = 0,
    DateTime? selectedWeekStart,
    DateTime? selectedMonthStart,
    this.touchedPieIndex,
    this.weeklyExpenseAmount = 0,
    this.monthlyExpenseAmount = 0,
  })  : selectedWeekStart = selectedWeekStart ?? _thisWeekStart(),
        selectedMonthStart = selectedMonthStart ?? _thisMonthStart();

  static DateTime _thisWeekStart() {
    final now = DateTime.now();
    final monday = now.subtract(Duration(days: now.weekday - 1));
    return DateTime(monday.year, monday.month, monday.day);
  }

  static DateTime _thisMonthStart() {
    final now = DateTime.now();
    return DateTime(now.year, now.month, 1);
  }

  int get totalSpent => categories.fold(0, (sum, c) => sum + c.amount);

  SpendingState copyWith({
    bool? isLoading,
    String? errorMessage,
    bool clearErrorMessage = false,
    bool? isWeekly,
    List<SpendingCategory>? categories,
    List<DailySpending>? dailySpendings,
    List<DailySpending>? monthlySpendings,
    List<DailySpending>? monthlyComparisons,
    int? weeklyBudget,
    int? monthlyIncome,
    int? monthlySavingsGoal,
    int? monthlyFixedExpense,
    DateTime? selectedWeekStart,
    DateTime? selectedMonthStart,
    Object? touchedPieIndex = _sentinel,
    int? weeklyExpenseAmount,
    int? monthlyExpenseAmount,
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
      monthlyComparisons: monthlyComparisons ?? this.monthlyComparisons,
      weeklyBudget: weeklyBudget ?? this.weeklyBudget,
      monthlyIncome: monthlyIncome ?? this.monthlyIncome,
      monthlySavingsGoal: monthlySavingsGoal ?? this.monthlySavingsGoal,
      monthlyFixedExpense: monthlyFixedExpense ?? this.monthlyFixedExpense,
      selectedWeekStart: selectedWeekStart ?? this.selectedWeekStart,
      selectedMonthStart: selectedMonthStart ?? this.selectedMonthStart,
      touchedPieIndex: touchedPieIndex == _sentinel
          ? this.touchedPieIndex
          : touchedPieIndex as int?,
      weeklyExpenseAmount: weeklyExpenseAmount ?? this.weeklyExpenseAmount,
      monthlyExpenseAmount: monthlyExpenseAmount ?? this.monthlyExpenseAmount,
    );
  }

  static const _sentinel = Object();
}

class SpendingViewModel extends StateNotifier<SpendingState> {
  SpendingViewModel(this._repository)
    : super(SpendingState(isLoading: true)) {
    loadData();
  }

  final SpendingRepository _repository;

  Future<void> loadData() async {
    state = state.copyWith(isLoading: true, clearErrorMessage: true);

    try {
      try {
        await _repository.syncTransactions();
      } catch (_) {}
      try {
        await _repository.analyzeTransactions();
      } catch (_) {}
      final overview = await _repository.fetchSpendingOverview();
      state = state.copyWith(
        weeklyBudget: overview.weeklyBudget,
        monthlyIncome: overview.monthlyIncome,
        monthlySavingsGoal: overview.monthlySavingsGoal,
        monthlyFixedExpense: overview.monthlyFixedExpense,
      );
      await _loadPeriodData();
      state = state.copyWith(isLoading: false);
    } catch (error) {
      state = state.copyWith(isLoading: false, errorMessage: error.toString());
    }
  }

  Future<void> _loadPeriodData() async {
    try {
      if (state.isWeekly) {
        final dailySpendings = await _repository.fetchWeeklyDailySpending(state.selectedWeekStart);
        final result = await _repository.fetchWeeklyAnalysis(state.selectedWeekStart);
        state = state.copyWith(
          categories: result.categories,
          dailySpendings: dailySpendings,
          weeklyExpenseAmount: dailySpendings.fold<int>(0, (sum, d) => sum + d.amount),
        );
      } else {
        final selected = state.selectedMonthStart;
        final prev1 = DateTime(selected.year, selected.month - 1, 1);
        final prev2 = DateTime(selected.year, selected.month - 2, 1);

        final result = await _repository.fetchMonthlyAnalysis(selected);
        final monthlySpendings = await _repository.fetchMonthlyDailySpending(selected);
        final currentTotal = monthlySpendings.fold<int>(0, (sum, d) => sum + d.amount);

        final priorTotals = await Future.wait([prev2, prev1].map((m) async {
          try {
            final spendings = await _repository.fetchMonthlyDailySpending(m);
            return spendings.fold<int>(0, (sum, d) => sum + d.amount);
          } catch (_) {
            return 0;
          }
        }));

        final comparisons = [
          DailySpending(date: prev2, amount: priorTotals[0]),
          DailySpending(date: prev1, amount: priorTotals[1]),
          DailySpending(date: selected, amount: currentTotal),
        ];

        state = state.copyWith(
          categories: result.categories,
          monthlySpendings: monthlySpendings,
          monthlyComparisons: comparisons,
          monthlyExpenseAmount: currentTotal,
        );
      }
    } catch (e) {
      state = state.copyWith(errorMessage: e.toString());
    }
  }

  void togglePeriod(bool isWeekly) {
    state = state.copyWith(isWeekly: isWeekly);
    _loadPeriodData();
  }

  void navigatePeriod(int delta) {
    if (state.isWeekly) {
      state = state.copyWith(
        selectedWeekStart: state.selectedWeekStart.add(Duration(days: 7 * delta)),
      );
    } else {
      final current = state.selectedMonthStart;
      state = state.copyWith(
        selectedMonthStart: DateTime(current.year, current.month + delta, 1),
      );
    }
    _loadPeriodData();
  }

  void setTouchedPieIndex(int? index) {
    state = state.copyWith(touchedPieIndex: index);
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
