import 'package:more_app/data/datasources/remote/menu_remote_datasource.dart';
import 'package:more_app/data/datasources/remote/seed/seed_data_store.dart';
import 'package:more_app/data/models/app_snapshot_dto.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/entities/menu_summary.dart';

class SeedMenuRemoteDataSource implements MenuRemoteDataSource {
  SeedMenuRemoteDataSource({required SeedDataStore dataStore})
    : _dataStore = dataStore;

  final SeedDataStore _dataStore;

  @override
  Future<MenuSummary> fetchMenuDashboard() async {
    await Future.delayed(const Duration(milliseconds: 220));
    final snapshot = _dataStore.clone(await _dataStore.ensureSessionSnapshot());
    return _toMenuSummary(snapshot);
  }

  @override
  Future<MenuSummary> updateMenuPreferences({
    bool? notificationEnabled,
    bool? biometricEnabled,
  }) async {
    final snapshot = await _dataStore.mutate((snapshot) {
      final updatedPreferences = snapshot.menuPreferences.copyWith(
        notificationEnabled: notificationEnabled,
        biometricEnabled: biometricEnabled,
      );
      return snapshot.copyWith(menuPreferences: updatedPreferences);
    });
    return _toMenuSummary(snapshot);
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
