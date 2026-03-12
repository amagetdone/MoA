import 'dart:typed_data';

import 'package:more_app/data/datasources/local/app_local_datasource.dart';
import 'package:more_app/data/datasources/remote/app_remote_datasource.dart';
import 'package:more_app/data/models/app_snapshot.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/entities/spending.dart';
import 'package:more_app/domain/repositories/goal_repository.dart';

class GoalRepositoryImpl implements GoalRepository {
  GoalRepositoryImpl({
    required AppRemoteDataSource remoteDataSource,
    required AppLocalDataSource localDataSource,
  }) : _remoteDataSource = remoteDataSource,
       _localDataSource = localDataSource;

  final AppRemoteDataSource _remoteDataSource;
  final AppLocalDataSource _localDataSource;

  @override
  Future<List<Goal>> fetchGoals() async {
    final snapshot = await _loadSnapshot();
    return _sortedGoals(snapshot.goals);
  }

  @override
  Future<Goal?> findGoalById(String id) async {
    final snapshot = await _loadSnapshot();
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
    final snapshot = await _loadSnapshot();
    return snapshot.goalSavingsHistoryByGoalId.map(
      (goalId, history) => MapEntry(goalId, List<DailySpending>.from(history)),
    );
  }

  @override
  Future<Uint8List?> getGoalSketchVideo(String goalId) async {
    await _syncSnapshotSilently();
    return _localDataSource.getGoalSketchVideo(goalId);
  }

  @override
  Future<Goal> createGoal(Goal goal) async {
    try {
      final snapshot = await _remoteDataSource.createGoal(goal);
      await _localDataSource.saveCachedSnapshot(snapshot);
      return snapshot.goals.firstWhere((item) => item.id == goal.id);
    } catch (_) {
      return _localDataSource.createGoal(goal);
    }
  }

  @override
  Future<Goal> updateGoal(Goal goal) async {
    try {
      final snapshot = await _remoteDataSource.updateGoal(goal);
      await _localDataSource.saveCachedSnapshot(snapshot);
      return snapshot.goals.firstWhere((item) => item.id == goal.id);
    } catch (_) {
      return _localDataSource.updateGoal(goal);
    }
  }

  @override
  Future<List<Goal>> saveGoalPriority(List<Goal> goals) async {
    try {
      final snapshot = await _remoteDataSource.saveGoalPriority(goals);
      await _localDataSource.saveCachedSnapshot(snapshot);
      return _sortedGoals(snapshot.goals);
    } catch (_) {
      return _localDataSource.saveGoalPriority(goals);
    }
  }

  Future<AppSnapshot> _loadSnapshot() async {
    try {
      final snapshot = await _remoteDataSource.fetchSnapshot();
      await _localDataSource.saveCachedSnapshot(snapshot);
      return snapshot;
    } catch (_) {
      return _localDataSource.getCachedSnapshot();
    }
  }

  Future<void> _syncSnapshotSilently() async {
    try {
      final snapshot = await _remoteDataSource.fetchSnapshot();
      await _localDataSource.saveCachedSnapshot(snapshot);
    } catch (_) {}
  }

  List<Goal> _sortedGoals(List<Goal> goals) {
    final items = List<Goal>.from(goals);
    items.sort((a, b) => a.priority.compareTo(b.priority));
    return items;
  }
}
