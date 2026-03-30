import 'dart:typed_data';

import 'package:more_app/core/utils/goal_video_cache.dart';
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
      final goals = await _remoteDataSource.fetchGoals();
      final existingGoals = await _localDataSource.getGoals();
      final existingGoalById = {
        for (final goal in existingGoals) goal.id: goal,
      };
      final mergedGoals = goals
          .map((goal) => _mergeGoal(existingGoalById[goal.id], goal))
          .toList();
      await _localDataSource.replaceGoals(mergedGoals);
      return mergedGoals;
    } catch (_) {
      return _localDataSource.getGoals();
    }
  }

  @override
  Future<Goal?> findGoalById(String id) async {
    try {
      final goal = await _remoteDataSource.findGoalById(id);
      if (goal != null) {
        final existing = await _localDataSource.getGoalById(id);
        final mergedGoal = _mergeGoal(existing, goal);
        await _upsertGoal(mergedGoal);
        return mergedGoal;
      }
      return goal;
    } catch (_) {
      return _localDataSource.getGoalById(id);
    }
  }

  @override
  Future<void> deleteGoal(String id) async {
    try {
      await _remoteDataSource.deleteGoal(id);
    } finally {
      await GoalVideoCache.evict(id);
      await _localDataSource.removeGoal(id);
    }
  }

  @override
  Future<GoalWeeklyPlanPage> fetchGoalWeeklyPlans(
    String id, {
    int page = 0,
    int size = 100,
  }) async {
    try {
      return await _remoteDataSource.fetchGoalWeeklyPlans(
        id,
        page: page,
        size: size,
      );
    } catch (_) {
      final goal = await _localDataSource.getGoalById(id);
      final weeklyPlans = goal?.weeklyPlans ?? const <GoalWeeklyPlan>[];
      final start = page * size;
      final end = (start + size).clamp(0, weeklyPlans.length);
      final sliced = start >= weeklyPlans.length
          ? const <GoalWeeklyPlan>[]
          : weeklyPlans.sublist(start, end);
      final totalPages = weeklyPlans.isEmpty
          ? 0
          : ((weeklyPlans.length - 1) ~/ size) + 1;

      return GoalWeeklyPlanPage(
        goalId: id,
        weeklyPlans: sliced,
        page: page,
        size: size,
        totalElements: weeklyPlans.length,
        totalPages: totalPages,
        hasNext: page + 1 < totalPages,
      );
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
      final createdGoal = await _remoteDataSource.createGoal(goal);
      await _upsertGoal(createdGoal);
      return createdGoal;
    } catch (_) {
      return _localDataSource.createGoal(goal);
    }
  }

  @override
  Future<Goal> updateGoal(Goal goal) async {
    try {
      await GoalVideoCache.evict(goal.id);
      final updatedGoal = await _remoteDataSource.updateGoal(goal);
      await _localDataSource.updateGoal(updatedGoal);
      return updatedGoal;
    } catch (_) {
      rethrow;
    }
  }

  @override
  Future<List<Goal>> saveGoalPriority(List<Goal> goals) async {
    try {
      final reorderedGoals = await _remoteDataSource.saveGoalPriority(goals);
      await _localDataSource.replaceGoals(reorderedGoals);
      return reorderedGoals;
    } catch (_) {
      return _localDataSource.saveGoalPriority(goals);
    }
  }

  Future<void> _upsertGoal(Goal goal) async {
    final existing = await _localDataSource.getGoalById(goal.id);
    if (existing == null) {
      await _localDataSource.createGoal(goal);
      return;
    }

    await _localDataSource.updateGoal(goal);
  }

  Goal _mergeGoal(Goal? existing, Goal incoming) {
    if (existing == null) {
      return incoming;
    }

    var merged = incoming.copyWith(
      iconName: incoming.iconName ?? existing.iconName,
      sketchVideoAssetPath:
          incoming.sketchVideoAssetPath ?? existing.sketchVideoAssetPath,
      apiStatus: incoming.apiStatus ?? existing.apiStatus,
      aiPipelineStatus: incoming.aiPipelineStatus ?? existing.aiPipelineStatus,
    );

    if (merged.isAiPending || merged.isAiFailed) {
      merged = merged.copyWith(clearSketchVideoAssetPath: true);
    }

    return merged;
  }
}
