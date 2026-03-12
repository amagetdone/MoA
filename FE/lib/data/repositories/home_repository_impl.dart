import 'dart:async';

import 'package:more_app/data/datasources/local/app_local_datasource.dart';
import 'package:more_app/data/datasources/remote/app_remote_datasource.dart';
import 'package:more_app/data/models/app_snapshot.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/entities/home_dashboard.dart';
import 'package:more_app/domain/repositories/home_repository.dart';

class HomeRepositoryImpl implements HomeRepository {
  HomeRepositoryImpl({
    required AppRemoteDataSource remoteDataSource,
    required AppLocalDataSource localDataSource,
  }) : _remoteDataSource = remoteDataSource,
       _localDataSource = localDataSource;

  final AppRemoteDataSource _remoteDataSource;
  final AppLocalDataSource _localDataSource;

  @override
  Future<HomeDashboard> fetchHomeDashboard() async {
    final snapshot = await _loadSnapshot();
    return _toHomeDashboard(snapshot);
  }

  Future<AppSnapshot> _loadSnapshot() async {
    try {
      final snapshot = await _remoteDataSource.fetchSnapshot();
      unawaited(_localDataSource.saveCachedSnapshot(snapshot));
      return snapshot;
    } catch (_) {
      return _localDataSource.getCachedSnapshot();
    }
  }

  HomeDashboard _toHomeDashboard(AppSnapshot snapshot) {
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
