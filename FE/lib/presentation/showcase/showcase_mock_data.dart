import 'package:more_app/domain/entities/account.dart';
import 'package:more_app/domain/entities/budget.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/entities/spending.dart';
import 'package:more_app/presentation/viewmodels/goal_viewmodel.dart';
import 'package:more_app/presentation/viewmodels/home_viewmodel.dart';
import 'package:more_app/presentation/viewmodels/spending_budget_viewmodel.dart';
import 'package:more_app/presentation/viewmodels/spending_viewmodel.dart';

class ShowcaseMockData {
  ShowcaseMockData._();

  static HomeState homeState() {
    return HomeState(
      isLoading: false,
      status: HomeStatus.ready,
      accounts: _demoAccounts,
      activeGoals: _inProgressGoals,
      totalAsset: 5737000,
      weeklyBudget: 288000,
      weeklySpent: 166500,
      weeklyRemaining: 121500,
      daysRemaining: _daysRemainingInWeek(),
      dailyAvailable: 40500,
      weeklySaved: 52100,
      savingTargetAmount: 75000,
      savingProgressRate: 69,
    );
  }

  static SpendingState spendingState({required SpendingState base}) {
    return base.copyWith(
      isLoading: false,
      isPeriodLoading: false,
      clearErrorMessage: true,
      categories: base.isWeekly ? _weeklyCategories : _monthlyCategories,
      dailySpendings: _weeklySpendings(base.selectedWeekStart),
      monthlySpendings: _monthlySpendings(base.selectedMonthStart),
      weeklyBudget: base.weeklyBudget > 0 ? base.weeklyBudget : 288000,
      monthlyIncome: base.monthlyIncome > 0 ? base.monthlyIncome : 3400000,
      monthlySavingsGoal: base.monthlySavingsGoal > 0
          ? base.monthlySavingsGoal
          : 900000,
      monthlyFixedExpense: base.monthlyFixedExpense > 0
          ? base.monthlyFixedExpense
          : 1120000,
      weeklyExpenseAmount: 166500,
      monthlyExpenseAmount: 742000,
      touchedPieIndex: null,
    );
  }

  static SpendingBudgetState spendingBudgetState() {
    return const SpendingBudgetState(
      isLoading: false,
      isSaving: false,
      setup: BudgetSetup(
        monthlyIncome: 3400000,
        savingsAmount: 150000,
        fixedExpenseAmount: 210000,
        variableExpenseAmount: 185000,
        freeAmount: 230000,
        remainingFreeAmount: 168000,
        fixedEssentials: [
          BudgetEssential(
            categoryName: '주거/통신',
            plannedAmount: 110000,
            actualSpentAmount: 108000,
          ),
          BudgetEssential(
            categoryName: '구독료',
            plannedAmount: 25000,
            actualSpentAmount: 25000,
          ),
          BudgetEssential(
            categoryName: '금융/보험',
            plannedAmount: 75000,
            actualSpentAmount: 75000,
          ),
        ],
        variableEssentials: [
          BudgetEssential(
            categoryName: '식비',
            plannedAmount: 92000,
            actualSpentAmount: 74000,
          ),
          BudgetEssential(
            categoryName: '교통비',
            plannedAmount: 54000,
            actualSpentAmount: 31500,
          ),
          BudgetEssential(
            categoryName: '카페',
            plannedAmount: 39000,
            actualSpentAmount: 28500,
          ),
        ],
      ),
    );
  }

  static GoalState goalState({required bool showAchieved}) {
    return GoalState(
      isLoading: false,
      goals: _allGoals,
      showAchieved: showAchieved,
      savingsHistoryByGoalId: {
        for (final goal in _allGoals) goal.id: const [],
      },
    );
  }

  static List<SpendingCategory> homeSpendingCategoriesForOffset(int offset) {
    switch (offset) {
      case 0:
        return _weeklyCategories;
      case 1:
        return const [
          SpendingCategory(
            name: '식비',
            amount: 74500,
            budget: 0,
            colorValue: 0xFFFFB74D,
            iconKey: 'restaurant',
          ),
          SpendingCategory(
            name: '교통비',
            amount: 43800,
            budget: 0,
            colorValue: 0xFF5B8DEF,
            iconKey: 'directions_bus',
          ),
          SpendingCategory(
            name: '카페',
            amount: 22800,
            budget: 0,
            colorValue: 0xFF81C784,
            iconKey: 'local_cafe',
          ),
          SpendingCategory(
            name: '쇼핑/패션',
            amount: 18200,
            budget: 0,
            colorValue: 0xFFE57373,
            iconKey: 'checkroom',
          ),
        ];
      default:
        return const [
          SpendingCategory(
            name: '식비',
            amount: 68300,
            budget: 0,
            colorValue: 0xFFFFB74D,
            iconKey: 'restaurant',
          ),
          SpendingCategory(
            name: '교통비',
            amount: 39100,
            budget: 0,
            colorValue: 0xFF5B8DEF,
            iconKey: 'directions_bus',
          ),
          SpendingCategory(
            name: '생활/반려동물',
            amount: 24900,
            budget: 0,
            colorValue: 0xFF81C784,
            iconKey: 'pets',
          ),
          SpendingCategory(
            name: '기타서비스',
            amount: 16600,
            budget: 0,
            colorValue: 0xFF90A4AE,
            iconKey: 'more_horiz',
          ),
        ];
    }
  }

  static List<DailySpending> _weeklySpendings(DateTime weekStart) {
    final start = DateTime(weekStart.year, weekStart.month, weekStart.day);
    return const [18000, 23000, 12000, 30500, 21000, 42000, 20000]
        .asMap()
        .entries
        .map(
          (entry) => DailySpending(
            date: start.add(Duration(days: entry.key)),
            amount: entry.value,
          ),
        )
        .toList();
  }

  static List<DailySpending> _monthlySpendings(DateTime monthStart) {
    return [
      DailySpending(
        date: DateTime(monthStart.year, monthStart.month, 1),
        amount: 148000,
        label: '1주차',
      ),
      DailySpending(
        date: DateTime(monthStart.year, monthStart.month, 8),
        amount: 176000,
        label: '2주차',
      ),
      DailySpending(
        date: DateTime(monthStart.year, monthStart.month, 15),
        amount: 198000,
        label: '3주차',
      ),
      DailySpending(
        date: DateTime(monthStart.year, monthStart.month, 22),
        amount: 220000,
        label: '4주차',
      ),
    ];
  }

  static int _daysRemainingInWeek() {
    final now = DateTime.now();
    final remaining = 8 - now.weekday;
    return remaining.clamp(1, 7);
  }

  static final List<Account> _demoAccounts = [
    const Account(
      id: 'showcase_account_1',
      bankName: 'SSAFY은행',
      accountNumber: '123-456-789012',
      accountName: '주거래 통장',
      balance: 3250000,
      isPrimary: true,
    ),
    const Account(
      id: 'showcase_account_2',
      bankName: '한국은행',
      accountNumber: '200-301-987654',
      accountName: '비상금 통장',
      balance: 2487000,
    ),
  ];

  static final List<Goal> _inProgressGoals = [
    Goal(
      id: 'showcase_goal_1',
      name: '여행 자금 모으기',
      targetAmount: 600000,
      currentAmount: 408792,
      startDate: DateTime.now().subtract(const Duration(days: 42)),
      endDate: DateTime.now().add(const Duration(days: 12)),
      priority: 1,
      status: GoalStatus.inProgress,
      iconName: 'travel',
      dday: 12,
      achievementRateOverride: 68.1,
      weeklySavedAmountOverride: 46154,
      weeklyTargetAmountOverride: 69231,
      currentWeekOverride: 6,
      totalWeeksOverride: 9,
      aiPipelineStatus: GoalAiPipelineStatus.succeeded,
    ),
    Goal(
      id: 'showcase_goal_2',
      name: '자격증 교재 구매',
      targetAmount: 300000,
      currentAmount: 102198,
      startDate: DateTime.now().subtract(const Duration(days: 28)),
      endDate: DateTime.now().add(const Duration(days: 27)),
      priority: 2,
      status: GoalStatus.inProgress,
      iconName: 'travel',
      dday: 27,
      achievementRateOverride: 34.1,
      weeklySavedAmountOverride: 23077,
      weeklyTargetAmountOverride: 23077,
      currentWeekOverride: 4,
      totalWeeksOverride: 8,
      aiPipelineStatus: GoalAiPipelineStatus.succeeded,
    ),
  ];

  static final List<Goal> _achievedGoals = [
    Goal(
      id: 'showcase_goal_3',
      name: '노트북 구매',
      targetAmount: 1200000,
      currentAmount: 1200000,
      startDate: DateTime.now().subtract(const Duration(days: 84)),
      endDate: DateTime.now().subtract(const Duration(days: 3)),
      priority: 3,
      status: GoalStatus.achieved,
      iconName: 'travel',
      dday: -3,
      achievementRateOverride: 100,
      weeklySavedAmountOverride: 0,
      weeklyTargetAmountOverride: 0,
      currentWeekOverride: 12,
      totalWeeksOverride: 12,
      aiPipelineStatus: GoalAiPipelineStatus.succeeded,
    ),
  ];

  static List<Goal> get _allGoals => [..._inProgressGoals, ..._achievedGoals];

  static const List<SpendingCategory> _weeklyCategories = [
    SpendingCategory(
      name: '식비',
      amount: 86500,
      budget: 0,
      colorValue: 0xFFFFB74D,
      iconKey: 'restaurant',
      ratio: 52.0,
    ),
    SpendingCategory(
      name: '교통비',
      amount: 38500,
      budget: 0,
      colorValue: 0xFF5B8DEF,
      iconKey: 'directions_bus',
      ratio: 23.1,
    ),
    SpendingCategory(
      name: '카페',
      amount: 19000,
      budget: 0,
      colorValue: 0xFF81C784,
      iconKey: 'local_cafe',
      ratio: 11.4,
    ),
    SpendingCategory(
      name: '쇼핑/패션',
      amount: 12500,
      budget: 0,
      colorValue: 0xFFE57373,
      iconKey: 'checkroom',
      ratio: 7.5,
    ),
    SpendingCategory(
      name: '기타서비스',
      amount: 10000,
      budget: 0,
      colorValue: 0xFF90A4AE,
      iconKey: 'more_horiz',
      ratio: 6.0,
    ),
  ];

  static const List<SpendingCategory> _monthlyCategories = [
    SpendingCategory(
      name: '식비',
      amount: 302000,
      budget: 0,
      colorValue: 0xFFFFB74D,
      iconKey: 'restaurant',
      ratio: 40.7,
    ),
    SpendingCategory(
      name: '교통비',
      amount: 156000,
      budget: 0,
      colorValue: 0xFF5B8DEF,
      iconKey: 'directions_bus',
      ratio: 21.0,
    ),
    SpendingCategory(
      name: '주거/통신',
      amount: 122000,
      budget: 0,
      colorValue: 0xFF81C784,
      iconKey: 'home',
      ratio: 16.4,
    ),
    SpendingCategory(
      name: '쇼핑/패션',
      amount: 94000,
      budget: 0,
      colorValue: 0xFFE57373,
      iconKey: 'checkroom',
      ratio: 12.7,
    ),
    SpendingCategory(
      name: '기타서비스',
      amount: 68000,
      budget: 0,
      colorValue: 0xFF90A4AE,
      iconKey: 'more_horiz',
      ratio: 9.2,
    ),
  ];
}
