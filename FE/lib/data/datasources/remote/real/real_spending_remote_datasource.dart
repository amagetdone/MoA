import 'package:dio/dio.dart';
import 'package:more_app/data/datasources/remote/spending_remote_datasource.dart';
import 'package:more_app/data/models/app_snapshot_dto.dart';
import 'package:more_app/data/models/weekly_analysis_dto.dart';
// MonthlyAnalysisDto is defined in weekly_analysis_dto.dart
import 'package:more_app/domain/entities/spending.dart';

class RealSpendingRemoteDataSource implements SpendingRemoteDataSource {
  RealSpendingRemoteDataSource({required Dio dio}) : _dio = dio;

  final Dio _dio;

  @override
  Future<void> syncTransactions({int months = 1, bool relabel = false}) async {
    await _dio.post(
      '/transactions/sync',
      data: {'months': months, 'forceFullSync': false},
    );
  }

  @override
  Future<void> analyzeTransactions({int months = 1, bool relabel = false}) async {
    await _dio.post(
      '/transactions/analysis',
      data: {'months': months},
    );
  }

  @override
  Future<WeeklyAnalysisDto> fetchWeeklyAnalysis(String yearWeek) async {
    final response = await _dio.get('/transactions/analysis/weeks/$yearWeek');
    return WeeklyAnalysisDto.fromJson(response.data as Map<String, dynamic>);
  }

  String _toMonthWeek(DateTime weekMonday) {
    final year = weekMonday.year;
    final month = weekMonday.month;
    final firstDayOfMonth = DateTime(year, month, 1);
    final daysToFirstMonday = (8 - firstDayOfMonth.weekday) % 7;
    final firstMonday = firstDayOfMonth.add(Duration(days: daysToFirstMonday));
    final weekNum = weekMonday.difference(firstMonday).inDays ~/ 7 + 1;
    return '$year-${month.toString().padLeft(2, '0')}-$weekNum';
  }

  @override
  Future<List<DailySpending>> fetchWeeklyDailySpending(DateTime weekStart) async {
    final yearWeek = _toMonthWeek(weekStart);
    final response = await _dio.get('/transactions/expenses/graphs/weeks/$yearWeek');
    final points = (response.data['points'] as List<dynamic>? ?? []);
    return points.map((p) {
      final date = DateTime.parse(p['date'] as String);
      final amount = (p['expenseAmount'] as num?)?.toInt() ?? 0;
      return DailySpending(date: date, amount: amount);
    }).toList();
  }

  @override
  Future<MonthlyAnalysisDto> fetchMonthlyAnalysis(String yearMonth) async {
    final response = await _dio.get('/transactions/analysis/months/$yearMonth');
    return MonthlyAnalysisDto.fromJson(response.data as Map<String, dynamic>);
  }

  @override
  Future<List<DailySpending>> fetchMonthlyDailySpending(DateTime monthStart) async {
    final yearMonth =
        '${monthStart.year}-${monthStart.month.toString().padLeft(2, '0')}';
    final response =
        await _dio.get('/transactions/expenses/graphs/months/$yearMonth');
    final points = (response.data['points'] as List<dynamic>? ?? []);
    return points
        .where((p) {
          final yearWeek = p['yearWeek'] as String? ?? '';
          if (!yearWeek.startsWith(yearMonth)) return false;
          // 6주차 이상 제외 (1~5주차만 표시)
          final parts = yearWeek.split('-');
          final weekNum = parts.length >= 3 ? (int.tryParse(parts.last) ?? 0) : 0;
          return weekNum >= 1 && weekNum <= 5;
        })
        .where((p) => p['weekStartDate'] != null)
        .map((p) {
          final date = DateTime.parse(p['weekStartDate'] as String);
          final amount = (p['expenseAmount'] as num?)?.toInt() ?? 0;
          final label = p['label'] as String?;
          return DailySpending(date: date, amount: amount, label: label);
        })
        .toList()
      ..sort((a, b) => a.date.compareTo(b.date));
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
  Future<MonthlyAnalysisTriggerDto> analyzeMonthlyTransactions(String yearMonth) async {
    final res = await _dio.post(
      '/transactions/analysis/monthly',
      queryParameters: {'month': yearMonth},
    );
    return MonthlyAnalysisTriggerDto.fromJson(res.data as Map<String, dynamic>);
  }

  @override
  Future<MonthlyTransactionSummary> fetchWeeklyTransactionSummary(DateTime weekStart) async {
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
    final categoryMap = <String, int>{};

    for (final item in items) {
      final direction = item['direction'] as String? ?? '';
      if (direction != 'OUT') continue;
      final amount = (item['amount'] as num?)?.toInt() ?? 0;
      if (amount <= 0) continue;
      final categoryObj = item['category'] as Map<String, dynamic>?;
      final categoryName = categoryObj?['name'] as String? ?? '기타';
      categoryMap[categoryName] = (categoryMap[categoryName] ?? 0) + amount;
    }

    return MonthlyTransactionSummary(incomeTotal: 0, categoryAmounts: categoryMap);
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