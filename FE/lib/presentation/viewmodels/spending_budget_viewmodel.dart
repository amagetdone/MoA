import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:more_app/domain/entities/budget.dart';
import 'package:more_app/domain/repositories/budget_repository.dart';
import 'package:more_app/presentation/providers/app_data_providers.dart';

class SpendingBudgetState {
  final bool isLoading;
  final bool isSaving;
  final BudgetSetup setup;

  const SpendingBudgetState({
    this.isLoading = true,
    this.isSaving = false,
    this.setup = const BudgetSetup(),
  });

  SpendingBudgetState copyWith({
    bool? isLoading,
    bool? isSaving,
    BudgetSetup? setup,
  }) {
    return SpendingBudgetState(
      isLoading: isLoading ?? this.isLoading,
      isSaving: isSaving ?? this.isSaving,
      setup: setup ?? this.setup,
    );
  }
}

class SpendingBudgetViewModel extends StateNotifier<SpendingBudgetState> {
  SpendingBudgetViewModel(this._repository)
      : super(const SpendingBudgetState()) {
    loadData();
  }

  final BudgetRepository _repository;

  Future<void> loadData() async {
    state = state.copyWith(isLoading: true);
    try {
      final setup = await _repository.fetchBudgetSetup();
      state = state.copyWith(isLoading: false, setup: setup);
    } catch (_) {
      state = state.copyWith(isLoading: false);
    }
  }

  Future<void> saveIncome(int monthlyIncome) async {
    state = state.copyWith(isSaving: true);
    try {
      final setup = await _repository.saveIncomeAndAllocate(monthlyIncome);
      state = state.copyWith(isSaving: false, setup: setup);
    } catch (e) {
      state = state.copyWith(isSaving: false);
      rethrow;
    }
  }
}

final spendingBudgetViewModelProvider =
    StateNotifierProvider<SpendingBudgetViewModel, SpendingBudgetState>((ref) {
  return SpendingBudgetViewModel(ref.read(budgetRepositoryProvider));
});