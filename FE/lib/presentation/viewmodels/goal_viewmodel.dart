import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:more_app/core/utils/mock_data.dart';
import 'package:more_app/domain/entities/goal.dart';

class GoalState {
  final List<Goal> goals;
  final bool showAchieved;

  const GoalState({
    this.goals = const [],
    this.showAchieved = false,
  });

  List<Goal> get filteredGoals => showAchieved
      ? goals.where((g) => g.status == GoalStatus.achieved).toList()
      : goals.where((g) => g.status == GoalStatus.inProgress).toList();

  GoalState copyWith({List<Goal>? goals, bool? showAchieved}) {
    return GoalState(
      goals: goals ?? this.goals,
      showAchieved: showAchieved ?? this.showAchieved,
    );
  }
}

class GoalViewModel extends StateNotifier<GoalState> {
  GoalViewModel() : super(const GoalState()) {
    _loadData();
  }

  void _loadData() {
    state = GoalState(goals: MockData.goals);
  }

  void toggleFilter(bool showAchieved) {
    state = state.copyWith(showAchieved: showAchieved);
  }

  void addGoal(Goal goal) {
    state = state.copyWith(goals: [...state.goals, goal]);
  }

  void updateGoal(Goal goal) {
    state = state.copyWith(
      goals: state.goals.map((g) => g.id == goal.id ? goal : g).toList(),
    );
  }

  void reorderGoals(List<Goal> reordered) {
    state = state.copyWith(goals: reordered);
  }
}

final goalViewModelProvider =
    StateNotifierProvider<GoalViewModel, GoalState>((ref) {
  return GoalViewModel();
});
