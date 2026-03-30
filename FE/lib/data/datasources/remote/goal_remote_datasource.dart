import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/entities/spending.dart';

abstract class GoalRemoteDataSource {
  Future<List<Goal>> fetchGoals();
  Future<Goal?> findGoalById(String id);
  Future<void> deleteGoal(String id);
  Future<GoalWeeklyPlanPage> fetchGoalWeeklyPlans(
    String id, {
    int page = 0,
    int size = 100,
  });
  Future<Map<String, List<DailySpending>>> fetchGoalSavingsHistoryByGoalId();
  Future<Goal> createGoal(Goal goal);
  Future<Goal> updateGoal(Goal goal);
  Future<List<Goal>> saveGoalPriority(List<Goal> goals);
}
