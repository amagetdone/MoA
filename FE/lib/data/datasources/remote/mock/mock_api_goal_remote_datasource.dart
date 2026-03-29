import 'dart:convert';
import 'dart:math' as math;

import 'package:flutter/services.dart';
import 'package:intl/intl.dart';
import 'package:more_app/data/datasources/remote/goal_remote_datasource.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/entities/spending.dart';

class MockApiGoalRemoteDataSource implements GoalRemoteDataSource {
  MockApiGoalRemoteDataSource({AssetBundle? assetBundle})
    : _assetBundle = assetBundle ?? rootBundle;

  static const _plansListPath = 'assets/mock_api/plans_get.json';
  static const _planCreatePath = 'assets/mock_api/plans_post.json';
  static const _planDetail17Path = 'assets/mock_api/plans_detail_17.json';
  static const _planWeeklyPlans17Path =
      'assets/mock_api/plans_weekly_plans_17_page0_size5.json';
  static const _planVideoSuccessPath =
      'assets/mock_api/plans_video_download_success.json';
  static const _planPriorityPatchPath =
      'assets/mock_api/plans_patch_priority.json';
  static final _dateFmt = DateFormat('yyyy-MM-dd');

  static bool _initialized = false;
  static List<Map<String, dynamic>> _planListItems = [];
  static final Map<int, Map<String, dynamic>> _planDetails = {};
  static final Map<int, GoalWeeklyPlanPage> _seededWeeklyPlanPages = {};
  static final Map<int, String> _videoUrls = {};
  static Map<String, dynamic> _createTemplate = {};
  static Map<String, dynamic> _priorityPatchTemplate = {};
  static int _nextPlanId = 100;

  final AssetBundle _assetBundle;

  @override
  Future<List<Goal>> fetchGoals() async {
    await _ensureLoaded();
    await Future.delayed(const Duration(milliseconds: 180));
    final goals = await Future.wait(_planListItems.map(_goalFromListJson));
    goals.sort((a, b) => a.priority.compareTo(b.priority));
    return goals;
  }

  @override
  Future<Goal?> findGoalById(String id) async {
    await _ensureLoaded();
    await Future.delayed(const Duration(milliseconds: 180));

    final planId = int.tryParse(id);
    if (planId == null) {
      return null;
    }

    final detailJson = _planDetails[planId];
    if (detailJson != null) {
      return _goalFromDetailJson(detailJson);
    }

    final listJson = _findListJson(planId);
    if (listJson == null) {
      return null;
    }

    final synthesizedDetail = _buildDetailJsonFromList(listJson);
    _planDetails[planId] = synthesizedDetail;
    return _goalFromDetailJson(synthesizedDetail);
  }

  @override
  Future<void> deleteGoal(String id) async {
    await _ensureLoaded();
    await Future.delayed(const Duration(milliseconds: 180));

    final planId = int.tryParse(id);
    if (planId == null) {
      return;
    }

    _planListItems.removeWhere(
      (item) => ((item['planId'] as num?)?.toInt() ?? 0) == planId,
    );
    _planDetails.remove(planId);
    _seededWeeklyPlanPages.remove(planId);
    _videoUrls.remove(planId);
  }

  @override
  Future<GoalWeeklyPlanPage> fetchGoalWeeklyPlans(
    String id, {
    int page = 0,
    int size = 100,
  }) async {
    await _ensureLoaded();
    await Future.delayed(const Duration(milliseconds: 180));

    final planId = int.tryParse(id);
    if (planId == null) {
      return GoalWeeklyPlanPage(goalId: id);
    }

    final seededPage = _seededWeeklyPlanPages[planId];
    if (seededPage != null && page == 0 && size == seededPage.size) {
      return seededPage;
    }

    final detailJson = _planDetails[planId] ?? _findListJson(planId);
    if (detailJson == null) {
      return GoalWeeklyPlanPage(goalId: id);
    }

    final normalizedDetail = _planDetails[planId] ?? _buildDetailJsonFromList(detailJson);
    _planDetails[planId] = normalizedDetail;
    final weeklyPlans = _weeklyPlansFromJson(
      normalizedDetail['weeklyPlans'] as List<dynamic>?,
    );

    final start = page * size;
    final end = math.min(start + size, weeklyPlans.length);
    final sliced = start >= weeklyPlans.length
        ? const <GoalWeeklyPlan>[]
        : weeklyPlans.sublist(start, end);
    final totalPages = weeklyPlans.isEmpty ? 0 : ((weeklyPlans.length - 1) ~/ size) + 1;

    return GoalWeeklyPlanPage(
      goalId: id,
      weeklyPlans: sliced,
      page: page,
      size: size,
      totalElements: weeklyPlans.length,
      totalPages: totalPages,
      hasNext: page + 1 < totalPages,
    );
  }

  @override
  Future<Map<String, List<DailySpending>>> fetchGoalSavingsHistoryByGoalId() async {
    throw UnimplementedError(
      'Mock goal API does not provide savings history directly.',
    );
  }

  @override
  Future<Goal> createGoal(Goal goal) async {
    await _ensureLoaded();
    await Future.delayed(const Duration(milliseconds: 180));

    final createdPlanId = _nextPlanId++;
    final weeksToGoal = _computeWeeksToGoal(goal.startDate, goal.endDate);
    final weeklySavingsAmount = _computeWeeklySavings(
      goal.targetAmount,
      weeksToGoal,
    );
    final today = DateTime.now();
    final dday = DateTime(
      goal.endDate.year,
      goal.endDate.month,
      goal.endDate.day,
    ).difference(DateTime(today.year, today.month, today.day)).inDays;

    final createdResponse = _cloneMap(_createTemplate);
    createdResponse['planId'] = createdPlanId;
    createdResponse['planTitle'] = goal.name;
    createdResponse['status'] = 'ACTIVE';
    createdResponse['weeklyMission'] = {
      'weeklySavingsAmount': weeklySavingsAmount,
      'weeksToGoal': weeksToGoal,
    };

    final weeklyBudget = _cloneMap(
      (createdResponse['weeklyBudget'] as Map<String, dynamic>? ?? const {}),
    );
    final totalIncomeAmount =
        (weeklyBudget['totalIncomeAmount'] as num?)?.toInt() ?? 950000;
    final fixedExpenseTotal =
        ((weeklyBudget['fixedExpense'] as Map<String, dynamic>?)
                ?['totalAmount'] as num?)
            ?.toInt() ??
        0;
    final variableExpenseTotal =
        ((weeklyBudget['variableExpense'] as Map<String, dynamic>?)
                ?['totalAmount'] as num?)
            ?.toInt() ??
        0;
    weeklyBudget['savingAmount'] = weeklySavingsAmount;
    weeklyBudget['freeAmount'] =
        totalIncomeAmount - fixedExpenseTotal - variableExpenseTotal - weeklySavingsAmount;
    createdResponse['weeklyBudget'] = weeklyBudget;

    final listJson = <String, dynamic>{
      'planId': createdPlanId,
      'title': goal.name,
      'savedAmount': 0,
      'targetAmount': goal.targetAmount,
      'priority': goal.priority,
      'weekNumber': 1,
      'dday': dday,
      'achievementRate': 0,
      'endDate': _formatDate(goal.endDate),
      'originalEndDate': _formatDate(goal.originalEndDate ?? goal.endDate),
    };
    _planListItems.add(listJson);

    final detailJson = {
      'planId': createdPlanId,
      'title': goal.name,
      'description': goal.description ?? '',
      'priority': goal.priority,
      'targetAmount': goal.targetAmount,
      'savedAmount': 0,
      'achievementRate': 0,
      'dday': dday,
      'endDate': _formatDate(goal.endDate),
      'originalEndDate': _formatDate(goal.originalEndDate ?? goal.endDate),
      'graphData': _buildGraphDataJson(
        weeksToGoal: weeksToGoal,
        weeklySavingsAmount: weeklySavingsAmount,
      ),
      'weeklyPlans': _buildWeeklyPlansJson(
        weeksToGoal: weeksToGoal,
        weeklySavingsAmount: weeklySavingsAmount,
        targetAmount: goal.targetAmount,
      ),
      'currentWeek': 1,
      'totalWeeks': weeksToGoal,
    };
    _planDetails[createdPlanId] = detailJson;

    return Goal(
      id: createdPlanId.toString(),
      name: goal.name,
      description: goal.description,
      targetAmount: goal.targetAmount,
      currentAmount: 0,
      startDate: goal.startDate,
      endDate: goal.endDate,
      originalEndDate: goal.originalEndDate ?? goal.endDate,
      priority: goal.priority,
      status: GoalStatus.inProgress,
      iconName: goal.iconName ?? _inferIconName(goal.name),
      weekNumber: 1,
      dday: dday,
      achievementRateOverride: 0,
      graphData: _graphDataFromJson(detailJson['graphData'] as Map<String, dynamic>?),
      weeklyPlans: _weeklyPlansFromJson(detailJson['weeklyPlans'] as List<dynamic>?),
      weeklyMission: GoalWeeklyMission(
        weeklySavingsAmount: weeklySavingsAmount,
        weeksToGoal: weeksToGoal,
      ),
      weeklyBudget: _weeklyBudgetFromJson(
        createdResponse['weeklyBudget'] as Map<String, dynamic>?,
      ),
      currentWeekOverride: 1,
      totalWeeksOverride: weeksToGoal,
      detailLoaded: true,
      apiStatus: createdResponse['status'] as String?,
      aiPipelineStatus: GoalAiPipelineStatus.pending,
    );
  }

  @override
  Future<Goal> updateGoal(Goal goal) async {
    await _ensureLoaded();
    await Future.delayed(const Duration(milliseconds: 180));

    final planId = int.tryParse(goal.id);
    if (planId == null) {
      throw StateError('Invalid plan id.');
    }

    final listJson = _findListJson(planId);
    if (listJson == null) {
      throw StateError('Goal not found.');
    }

    listJson['title'] = goal.name;
    listJson['targetAmount'] = goal.targetAmount;
    listJson['priority'] = goal.priority;
    listJson['savedAmount'] = goal.currentAmount;
    listJson['dday'] = goal.remainingDays;
    listJson['achievementRate'] = goal.progressRate * 100;
    listJson['endDate'] = _formatDate(goal.endDate);
    listJson['originalEndDate'] = _formatDate(
      goal.originalEndDate ?? goal.endDate,
    );

    final detailJson = _planDetails[planId] ?? <String, dynamic>{'planId': planId};
    detailJson['title'] = goal.name;
    detailJson['description'] = goal.description ?? '';
    detailJson['priority'] = goal.priority;
    detailJson['targetAmount'] = goal.targetAmount;
    detailJson['savedAmount'] = goal.currentAmount;
    detailJson['achievementRate'] = goal.progressRate * 100;
    detailJson['dday'] = goal.remainingDays;
    detailJson['endDate'] = _formatDate(goal.endDate);
    detailJson['originalEndDate'] = _formatDate(
      goal.originalEndDate ?? goal.endDate,
    );
    detailJson['currentWeek'] = goal.currentWeekNumber;
    detailJson['totalWeeks'] = goal.totalWeeks;
    detailJson['weeklyPlans'] = goal.weeklyPlans
        .map((item) => {'week': item.week, 'amount': item.amount})
        .toList();
    detailJson['graphData'] = {
      'weeks': goal.graphData?.weeks ?? const <int>[],
      'planned': goal.graphData?.planned ?? const <int>[],
      'actual': goal.graphData?.actual ?? const <int>[],
    };
    _planDetails[planId] = detailJson;

    return goal.copyWith(
      aiPipelineStatus: GoalAiPipelineStatus.pending,
      clearSketchVideoAssetPath: true,
    );
  }

  @override
  Future<List<Goal>> saveGoalPriority(List<Goal> goals) async {
    await _ensureLoaded();
    await Future.delayed(const Duration(milliseconds: 180));

    for (var index = 0; index < goals.length; index++) {
      final planId = int.tryParse(goals[index].id);
      if (planId == null) {
        continue;
      }

      final priority = index + 1;
      final listJson = _findListJson(planId);
      if (listJson != null) {
        listJson['priority'] = priority;
      }

      final detailJson = _planDetails[planId];
      if (detailJson != null) {
        detailJson['priority'] = priority;
      }
    }

    _priorityPatchTemplate = {
      ..._priorityPatchTemplate,
      'plans': [
        for (var index = 0; index < goals.length; index++)
          {
            'planId': int.tryParse(goals[index].id) ?? 0,
            'priority': index + 1,
          },
      ],
    };

    _planListItems.sort(
      (a, b) => ((a['priority'] as num?)?.toInt() ?? 0).compareTo(
        (b['priority'] as num?)?.toInt() ?? 0,
      ),
    );

    return fetchGoals();
  }

  Future<void> _ensureLoaded() async {
    if (_initialized) {
      return;
    }

    final listJson = await _loadJson(_plansListPath);
    _planListItems = ((listJson['data'] as List<dynamic>? ?? const []))
        .map((item) => _cloneMap(item as Map<String, dynamic>))
        .toList();

    final createJson = await _loadJson(_planCreatePath);
    _createTemplate = _cloneMap(createJson['data'] as Map<String, dynamic>);

    final priorityPatchJson = await _loadJson(_planPriorityPatchPath);
    _priorityPatchTemplate = _cloneMap(
      priorityPatchJson['data'] as Map<String, dynamic>? ?? const {},
    );

    final detailJson = await _loadJson(_planDetail17Path);
    final detailData = _cloneMap(detailJson['data'] as Map<String, dynamic>);
    final detailPlanId = (detailData['planId'] as num?)?.toInt() ?? 17;
    _planDetails[detailPlanId] = detailData;

    final weeklyPlansJson = await _loadJson(_planWeeklyPlans17Path);
    final weeklyPlansData = weeklyPlansJson['data'] as Map<String, dynamic>;
    _seededWeeklyPlanPages[detailPlanId] = GoalWeeklyPlanPage(
      goalId:
          ((weeklyPlansData['planId'] as num?)?.toInt() ?? detailPlanId)
              .toString(),
      weeklyPlans: _weeklyPlansFromJson(
        weeklyPlansData['weeklyPlans'] as List<dynamic>?,
      ),
      page: (weeklyPlansData['page'] as num?)?.toInt() ?? 0,
      size: (weeklyPlansData['size'] as num?)?.toInt() ?? 0,
      totalElements: (weeklyPlansData['totalElements'] as num?)?.toInt() ?? 0,
      totalPages: (weeklyPlansData['totalPages'] as num?)?.toInt() ?? 0,
      hasNext: weeklyPlansData['hasNext'] as bool? ?? false,
    );

    final videoJson = await _loadJson(_planVideoSuccessPath);
    final videoUrl = videoJson['data'] as String?;
    if (videoUrl != null && videoUrl.isNotEmpty) {
      _videoUrls[detailPlanId] = videoUrl;
    }

    _nextPlanId =
        _planListItems
            .map((item) => (item['planId'] as num?)?.toInt() ?? 0)
            .fold<int>(0, math.max) +
        1;
    _initialized = true;
  }

  Future<Map<String, dynamic>> _loadJson(String assetPath) async {
    final jsonString = await _assetBundle.loadString(assetPath);
    return jsonDecode(jsonString) as Map<String, dynamic>;
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

    final endDate = _parseDate(json['endDate']) ?? _inferEndDate(dday);
    final originalEndDate = _parseDate(json['originalEndDate']);
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
      originalEndDate: originalEndDate,
      priority: priority,
      status: _parseStatus(
        savedAmount: savedAmount,
        targetAmount: targetAmount,
        dday: dday,
        achievementRate: achievementRate,
      ),
      iconName: _inferIconName(title),
      sketchVideoAssetPath: _videoUrls[planId],
      weekNumber: weekNumber,
      dday: dday,
      achievementRateOverride: achievementRate,
      aiPipelineStatus: _videoUrls[planId] == null
          ? null
          : GoalAiPipelineStatus.succeeded,
    );
  }

  Future<Goal> _goalFromDetailJson(Map<String, dynamic> json) async {
    final planId = (json['planId'] as num?)?.toInt() ?? 0;
    final title = json['title'] as String? ?? '';
    final targetAmount = (json['targetAmount'] as num?)?.toInt() ?? 0;
    final savedAmount = (json['savedAmount'] as num?)?.toInt() ?? 0;
    final priority = (json['priority'] as num?)?.toInt() ?? 0;
    final dday = (json['dday'] as num?)?.toInt();
    final achievementRate = (json['achievementRate'] as num?)?.toDouble();
    final currentWeek = (json['currentWeek'] as num?)?.toInt();
    final totalWeeks = (json['totalWeeks'] as num?)?.toInt();

    final endDate = _parseDate(json['endDate']) ?? _inferEndDate(dday);
    final originalEndDate = _parseDate(json['originalEndDate']);
    final startDate = _inferStartDate(
      endDate: endDate,
      currentWeek: currentWeek,
      totalWeeks: totalWeeks,
    );

    return Goal(
      id: planId.toString(),
      name: title,
      description: json['description'] as String?,
      targetAmount: targetAmount,
      currentAmount: savedAmount,
      startDate: startDate,
      endDate: endDate,
      originalEndDate: originalEndDate,
      priority: priority,
      status: _parseStatus(
        savedAmount: savedAmount,
        targetAmount: targetAmount,
        dday: dday,
        achievementRate: achievementRate,
      ),
      iconName: _inferIconName(title),
      sketchVideoAssetPath: _videoUrls[planId],
      weekNumber: currentWeek,
      dday: dday,
      achievementRateOverride: achievementRate,
      graphData: _graphDataFromJson(json['graphData'] as Map<String, dynamic>?),
      weeklyPlans: _weeklyPlansFromJson(json['weeklyPlans'] as List<dynamic>?),
      currentWeekOverride: currentWeek,
      totalWeeksOverride: totalWeeks,
      detailLoaded: true,
      aiPipelineStatus: _videoUrls[planId] == null
          ? null
          : GoalAiPipelineStatus.succeeded,
    );
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

  DateTime? _parseDate(dynamic value) {
    if (value is! String || value.isEmpty) {
      return null;
    }

    try {
      final parsed = DateTime.parse(value);
      return DateTime(parsed.year, parsed.month, parsed.day);
    } catch (_) {
      return null;
    }
  }

  String _formatDate(DateTime value) {
    return _dateFmt.format(value);
  }

  DateTime _inferEndDate(int? dday) {
    final today = DateTime.now();
    return DateTime(today.year, today.month, today.day).add(
      Duration(days: dday ?? 0),
    );
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

  int _computeWeeksToGoal(DateTime startDate, DateTime endDate) {
    final weeks = ((endDate.difference(startDate).inDays) / 7).ceil();
    return weeks < 1 ? 1 : weeks;
  }

  int _computeWeeklySavings(int targetAmount, int weeksToGoal) {
    if (weeksToGoal <= 0) {
      return targetAmount;
    }
    return (targetAmount / weeksToGoal).ceil();
  }

  Map<String, dynamic> _buildGraphDataJson({
    required int weeksToGoal,
    required int weeklySavingsAmount,
  }) {
    final weeks = <int>[];
    final planned = <int>[];
    final actual = <int>[];
    var plannedCumulative = 0;

    for (var week = 1; week <= weeksToGoal; week++) {
      plannedCumulative += weeklySavingsAmount;
      weeks.add(week);
      planned.add(plannedCumulative);
      actual.add(0);
    }

    return {
      'weeks': weeks,
      'planned': planned,
      'actual': actual,
    };
  }

  List<Map<String, dynamic>> _buildWeeklyPlansJson({
    required int weeksToGoal,
    required int weeklySavingsAmount,
    required int targetAmount,
  }) {
    final plans = <Map<String, dynamic>>[];
    var remaining = targetAmount;

    for (var week = 1; week <= weeksToGoal; week++) {
      final amount = week == weeksToGoal
          ? remaining
          : math.min(weeklySavingsAmount, remaining);
      remaining -= amount;
      plans.add({'week': week, 'amount': amount});
    }

    return plans;
  }

  Map<String, dynamic> _buildDetailJsonFromList(Map<String, dynamic> listJson) {
    final planId = (listJson['planId'] as num?)?.toInt() ?? 0;
    final targetAmount = (listJson['targetAmount'] as num?)?.toInt() ?? 0;
    final savedAmount = (listJson['savedAmount'] as num?)?.toInt() ?? 0;
    final weekNumber = math.max((listJson['weekNumber'] as num?)?.toInt() ?? 1, 1);
    final achievementRate = (listJson['achievementRate'] as num?)?.toDouble() ?? 0;
    final remainingAmount = math.max(targetAmount - savedAmount, 0);
    final weeklySavingsAmount = _computeWeeklySavings(remainingAmount, weekNumber);

    return {
      'planId': planId,
      'title': listJson['title'],
      'description': '',
      'priority': listJson['priority'],
      'targetAmount': targetAmount,
      'savedAmount': savedAmount,
      'achievementRate': achievementRate,
      'dday': listJson['dday'],
      'endDate': listJson['endDate'],
      'originalEndDate': listJson['originalEndDate'],
      'graphData': _buildGraphDataJson(
        weeksToGoal: weekNumber,
        weeklySavingsAmount: weeklySavingsAmount,
      ),
      'weeklyPlans': _buildWeeklyPlansJson(
        weeksToGoal: weekNumber,
        weeklySavingsAmount: weeklySavingsAmount,
        targetAmount: remainingAmount,
      ),
      'currentWeek': weekNumber,
      'totalWeeks': weekNumber,
    };
  }

  Map<String, dynamic> _cloneMap(Map<String, dynamic> json) {
    return jsonDecode(jsonEncode(json)) as Map<String, dynamic>;
  }

  Map<String, dynamic>? _findListJson(int planId) {
    for (final item in _planListItems) {
      if ((item['planId'] as num?)?.toInt() == planId) {
        return item;
      }
    }
    return null;
  }

  String _inferIconName(String title) {
    final normalized = title.toLowerCase();
    if (normalized.contains('travel')) {
      return 'travel';
    }
    if (normalized.contains('laptop') || normalized.contains('notebook')) {
      return 'laptop';
    }
    if (normalized.contains('car')) {
      return 'car';
    }
    if (normalized.contains('gift')) {
      return 'gift';
    }
    if (normalized.contains('fitness')) {
      return 'fitness';
    }
    if (normalized.contains('electronic')) {
      return 'electronics';
    }
    return 'travel';
  }
}
