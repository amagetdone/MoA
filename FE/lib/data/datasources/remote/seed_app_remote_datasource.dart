import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:more_app/data/datasources/remote/app_remote_datasource.dart';
import 'package:more_app/data/models/app_snapshot.dart';
import 'package:more_app/domain/entities/account.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/entities/spending.dart';

class SeedAppRemoteDataSource implements AppRemoteDataSource {
  SeedAppRemoteDataSource({AssetBundle? assetBundle})
    : _assetBundle = assetBundle ?? rootBundle;

  static const _seedAssetPath = 'assets/seeds/app_state.json';

  static AppSnapshot? _sessionSnapshot;

  final AssetBundle _assetBundle;

  @override
  Future<AppSnapshot> fetchSnapshot() async {
    await Future.delayed(const Duration(milliseconds: 220));
    return _clone(await _ensureSessionSnapshot());
  }

  @override
  Future<AppSnapshot> addAccount(Account account) async {
    return _mutate((snapshot) {
      final updatedAccounts = List<Account>.from(snapshot.accounts);
      final existingIndex = updatedAccounts.indexWhere(
        (item) => item.id == account.id,
      );

      if (existingIndex == -1) {
        updatedAccounts.add(account);
      } else {
        updatedAccounts[existingIndex] = account;
      }

      return snapshot.copyWith(accounts: updatedAccounts);
    });
  }

  @override
  Future<AppSnapshot> removeAccount(String id) async {
    return _mutate((snapshot) {
      final remaining = snapshot.accounts
          .where((item) => item.id != id)
          .toList();

      if (remaining.isNotEmpty && !remaining.any((item) => item.isPrimary)) {
        remaining[0] = remaining[0].copyWith(isPrimary: true);
      }

      final remainingAccountIds = remaining.map((item) => item.id).toSet();
      final updatedTransactions = snapshot.transactions
          .where(
            (transaction) =>
                transaction.accountId == null ||
                remainingAccountIds.contains(transaction.accountId),
          )
          .toList();

      return snapshot.copyWith(
        accounts: remaining,
        transactions: updatedTransactions,
      );
    });
  }

  @override
  Future<AppSnapshot> createGoal(Goal goal) async {
    return _mutate((snapshot) {
      final updatedGoals = List<Goal>.from(snapshot.goals)..add(goal);
      final updatedHistory = Map<String, List<DailySpending>>.from(
        snapshot.goalSavingsHistoryByGoalId,
      )..putIfAbsent(goal.id, () => const []);

      return snapshot.copyWith(
        goals: updatedGoals,
        goalSavingsHistoryByGoalId: updatedHistory,
      );
    });
  }

  @override
  Future<AppSnapshot> updateGoal(Goal goal) async {
    return _mutate((snapshot) {
      final updatedGoals = List<Goal>.from(snapshot.goals);
      final index = updatedGoals.indexWhere((item) => item.id == goal.id);

      if (index == -1) {
        throw StateError('목표를 찾을 수 없습니다.');
      }

      updatedGoals[index] = goal;
      return snapshot.copyWith(goals: updatedGoals);
    });
  }

  @override
  Future<AppSnapshot> saveGoalPriority(List<Goal> goals) async {
    return _mutate((snapshot) {
      final prioritizedGoals = <String, Goal>{};

      for (var index = 0; index < goals.length; index++) {
        prioritizedGoals[goals[index].id] = goals[index].copyWith(
          priority: index + 1,
        );
      }

      final updatedGoals =
          snapshot.goals
              .map((goal) => prioritizedGoals[goal.id] ?? goal)
              .toList()
            ..sort((a, b) => a.priority.compareTo(b.priority));

      return snapshot.copyWith(goals: updatedGoals);
    });
  }

  @override
  Future<AppSnapshot> updateMenuPreferences({
    bool? notificationEnabled,
    bool? biometricEnabled,
  }) async {
    return _mutate((snapshot) {
      final updatedPreferences = snapshot.menuPreferences.copyWith(
        notificationEnabled: notificationEnabled,
        biometricEnabled: biometricEnabled,
      );

      return snapshot.copyWith(menuPreferences: updatedPreferences);
    });
  }

  @override
  Future<AppSnapshot> updateSpendingSnapshot(SpendingSnapshot snapshot) async {
    return _mutate((current) => current.copyWith(spending: snapshot));
  }

  Future<AppSnapshot> _mutate(
    AppSnapshot Function(AppSnapshot snapshot) transform,
  ) async {
    await Future.delayed(const Duration(milliseconds: 180));
    final current = await _ensureSessionSnapshot();
    _sessionSnapshot = transform(current);
    return _clone(_sessionSnapshot!);
  }

  Future<AppSnapshot> _ensureSessionSnapshot() async {
    if (_sessionSnapshot != null) {
      return _sessionSnapshot!;
    }

    final jsonString = await _assetBundle.loadString(_seedAssetPath);
    _sessionSnapshot = AppSnapshot.fromJson(
      jsonDecode(jsonString) as Map<String, dynamic>,
    );
    return _sessionSnapshot!;
  }

  AppSnapshot _clone(AppSnapshot snapshot) {
    return AppSnapshot.fromJson(
      jsonDecode(jsonEncode(snapshot.toJson())) as Map<String, dynamic>,
    );
  }
}
