import 'dart:typed_data';

import 'package:more_app/data/datasources/local/app_local_datasource.dart';
import 'package:more_app/data/datasources/remote/goal_remote_datasource.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/entities/spending.dart';
import 'package:more_app/domain/repositories/goal_repository.dart';

class GoalRepositoryImpl implements GoalRepository {
  GoalRepositoryImpl({
    required GoalRemoteDataSource remoteDataSource,
    required AppLocalDataSource localDataSource,
  }) : _remoteDataSource = remoteDataSource,
       _localDataSource = localDataSource;

  final GoalRemoteDataSource _remoteDataSource;
  final AppLocalDataSource _localDataSource;

  @override
  Future<List<Goal>> fetchGoals() async {
    try {
      return await _remoteDataSource.fetchGoals();
    } catch (_) {
      return _localDataSource.getGoals();
    }
  }

  @override
  Future<Goal?> findGoalById(String id) async {
    try {
      return await _remoteDataSource.findGoalById(id);
    } catch (_) {
      return _localDataSource.getGoalById(id);
    }
  }

  @override
  Future<Map<String, List<DailySpending>>>
  fetchGoalSavingsHistoryByGoalId() async {
    try {
      return await _remoteDataSource.fetchGoalSavingsHistoryByGoalId();
    } catch (_) {
      return _localDataSource.getGoalSavingsHistoryByGoalId();
    }
  }

  @override
  Future<Uint8List?> getGoalSketchVideo(String goalId) async {
    return _localDataSource.getGoalSketchVideo(goalId);
  }

  @override
  Future<Goal> createGoal(Goal goal) async {
    try {
      return await _remoteDataSource.createGoal(goal);
    } catch (_) {
      return _localDataSource.createGoal(goal);
    }
  }

  @override
  Future<Goal> updateGoal(Goal goal) async {
    try {
      return await _remoteDataSource.updateGoal(goal);
    } catch (_) {
      return _localDataSource.updateGoal(goal);
    }
  }

  @override
  Future<List<Goal>> saveGoalPriority(List<Goal> goals) async {
    try {
      return await _remoteDataSource.saveGoalPriority(goals);
    } catch (_) {
      return _localDataSource.saveGoalPriority(goals);
    }
  }
}
