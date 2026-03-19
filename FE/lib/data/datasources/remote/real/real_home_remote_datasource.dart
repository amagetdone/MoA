import 'package:dio/dio.dart';
import 'package:more_app/data/datasources/remote/home_remote_datasource.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/entities/home_dashboard.dart';

class RealHomeRemoteDataSource implements HomeRemoteDataSource {
  RealHomeRemoteDataSource({required Dio dio}) : _dio = dio;

  final Dio _dio;

  @override
  Future<HomeDashboard> fetchHomeDashboard() async {
    final response = await _dio.get('budgets/home');
    final data = response.data as Map<String, dynamic>;

    final freeSpending = data['freeSpending'] as Map<String, dynamic>;
    final weeklySaving = data['weeklySaving'] as Map<String, dynamic>;
    final assetSummary = data['assetSummary'] as Map<String, dynamic>;
    final plansRaw = data['plans'] as List<dynamic>;

    final now = DateTime.now();
    final activeGoals = plansRaw.map((e) {
      final plan = e as Map<String, dynamic>;
      final dDay = (plan['dDay'] as num).toInt();
      return Goal(
        id: plan['planId'].toString(),
        name: plan['title'] as String,
        targetAmount: (plan['targetAmount'] as num).toInt(),
        currentAmount: (plan['savedAmount'] as num).toInt(),
        startDate: now,
        endDate: now.add(Duration(days: dDay)),
        priority: (plan['priority'] as num).toInt(),
        status: GoalStatus.inProgress,
        dDay: dDay,
        apiProgressRate: (plan['progressRate'] as num).toInt(),
      );
    }).toList();

    return HomeDashboard(
      accounts: const [],
      activeGoals: activeGoals,
      totalAsset: (assetSummary['totalAssetAmount'] as num).toInt(),
      weeklyBudget: (freeSpending['totalAmount'] as num).toInt(),
      weeklySpent: (freeSpending['usedAmount'] as num).toInt(),
      weeklyRemaining: (freeSpending['remainingAmount'] as num).toInt(),
      daysRemaining: (freeSpending['daysRemaining'] as num).toInt(),
      dailyAvailable: (freeSpending['dailyAvailable'] as num).toInt(),
      weeklySaved: (weeklySaving['thisWeekSavingAmount'] as num).toInt(),
      savingProgressRate: (weeklySaving['progressRate'] as num).toInt(),
    );
  }
}
