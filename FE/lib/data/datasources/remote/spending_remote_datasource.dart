import 'package:more_app/data/models/app_snapshot_dto.dart';
import 'package:more_app/data/models/weekly_analysis_dto.dart';
import 'package:more_app/domain/entities/spending.dart';

class MonthlyTransactionSummary {
  final int incomeTotal;
  final Map<String, int> categoryAmounts;

  const MonthlyTransactionSummary({
    this.incomeTotal = 0,
    this.categoryAmounts = const {},
  });
}

abstract class SpendingRemoteDataSource {
  Future<void> syncTransactions({int months = 3, bool relabel = false});
  Future<void> analyzeTransactions({int months = 3, bool relabel = false});
  Future<WeeklyAnalysisDto> fetchWeeklyAnalysis(String yearWeek);
  Future<List<DailySpending>> fetchWeeklyDailySpending(DateTime weekStart);
  Future<MonthlyAnalysisDto> fetchMonthlyAnalysis(String yearMonth);
  Future<List<DailySpending>> fetchMonthlyDailySpending(DateTime monthStart);
  Future<MonthlyTransactionSummary> fetchMonthlyTransactionSummary(DateTime monthStart);
  Future<SpendingSnapshotDto> fetchSpendingSnapshotDto();
  Future<SpendingSnapshotDto> updateSpendingSnapshotDto(SpendingSnapshotDto snapshot);
}
