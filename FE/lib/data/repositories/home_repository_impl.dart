import 'package:more_app/data/datasources/local/app_local_datasource.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/entities/home_dashboard.dart';
import 'package:more_app/domain/repositories/home_repository.dart';

class HomeRepositoryImpl implements HomeRepository {
  HomeRepositoryImpl({required AppLocalDataSource localDataSource})
    : _localDataSource = localDataSource;

  final AppLocalDataSource _localDataSource;

  @override
  Future<HomeDashboard> fetchHomeDashboard() async {
    final accounts = await _localDataSource.getAccounts();
    final goals = await _localDataSource.getGoals();
    final spending = await _localDataSource.getSpendingSnapshot();
    final totalAsset = accounts.fold(
      0,
      (sum, account) => sum + account.balance,
    );
    final activeGoals =
        goals.where((goal) => goal.status == GoalStatus.inProgress).toList()
          ..sort((a, b) => a.priority.compareTo(b.priority));

    return HomeDashboard(
      accounts: accounts,
      activeGoals: activeGoals,
      totalAsset: totalAsset,
      weeklyBudget: spending.weeklyBudget,
      weeklySpent: spending.categories.fold(
        0,
        (sum, item) => sum + item.amount,
      ),
      weeklySaved: spending.monthlySavingsGoal ~/ 4,
    );
  }
}
