class WeeklyAnalysisDto {
  final String yearWeek;
  final String? weekStartDate;
  final String? weekEndDate;
  final int expenseAmount;
  final int essentialAmount;
  final int fixedEssentialAmount;
  final int variableEssentialAmount;
  final List<WeeklyCategoryExpenseDto> categoryExpenses;

  const WeeklyAnalysisDto({
    required this.yearWeek,
    this.weekStartDate,
    this.weekEndDate,
    this.expenseAmount = 0,
    this.essentialAmount = 0,
    this.fixedEssentialAmount = 0,
    this.variableEssentialAmount = 0,
    this.categoryExpenses = const [],
  });

  factory WeeklyAnalysisDto.fromJson(Map<String, dynamic> json) {
    return WeeklyAnalysisDto(
      yearWeek: json['yearWeek'] as String? ?? '',
      weekStartDate: json['weekStartDate'] as String?,
      weekEndDate: json['weekEndDate'] as String?,
      expenseAmount: json['expenseAmount'] as int? ?? 0,
      essentialAmount: json['essentialAmount'] as int? ?? 0,
      fixedEssentialAmount: json['fixedEssentialAmount'] as int? ?? 0,
      variableEssentialAmount: json['variableEssentialAmount'] as int? ?? 0,
      categoryExpenses: (json['categoryExpenses'] as List<dynamic>? ?? [])
          .map((e) => WeeklyCategoryExpenseDto.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}

class MonthlyAnalysisDto {
  final String yearMonth;
  final int expenseAmount;
  final int monthlyEssentialAmount;
  final int fixedEssentialAmount;
  final int variableEssentialAmount;
  final List<WeeklyCategoryExpenseDto> categoryExpenses;

  const MonthlyAnalysisDto({
    required this.yearMonth,
    this.expenseAmount = 0,
    this.monthlyEssentialAmount = 0,
    this.fixedEssentialAmount = 0,
    this.variableEssentialAmount = 0,
    this.categoryExpenses = const [],
  });

  factory MonthlyAnalysisDto.fromJson(Map<String, dynamic> json) {
    return MonthlyAnalysisDto(
      yearMonth: json['yearMonth'] as String? ?? '',
      expenseAmount: json['expenseAmount'] as int? ?? 0,
      monthlyEssentialAmount: json['monthlyEssentialAmount'] as int? ?? 0,
      fixedEssentialAmount: json['fixedEssentialAmount'] as int? ?? 0,
      variableEssentialAmount: json['variableEssentialAmount'] as int? ?? 0,
      categoryExpenses: (json['categoryExpenses'] as List<dynamic>? ?? [])
          .map((e) => WeeklyCategoryExpenseDto.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}

class WeeklyCategoryExpenseDto {
  final int categoryId;
  final String categoryName;
  final int amount;
  final double ratio;

  const WeeklyCategoryExpenseDto({
    required this.categoryId,
    required this.categoryName,
    required this.amount,
    this.ratio = 0.0,
  });

  factory WeeklyCategoryExpenseDto.fromJson(Map<String, dynamic> json) {
    return WeeklyCategoryExpenseDto(
      categoryId: json['categoryId'] as int? ?? 0,
      categoryName: json['categoryName'] as String? ?? '',
      amount: json['amount'] as int? ?? 0,
      ratio: (json['ratio'] as num?)?.toDouble() ?? 0.0,
    );
  }
}
