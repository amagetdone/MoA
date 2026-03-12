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
      return await _remoteDataSource.fetchHomeDashboard();
    } catch (_) {
      final snapshot = await _localDataSource.getCachedSnapshot();
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
}
