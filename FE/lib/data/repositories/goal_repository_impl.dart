import 'dart:typed_data';

import 'package:more_app/data/datasources/local/app_local_datasource.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/entities/spending.dart';
import 'package:more_app/domain/repositories/goal_repository.dart';

class GoalRepositoryImpl implements GoalRepository {
  GoalRepositoryImpl({required AppLocalDataSource localDataSource})
    : _localDataSource = localDataSource;

  final AppLocalDataSource _localDataSource;

  @override
  Future<List<Goal>> fetchGoals() async {
    final goals = await _localDataSource.getGoals();
    goals.sort((a, b) => a.priority.compareTo(b.priority));
    return goals;
  }

  @override
  Future<Goal?> findGoalById(String id) => _localDataSource.getGoalById(id);

  @override
  Future<Map<String, List<DailySpending>>> fetchGoalSavingsHistoryByGoalId() =>
      _localDataSource.getGoalSavingsHistoryByGoalId();

  @override
  Future<Uint8List?> getGoalSketchVideo(String goalId) =>
      _localDataSource.getGoalSketchVideo(goalId);

  @override
  Future<Goal> createGoal(Goal goal) => _localDataSource.createGoal(goal);

  @override
  Future<Goal> updateGoal(Goal goal) => _localDataSource.updateGoal(goal);

  @override
  Future<List<Goal>> saveGoalPriority(List<Goal> goals) =>
      _localDataSource.saveGoalPriority(goals);
}
