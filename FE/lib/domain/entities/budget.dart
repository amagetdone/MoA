class BudgetEssential {
  final String categoryName;
  final int plannedAmount;
  final int actualSpentAmount;

  const BudgetEssential({
    required this.categoryName,
    required this.plannedAmount,
    required this.actualSpentAmount,
  });
}

class BudgetSetup {
  final int monthlyIncome;
  final int savingsAmount;
  final int fixedExpenseAmount;
  final int variableExpenseAmount;
  final int freeAmount;
  final int remainingFreeAmount;
  final List<BudgetEssential> fixedEssentials;
  final List<BudgetEssential> variableEssentials;

  const BudgetSetup({
    this.monthlyIncome = 0,
    this.savingsAmount = 0,
    this.fixedExpenseAmount = 0,
    this.variableExpenseAmount = 0,
    this.freeAmount = 0,
    this.remainingFreeAmount = 0,
    this.fixedEssentials = const [],
    this.variableEssentials = const [],
  });
}