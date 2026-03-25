import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/entities/spending.dart';
import 'package:more_app/domain/repositories/goal_repository.dart';
import 'package:more_app/presentation/providers/app_data_providers.dart';

class GoalState {
  final bool isLoading;
  final String? errorMessage;
  final List<Goal> goals;
  final Map<String, List<DailySpending>> savingsHistoryByGoalId;
  final bool showAchieved;
  final Set<String> loadingDetailGoalIds;

  const GoalState({
    this.isLoading = false,
    this.errorMessage,
    this.goals = const [],
    this.savingsHistoryByGoalId = const {},
    this.showAchieved = false,
    this.loadingDetailGoalIds = const <String>{},
  });

  List<Goal> get filteredGoals => showAchieved
      ? goals.where((g) => g.status == GoalStatus.achieved).toList()
      : goals.where((g) => g.status == GoalStatus.inProgress).toList();

  List<Goal> get inProgressGoals =>
      goals.where((g) => g.status == GoalStatus.inProgress).toList()
        ..sort((a, b) => a.priority.compareTo(b.priority));

  Goal? findGoalById(String id) {
    for (final goal in goals) {
      if (goal.id == id) {
        return goal;
      }
    }
    return null;
  }

  List<DailySpending> savingsHistoryFor(String goalId) {
    return savingsHistoryByGoalId[goalId] ?? const [];
  }

  bool isDetailLoading(String goalId) => loadingDetailGoalIds.contains(goalId);

  GoalState copyWith({
    bool? isLoading,
    String? errorMessage,
    bool clearErrorMessage = false,
    List<Goal>? goals,
    Map<String, List<DailySpending>>? savingsHistoryByGoalId,
    bool? showAchieved,
    Set<String>? loadingDetailGoalIds,
  }) {
    return GoalState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: clearErrorMessage
          ? null
          : errorMessage ?? this.errorMessage,
      goals: goals ?? this.goals,
      savingsHistoryByGoalId:
          savingsHistoryByGoalId ?? this.savingsHistoryByGoalId,
      showAchieved: showAchieved ?? this.showAchieved,
      loadingDetailGoalIds: loadingDetailGoalIds ?? this.loadingDetailGoalIds,
    );
  }
}

class GoalViewModel extends StateNotifier<GoalState> {
  GoalViewModel(this._repository) : super(const GoalState(isLoading: true)) {
    loadGoals();
  }

  final GoalRepository _repository;

  Future<void> loadGoals() async {
    state = state.copyWith(isLoading: true, clearErrorMessage: true);

    try {
      final goals = await _repository.fetchGoals();
      final histories = await _repository.fetchGoalSavingsHistoryByGoalId();
      state = state.copyWith(
        isLoading: false,
        goals: goals,
        savingsHistoryByGoalId: histories,
      );
    } catch (error) {
      state = state.copyWith(isLoading: false, errorMessage: error.toString());
    }
  }

  Future<void> loadGoalDetail(String goalId) async {
    if (state.loadingDetailGoalIds.contains(goalId)) {
      return;
    }

    state = state.copyWith(
      loadingDetailGoalIds: {...state.loadingDetailGoalIds, goalId},
      clearErrorMessage: true,
    );

    try {
      final detailedGoal = await _repository.findGoalById(goalId);
      if (detailedGoal != null) {
        final existingIndex = state.goals.indexWhere((item) => item.id == goalId);
        final updatedGoals = List<Goal>.from(state.goals);
        if (existingIndex == -1) {
          updatedGoals.add(detailedGoal);
        } else {
          updatedGoals[existingIndex] = detailedGoal;
        }

        state = state.copyWith(goals: updatedGoals);
      }
    } catch (error) {
      state = state.copyWith(errorMessage: error.toString());
    } finally {
      final remaining = Set<String>.from(state.loadingDetailGoalIds)
        ..remove(goalId);
      state = state.copyWith(loadingDetailGoalIds: remaining);
    }
  }

  void toggleFilter(bool showAchieved) {
    state = state.copyWith(showAchieved: showAchieved);
  }

  Future<void> addGoal(Goal goal) async {
    final createdGoal = await _repository.createGoal(goal);
    state = state.copyWith(
      goals: [...state.goals, createdGoal],
      savingsHistoryByGoalId: {
        ...state.savingsHistoryByGoalId,
        createdGoal.id: const [],
      },
    );
  }

  Future<void> updateGoal(Goal goal) async {
    try {
      final updatedGoal = await _repository.updateGoal(goal);
      state = state.copyWith(
        clearErrorMessage: true,
        goals: state.goals
            .map((item) => item.id == updatedGoal.id ? updatedGoal : item)
            .toList(),
      );
    } catch (error) {
      state = state.copyWith(errorMessage: error.toString());
      rethrow;
    }
  }

  Future<void> retryGoalVideoGeneration(String goalId) async {
    final goal = state.findGoalById(goalId);
    if (goal == null) {
      return;
    }

    final pendingGoal = goal.copyWith(
      aiPipelineStatus: GoalAiPipelineStatus.pending,
      clearSketchVideoAssetPath: true,
    );

    state = state.copyWith(
      clearErrorMessage: true,
      goals: state.goals
          .map((item) => item.id == goalId ? pendingGoal : item)
          .toList(),
    );

    try {
      final updatedGoal = await _repository.updateGoal(goal);
      state = state.copyWith(
        goals: state.goals
            .map((item) => item.id == goalId ? updatedGoal : item)
            .toList(),
      );
    } catch (error) {
      state = state.copyWith(
        errorMessage: error.toString(),
        goals: state.goals
            .map((item) => item.id == goalId ? goal : item)
            .toList(),
      );
      rethrow;
    }
  }

  Future<void> removeGoal(String goalId) async {
    try {
      await _repository.deleteGoal(goalId);
      final updatedHistory = Map<String, List<DailySpending>>.from(
        state.savingsHistoryByGoalId,
      )..remove(goalId);
      state = state.copyWith(
        goals: state.goals.where((g) => g.id != goalId).toList(),
        savingsHistoryByGoalId: updatedHistory,
      );
    } catch (error) {
      state = state.copyWith(errorMessage: error.toString());
    }
  }

  Future<void> reorderGoals(List<Goal> reordered) async {
    final reorderedGoals = await _repository.saveGoalPriority(reordered);
    state = state.copyWith(goals: reorderedGoals);
  }
}

final goalViewModelProvider = StateNotifierProvider<GoalViewModel, GoalState>((
  ref,
) {
  return GoalViewModel(ref.read(goalRepositoryProvider));
});
