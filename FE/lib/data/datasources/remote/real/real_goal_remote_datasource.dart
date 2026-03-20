import 'package:dio/dio.dart';
import 'package:intl/intl.dart';
import 'package:more_app/data/datasources/remote/goal_remote_datasource.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/entities/spending.dart';

class RealGoalRemoteDataSource implements GoalRemoteDataSource {
  RealGoalRemoteDataSource({required Dio dio}) : _dio = dio;

  final Dio _dio;

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
    final createdPlanId = (data['planId'] as num?)?.toInt() ?? 0;

    if (createdPlanId > 0) {
      try {
        final detailedGoal = await findGoalById(createdPlanId.toString());
        if (detailedGoal != null) {
          return detailedGoal;
        }
      } catch (_) {}
    }

    final videoUrl = await _fetchVideoUrlSafely(createdPlanId);

    return goal.copyWith(
      id: createdPlanId > 0 ? createdPlanId.toString() : goal.id,
      name: data['planTitle'] as String? ?? goal.name,
      sketchVideoAssetPath: videoUrl,
      weeklyMission: _weeklyMissionFromJson(
        data['weeklyMission'] as Map<String, dynamic>?,
      ),
      weeklyBudget: _weeklyBudgetFromJson(
        data['weeklyBudget'] as Map<String, dynamic>?,
      ),
      apiStatus: data['status'] as String?,
    );
  }

  @override
  Future<List<Goal>> fetchGoals() async {
    final response = await _dio.get('v1/plans');
    final items = response.data as List<dynamic>? ?? const [];

    final goals = await Future.wait(
      items.map((item) => _goalFromListJson(item as Map<String, dynamic>)),
    );

    goals.sort((a, b) => a.priority.compareTo(b.priority));
    return goals;
  }

  @override
  Future<Goal?> findGoalById(String id) async {
    final response = await _dio.get('v1/plans/$id');
    final data = response.data as Map<String, dynamic>;
    final planId = (data['planId'] as num?)?.toInt() ?? int.tryParse(id) ?? 0;

    GoalWeeklyPlanPage weeklyPlanPage;
    try {
      weeklyPlanPage = await fetchGoalWeeklyPlans(
        id,
        page: 0,
        size: 200,
      );
    } catch (_) {
      weeklyPlanPage = GoalWeeklyPlanPage(goalId: id);
    }
    final videoUrl = await _fetchVideoUrlSafely(planId);

    final targetAmount = (data['targetAmount'] as num?)?.toInt() ?? 0;
    final savedAmount = (data['savedAmount'] as num?)?.toInt() ?? 0;
    final priority = (data['priority'] as num?)?.toInt() ?? 0;
    final currentWeek = (data['currentWeek'] as num?)?.toInt();
    final totalWeeks = (data['totalWeeks'] as num?)?.toInt();
    final dday = (data['dday'] as num?)?.toInt();
    final achievementRate = (data['achievementRate'] as num?)?.toDouble();

    final endDate = _inferEndDate(dday);
    final startDate = _inferStartDate(
      endDate: endDate,
      currentWeek: currentWeek,
      totalWeeks: totalWeeks,
    );

    return Goal(
      id: planId.toString(),
      name: data['title'] as String? ?? '',
      description: data['description'] as String?,
      targetAmount: targetAmount,
      currentAmount: savedAmount,
      startDate: startDate,
      endDate: endDate,
      priority: priority,
      status: _parseStatus(
        savedAmount: savedAmount,
        targetAmount: targetAmount,
        dday: dday,
        achievementRate: achievementRate,
      ),
      iconName: _inferIconName(data['title'] as String? ?? ''),
      sketchVideoAssetPath: videoUrl,
      weekNumber: currentWeek,
      dday: dday,
      achievementRateOverride: achievementRate,
      graphData: _graphDataFromJson(data['graphData'] as Map<String, dynamic>?),
      weeklyPlans: weeklyPlanPage.weeklyPlans.isNotEmpty
          ? weeklyPlanPage.weeklyPlans
          : _weeklyPlansFromJson(data['weeklyPlans'] as List<dynamic>?),
      currentWeekOverride: currentWeek,
      totalWeeksOverride: totalWeeks,
      detailLoaded: true,
    );
  }

  @override
  Future<void> deleteGoal(String id) async {
    await _dio.delete('v1/plans/$id');
  }

  @override
  Future<GoalWeeklyPlanPage> fetchGoalWeeklyPlans(
    String id, {
    int page = 0,
    int size = 100,
  }) async {
    final response = await _dio.get(
      'v1/plans/$id/weekly-plans',
      queryParameters: {
        'page': page,
        'size': size,
      },
    );

    final data = response.data as Map<String, dynamic>;
    return GoalWeeklyPlanPage(
      goalId: ((data['planId'] as num?)?.toInt() ?? int.tryParse(id) ?? 0)
          .toString(),
      weeklyPlans: _weeklyPlansFromJson(data['weeklyPlans'] as List<dynamic>?),
      page: (data['page'] as num?)?.toInt() ?? page,
      size: (data['size'] as num?)?.toInt() ?? size,
      totalElements: (data['totalElements'] as num?)?.toInt() ?? 0,
      totalPages: (data['totalPages'] as num?)?.toInt() ?? 0,
      hasNext: data['hasNext'] as bool? ?? false,
    );
  }

  @override
  Future<Map<String, List<DailySpending>>>
  fetchGoalSavingsHistoryByGoalId() async {
    throw UnimplementedError(
      'No dedicated goal savings history API is available.',
    );
  }

  @override
  Future<Goal> updateGoal(Goal goal) async {
    throw UnimplementedError('Goal update API is not available.');
  }

  @override
  Future<List<Goal>> saveGoalPriority(List<Goal> goals) async {
    await _dio.patch(
      'v1/plans/priority',
      data: {
        'plans': [
          for (var index = 0; index < goals.length; index++)
            {
              'planId': int.tryParse(goals[index].id) ?? 0,
              'priority': index + 1,
            },
        ],
      },
    );

    return fetchGoals();
  }

  Future<Goal> _goalFromListJson(Map<String, dynamic> json) async {
    final planId = (json['planId'] as num?)?.toInt() ?? 0;
    final title = json['title'] as String? ?? '';
    final savedAmount = (json['savedAmount'] as num?)?.toInt() ?? 0;
    final targetAmount = (json['targetAmount'] as num?)?.toInt() ?? 0;
    final priority = (json['priority'] as num?)?.toInt() ?? 0;
    final weekNumber = (json['weekNumber'] as num?)?.toInt();
    final dday = (json['dday'] as num?)?.toInt();
    final achievementRate = (json['achievementRate'] as num?)?.toDouble();
    final videoUrl = await _fetchVideoUrlSafely(planId);

    final endDate = _inferEndDate(dday);
    final startDate = _inferStartDate(
      endDate: endDate,
      currentWeek: weekNumber,
      totalWeeks: null,
    );

    return Goal(
      id: planId.toString(),
      name: title,
      targetAmount: targetAmount,
      currentAmount: savedAmount,
      startDate: startDate,
      endDate: endDate,
      priority: priority,
      status: _parseStatus(
        savedAmount: savedAmount,
        targetAmount: targetAmount,
        dday: dday,
        achievementRate: achievementRate,
      ),
      iconName: _inferIconName(title),
      sketchVideoAssetPath: videoUrl,
      weekNumber: weekNumber,
      dday: dday,
      achievementRateOverride: achievementRate,
    );
  }

  Future<String?> _fetchVideoUrlSafely(int planId) async {
    if (planId <= 0) {
      return null;
    }

    try {
      final response = await _dio.get('plans/$planId/video/download');
      final data = response.data;
      if (data is String && data.isNotEmpty) {
        return data;
      }
    } catch (_) {}

    return null;
  }

  GoalStatus _parseStatus({
    required int savedAmount,
    required int targetAmount,
    required int? dday,
    required double? achievementRate,
  }) {
    if ((achievementRate ?? 0) >= 100 ||
        (targetAmount > 0 && savedAmount >= targetAmount)) {
      return GoalStatus.achieved;
    }
    if ((dday ?? 0) < 0) {
      return GoalStatus.failed;
    }
    return GoalStatus.inProgress;
  }

  GoalGraphData? _graphDataFromJson(Map<String, dynamic>? json) {
    if (json == null) {
      return null;
    }

    return GoalGraphData(
      weeks: (json['weeks'] as List<dynamic>? ?? const [])
          .map((item) => (item as num).toInt())
          .toList(),
      planned: (json['planned'] as List<dynamic>? ?? const [])
          .map((item) => (item as num).toInt())
          .toList(),
      actual: (json['actual'] as List<dynamic>? ?? const [])
          .map((item) => (item as num).toInt())
          .toList(),
    );
  }

  List<GoalWeeklyPlan> _weeklyPlansFromJson(List<dynamic>? json) {
    return (json ?? const [])
        .map(
          (item) => GoalWeeklyPlan(
            week: (item['week'] as num?)?.toInt() ?? 0,
            amount: (item['amount'] as num?)?.toInt() ?? 0,
          ),
        )
        .toList();
  }

  GoalWeeklyMission? _weeklyMissionFromJson(Map<String, dynamic>? json) {
    if (json == null) {
      return null;
    }

    return GoalWeeklyMission(
      weeklySavingsAmount: (json['weeklySavingsAmount'] as num?)?.toInt() ?? 0,
      weeksToGoal: (json['weeksToGoal'] as num?)?.toInt() ?? 0,
    );
  }

  GoalWeeklyBudget? _weeklyBudgetFromJson(Map<String, dynamic>? json) {
    if (json == null) {
      return null;
    }

    return GoalWeeklyBudget(
      totalIncomeAmount: (json['totalIncomeAmount'] as num?)?.toInt() ?? 0,
      savingAmount: (json['savingAmount'] as num?)?.toInt() ?? 0,
      fixedExpense: _budgetSectionFromJson(
        json['fixedExpense'] as Map<String, dynamic>?,
      ),
      variableExpense: _budgetSectionFromJson(
        json['variableExpense'] as Map<String, dynamic>?,
      ),
      freeAmount: (json['freeAmount'] as num?)?.toInt() ?? 0,
    );
  }

  GoalBudgetSection _budgetSectionFromJson(Map<String, dynamic>? json) {
    if (json == null) {
      return const GoalBudgetSection();
    }

    return GoalBudgetSection(
      totalAmount: (json['totalAmount'] as num?)?.toInt() ?? 0,
      items: (json['items'] as List<dynamic>? ?? const [])
          .map(
            (item) => GoalBudgetItem(
              category: item['category'] as String? ?? '',
              amount: (item['amount'] as num?)?.toInt() ?? 0,
            ),
          )
          .toList(),
    );
  }

  DateTime _inferEndDate(int? dday) {
    final today = DateTime.now();
    return DateTime(
      today.year,
      today.month,
      today.day,
    ).add(Duration(days: dday ?? 0));
  }

  DateTime _inferStartDate({
    required DateTime endDate,
    required int? currentWeek,
    required int? totalWeeks,
  }) {
    if (totalWeeks != null && totalWeeks > 0) {
      return endDate.subtract(Duration(days: totalWeeks * 7));
    }
    if (currentWeek != null && currentWeek > 0) {
      return DateTime.now().subtract(Duration(days: (currentWeek - 1) * 7));
    }
    return DateTime.now();
  }

  String _inferIconName(String title) {
    if (title.contains('여행') || title.contains('휴가') || title.contains('제주')) {
      return 'travel';
    }
    if (title.contains('노트북') || title.contains('컴퓨터') || title.contains('아이패드')) {
      return 'laptop';
    }
    if (title.contains('비상')) {
      return 'emergency';
    }
    if (title.contains('운동') || title.contains('헬스')) {
      return 'fitness';
    }
    if (title.contains('차') || title.contains('자동차')) {
      return 'car';
    }
    if (title.contains('선물')) {
      return 'gift';
    }
    if (title.contains('전자') || title.contains('기기')) {
      return 'electronics';
    }
    return 'travel';
  }
}
