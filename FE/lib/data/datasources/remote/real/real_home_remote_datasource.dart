import 'package:dio/dio.dart';
import 'package:more_app/data/datasources/remote/home_remote_datasource.dart';
import 'package:more_app/domain/entities/account.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/entities/home_dashboard.dart';

class RealHomeRemoteDataSource implements HomeRemoteDataSource {
  RealHomeRemoteDataSource({required Dio dio}) : _dio = dio;

  final Dio _dio;

  @override
  Future<HomeDashboard> fetchHomeDashboard() async {
    final response = await _dio.get('budgets/home');
    final data = response.data as Map<String, dynamic>? ?? {};

    final freeSpending = data['freeSpending'] as Map<String, dynamic>? ?? {};
    final weeklySaving = data['weeklySaving'] as Map<String, dynamic>? ?? {};
    final assetSummary = data['assetSummary'] as Map<String, dynamic>? ?? {};
    final plansRaw = data['plans'] as List<dynamic>? ?? [];

    final assetAccountsRaw = assetSummary['accounts'] as List<dynamic>? ?? [];
    final assetAccounts = assetAccountsRaw.asMap().entries.map((entry) {
      final a = entry.value as Map<String, dynamic>;
      return Account(
        id: a['accountNo'] as String? ?? '',
        bankName: a['bankName'] as String? ?? '',
        accountNumber: a['accountNo'] as String? ?? '',
        accountName: a['bankName'] as String? ?? '',
        balance: (a['balanceAmount'] as num?)?.toInt() ?? 0,
        isPrimary: entry.key == 0,
        verified: a['verified'] as bool? ?? true,
      );
    }).toList();

    final now = DateTime.now();
    final activeGoals = plansRaw.map((e) {
      final plan = e as Map<String, dynamic>;
      final dDay = (plan['dDay'] as num?)?.toInt() ?? 0;
      return Goal(
        id: plan['planId']?.toString() ?? '',
        name: plan['title'] as String? ?? '',
        targetAmount: (plan['targetAmount'] as num?)?.toInt() ?? 0,
        currentAmount: (plan['savedAmount'] as num?)?.toInt() ?? 0,
        startDate: now,
        endDate: now.add(Duration(days: dDay)),
        priority: (plan['priority'] as num?)?.toInt() ?? 0,
        status: GoalStatus.inProgress,
        dDay: dDay,
        apiProgressRate: (plan['progressRate'] as num?)?.toInt() ?? 0,
      );
    }).toList();

    return HomeDashboard(
      accounts: assetAccounts,
      activeGoals: activeGoals,
      totalAsset: (assetSummary['totalAssetAmount'] as num?)?.toInt() ?? 0,
      weeklyBudget: (freeSpending['totalAmount'] as num?)?.toInt() ?? 0,
      weeklySpent: (freeSpending['usedAmount'] as num?)?.toInt() ?? 0,
      weeklyRemaining: (freeSpending['remainingAmount'] as num?)?.toInt() ?? 0,
      daysRemaining: (freeSpending['daysRemaining'] as num?)?.toInt() ?? 0,
      dailyAvailable: (freeSpending['dailyAvailable'] as num?)?.toInt() ?? 0,
      weeklySaved: (weeklySaving['thisWeekSavingAmount'] as num?)?.toInt() ?? 0,
      savingTargetAmount: ((weeklySaving['thisWeekSavingAmount'] as num?)?.toInt() ?? 0) +
          ((weeklySaving['remainingToTargetAmount'] as num?)?.toInt() ?? 0),
      savingProgressRate: (weeklySaving['progressRate'] as num?)?.toInt() ?? 0,
    );
  }
}
