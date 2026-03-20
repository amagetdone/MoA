import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:more_app/data/datasources/remote/budget_remote_datasource.dart';

class RealBudgetRemoteDataSource implements BudgetRemoteDataSource {
  RealBudgetRemoteDataSource({required Dio dio}) : _dio = dio;

  final Dio _dio;

  @override
  Future<int> getIncome() async {
    debugPrint('[Budget] GET /budgets/income');
    final res = await _dio.get('/budgets/income');
    final data = res.data as Map<String, dynamic>?;
    if (data == null) return 0;
    return (data['monthlyIncome'] as num?)?.toInt() ?? 0;
  }

  @override
  Future<int> saveIncome(int monthlyIncome) async {
    debugPrint('[Budget] POST /budgets/income monthlyIncome=$monthlyIncome');
    final res = await _dio.post(
      '/budgets/income',
      data: {'monthlyIncome': monthlyIncome},
    );
    final data = res.data as Map<String, dynamic>?;
    if (data == null) return monthlyIncome;
    return (data['monthlyIncome'] as num?)?.toInt() ?? monthlyIncome;
  }

  @override
  Future<bool> allocateBudget() async {
    debugPrint('[Budget] POST /budgets/allocate');
    final res = await _dio.post('/budgets/allocate');
    final data = res.data as Map<String, dynamic>?;
    if (data == null) return true;
    return data['success'] as bool? ?? true;
  }

  @override
  Future<BudgetWeekSetupDto> getWeekSetup(String yearWeek) async {
    debugPrint('[Budget] GET /budgets/weeks/$yearWeek/setup');
    final res = await _dio.get('/budgets/weeks/$yearWeek/setup');
    final data = res.data as Map<String, dynamic>?;
    if (data == null) return const BudgetWeekSetupDto();
    return BudgetWeekSetupDto.fromJson(data);
  }

  @override
  Future<int> getAverageIncomeFromAnalysis() async {
    debugPrint('[Budget] GET /transactions/analysis (averageIncome)');
    final res = await _dio.get('/transactions/analysis');
    final data = res.data as Map<String, dynamic>?;
    if (data == null) return 0;
    return (data['averageIncome'] as num?)?.toInt() ?? 0;
  }
}