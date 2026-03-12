import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:hive_flutter/hive_flutter.dart';
import 'package:more_app/data/models/app_snapshot.dart';
import 'package:more_app/domain/entities/account.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/entities/spending.dart';
import 'package:more_app/domain/entities/transaction.dart';
import 'package:more_app/domain/entities/user.dart';

abstract class AppLocalDataSource {
  Future<AppSnapshot> getCachedSnapshot();
  Future<void> saveCachedSnapshot(AppSnapshot snapshot);
  Future<List<String>> getBankNames();
  Future<User> getUser();
  Future<MenuPreferencesSnapshot> getMenuPreferences();
  Future<MenuPreferencesSnapshot> updateMenuPreferences({
    bool? notificationEnabled,
    bool? biometricEnabled,
  });
  Future<List<Account>> getAccounts();
  Future<Account> addAccount(Account account);
  Future<void> removeAccount(String id);
  Future<List<Transaction>> getTransactions();
  Future<Transaction?> getTransactionById(String id);
  Future<List<Goal>> getGoals();
  Future<Goal?> getGoalById(String id);
  Future<Map<String, List<DailySpending>>> getGoalSavingsHistoryByGoalId();
  Future<List<DailySpending>> getGoalSavingsHistory(String goalId);
  Future<Goal> createGoal(Goal goal);
  Future<Goal> updateGoal(Goal goal);
  Future<List<Goal>> saveGoalPriority(List<Goal> goals);
  Future<Uint8List?> getGoalSketchVideo(String goalId);
  Future<SpendingSnapshot> getSpendingSnapshot();
  Future<SpendingSnapshot> updateSpendingSnapshot(SpendingSnapshot snapshot);
}

class AppLocalDataSourceImpl implements AppLocalDataSource {
  AppLocalDataSourceImpl({AssetBundle? assetBundle})
    : _assetBundle = assetBundle ?? rootBundle;

  static const _seedAssetPath = 'assets/seeds/app_state.json';
  static const _snapshotBoxName = 'more_app_snapshot_box';
  static const _snapshotKey = 'snapshot';
  static const _goalSketchVideoBoxName = 'more_goal_sketch_video_box';

  static bool _storageReady = false;
  static bool _storageAttempted = false;
  static Box<String>? _snapshotBox;
  static Box<dynamic>? _goalSketchVideoBox;
  static String? _memorySnapshotJson;
  static final Map<String, Uint8List> _memorySketchVideos = {};

  final AssetBundle _assetBundle;
  AppSnapshot? _snapshotCache;

  Future<void> _ensureStorageReady() async {
    if (_storageAttempted) {
      return;
    }

    _storageAttempted = true;

    try {
      await Hive.initFlutter('more_app');
      _snapshotBox = await Hive.openBox<String>(_snapshotBoxName);
      _goalSketchVideoBox = await Hive.openBox<dynamic>(
        _goalSketchVideoBoxName,
      );
      _storageReady = true;
    } catch (_) {
      _storageReady = false;
    }
  }

  Future<AppSnapshot> _loadSnapshot() async {
    if (_snapshotCache != null) {
      return _snapshotCache!;
    }

    await _ensureStorageReady();

    final storedJson = _storageReady
        ? _snapshotBox?.get(_snapshotKey)
        : _memorySnapshotJson;

    if (storedJson != null) {
      _snapshotCache = AppSnapshot.fromJson(
        jsonDecode(storedJson) as Map<String, dynamic>,
      );
    } else {
      final seedJson = await _assetBundle.loadString(_seedAssetPath);
      _snapshotCache = AppSnapshot.fromJson(
        jsonDecode(seedJson) as Map<String, dynamic>,
      );
      await _persistSnapshot(_snapshotCache!);
    }

    await _seedGoalSketchVideos(_snapshotCache!);
    return _snapshotCache!;
  }

  Future<void> _persistSnapshot(AppSnapshot snapshot) async {
    _snapshotCache = snapshot;
    final jsonString = jsonEncode(snapshot.toJson());

    await _ensureStorageReady();
    if (_storageReady) {
      await _snapshotBox?.put(_snapshotKey, jsonString);
    } else {
      _memorySnapshotJson = jsonString;
    }
  }

  @override
  Future<AppSnapshot> getCachedSnapshot() => _loadSnapshot();

  @override
  Future<void> saveCachedSnapshot(AppSnapshot snapshot) =>
      _persistSnapshot(snapshot);

  Future<void> _seedGoalSketchVideos(AppSnapshot snapshot) async {
    for (final goal in snapshot.goals) {
      final assetPath = goal.sketchVideoAssetPath;
      if (assetPath == null || assetPath.isEmpty) {
        continue;
      }

      final storedBytes = await _readGoalSketchVideo(goal.id);
      if (storedBytes != null) {
        continue;
      }

      try {
        final bytes = (await _assetBundle.load(assetPath)).buffer.asUint8List();
        await _writeGoalSketchVideo(goal.id, bytes);
      } catch (_) {}
    }
  }

  Future<Uint8List?> _readGoalSketchVideo(String goalId) async {
    await _ensureStorageReady();

    if (_storageReady) {
      final value = _goalSketchVideoBox?.get(goalId);
      if (value is Uint8List) {
        return value;
      }
      if (value is List<dynamic>) {
        return Uint8List.fromList(value.cast<int>());
      }
      if (value is List<int>) {
        return Uint8List.fromList(value);
      }
    }

    return _memorySketchVideos[goalId];
  }

  Future<void> _writeGoalSketchVideo(String goalId, Uint8List bytes) async {
    await _ensureStorageReady();

    if (_storageReady) {
      await _goalSketchVideoBox?.put(goalId, bytes);
    } else {
      _memorySketchVideos[goalId] = bytes;
    }
  }

  @override
  Future<List<String>> getBankNames() async {
    final snapshot = await _loadSnapshot();
    return List<String>.from(snapshot.bankNames);
  }

  @override
  Future<User> getUser() async {
    final snapshot = await _loadSnapshot();
    return snapshot.user;
  }

  @override
  Future<MenuPreferencesSnapshot> getMenuPreferences() async {
    final snapshot = await _loadSnapshot();
    return snapshot.menuPreferences;
  }

  @override
  Future<MenuPreferencesSnapshot> updateMenuPreferences({
    bool? notificationEnabled,
    bool? biometricEnabled,
  }) async {
    final snapshot = await _loadSnapshot();
    final updated = snapshot.menuPreferences.copyWith(
      notificationEnabled: notificationEnabled,
      biometricEnabled: biometricEnabled,
    );
    await _persistSnapshot(snapshot.copyWith(menuPreferences: updated));
    return updated;
  }

  @override
  Future<List<Account>> getAccounts() async {
    final snapshot = await _loadSnapshot();
    return List<Account>.from(snapshot.accounts);
  }

  @override
  Future<Account> addAccount(Account account) async {
    final snapshot = await _loadSnapshot();
    final existingIndex = snapshot.accounts.indexWhere(
      (item) => item.accountNumber == account.accountNumber,
    );
    final updatedAccounts = List<Account>.from(snapshot.accounts);

    if (existingIndex == -1) {
      updatedAccounts.add(account);
    } else {
      updatedAccounts[existingIndex] = account;
    }

    await _persistSnapshot(snapshot.copyWith(accounts: updatedAccounts));
    return account;
  }

  @override
  Future<void> removeAccount(String id) async {
    final snapshot = await _loadSnapshot();
    final remaining = snapshot.accounts.where((item) => item.id != id).toList();

    if (remaining.isNotEmpty && !remaining.any((item) => item.isPrimary)) {
      remaining[0] = remaining[0].copyWith(isPrimary: true);
    }

    await _persistSnapshot(snapshot.copyWith(accounts: remaining));
  }

  @override
  Future<List<Transaction>> getTransactions() async {
    final snapshot = await _loadSnapshot();
    return List<Transaction>.from(snapshot.transactions)
      ..sort((a, b) => b.dateTime.compareTo(a.dateTime));
  }

  @override
  Future<Transaction?> getTransactionById(String id) async {
    final transactions = await getTransactions();
    for (final transaction in transactions) {
      if (transaction.id == id) {
        return transaction;
      }
    }
    return null;
  }

  @override
  Future<List<Goal>> getGoals() async {
    final snapshot = await _loadSnapshot();
    return List<Goal>.from(snapshot.goals)
      ..sort((a, b) => a.priority.compareTo(b.priority));
  }

  @override
  Future<Goal?> getGoalById(String id) async {
    final goals = await getGoals();
    for (final goal in goals) {
      if (goal.id == id) {
        return goal;
      }
    }
    return null;
  }

  @override
  Future<Map<String, List<DailySpending>>>
  getGoalSavingsHistoryByGoalId() async {
    final snapshot = await _loadSnapshot();
    return snapshot.goalSavingsHistoryByGoalId.map(
      (goalId, history) => MapEntry(goalId, List<DailySpending>.from(history)),
    );
  }

  @override
  Future<List<DailySpending>> getGoalSavingsHistory(String goalId) async {
    final historyByGoalId = await getGoalSavingsHistoryByGoalId();
    return historyByGoalId[goalId] ?? const [];
  }

  @override
  Future<Goal> createGoal(Goal goal) async {
    final snapshot = await _loadSnapshot();
    final updatedGoals = List<Goal>.from(snapshot.goals)..add(goal);
    final updatedHistory = Map<String, List<DailySpending>>.from(
      snapshot.goalSavingsHistoryByGoalId,
    )..putIfAbsent(goal.id, () => []);
    await _persistSnapshot(
      snapshot.copyWith(
        goals: updatedGoals,
        goalSavingsHistoryByGoalId: updatedHistory,
      ),
    );
    return goal;
  }

  @override
  Future<Goal> updateGoal(Goal goal) async {
    final snapshot = await _loadSnapshot();
    final updatedGoals = List<Goal>.from(snapshot.goals);
    final index = updatedGoals.indexWhere((item) => item.id == goal.id);

    if (index == -1) {
      throw StateError('목표를 찾을 수 없습니다.');
    }

    updatedGoals[index] = goal;
    await _persistSnapshot(snapshot.copyWith(goals: updatedGoals));
    return goal;
  }

  @override
  Future<List<Goal>> saveGoalPriority(List<Goal> goals) async {
    final snapshot = await _loadSnapshot();
    final prioritizedGoals = <String, Goal>{};

    for (var index = 0; index < goals.length; index++) {
      prioritizedGoals[goals[index].id] = goals[index].copyWith(
        priority: index + 1,
      );
    }

    final updatedGoals = snapshot.goals.map((goal) {
      return prioritizedGoals[goal.id] ?? goal;
    }).toList()..sort((a, b) => a.priority.compareTo(b.priority));

    await _persistSnapshot(snapshot.copyWith(goals: updatedGoals));
    return updatedGoals;
  }

  @override
  Future<Uint8List?> getGoalSketchVideo(String goalId) async {
    final snapshot = await _loadSnapshot();
    final stored = await _readGoalSketchVideo(goalId);
    if (stored != null) {
      return stored;
    }

    Goal? goal;
    for (final item in snapshot.goals) {
      if (item.id == goalId) {
        goal = item;
        break;
      }
    }

    final assetPath = goal?.sketchVideoAssetPath;
    if (assetPath == null || assetPath.isEmpty) {
      return null;
    }

    try {
      final bytes = (await _assetBundle.load(assetPath)).buffer.asUint8List();
      await _writeGoalSketchVideo(goalId, bytes);
      return bytes;
    } catch (_) {
      return null;
    }
  }

  @override
  Future<SpendingSnapshot> getSpendingSnapshot() async {
    final snapshot = await _loadSnapshot();
    return snapshot.spending;
  }

  @override
  Future<SpendingSnapshot> updateSpendingSnapshot(
    SpendingSnapshot snapshotData,
  ) async {
    final snapshot = await _loadSnapshot();
    await _persistSnapshot(snapshot.copyWith(spending: snapshotData));
    return snapshotData;
  }
}
