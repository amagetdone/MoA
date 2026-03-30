import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:more_app/domain/entities/spending.dart';
import 'package:more_app/domain/repositories/spending_repository.dart';
import 'package:more_app/presentation/providers/app_data_providers.dart';

class SpendingPatternState {
  final bool isLoading;
  final String? errorMessage;
  final SpendingPatternResult result;
  final int monthOffset; // 0 = 지난 달, 1 = 2달 전, ...

  const SpendingPatternState({
    this.isLoading = false,
    this.errorMessage,
    this.result = const SpendingPatternResult(),
    this.monthOffset = 0,
  });

  SpendingPatternState copyWith({
    bool? isLoading,
    String? errorMessage,
    SpendingPatternResult? result,
    int? monthOffset,
  }) {
    return SpendingPatternState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage ?? this.errorMessage,
      result: result ?? this.result,
      monthOffset: monthOffset ?? this.monthOffset,
    );
  }
}

class SpendingPatternViewModel extends StateNotifier<SpendingPatternState> {
  SpendingPatternViewModel(this._repository)
      : super(const SpendingPatternState(isLoading: true)) {
    loadData();
  }

  final SpendingRepository _repository;
  static const _maxOffset = 11;

  Future<void> loadData({int? offset}) async {
    final newOffset = (offset ?? state.monthOffset).clamp(0, _maxOffset).toInt();
    state = state.copyWith(isLoading: true, monthOffset: newOffset);
    try {
      final now = DateTime.now();
      // fetchPreviousMonthPattern 내부에서 -1달 하므로 offset만큼 추가로 빼줌
      final result = await _repository.fetchPreviousMonthPattern(
        DateTime(now.year, now.month - newOffset, 1),
      );
      state = state.copyWith(isLoading: false, result: result);
    } catch (_) {
      state = state.copyWith(isLoading: false);
    }
  }

  void goToPreviousMonth() {
    if (state.monthOffset < _maxOffset) {
      loadData(offset: state.monthOffset + 1);
    }
  }

  void goToNextMonth() {
    if (state.monthOffset > 0) {
      loadData(offset: state.monthOffset - 1);
    }
  }
}

final spendingPatternViewModelProvider =
    StateNotifierProvider<SpendingPatternViewModel, SpendingPatternState>((ref) {
  return SpendingPatternViewModel(ref.read(spendingRepositoryProvider));
});
