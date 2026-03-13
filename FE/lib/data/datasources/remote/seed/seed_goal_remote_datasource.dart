import 'package:more_app/data/datasources/remote/goal_remote_datasource.dart';
import 'package:more_app/data/datasources/remote/seed/seed_data_store.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/entities/spending.dart';

class SeedGoalRemoteDataSource implements GoalRemoteDataSource {
  SeedGoalRemoteDataSource({required SeedDataStore dataStore})
    : _dataStore = dataStore;

  final SeedDataStore _dataStore;

  @override
  Future<List<Goal>> fetchGoals() async {
    await Future.delayed(const Duration(milliseconds: 220));
    final snapshot = _dataStore.clone(await _dataStore.ensureSessionSnapshot());
    final goals = List<Goal>.from(snapshot.goals);
    goals.sort((a, b) => a.priority.compareTo(b.priority));
    return goals;
  }

  @override
  Future<Goal?> findGoalById(String id) async {
    await Future.delayed(const Duration(milliseconds: 220));
    final snapshot = await _dataStore.ensureSessionSnapshot();
    for (final goal in snapshot.goals) {
      if (goal.id == id) {
        return goal;
      }
    }
    return null;
  }

  @override
  Future<Map<String, List<DailySpending>>>
  fetchGoalSavingsHistoryByGoalId() async {
    await Future.delayed(const Duration(milliseconds: 220));
    final snapshot = _dataStore.clone(await _dataStore.ensureSessionSnapshot());
    return snapshot.goalSavingsHistoryByGoalId.map(
      (goalId, history) => MapEntry(goalId, List<DailySpending>.from(history)),
    );
  }

  @override
  Future<Goal> createGoal(Goal goal) async {
    final snapshot = await _dataStore.mutate((snapshot) {
      final updatedGoals = List<Goal>.from(snapshot.goals)..add(goal);
      final updatedHistory = Map<String, List<DailySpending>>.from(
        snapshot.goalSavingsHistoryByGoalId,
      )..putIfAbsent(goal.id, () => const []);

      return snapshot.copyWith(
        goals: updatedGoals,
        goalSavingsHistoryByGoalId: updatedHistory,
      );
    });

    return snapshot.goals.firstWhere((item) => item.id == goal.id);
  }

  @override
  Future<Goal> updateGoal(Goal goal) async {
    final snapshot = await _dataStore.mutate((snapshot) {
      final updatedGoals = List<Goal>.from(snapshot.goals);
      final index = updatedGoals.indexWhere((item) => item.id == goal.id);

      if (index == -1) {
        throw StateError('목표를 찾을 수 없습니다.');
      }

      updatedGoals[index] = goal;
      return snapshot.copyWith(goals: updatedGoals);
    });

    return snapshot.goals.firstWhere((item) => item.id == goal.id);
  }

  @override
  Future<List<Goal>> saveGoalPriority(List<Goal> goals) async {
    final snapshot = await _dataStore.mutate((snapshot) {
      final prioritizedGoals = <String, Goal>{};

      for (var index = 0; index < goals.length; index++) {
        prioritizedGoals[goals[index].id] = goals[index].copyWith(
          priority: index + 1,
        );
      }

      final updatedGoals =
          snapshot.goals
              .map((goal) => prioritizedGoals[goal.id] ?? goal)
              .toList()
            ..sort((a, b) => a.priority.compareTo(b.priority));

      return snapshot.copyWith(goals: updatedGoals);
    });

    final result = List<Goal>.from(snapshot.goals);
    result.sort((a, b) => a.priority.compareTo(b.priority));
    return result;
  }
}
