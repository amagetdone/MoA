import 'package:more_app/data/datasources/remote/home_remote_datasource.dart';
import 'package:more_app/data/datasources/remote/seed/seed_data_store.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/entities/home_dashboard.dart';

class SeedHomeRemoteDataSource implements HomeRemoteDataSource {
  SeedHomeRemoteDataSource({required SeedDataStore dataStore})
    : _dataStore = dataStore;

  final SeedDataStore _dataStore;

  @override
  Future<HomeDashboard> fetchHomeDashboard() async {
    await Future.delayed(const Duration(milliseconds: 220));
    final snapshot = _dataStore.clone(await _dataStore.ensureSessionSnapshot());

    final totalAsset = snapshot.accounts.fold(0, (sum, account) {
      return sum + account.balance;
    });
    final activeGoals =
        snapshot.goals
            .where((goal) => goal.status == GoalStatus.inProgress)
            .toList()
          ..sort((a, b) => a.priority.compareTo(b.priority));

    return HomeDashboard(
      accounts: snapshot.accounts,
      activeGoals: activeGoals,
      totalAsset: totalAsset,
      weeklyBudget: snapshot.spending.weeklyBudget,
      weeklySpent: snapshot.spending.categories.fold(
        0,
        (sum, item) => sum + item.amount,
      ),
      weeklySaved: snapshot.spending.monthlySavingsGoal ~/ 4,
    );
  }
}
