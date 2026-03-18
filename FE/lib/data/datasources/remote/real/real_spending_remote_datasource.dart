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
  Future<void> syncTransactions({int months = 3, bool relabel = false}) async {
    await _dio.post(
      '/transactions/sync',
      data: {'months': months, 'relabel': relabel},
    );
  }

  @override
  Future<void> analyzeTransactions({int months = 3, bool relabel = false}) async {
    await _dio.post(
      '/transactions/analysis',
      data: {'months': months, 'relabel': relabel},
    );
  }

  @override
  Future<WeeklyAnalysisDto> fetchWeeklyAnalysis(String yearWeek) async {
    final response = await _dio.get('/transactions/analysis/weeks/$yearWeek');
    return WeeklyAnalysisDto.fromJson(response.data['data'] as Map<String, dynamic>);
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

    final items = (response.data['data']['transactions'] as List<dynamic>? ?? []);

    final dailyMap = <String, int>{};
    for (final item in items) {
      final type = item['transactionType'] as String? ?? '';
      if (type == '1') continue; // 수입 제외
      final date = item['transactionDate'] as String? ?? '';
      final amount = (item['transactionBalance'] as num?)?.toInt() ?? 0;
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
    final response = await _dio.get('/transactions/analysis/months/$yearMonth');
    return MonthlyAnalysisDto.fromJson(response.data['data'] as Map<String, dynamic>);
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
        'size': 500,
      },
    );

    final items = (response.data['data']['transactions'] as List<dynamic>? ?? []);

    final dailyMap = <String, int>{};
    for (final item in items) {
      final type = item['transactionType'] as String? ?? '';
      if (type == '1') continue;
      final date = item['transactionDate'] as String? ?? '';
      final amount = (item['transactionBalance'] as num?)?.toInt() ?? 0;
      if (date.isNotEmpty && amount > 0) {
        dailyMap[date] = (dailyMap[date] ?? 0) + amount;
      }
    }

    final dayCount = monthEnd.day;
    return List.generate(dayCount, (i) {
      final date = DateTime(monthStart.year, monthStart.month, i + 1);
      return DailySpending(date: date, amount: dailyMap[fmt(date)] ?? 0);
    });
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
