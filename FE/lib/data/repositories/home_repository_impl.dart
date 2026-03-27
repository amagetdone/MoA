import 'package:more_app/data/datasources/local/app_local_datasource.dart';
import 'package:more_app/data/datasources/remote/goal_remote_datasource.dart';
import 'package:more_app/data/datasources/remote/home_remote_datasource.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/entities/home_dashboard.dart';
import 'package:more_app/domain/repositories/home_repository.dart';

class HomeRepositoryImpl implements HomeRepository {
  HomeRepositoryImpl({
    required HomeRemoteDataSource remoteDataSource,
    required GoalRemoteDataSource goalRemoteDataSource,
    required AppLocalDataSource localDataSource,
  }) : _remoteDataSource = remoteDataSource,
       _goalRemoteDataSource = goalRemoteDataSource,
       _localDataSource = localDataSource;

  final HomeRemoteDataSource _remoteDataSource;
  final GoalRemoteDataSource _goalRemoteDataSource;
  final AppLocalDataSource _localDataSource;

  @override
  Future<HomeDashboard> fetchHomeDashboard() async {
    try {
      final dashboard = await _remoteDataSource.fetchHomeDashboard();
      final cachedGoals = await _loadGoalSummaries();
      var activeGoals = _mergeCachedGoalDetails(
        remoteGoals: dashboard.activeGoals,
        cachedGoals: cachedGoals,
      );
      activeGoals = await _hydratePrimaryGoalDetail(activeGoals);
      final metrics = await _resolveGoalBasedMetrics(
        dashboard: dashboard,
        activeGoals: activeGoals,
      );
      final correctedActiveGoals = _applyHomeMetricsToPrimaryGoal(
        activeGoals,
        metrics,
      );

      return HomeDashboard(
        accounts: dashboard.accounts,
        activeGoals: correctedActiveGoals,
        totalAsset: dashboard.totalAsset,
        weeklyBudget: metrics?.weeklyBudget ?? dashboard.weeklyBudget,
        weeklySpent: dashboard.weeklySpent,
        weeklyRemaining: metrics?.weeklyRemaining ?? dashboard.weeklyRemaining,
        daysRemaining: dashboard.daysRemaining,
        dailyAvailable: metrics?.dailyAvailable ?? dashboard.dailyAvailable,
        weeklySaved: metrics?.weeklySaved ?? dashboard.weeklySaved,
        savingTargetAmount:
            metrics?.savingTargetAmount ?? dashboard.savingTargetAmount,
        savingProgressRate:
            metrics?.savingProgressRate ?? dashboard.savingProgressRate,
      );
    } catch (_) {
      return const HomeDashboard();
    }
  }

  Future<List<Goal>> _loadGoalSummaries() async {
    final cachedGoals = await _localDataSource.getGoals();

    try {
      final remoteGoals = await _goalRemoteDataSource.fetchGoals();
      final cachedById = {for (final goal in cachedGoals) goal.id: goal};
      final mergedGoals = remoteGoals.map((goal) {
        final cached = cachedById[goal.id];
        if (cached == null) {
          return goal;
        }

        return goal.copyWith(
          description: cached.description,
          startDate: cached.startDate,
          endDate: cached.endDate,
          iconName: cached.iconName ?? goal.iconName,
          sketchVideoAssetPath:
              cached.sketchVideoAssetPath ?? goal.sketchVideoAssetPath,
          dDay: cached.dDay,
          weekNumber: cached.weekNumber,
          dday: cached.dday,
          achievementRateOverride:
              goal.achievementRateOverride ?? cached.achievementRateOverride,
          graphData: cached.graphData,
          weeklyPlans: cached.weeklyPlans,
          weeklyMission: cached.weeklyMission,
          weeklyBudget: cached.weeklyBudget,
          currentWeekOverride: cached.currentWeekOverride,
          totalWeeksOverride: cached.totalWeeksOverride,
          detailLoaded: cached.detailLoaded,
          apiStatus: goal.apiStatus ?? cached.apiStatus,
          aiPipelineStatus: goal.aiPipelineStatus ?? cached.aiPipelineStatus,
        );
      }).toList();

      await _localDataSource.replaceGoals(mergedGoals);
      return mergedGoals;
    } catch (_) {
      return cachedGoals;
    }
  }

  List<Goal> _mergeCachedGoalDetails({
    required List<Goal> remoteGoals,
    required List<Goal> cachedGoals,
  }) {
    if (remoteGoals.isEmpty) {
      return const [];
    }

    final cachedById = {
      for (final goal in cachedGoals.where(
        (goal) => goal.isEffectivelyInProgress,
      ))
        goal.id: goal,
    };

    final mergedGoals = remoteGoals.map((goal) {
      final cached = cachedById[goal.id];
      if (cached == null) {
        return goal;
      }

      final resolvedWeeklyTarget =
          _resolveGoalWeeklyTarget(cached) ?? _resolveGoalWeeklyTarget(goal);
      final resolvedWeeklySaved = _resolveGoalWeeklySaved(
        goal: cached,
        targetAmount: resolvedWeeklyTarget,
        fallback: goal.weeklySavedAmountOverride,
      );

      var merged = goal.copyWith(
        targetAmount: cached.targetAmount,
        currentAmount: cached.currentAmount,
        startDate: cached.startDate,
        endDate: cached.endDate,
        status: cached.status,
        iconName: cached.iconName,
        sketchVideoAssetPath: cached.sketchVideoAssetPath,
        dday: cached.dday,
        achievementRateOverride: cached.achievementRateOverride,
        apiProgressRate: cached.apiProgressRate,
        graphData: cached.graphData,
        weeklyPlans: cached.weeklyPlans,
        weeklyMission: cached.weeklyMission,
        weeklyBudget: cached.weeklyBudget,
        weeklySavedAmountOverride: resolvedWeeklySaved,
        weeklyTargetAmountOverride: resolvedWeeklyTarget,
        currentWeekOverride: cached.currentWeekOverride,
        totalWeeksOverride: cached.totalWeeksOverride,
        detailLoaded: cached.detailLoaded,
        apiStatus: cached.apiStatus,
        aiPipelineStatus: cached.aiPipelineStatus,
      );

      if (merged.isAiPending || merged.isAiFailed) {
        merged = merged.copyWith(clearSketchVideoAssetPath: true);
      }

      return merged;
    }).toList()..sort((a, b) => a.priority.compareTo(b.priority));

    return mergedGoals;
  }

  Future<List<Goal>> _hydratePrimaryGoalDetail(List<Goal> activeGoals) async {
    if (activeGoals.isEmpty) {
      return activeGoals;
    }

    final targetIndex = activeGoals.indexWhere(
      (goal) =>
          goal.isEffectivelyInProgress &&
          (!goal.hasDetailData ||
              goal.weeklyMission == null ||
              goal.weeklyBudget == null),
    );

    if (targetIndex == -1) {
      return activeGoals;
    }

    final targetGoal = activeGoals[targetIndex];

    try {
      final detailedGoal = await _goalRemoteDataSource.findGoalById(
        targetGoal.id,
      );
      if (detailedGoal == null) {
        return activeGoals;
      }

      final mergedGoal = _mergeDetailedGoal(
        targetGoal: targetGoal,
        detailedGoal: detailedGoal,
      );
      await _saveGoalLocally(mergedGoal);

      final updatedGoals = List<Goal>.from(activeGoals);
      updatedGoals[targetIndex] = mergedGoal;
      updatedGoals.sort((a, b) => a.priority.compareTo(b.priority));
      return updatedGoals;
    } catch (_) {
      return activeGoals;
    }
  }

  Goal _mergeDetailedGoal({
    required Goal targetGoal,
    required Goal detailedGoal,
  }) {
    final resolvedWeeklyTarget =
        _resolveGoalWeeklyTarget(detailedGoal) ??
        _resolveGoalWeeklyTarget(targetGoal);
    final resolvedWeeklySaved = _resolveGoalWeeklySaved(
      goal: detailedGoal,
      targetAmount: resolvedWeeklyTarget,
      fallback: targetGoal.weeklySavedAmountOverride,
    );

    return detailedGoal.copyWith(
      dDay: targetGoal.dDay,
      apiProgressRate:
          targetGoal.apiProgressRate ?? detailedGoal.apiProgressRate,
      weeklySavedAmountOverride: resolvedWeeklySaved,
      weeklyTargetAmountOverride: resolvedWeeklyTarget,
      apiStatus: targetGoal.apiStatus ?? detailedGoal.apiStatus,
      aiPipelineStatus:
          targetGoal.aiPipelineStatus ?? detailedGoal.aiPipelineStatus,
    );
  }

  Future<void> _saveGoalLocally(Goal goal) async {
    final existingGoal = await _localDataSource.getGoalById(goal.id);
    if (existingGoal == null) {
      await _localDataSource.createGoal(goal);
      return;
    }

    await _localDataSource.updateGoal(goal);
  }

  Future<_HomeGoalMetrics?> _resolveGoalBasedMetrics({
    required HomeDashboard dashboard,
    required List<Goal> activeGoals,
  }) async {
    if (activeGoals.isEmpty) {
      return null;
    }

    final primaryGoal = activeGoals.firstWhere(
      (goal) => goal.isEffectivelyInProgress,
      orElse: () => activeGoals.first,
    );

    final savingTargetAmount =
        _resolveGoalWeeklyTarget(primaryGoal) ?? dashboard.savingTargetAmount;
    final plannedFreeAmount = primaryGoal.weeklyBudget?.freeAmount ?? 0;

    if (savingTargetAmount <= 0 && plannedFreeAmount <= 0) {
      return null;
    }

    final historyBasedSaving = await _resolveWeeklySavedFromHistory(
      primaryGoal,
    );
    final progressRate = dashboard.savingProgressRate.clamp(0, 100);
    final progressBasedSaving = savingTargetAmount > 0
        ? ((savingTargetAmount * progressRate) / 100).round()
        : 0;

    var weeklySaved = historyBasedSaving ?? dashboard.weeklySaved;
    if (historyBasedSaving == null &&
        savingTargetAmount > 0 &&
        (weeklySaved < 0 || weeklySaved > savingTargetAmount)) {
      weeklySaved = progressBasedSaving;
    }

    if (weeklySaved < 0) {
      weeklySaved = progressBasedSaving;
    }

    if (savingTargetAmount > 0 && weeklySaved > savingTargetAmount) {
      weeklySaved = progressBasedSaving;
    }

    if (savingTargetAmount > 0) {
      weeklySaved = weeklySaved.clamp(0, savingTargetAmount);
    }

    final weeklyBudget = plannedFreeAmount > 0
        ? plannedFreeAmount
        : dashboard.weeklyBudget;
    final weeklyRemaining = plannedFreeAmount > 0
        ? (plannedFreeAmount - dashboard.weeklySpent).clamp(
            0,
            plannedFreeAmount,
          )
        : dashboard.weeklyRemaining;
    final dailyAvailable = dashboard.daysRemaining > 0
        ? (weeklyRemaining / dashboard.daysRemaining).floor()
        : weeklyRemaining;
    final savingProgressRate = savingTargetAmount > 0
        ? ((weeklySaved / savingTargetAmount) * 100).round().clamp(0, 100)
        : dashboard.savingProgressRate;

    return _HomeGoalMetrics(
      weeklyBudget: weeklyBudget,
      weeklyRemaining: weeklyRemaining,
      dailyAvailable: dailyAvailable,
      weeklySaved: weeklySaved,
      savingTargetAmount: savingTargetAmount,
      savingProgressRate: savingProgressRate,
    );
  }

  Future<int?> _resolveWeeklySavedFromHistory(Goal goal) async {
    try {
      final history = await _localDataSource.getGoalSavingsHistory(goal.id);
      if (history.isEmpty) {
        return null;
      }

      final weekStart = DateTime(
        goal.startDate.year,
        goal.startDate.month,
        goal.startDate.day,
      ).add(Duration(days: (goal.currentWeekNumber - 1) * 7));
      final weekEnd = weekStart.add(const Duration(days: 7));

      final weeklySaved = history.fold<int>(0, (sum, item) {
        if (_isDateInRange(
          item.date,
          start: weekStart,
          endExclusive: weekEnd,
        )) {
          return sum + item.amount;
        }
        return sum;
      });

      return weeklySaved > 0 ? weeklySaved : null;
    } catch (_) {
      return null;
    }
  }

  bool _isDateInRange(
    DateTime date, {
    required DateTime start,
    required DateTime endExclusive,
  }) {
    final normalizedDate = DateTime(date.year, date.month, date.day);
    return !normalizedDate.isBefore(start) &&
        normalizedDate.isBefore(endExclusive);
  }

  List<Goal> _applyHomeMetricsToPrimaryGoal(
    List<Goal> activeGoals,
    _HomeGoalMetrics? metrics,
  ) {
    if (metrics == null || activeGoals.isEmpty) {
      return activeGoals;
    }

    final primaryIndex = activeGoals.indexWhere(
      (goal) => goal.isEffectivelyInProgress,
    );

    if (primaryIndex == -1) {
      return activeGoals;
    }

    final updatedGoals = List<Goal>.from(activeGoals);
    final primaryGoal = updatedGoals[primaryIndex];
    updatedGoals[primaryIndex] = primaryGoal.copyWith(
      weeklySavedAmountOverride: metrics.weeklySaved,
      weeklyTargetAmountOverride: metrics.savingTargetAmount,
    );
    return updatedGoals;
  }

  int? _resolveGoalWeeklyTarget(Goal goal) {
    final missionAmount = goal.weeklyMission?.weeklySavingsAmount ?? 0;
    if (missionAmount > 0) {
      return missionAmount;
    }

    final savingAmount = goal.weeklyBudget?.savingAmount ?? 0;
    if (savingAmount > 0) {
      return savingAmount;
    }

    final override = goal.weeklyTargetAmountOverride;
    if (override != null && override > 0) {
      return override;
    }

    return null;
  }

  int _resolveGoalWeeklySaved({
    required Goal goal,
    required int? targetAmount,
    int? fallback,
  }) {
    final normalizedTarget = targetAmount ?? 0;

    final override = goal.weeklySavedAmountOverride;
    if (override != null &&
        override >= 0 &&
        (normalizedTarget <= 0 || override <= normalizedTarget)) {
      return override;
    }

    final savingAmount = goal.weeklyBudget?.savingAmount ?? 0;
    if (savingAmount > 0 &&
        (normalizedTarget <= 0 || savingAmount <= normalizedTarget)) {
      return savingAmount;
    }

    if (fallback != null &&
        fallback >= 0 &&
        (normalizedTarget <= 0 || fallback <= normalizedTarget)) {
      return fallback;
    }

    return 0;
  }
}

class _HomeGoalMetrics {
  const _HomeGoalMetrics({
    required this.weeklyBudget,
    required this.weeklyRemaining,
    required this.dailyAvailable,
    required this.weeklySaved,
    required this.savingTargetAmount,
    required this.savingProgressRate,
  });

  final int weeklyBudget;
  final int weeklyRemaining;
  final int dailyAvailable;
  final int weeklySaved;
  final int savingTargetAmount;
  final int savingProgressRate;
}
