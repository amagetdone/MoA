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
    try {
      return await _remoteDataSource.fetchMenuDashboard();
    } catch (_) {
      final snapshot = await _localDataSource.getCachedSnapshot();
      return _toMenuSummary(snapshot);
    }
  }

  @override
  Future<MenuSummary> updateNotification(bool enabled) async {
    try {
      return await _remoteDataSource.updateMenuPreferences(
        notificationEnabled: enabled,
      );
    } catch (_) {
      await _localDataSource.updateMenuPreferences(
        notificationEnabled: enabled,
      );
      return fetchMenuSummary();
    }
  }

  @override
  Future<MenuSummary> updateBiometric(bool enabled) async {
    try {
      return await _remoteDataSource.updateMenuPreferences(
        biometricEnabled: enabled,
      );
    } catch (_) {
      await _localDataSource.updateMenuPreferences(biometricEnabled: enabled);
      return fetchMenuSummary();
    }
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
