import 'package:more_app/data/datasources/remote/goal_remote_datasource.dart';
import 'package:more_app/data/datasources/remote/seed/seed_data_store.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/entities/spending.dart';

class SeedGoalRemoteDataSource implements GoalRemoteDataSource {
  SeedGoalRemoteDataSource({required SeedDataStore dataStore})
    : _dataStore = dataStore;

  final SeedDataStore _dataStore;

  @override
  Future<List<Goal>> fetchGoals() async {
    await Future.delayed(const Duration(milliseconds: 220));
    final snapshot = _dataStore.clone(await _dataStore.ensureSessionSnapshot());
    final goals = List<Goal>.from(snapshot.goals);
    goals.sort((a, b) => a.priority.compareTo(b.priority));
    return goals;
  }

  @override
  Future<Goal?> findGoalById(String id) async {
    await Future.delayed(const Duration(milliseconds: 220));
    final snapshot = await _dataStore.ensureSessionSnapshot();
    Goal? goal;
    for (final item in snapshot.goals) {
      if (item.id == id) {
        goal = item;
        break;
      }
    }
    if (goal == null) {
      return null;
    }

    final history = snapshot.goalSavingsHistoryByGoalId[id] ?? const [];
    final weeklyPlans = _buildWeeklyPlans(goal, history);

    return goal.copyWith(
      graphData: _buildGraphData(goal, history),
      weeklyPlans: weeklyPlans,
      currentWeekOverride: goal.currentWeekNumber,
      totalWeeksOverride: goal.totalWeeks,
      detailLoaded: true,
    );
  }

  @override
  Future<GoalWeeklyPlanPage> fetchGoalWeeklyPlans(
    String id, {
    int page = 0,
    int size = 100,
  }) async {
    await Future.delayed(const Duration(milliseconds: 220));
    final snapshot = await _dataStore.ensureSessionSnapshot();
    Goal? goal;
    for (final item in snapshot.goals) {
      if (item.id == id) {
        goal = item;
        break;
      }
    }

    if (goal == null) {
      return GoalWeeklyPlanPage(goalId: id);
    }

    final history = snapshot.goalSavingsHistoryByGoalId[id] ?? const [];
    final weeklyPlans = goal.weeklyPlans.isNotEmpty
        ? goal.weeklyPlans
        : _buildWeeklyPlans(goal, history);

    final start = page * size;
    final end = (start + size).clamp(0, weeklyPlans.length);
    final sliced = start >= weeklyPlans.length
        ? const <GoalWeeklyPlan>[]
        : weeklyPlans.sublist(start, end);

    final totalPages = weeklyPlans.isEmpty ? 0 : ((weeklyPlans.length - 1) ~/ size) + 1;

    return GoalWeeklyPlanPage(
      goalId: id,
      weeklyPlans: sliced,
      page: page,
      size: size,
      totalElements: weeklyPlans.length,
      totalPages: totalPages,
      hasNext: page + 1 < totalPages,
    );
  }

  @override
  Future<Map<String, List<DailySpending>>> fetchGoalSavingsHistoryByGoalId() async {
    await Future.delayed(const Duration(milliseconds: 220));
    final snapshot = _dataStore.clone(await _dataStore.ensureSessionSnapshot());
    return snapshot.goalSavingsHistoryByGoalId.map(
      (goalId, history) => MapEntry(goalId, List<DailySpending>.from(history)),
    );
  }

  @override
  Future<Goal> createGoal(Goal goal) async {
    final snapshot = await _dataStore.mutate((snapshot) {
      final updatedGoals = List<Goal>.from(snapshot.goals)..add(goal);
      final updatedHistory = Map<String, List<DailySpending>>.from(
        snapshot.goalSavingsHistoryByGoalId,
      )..putIfAbsent(goal.id, () => const []);

      return snapshot.copyWith(
        goals: updatedGoals,
        goalSavingsHistoryByGoalId: updatedHistory,
      );
    });

    return snapshot.goals.firstWhere((item) => item.id == goal.id);
  }

  @override
  Future<Goal> updateGoal(Goal goal) async {
    final snapshot = await _dataStore.mutate((snapshot) {
      final updatedGoals = List<Goal>.from(snapshot.goals);
      final index = updatedGoals.indexWhere((item) => item.id == goal.id);

      if (index == -1) {
        throw StateError('Goal not found.');
      }

      updatedGoals[index] = goal;
      return snapshot.copyWith(goals: updatedGoals);
    });

    return snapshot.goals.firstWhere((item) => item.id == goal.id);
  }

  @override
  Future<List<Goal>> saveGoalPriority(List<Goal> goals) async {
    final snapshot = await _dataStore.mutate((snapshot) {
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

    final result = List<Goal>.from(snapshot.goals);
    result.sort((a, b) => a.priority.compareTo(b.priority));
    return result;
  }

  GoalGraphData _buildGraphData(Goal goal, List<DailySpending> history) {
    final totalWeeks = goal.totalWeeks;
    if (totalWeeks <= 0) {
      return const GoalGraphData();
    }

    final weeklyPlans = _buildWeeklyPlans(goal, history);
    final actualWeekly = <int, int>{};
    for (final item in history) {
      final week = ((item.date.difference(goal.startDate).inDays) ~/ 7) + 1;
      actualWeekly[week] = (actualWeekly[week] ?? 0) + item.amount;
    }

    final weeks = <int>[];
    final planned = <int>[];
    final actual = <int>[];
    var plannedCumulative = 0;
    var actualCumulative = 0;

    for (var week = 1; week <= totalWeeks; week++) {
      plannedCumulative += weeklyPlans
          .firstWhere(
            (item) => item.week == week,
            orElse: () => const GoalWeeklyPlan(week: 0, amount: 0),
          )
          .amount;
      actualCumulative += actualWeekly[week] ?? 0;
      weeks.add(week);
      planned.add(plannedCumulative);
      actual.add(actualCumulative);
    }

    return GoalGraphData(weeks: weeks, planned: planned, actual: actual);
  }

  List<GoalWeeklyPlan> _buildWeeklyPlans(Goal goal, List<DailySpending> history) {
    final totalWeeks = goal.totalWeeks;
    if (totalWeeks <= 0) {
      return const [];
    }

    final baseAmount = (goal.targetAmount / totalWeeks).ceil();
    final plans = <GoalWeeklyPlan>[];
    for (var week = 1; week <= totalWeeks; week++) {
      plans.add(GoalWeeklyPlan(week: week, amount: baseAmount));
    }

    if (history.isEmpty) {
      return plans;
    }

    final actualTotals = <int, int>{};
    for (final item in history) {
      final week = ((item.date.difference(goal.startDate).inDays) ~/ 7) + 1;
      actualTotals[week] = (actualTotals[week] ?? 0) + item.amount;
    }

    return plans
        .map(
          (plan) => GoalWeeklyPlan(
            week: plan.week,
            amount: actualTotals[plan.week] ?? plan.amount,
          ),
        )
        .toList();
  }
}
