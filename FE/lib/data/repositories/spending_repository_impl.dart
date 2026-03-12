import 'dart:async';

import 'package:more_app/data/datasources/local/app_local_datasource.dart';
import 'package:more_app/data/datasources/remote/app_remote_datasource.dart';
import 'package:more_app/data/models/app_snapshot.dart';
import 'package:more_app/domain/entities/spending.dart';
import 'package:more_app/domain/entities/spending_overview.dart';
import 'package:more_app/domain/repositories/spending_repository.dart';

class SpendingRepositoryImpl implements SpendingRepository {
  SpendingRepositoryImpl({
    required AppRemoteDataSource remoteDataSource,
    required AppLocalDataSource localDataSource,
  }) : _remoteDataSource = remoteDataSource,
       _localDataSource = localDataSource;

  final AppRemoteDataSource _remoteDataSource;
  final AppLocalDataSource _localDataSource;

  @override
  Future<SpendingOverview> fetchSpendingOverview() async {
    final snapshot = await _loadSnapshot();
    return _toOverview(snapshot.spending);
  }

  @override
  Future<SpendingOverview> updateBudgetPlan({
    required int monthlyIncome,
    required int monthlySavingsGoal,
    required int monthlyFixedExpense,
    required List<SpendingCategory> categories,
  }) async {
    final current = (await _loadSnapshot()).spending;
    final available = monthlyIncome - monthlySavingsGoal - monthlyFixedExpense;
    final weeklyBudget = available <= 0 ? 0 : (available / 4.345).round();

    final updated = SpendingSnapshot(
      categories: categories,
      dailySpendings: current.dailySpendings,
      monthlySpendings: current.monthlySpendings,
      weeklyBudget: weeklyBudget,
      monthlyIncome: monthlyIncome,
      monthlySavingsGoal: monthlySavingsGoal,
      monthlyFixedExpense: monthlyFixedExpense,
    );

    try {
      final snapshot = await _remoteDataSource.updateSpendingSnapshot(updated);
      unawaited(_localDataSource.saveCachedSnapshot(snapshot));
      return _toOverview(snapshot.spending);
    } catch (_) {
      final saved = await _localDataSource.updateSpendingSnapshot(updated);
      return _toOverview(saved);
    }
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

  SpendingOverview _toOverview(SpendingSnapshot snapshot) {
    return SpendingOverview(
      categories: snapshot.categories,
      dailySpendings: snapshot.dailySpendings,
      monthlySpendings: snapshot.monthlySpendings,
      weeklyBudget: snapshot.weeklyBudget,
      monthlyIncome: snapshot.monthlyIncome,
      monthlySavingsGoal: snapshot.monthlySavingsGoal,
      monthlyFixedExpense: snapshot.monthlyFixedExpense,
    );
  }
}
