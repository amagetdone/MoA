import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/presentation/providers/app_data_providers.dart';

class CategoryExpense {
  final int categoryId;
  final String categoryName;
  final int amount;

  CategoryExpense({
    required this.categoryId,
    required this.categoryName,
    required this.amount,
  });

  factory CategoryExpense.fromJson(Map<String, dynamic> json) {
    return CategoryExpense(
      categoryId: json['categoryId'] as int,
      categoryName: json['categoryName'] as String,
      amount: json['amount'] as int,
    );
  }
}

class AnalysisResult {
  final String summaryText;
  final int averageIncome;
  final int averageExpense;
  final int remainingAmount;
  final double fixedExpenseRatio;
  final double variableExpenseRatio;
  final String insightMessage;
  final List<CategoryExpense> categoryExpenses;

  AnalysisResult({
    required this.summaryText,
    required this.averageIncome,
    required this.averageExpense,
    required this.remainingAmount,
    required this.fixedExpenseRatio,
    required this.variableExpenseRatio,
    required this.insightMessage,
    required this.categoryExpenses,
  });

  factory AnalysisResult.fromJson(Map<String, dynamic> json) {
    return AnalysisResult(
      summaryText: json['summaryText'] as String,
      averageIncome: (json['averageIncome'] as num).toInt(),
      averageExpense: (json['averageExpense'] as num).toInt(),
      remainingAmount: (json['remainingAmount'] as num).toInt(),
      fixedExpenseRatio: (json['fixedExpenseRatio'] as num).toDouble(),
      variableExpenseRatio: (json['variableExpenseRatio'] as num).toDouble(),
      insightMessage: json['insightMessage'] as String,
      categoryExpenses: (json['categoryExpenses'] as List<dynamic>)
          .map((e) => CategoryExpense.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}

class PlanBudgetItem {
  final String category;
  final int amount;

  PlanBudgetItem({required this.category, required this.amount});

  factory PlanBudgetItem.fromJson(Map<String, dynamic> json) {
    return PlanBudgetItem(
      category: json['category'] as String,
      amount: (json['amount'] as num).toInt(),
    );
  }
}

class PlanExpense {
  final int totalAmount;
  final List<PlanBudgetItem> items;

  PlanExpense({required this.totalAmount, required this.items});

  factory PlanExpense.fromJson(Map<String, dynamic> json) {
    return PlanExpense(
      totalAmount: (json['totalAmount'] as num).toInt(),
      items: (json['items'] as List<dynamic>)
          .map((e) => PlanBudgetItem.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}

class PlanWeeklyBudget {
  final int totalIncomeAmount;
  final int savingAmount;
  final PlanExpense fixedExpense;
  final PlanExpense variableExpense;
  final int freeAmount;

  PlanWeeklyBudget({
    required this.totalIncomeAmount,
    required this.savingAmount,
    required this.fixedExpense,
    required this.variableExpense,
    required this.freeAmount,
  });

  factory PlanWeeklyBudget.fromJson(Map<String, dynamic> json) {
    return PlanWeeklyBudget(
      totalIncomeAmount: (json['totalIncomeAmount'] as num).toInt(),
      savingAmount: (json['savingAmount'] as num).toInt(),
      fixedExpense: PlanExpense.fromJson(
        json['fixedExpense'] as Map<String, dynamic>,
      ),
      variableExpense: PlanExpense.fromJson(
        json['variableExpense'] as Map<String, dynamic>,
      ),
      freeAmount: (json['freeAmount'] as num).toInt(),
    );
  }
}

class PlanResult {
  final int planId;
  final String planTitle;
  final int weeklySavingsAmount;
  final int weeksToGoal;
  final PlanWeeklyBudget weeklyBudget;

  PlanResult({
    required this.planId,
    required this.planTitle,
    required this.weeklySavingsAmount,
    required this.weeksToGoal,
    required this.weeklyBudget,
  });

  factory PlanResult.fromJson(Map<String, dynamic> json) {
    final mission = json['weeklyMission'] as Map<String, dynamic>;
    return PlanResult(
      planId: (json['planId'] as num).toInt(),
      planTitle: json['planTitle'] as String,
      weeklySavingsAmount: (mission['weeklySavingsAmount'] as num).toInt(),
      weeksToGoal: (mission['weeksToGoal'] as num).toInt(),
      weeklyBudget: PlanWeeklyBudget.fromJson(
        json['weeklyBudget'] as Map<String, dynamic>,
      ),
    );
  }

  factory PlanResult.fromGoal(Goal goal) {
    final mission = goal.weeklyMission ?? const GoalWeeklyMission();
    final budget = goal.weeklyBudget ?? const GoalWeeklyBudget();

    return PlanResult(
      planId: int.tryParse(goal.id) ?? 0,
      planTitle: goal.name,
      weeklySavingsAmount: mission.weeklySavingsAmount > 0
          ? mission.weeklySavingsAmount
          : budget.savingAmount,
      weeksToGoal: mission.weeksToGoal > 0
          ? mission.weeksToGoal
          : goal.totalWeeks,
      weeklyBudget: PlanWeeklyBudget(
        totalIncomeAmount: budget.totalIncomeAmount,
        savingAmount: budget.savingAmount,
        fixedExpense: PlanExpense(
          totalAmount: budget.fixedExpense.totalAmount,
          items: budget.fixedExpense.items
              .map(
                (item) => PlanBudgetItem(
                  category: item.category,
                  amount: item.amount,
                ),
              )
              .toList(),
        ),
        variableExpense: PlanExpense(
          totalAmount: budget.variableExpense.totalAmount,
          items: budget.variableExpense.items
              .map(
                (item) => PlanBudgetItem(
                  category: item.category,
                  amount: item.amount,
                ),
              )
              .toList(),
        ),
        freeAmount: budget.freeAmount,
      ),
    );
  }
}

class OnboardingState {
  final bool isLoading;
  final AnalysisResult? analysisResult;
  final PlanResult? planResult;
  final String? error;

  const OnboardingState({
    this.isLoading = false,
    this.analysisResult,
    this.planResult,
    this.error,
  });

  OnboardingState copyWith({
    bool? isLoading,
    AnalysisResult? analysisResult,
    PlanResult? planResult,
    String? error,
    bool clearError = false,
  }) {
    return OnboardingState(
      isLoading: isLoading ?? this.isLoading,
      analysisResult: analysisResult ?? this.analysisResult,
      planResult: planResult ?? this.planResult,
      error: clearError ? null : error ?? this.error,
    );
  }
}

class OnboardingViewModel extends StateNotifier<OnboardingState> {
  final Dio _dio;
  static final _dateFmt = DateFormat('yyyy-MM-dd');

  OnboardingViewModel(this._dio) : super(const OnboardingState());

  Future<void> syncAndAnalyze() async {
    state = state.copyWith(isLoading: true, clearError: true);
    try {
      await _dio.post(
        'transactions/sync',
        data: {'months': 3, 'forceFullSync': true},
      );

      final response = await _dio.get('transactions/analysis');

      state = state.copyWith(
        isLoading: false,
        analysisResult: AnalysisResult.fromJson(
          response.data as Map<String, dynamic>,
        ),
      );
    } catch (e) {
      state = state.copyWith(isLoading: false, error: e.toString());
    }
  }

  Future<int> loadIncome() async {
    final response = await _dio.get('budgets/income');
    return ((response.data as Map<String, dynamic>)['monthlyIncome'] as num)
        .toInt();
  }

  Future<void> saveIncome(int income) async {
    await _dio.post('budgets/income', data: {'monthlyIncome': income});

    final analysisIncome = state.analysisResult?.averageIncome ?? 0;
    if (income != analysisIncome) {
      await syncAndAnalyze();
    }
  }

  Future<void> createPlan(Goal goal) async {
    state = state.copyWith(isLoading: true, clearError: true);
    try {
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
      state = state.copyWith(
        isLoading: false,
        planResult: PlanResult.fromJson(response.data as Map<String, dynamic>),
      );
    } catch (e) {
      state = state.copyWith(isLoading: false, error: e.toString());
    }
  }

  void cacheCreatedPlan(Goal goal) {
    state = state.copyWith(
      isLoading: false,
      planResult: PlanResult.fromGoal(goal),
      clearError: true,
    );
  }
}

final onboardingViewModelProvider =
    StateNotifierProvider<OnboardingViewModel, OnboardingState>((ref) {
      return OnboardingViewModel(ref.read(apiClientProvider).dio);
    });
