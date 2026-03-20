import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:more_app/domain/entities/spending.dart';
import 'package:more_app/domain/repositories/spending_repository.dart';
import 'package:more_app/presentation/providers/app_data_providers.dart';

class SpendingPatternState {
  final bool isLoading;
  final String? errorMessage;
  final SpendingPatternResult result;

  const SpendingPatternState({
    this.isLoading = false,
    this.errorMessage,
    this.result = const SpendingPatternResult(),
  });

  SpendingPatternState copyWith({
    bool? isLoading,
    String? errorMessage,
    SpendingPatternResult? result,
  }) {
    return SpendingPatternState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage ?? this.errorMessage,
      result: result ?? this.result,
    );
  }
}

class SpendingPatternViewModel extends StateNotifier<SpendingPatternState> {
  SpendingPatternViewModel(this._repository)
      : super(const SpendingPatternState(isLoading: true)) {
    loadData();
  }

  final SpendingRepository _repository;

  Future<void> loadData() async {
    state = state.copyWith(isLoading: true);
    try {
      final now = DateTime.now();
      final result = await _repository.fetchThreeMonthPattern(
        DateTime(now.year, now.month, 1),
      );
      state = state.copyWith(isLoading: false, result: result);
    } catch (_) {
      state = state.copyWith(isLoading: false);
    }
  }
}

final spendingPatternViewModelProvider =
    StateNotifierProvider<SpendingPatternViewModel, SpendingPatternState>((ref) {
  return SpendingPatternViewModel(ref.read(spendingRepositoryProvider));
});