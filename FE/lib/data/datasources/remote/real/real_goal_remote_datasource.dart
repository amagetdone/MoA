import 'package:dio/dio.dart';
import 'package:intl/intl.dart';
import 'package:more_app/data/datasources/remote/goal_remote_datasource.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/entities/spending.dart';

class RealGoalRemoteDataSource implements GoalRemoteDataSource {
  final Dio _dio;

  RealGoalRemoteDataSource({required Dio dio}) : _dio = dio;

  static final _dateFmt = DateFormat('yyyy-MM-dd');

  @override
  Future<Goal> createGoal(Goal goal) async {
    final response = await _dio.post(
      'v1/plans',
      data: {
        'title': goal.name,
        'description': goal.description ?? '',
        'targetAmount': goal.targetAmount,
        'startDate': _dateFmt.format(goal.startDate),
        'endDate': _dateFmt.format(goal.endDate),
        'priority': goal.priority,
      },
    );
    final data = response.data as Map<String, dynamic>;
    return _goalFromJson(data);
  }

  @override
  Future<List<Goal>> fetchGoals() async {
    // TODO: 목표 목록 조회 API 연동
    throw UnimplementedError('fetchGoals API not yet implemented');
  }

  @override
  Future<Goal?> findGoalById(String id) async {
    // TODO: 목표 상세 조회 API 연동
    throw UnimplementedError('findGoalById API not yet implemented');
  }

  @override
  Future<Map<String, List<DailySpending>>> fetchGoalSavingsHistoryByGoalId() async {
    // TODO: 저축 히스토리 API 연동
    throw UnimplementedError('fetchGoalSavingsHistoryByGoalId API not yet implemented');
  }

  @override
  Future<Goal> updateGoal(Goal goal) async {
    // TODO: 목표 수정 API 연동
    throw UnimplementedError('updateGoal API not yet implemented');
  }

  @override
  Future<List<Goal>> saveGoalPriority(List<Goal> goals) async {
    // TODO: 우선순위 저장 API 연동
    throw UnimplementedError('saveGoalPriority API not yet implemented');
  }

  Goal _goalFromJson(Map<String, dynamic> json) {
    return Goal(
      id: json['planId'].toString(),
      name: json['title'] as String? ?? '',
      description: json['description'] as String?,
      targetAmount: json['targetAmount'] as int? ?? 0,
      currentAmount: 0,
      startDate: DateTime.parse(json['startDate'] as String),
      endDate: DateTime.parse(json['endDate'] as String),
      priority: json['priority'] as int? ?? 0,
      status: _parseStatus(json['status'] as String?),
    );
  }

  GoalStatus _parseStatus(String? status) {
    switch (status) {
      case 'ACHIEVED':
        return GoalStatus.achieved;
      case 'FAILED':
        return GoalStatus.failed;
      default:
        return GoalStatus.inProgress;
    }
  }
}
