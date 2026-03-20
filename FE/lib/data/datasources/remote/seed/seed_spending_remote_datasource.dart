import 'package:more_app/data/datasources/remote/seed/seed_data_store.dart';
import 'package:more_app/data/datasources/remote/spending_remote_datasource.dart';
import 'package:more_app/data/models/app_snapshot_dto.dart';
import 'package:more_app/data/models/weekly_analysis_dto.dart';
import 'package:more_app/domain/entities/spending.dart';

class SeedSpendingRemoteDataSource implements SpendingRemoteDataSource {
  SeedSpendingRemoteDataSource({required SeedDataStore dataStore})
    : _dataStore = dataStore;

  final SeedDataStore _dataStore;

  @override
  Future<void> syncTransactions({int months = 3, bool relabel = false}) async {
    // seed 환경에서는 no-op
  }

  @override
  Future<void> analyzeTransactions({int months = 3, bool relabel = false}) async {
    // seed 환경에서는 no-op
  }

  @override
  Future<WeeklyAnalysisDto> fetchWeeklyAnalysis(String yearWeek) async {
    await Future.delayed(const Duration(milliseconds: 120));
    final snapshot = _dataStore.clone(await _dataStore.ensureSessionSnapshot());
    final categories = snapshot.spending.categories;
    return WeeklyAnalysisDto(
      yearWeek: yearWeek,
      expenseAmount: categories.fold(0, (sum, c) => sum + c.amount),
      categoryExpenses: categories
          .map((c) => WeeklyCategoryExpenseDto(
                categoryId: 0,
                categoryName: c.name,
                amount: c.amount,
              ))
          .toList(),
    );
  }

  @override
  Future<List<DailySpending>> fetchWeeklyDailySpending(DateTime weekStart) async {
    await Future.delayed(const Duration(milliseconds: 100));
    const amounts = [30000, 85000, 20000, 60000, 110000, 45000, 15000];
    return List.generate(7, (i) => DailySpending(
      date: weekStart.add(Duration(days: i)),
      amount: amounts[i],
    ));
  }

  @override
  Future<MonthlyAnalysisDto> fetchMonthlyAnalysis(String yearMonth) async {
    await Future.delayed(const Duration(milliseconds: 120));
    return const MonthlyAnalysisDto(yearMonth: '', categoryExpenses: []);
  }

  @override
  Future<List<DailySpending>> fetchMonthlyDailySpending(DateTime monthStart) async {
    await Future.delayed(const Duration(milliseconds: 100));
    final dayCount = DateTime(monthStart.year, monthStart.month + 1, 0).day;
    return List.generate(dayCount, (i) => DailySpending(
      date: DateTime(monthStart.year, monthStart.month, i + 1),
      amount: (i % 7 == 5 || i % 7 == 6) ? 0 : 30000 + (i * 3000),
    ));
  }

  @override
  Future<MonthlyTransactionSummary> fetchMonthlyTransactionSummary(DateTime monthStart) async {
    return const MonthlyTransactionSummary();
  }

  @override
  Future<SpendingSnapshotDto> fetchSpendingSnapshotDto() async {
    await Future.delayed(const Duration(milliseconds: 220));
    final snapshot = _dataStore.clone(await _dataStore.ensureSessionSnapshot());
    return snapshot.spending;
  }

  @override
  Future<SpendingSnapshotDto> updateSpendingSnapshotDto(
    SpendingSnapshotDto spending,
  ) async {
    final snapshot = await _dataStore.mutate(
      (current) => current.copyWith(spending: spending),
    );
    return snapshot.spending;
  }
}
