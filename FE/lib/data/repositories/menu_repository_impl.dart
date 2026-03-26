import 'package:more_app/data/datasources/local/app_local_datasource.dart';
import 'package:more_app/data/datasources/remote/menu_remote_datasource.dart';
import 'package:more_app/data/models/app_snapshot_dto.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/entities/menu_summary.dart';
import 'package:more_app/domain/repositories/menu_repository.dart';

class MenuRepositoryImpl implements MenuRepository {
  MenuRepositoryImpl({
    required MenuRemoteDataSource remoteDataSource,
    required AppLocalDataSource localDataSource,
  }) : _remoteDataSource = remoteDataSource,
       _localDataSource = localDataSource;

  final MenuRemoteDataSource _remoteDataSource;
  final AppLocalDataSource _localDataSource;

  @override
  Future<MenuSummary> fetchMenuSummary() async {
    final preferences = await _localDataSource.getMenuPreferences();

    try {
      final summary = await _remoteDataSource.fetchMenuDashboard();
      return MenuSummary(
        user: summary.user,
        activeGoalCount: summary.activeGoalCount,
        achievedGoalCount: summary.achievedGoalCount,
        totalSaved: summary.totalSaved,
        notificationEnabled: preferences.notificationEnabled,
        biometricEnabled: preferences.biometricEnabled,
      );
    } catch (e) {
      final snapshot = await _localDataSource.getCachedSnapshot();
      return _toMenuSummary(snapshot);
    }
  }

  @override
  Future<MenuSummary> updateNotification(bool enabled) async {
    await _localDataSource.updateMenuPreferences(
      notificationEnabled: enabled,
    );

    try {
      await _remoteDataSource.updateMenuPreferences(
        notificationEnabled: enabled,
      );
    } catch (_) {}

    return fetchMenuSummary();
  }

  @override
  Future<MenuSummary> updateBiometric(bool enabled) async {
    await _localDataSource.updateMenuPreferences(biometricEnabled: enabled);

    try {
      await _remoteDataSource.updateMenuPreferences(
        biometricEnabled: enabled,
      );
    } catch (_) {}

    return fetchMenuSummary();
  }

  MenuSummary _toMenuSummary(AppSnapshotDto snapshot) {
    final activeGoalCount = snapshot.goals
        .where((goal) => goal.status == GoalStatus.inProgress)
        .length;
    final achievedGoalCount = snapshot.goals
        .where((goal) => goal.status == GoalStatus.achieved)
        .length;
    final totalSaved = snapshot.goals.fold(0, (sum, goal) {
      return sum + goal.currentAmount;
    });

    return MenuSummary(
      user: snapshot.user,
      activeGoalCount: activeGoalCount,
      achievedGoalCount: achievedGoalCount,
      totalSaved: totalSaved,
      notificationEnabled: snapshot.menuPreferences.notificationEnabled,
      biometricEnabled: snapshot.menuPreferences.biometricEnabled,
    );
  }
}
