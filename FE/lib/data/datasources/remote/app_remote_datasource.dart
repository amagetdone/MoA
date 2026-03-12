import 'package:more_app/data/models/app_snapshot.dart';
import 'package:more_app/domain/entities/account.dart';
import 'package:more_app/domain/entities/goal.dart';

abstract class AppRemoteDataSource {
  Future<AppSnapshot> fetchSnapshot();

  Future<AppSnapshot> addAccount(Account account);

  Future<AppSnapshot> removeAccount(String id);

  Future<AppSnapshot> createGoal(Goal goal);

  Future<AppSnapshot> updateGoal(Goal goal);

  Future<AppSnapshot> saveGoalPriority(List<Goal> goals);

  Future<AppSnapshot> updateMenuPreferences({
    bool? notificationEnabled,
    bool? biometricEnabled,
  });

  Future<AppSnapshot> updateSpendingSnapshot(SpendingSnapshot snapshot);
}
