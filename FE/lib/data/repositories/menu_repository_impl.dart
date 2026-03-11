import 'package:more_app/data/datasources/local/app_local_datasource.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/entities/menu_summary.dart';
import 'package:more_app/domain/repositories/menu_repository.dart';

class MenuRepositoryImpl implements MenuRepository {
  MenuRepositoryImpl({required AppLocalDataSource localDataSource})
    : _localDataSource = localDataSource;

  final AppLocalDataSource _localDataSource;

  @override
  Future<MenuSummary> fetchMenuSummary() async {
    final user = await _localDataSource.getUser();
    final preferences = await _localDataSource.getMenuPreferences();
    final goals = await _localDataSource.getGoals();

    final activeGoalCount = goals
        .where((goal) => goal.status == GoalStatus.inProgress)
        .length;
    final achievedGoalCount = goals
        .where((goal) => goal.status == GoalStatus.achieved)
        .length;
    final totalSaved = goals.fold(0, (sum, goal) => sum + goal.currentAmount);

    return MenuSummary(
      user: user,
      activeGoalCount: activeGoalCount,
      achievedGoalCount: achievedGoalCount,
      totalSaved: totalSaved,
      notificationEnabled: preferences.notificationEnabled,
      biometricEnabled: preferences.biometricEnabled,
    );
  }

  @override
  Future<MenuSummary> updateNotification(bool enabled) async {
    await _localDataSource.updateMenuPreferences(notificationEnabled: enabled);
    return fetchMenuSummary();
  }

  @override
  Future<MenuSummary> updateBiometric(bool enabled) async {
    await _localDataSource.updateMenuPreferences(biometricEnabled: enabled);
    return fetchMenuSummary();
  }
}
