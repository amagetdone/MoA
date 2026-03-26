import 'package:more_app/data/datasources/local/app_local_datasource.dart';
import 'package:more_app/data/datasources/remote/home_remote_datasource.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/entities/home_dashboard.dart';
import 'package:more_app/domain/repositories/home_repository.dart';

class HomeRepositoryImpl implements HomeRepository {
  HomeRepositoryImpl({
    required HomeRemoteDataSource remoteDataSource,
    required AppLocalDataSource localDataSource,
  }) : _remoteDataSource = remoteDataSource,
       _localDataSource = localDataSource;

  final HomeRemoteDataSource _remoteDataSource;
  final AppLocalDataSource _localDataSource;

  @override
  Future<HomeDashboard> fetchHomeDashboard() async {
    try {
      final dashboard = await _remoteDataSource.fetchHomeDashboard();
      final cachedGoals = await _localDataSource.getGoals();
      final activeGoals = _mergeCachedGoalDetails(
        remoteGoals: dashboard.activeGoals,
        cachedGoals: cachedGoals,
      );

      return HomeDashboard(
        accounts: dashboard.accounts,
        activeGoals: activeGoals,
        totalAsset: dashboard.totalAsset,
        weeklyBudget: dashboard.weeklyBudget,
        weeklySpent: dashboard.weeklySpent,
        weeklyRemaining: dashboard.weeklyRemaining,
        daysRemaining: dashboard.daysRemaining,
        dailyAvailable: dashboard.dailyAvailable,
        weeklySaved: dashboard.weeklySaved,
        savingTargetAmount: dashboard.savingTargetAmount,
        savingProgressRate: dashboard.savingProgressRate,
      );
    } catch (_) {
      return const HomeDashboard();
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

      var merged = goal.copyWith(
        iconName: cached.iconName,
        sketchVideoAssetPath: cached.sketchVideoAssetPath,
        graphData: cached.graphData,
        weeklyPlans: cached.weeklyPlans,
        weeklyMission: cached.weeklyMission,
        weeklyBudget: cached.weeklyBudget,
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
}
