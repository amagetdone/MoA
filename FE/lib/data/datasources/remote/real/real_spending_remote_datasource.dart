import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:more_app/data/datasources/remote/spending_remote_datasource.dart';
import 'package:more_app/data/models/app_snapshot_dto.dart';
import 'package:more_app/data/models/weekly_analysis_dto.dart';
// MonthlyAnalysisDto is defined in weekly_analysis_dto.dart
import 'package:more_app/domain/entities/spending.dart';

class RealSpendingRemoteDataSource implements SpendingRemoteDataSource {
  RealSpendingRemoteDataSource({required Dio dio}) : _dio = dio;

  final Dio _dio;

  @override
  Future<void> syncTransactions({int months = 3, bool relabel = false}) async {
    debugPrint('[Spending] POST /transactions/sync months=$months');
    final res = await _dio.post(
      '/transactions/sync',
      data: {'months': months, 'forceFullSync': false},
    );
    debugPrint('[Spending] sync result: ${res.statusCode} ${res.data}');
  }

  @override
  Future<void> analyzeTransactions({int months = 3, bool relabel = false}) async {
    debugPrint('[Spending] POST /transactions/analysis months=$months');
    final res = await _dio.post(
      '/transactions/analysis',
      data: {'months': months, 'relabel': relabel},
    );
    debugPrint('[Spending] analyze result: ${res.statusCode} ${res.data}');
  }

  @override
  Future<WeeklyAnalysisDto> fetchWeeklyAnalysis(String yearWeek) async {
    debugPrint('[Spending] GET /transactions/analysis/weeks/$yearWeek');
    final response = await _dio.get('/transactions/analysis/weeks/$yearWeek');
    debugPrint('[Spending] weekly result: ${response.statusCode} ${response.data}');
    return WeeklyAnalysisDto.fromJson(response.data as Map<String, dynamic>);
  }

  @override
  Future<List<DailySpending>> fetchWeeklyDailySpending(DateTime weekStart) async {
    final weekEnd = weekStart.add(const Duration(days: 6));
    String fmt(DateTime d) =>
        '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

    final response = await _dio.get(
      '/transactions',
      queryParameters: {
        'fromDate': fmt(weekStart),
        'toDate': fmt(weekEnd),
        'analysisInclusion': 'INCLUDED',
        'page': 0,
        'size': 200,
      },
    );

    final items = (response.data['items'] as List<dynamic>? ?? []);

    final dailyMap = <String, int>{};
    for (final item in items) {
      final direction = item['direction'] as String? ?? '';
      if (direction == 'IN') continue; // 수입 제외
      final occurredAt = item['occurredAt'] as String? ?? '';
      final date = occurredAt.length >= 10 ? occurredAt.substring(0, 10) : '';
      final amount = (item['amount'] as num?)?.toInt() ?? 0;
      if (date.isNotEmpty && amount > 0) {
        dailyMap[date] = (dailyMap[date] ?? 0) + amount;
      }
    }

    return List.generate(7, (i) {
      final date = weekStart.add(Duration(days: i));
      return DailySpending(date: date, amount: dailyMap[fmt(date)] ?? 0);
    });
  }

  @override
  Future<MonthlyAnalysisDto> fetchMonthlyAnalysis(String yearMonth) async {
    debugPrint('[Spending] GET /transactions/analysis/months/$yearMonth');
    final response = await _dio.get('/transactions/analysis/months/$yearMonth');
    debugPrint('[Spending] monthly result: ${response.statusCode} ${response.data}');
    return MonthlyAnalysisDto.fromJson(response.data as Map<String, dynamic>);
  }

  @override
  Future<List<DailySpending>> fetchMonthlyDailySpending(DateTime monthStart) async {
    final monthEnd = DateTime(monthStart.year, monthStart.month + 1, 0);
    String fmt(DateTime d) =>
        '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

    final response = await _dio.get(
      '/transactions',
      queryParameters: {
        'fromDate': fmt(monthStart),
        'toDate': fmt(monthEnd),
        'analysisInclusion': 'INCLUDED',
        'page': 0,
        'size': 1000,
      },
    );

    final items = (response.data['items'] as List<dynamic>? ?? []);

    final dailyMap = <String, int>{};
    for (final item in items) {
      final direction = item['direction'] as String? ?? '';
      if (direction == 'IN') continue;
      final occurredAt = item['occurredAt'] as String? ?? '';
      final date = occurredAt.length >= 10 ? occurredAt.substring(0, 10) : '';
      final amount = (item['amount'] as num?)?.toInt() ?? 0;
      if (date.isNotEmpty && amount > 0) {
        dailyMap[date] = (dailyMap[date] ?? 0) + amount;
      }
    }

    final daysInMonth = monthEnd.day;
    return List.generate(daysInMonth, (i) {
      final date = DateTime(monthStart.year, monthStart.month, i + 1);
      return DailySpending(date: date, amount: dailyMap[fmt(date)] ?? 0);
    });
  }

  @override
  Future<MonthlyTransactionSummary> fetchMonthlyTransactionSummary(DateTime monthStart) async {
    final monthEnd = DateTime(monthStart.year, monthStart.month + 1, 0);
    String fmt(DateTime d) =>
        '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

    final response = await _dio.get(
      '/transactions',
      queryParameters: {
        'fromDate': fmt(monthStart),
        'toDate': fmt(monthEnd),
        'page': 0,
        'size': 1000,
      },
    );

    final items = (response.data['items'] as List<dynamic>? ?? []);
    int incomeTotal = 0;
    final categoryMap = <String, int>{};

    for (final item in items) {
      final direction = item['direction'] as String? ?? '';
      final amount = (item['amount'] as num?)?.toInt() ?? 0;
      if (amount <= 0) continue;

      if (direction == 'IN') {
        incomeTotal += amount;
      } else if (direction == 'OUT') {
        final categoryObj = item['category'] as Map<String, dynamic>?;
        final categoryName = categoryObj?['name'] as String? ?? '기타';
        categoryMap[categoryName] = (categoryMap[categoryName] ?? 0) + amount;
      }
    }

    return MonthlyTransactionSummary(
      incomeTotal: incomeTotal,
      categoryAmounts: categoryMap,
    );
  }

  @override
  Future<SpendingSnapshotDto> fetchSpendingSnapshotDto() {
    throw UnimplementedError();
  }

  @override
  Future<SpendingSnapshotDto> updateSpendingSnapshotDto(SpendingSnapshotDto snapshot) {
    throw UnimplementedError();
  }
}
