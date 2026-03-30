class Goal {
  final String id;
  final String name;
  final String? description;
  final int targetAmount;
  final int currentAmount;
  final DateTime startDate;
  final DateTime endDate;
  final DateTime? originalEndDate;
  final int priority;
  final GoalStatus status;
  final String? iconName;
  final String? sketchVideoAssetPath;
  // 홈 API에서 받은 값 (int, 0~100)
  final int? dDay;
  final int? apiProgressRate;
  // 목표 상세 API에서 받은 값
  final int? weekNumber;
  final int? dday;
  final double? achievementRateOverride;
  final GoalGraphData? graphData;
  final List<GoalWeeklyPlan> weeklyPlans;
  final GoalWeeklyMission? weeklyMission;
  final GoalWeeklyBudget? weeklyBudget;
  final int? weeklySavedAmountOverride;
  final int? weeklyTargetAmountOverride;
  final int? currentWeekOverride;
  final int? totalWeeksOverride;
  final bool detailLoaded;
  final String? apiStatus;
  final GoalAiPipelineStatus? aiPipelineStatus;

  const Goal({
    required this.id,
    required this.name,
    this.description,
    required this.targetAmount,
    required this.currentAmount,
    required this.startDate,
    required this.endDate,
    this.originalEndDate,
    this.priority = 0,
    this.status = GoalStatus.inProgress,
    this.iconName,
    this.sketchVideoAssetPath,
    this.dDay,
    this.apiProgressRate,
    this.weekNumber,
    this.dday,
    this.achievementRateOverride,
    this.graphData,
    this.weeklyPlans = const [],
    this.weeklyMission,
    this.weeklyBudget,
    this.weeklySavedAmountOverride,
    this.weeklyTargetAmountOverride,
    this.currentWeekOverride,
    this.totalWeeksOverride,
    this.detailLoaded = false,
    this.apiStatus,
    this.aiPipelineStatus,
  });

  double get progressRate {
    if (apiProgressRate != null) {
      return (apiProgressRate! / 100).clamp(0.0, 1.0);
    }
    if (achievementRateOverride != null) {
      return (achievementRateOverride! / 100).clamp(0.0, 1.0);
    }
    return targetAmount > 0
        ? (currentAmount / targetAmount).clamp(0.0, 1.0)
        : 0.0;
  }

  double get overallProgressRate {
    if (achievementRateOverride != null) {
      return (achievementRateOverride! / 100).clamp(0.0, 1.0);
    }
    if (targetAmount > 0) {
      return (currentAmount / targetAmount).clamp(0.0, 1.0);
    }
    if (status == GoalStatus.achieved) {
      return 1.0;
    }
    return 0.0;
  }

  int get remainingAmount =>
      (targetAmount - currentAmount).clamp(0, targetAmount);

  int get currentWeekNumber {
    if (currentWeekOverride != null) {
      if (currentWeekOverride! > 0) {
        return currentWeekOverride!;
      }
      final knownWeekCount = _knownWeekCount;
      if (knownWeekCount > 0) {
        return knownWeekCount;
      }
      return 1;
    }
    if (weekNumber != null && weekNumber! > 0) {
      return weekNumber!;
    }
    final weeks = DateTime.now().difference(startDate).inDays ~/ 7 + 1;
    return weeks.clamp(1, totalWeeks);
  }

  int get totalWeeks {
    if (totalWeeksOverride != null && totalWeeksOverride! > 0) {
      return totalWeeksOverride!;
    }
    final inclusiveDays = endDate.difference(startDate).inDays + 1;
    final weeks = (inclusiveDays + 6) ~/ 7;
    return weeks.clamp(1, 9999);
  }

  int get remainingDays {
    if (dDay != null) return dDay!;
    if (dday != null) return dday!;
    return endDate.difference(DateTime.now()).inDays;
  }

  bool get hasExtendedDeadline {
    final original = originalEndDate;
    if (original == null) {
      return false;
    }
    return !_isSameCalendarDay(original, endDate);
  }

  bool get hasDetailData =>
      detailLoaded ||
      graphData != null ||
      weeklyPlans.isNotEmpty ||
      description != null;

  bool get isAiPending =>
      aiPipelineStatus == GoalAiPipelineStatus.pending ||
      aiPipelineStatus == GoalAiPipelineStatus.processing;

  bool get isAiFailed => aiPipelineStatus == GoalAiPipelineStatus.failed;

  bool get isAiSucceeded => aiPipelineStatus == GoalAiPipelineStatus.succeeded;

  bool get isEffectivelyAchieved {
    if (status == GoalStatus.achieved) {
      return true;
    }
    if ((apiProgressRate ?? 0) >= 100) {
      return true;
    }
    if ((achievementRateOverride ?? 0) >= 100) {
      return true;
    }
    return targetAmount > 0 && currentAmount >= targetAmount;
  }

  bool get isEffectivelyInProgress {
    if (isEffectivelyAchieved) {
      return false;
    }
    return status == GoalStatus.inProgress;
  }

  int get weeklySavedAmount {
    if (weeklySavedAmountOverride != null) {
      return weeklySavedAmountOverride!;
    }
    return currentAmount;
  }

  int get weeklyTargetAmount {
    if (weeklyTargetAmountOverride != null) {
      return weeklyTargetAmountOverride!;
    }
    if (weeklyMission != null && weeklyMission!.weeklySavingsAmount > 0) {
      return weeklyMission!.weeklySavingsAmount;
    }
    return targetAmount;
  }

  int? get homeWeeklyPlannedAmount {
    final currentWeekPlan = _currentWeekPlanAmount;
    if (currentWeekPlan != null) {
      return currentWeekPlan;
    }

    final currentWeekActual = _currentWeekGraphActualAmount;
    if (currentWeekActual != null) {
      return currentWeekActual;
    }

    if (_hasDistinctWeeklyOverrides && weeklySavedAmountOverride != null) {
      return weeklySavedAmountOverride;
    }

    if (homeWeeklyTargetAmount != null) {
      return 0;
    }

    return null;
  }

  int? get homeWeeklyTargetAmount {
    final currentWeekPlanned = _currentWeekGraphPlannedAmount;
    if (currentWeekPlanned != null && currentWeekPlanned > 0) {
      return currentWeekPlanned;
    }

    final missionAmount = weeklyMission?.weeklySavingsAmount ?? 0;
    if (missionAmount > 0) {
      return missionAmount;
    }

    final weeklyBudgetSaving = weeklyBudget?.savingAmount ?? 0;
    if (weeklyBudgetSaving > 0) {
      return weeklyBudgetSaving;
    }

    if (_hasDistinctWeeklyOverrides && weeklyTargetAmountOverride != null) {
      return weeklyTargetAmountOverride;
    }

    return null;
  }

  GoalStatus get effectiveStatus {
    if (isEffectivelyAchieved) {
      return GoalStatus.achieved;
    }
    return status;
  }

  Goal copyWith({
    String? id,
    String? name,
    String? description,
    int? targetAmount,
    int? currentAmount,
    DateTime? startDate,
    DateTime? endDate,
    DateTime? originalEndDate,
    int? priority,
    GoalStatus? status,
    String? iconName,
    String? sketchVideoAssetPath,
    int? dDay,
    int? apiProgressRate,
    int? weekNumber,
    int? dday,
    double? achievementRateOverride,
    GoalGraphData? graphData,
    List<GoalWeeklyPlan>? weeklyPlans,
    GoalWeeklyMission? weeklyMission,
    GoalWeeklyBudget? weeklyBudget,
    int? weeklySavedAmountOverride,
    int? weeklyTargetAmountOverride,
    int? currentWeekOverride,
    int? totalWeeksOverride,
    bool? detailLoaded,
    String? apiStatus,
    GoalAiPipelineStatus? aiPipelineStatus,
    bool clearSketchVideoAssetPath = false,
  }) {
    return Goal(
      id: id ?? this.id,
      name: name ?? this.name,
      description: description ?? this.description,
      targetAmount: targetAmount ?? this.targetAmount,
      currentAmount: currentAmount ?? this.currentAmount,
      startDate: startDate ?? this.startDate,
      endDate: endDate ?? this.endDate,
      originalEndDate: originalEndDate ?? this.originalEndDate,
      priority: priority ?? this.priority,
      status: status ?? this.status,
      iconName: iconName ?? this.iconName,
      sketchVideoAssetPath: clearSketchVideoAssetPath
          ? null
          : sketchVideoAssetPath ?? this.sketchVideoAssetPath,
      dDay: dDay ?? this.dDay,
      apiProgressRate: apiProgressRate ?? this.apiProgressRate,
      weekNumber: weekNumber ?? this.weekNumber,
      dday: dday ?? this.dday,
      achievementRateOverride:
          achievementRateOverride ?? this.achievementRateOverride,
      graphData: graphData ?? this.graphData,
      weeklyPlans: weeklyPlans ?? this.weeklyPlans,
      weeklyMission: weeklyMission ?? this.weeklyMission,
      weeklyBudget: weeklyBudget ?? this.weeklyBudget,
      weeklySavedAmountOverride:
          weeklySavedAmountOverride ?? this.weeklySavedAmountOverride,
      weeklyTargetAmountOverride:
          weeklyTargetAmountOverride ?? this.weeklyTargetAmountOverride,
      currentWeekOverride: currentWeekOverride ?? this.currentWeekOverride,
      totalWeeksOverride: totalWeeksOverride ?? this.totalWeeksOverride,
      detailLoaded: detailLoaded ?? this.detailLoaded,
      apiStatus: apiStatus ?? this.apiStatus,
      aiPipelineStatus: aiPipelineStatus ?? this.aiPipelineStatus,
    );
  }

  int? get _currentWeekPlanAmount {
    for (final plan in weeklyPlans) {
      if (plan.week == currentWeekNumber) {
        return plan.amount;
      }
    }
    return null;
  }

  int? get _currentWeekGraphPlannedAmount {
    final planned = graphData?.planned;
    if (planned == null || planned.isEmpty) {
      return null;
    }

    final index = currentWeekNumber - 1;
    if (index < 0 || index >= planned.length) {
      return null;
    }

    return planned[index];
  }

  int? get _currentWeekGraphActualAmount {
    final actual = graphData?.actual;
    if (actual == null || actual.isEmpty) {
      return null;
    }

    final index = currentWeekNumber - 1;
    if (index < 0 || index >= actual.length) {
      return null;
    }

    return actual[index];
  }

  bool get _hasDistinctWeeklyOverrides {
    final weeklySaved = weeklySavedAmountOverride;
    final weeklyTarget = weeklyTargetAmountOverride;
    if (weeklySaved == null && weeklyTarget == null) {
      return false;
    }

    return weeklySaved != currentAmount || weeklyTarget != targetAmount;
  }

  int get _knownWeekCount {
    var maxWeek = 0;

    for (final plan in weeklyPlans) {
      if (plan.week > maxWeek) {
        maxWeek = plan.week;
      }
    }

    final graph = graphData;
    if (graph != null) {
      for (final week in graph.weeks) {
        if (week > maxWeek) {
          maxWeek = week;
        }
      }
      if (graph.planned.length > maxWeek) {
        maxWeek = graph.planned.length;
      }
      if (graph.actual.length > maxWeek) {
        maxWeek = graph.actual.length;
      }
    }

    if (totalWeeksOverride != null && totalWeeksOverride! > maxWeek) {
      maxWeek = totalWeeksOverride!;
    }

    return maxWeek;
  }
}

bool _isSameCalendarDay(DateTime left, DateTime right) {
  return left.year == right.year &&
      left.month == right.month &&
      left.day == right.day;
}

class GoalGraphData {
  final List<int> weeks;
  final List<int> planned;
  final List<int> actual;

  const GoalGraphData({
    this.weeks = const [],
    this.planned = const [],
    this.actual = const [],
  });
}

class GoalWeeklyPlan {
  final int week;
  final int amount;

  const GoalWeeklyPlan({required this.week, required this.amount});
}

class GoalWeeklyPlanPage {
  final String goalId;
  final List<GoalWeeklyPlan> weeklyPlans;
  final int page;
  final int size;
  final int totalElements;
  final int totalPages;
  final bool hasNext;

  const GoalWeeklyPlanPage({
    required this.goalId,
    this.weeklyPlans = const [],
    this.page = 0,
    this.size = 0,
    this.totalElements = 0,
    this.totalPages = 0,
    this.hasNext = false,
  });
}

class GoalWeeklyMission {
  final int weeklySavingsAmount;
  final int weeksToGoal;

  const GoalWeeklyMission({this.weeklySavingsAmount = 0, this.weeksToGoal = 0});
}

class GoalBudgetItem {
  final String category;
  final int amount;

  const GoalBudgetItem({required this.category, required this.amount});
}

class GoalBudgetSection {
  final int totalAmount;
  final List<GoalBudgetItem> items;

  const GoalBudgetSection({this.totalAmount = 0, this.items = const []});
}

class GoalWeeklyBudget {
  final int totalIncomeAmount;
  final int savingAmount;
  final GoalBudgetSection fixedExpense;
  final GoalBudgetSection variableExpense;
  final int freeAmount;

  const GoalWeeklyBudget({
    this.totalIncomeAmount = 0,
    this.savingAmount = 0,
    this.fixedExpense = const GoalBudgetSection(),
    this.variableExpense = const GoalBudgetSection(),
    this.freeAmount = 0,
  });
}

enum GoalStatus { inProgress, achieved, failed }

enum GoalAiPipelineStatus { pending, processing, succeeded, failed }

GoalAiPipelineStatus? parseGoalAiPipelineStatus(String? value) {
  switch (value?.toUpperCase()) {
    case 'PENDING':
      return GoalAiPipelineStatus.pending;
    case 'PROCESSING':
      return GoalAiPipelineStatus.processing;
    case 'SUCCEEDED':
      return GoalAiPipelineStatus.succeeded;
    case 'FAILED':
      return GoalAiPipelineStatus.failed;
    default:
      return null;
  }
}

String? goalAiPipelineStatusToWire(GoalAiPipelineStatus? status) {
  switch (status) {
    case GoalAiPipelineStatus.pending:
      return 'PENDING';
    case GoalAiPipelineStatus.processing:
      return 'PROCESSING';
    case GoalAiPipelineStatus.succeeded:
      return 'SUCCEEDED';
    case GoalAiPipelineStatus.failed:
      return 'FAILED';
    case null:
      return null;
  }
}
