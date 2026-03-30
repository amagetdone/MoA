abstract class BudgetRemoteDataSource {
  Future<int> getIncome();
  Future<int> saveIncome(int monthlyIncome);
  Future<bool> allocateBudget();
  Future<BudgetWeekSetupDto> getWeekSetup(String yearWeek);
  Future<int> getAverageIncomeFromAnalysis();
}

class BudgetWeekSetupDto {
  final int plannedIncomeAmount;
  final int plannedSavingAmount;
  final int plannedEssentialAmount;
  final int plannedFreeAmount;
  final int remainingFreeAmount;
  final List<BudgetEssentialDto> essentials;

  const BudgetWeekSetupDto({
    this.plannedIncomeAmount = 0,
    this.plannedSavingAmount = 0,
    this.plannedEssentialAmount = 0,
    this.plannedFreeAmount = 0,
    this.remainingFreeAmount = 0,
    this.essentials = const [],
  });

  factory BudgetWeekSetupDto.fromJson(Map<String, dynamic> json) {
    return BudgetWeekSetupDto(
      plannedIncomeAmount: (json['plannedIncomeAmount'] as num?)?.toInt() ?? 0,
      plannedSavingAmount: (json['plannedSavingAmount'] as num?)?.toInt() ?? 0,
      plannedEssentialAmount: (json['plannedEssentialAmount'] as num?)?.toInt() ?? 0,
      plannedFreeAmount: (json['plannedFreeAmount'] as num?)?.toInt() ?? 0,
      remainingFreeAmount: (json['remainingFreeAmount'] as num?)?.toInt() ?? 0,
      essentials: (json['essentials'] as List<dynamic>? ?? [])
          .map((e) => BudgetEssentialDto.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}

class BudgetEssentialDto {
  final String categoryName;
  final String expenseScope;
  final int plannedAmount;
  final int actualSpentAmount;

  const BudgetEssentialDto({
    required this.categoryName,
    required this.expenseScope,
    required this.plannedAmount,
    required this.actualSpentAmount,
  });

  factory BudgetEssentialDto.fromJson(Map<String, dynamic> json) {
    return BudgetEssentialDto(
      categoryName: json['categoryName'] as String? ?? '',
      expenseScope: json['expenseScope'] as String? ?? '',
      plannedAmount: (json['plannedAmount'] as num?)?.toInt() ?? 0,
      actualSpentAmount: (json['actualSpentAmount'] as num?)?.toInt() ?? 0,
    );
  }
}