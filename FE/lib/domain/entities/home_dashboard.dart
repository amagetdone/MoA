import 'package:more_app/domain/entities/account.dart';
import 'package:more_app/domain/entities/goal.dart';

class HomeDashboard {
  final List<Account> accounts;
  final List<Goal> activeGoals;
  final int totalAsset;
  final int weeklyBudget;
  final int weeklySpent;
  final int weeklyRemaining;
  final int daysRemaining;
  final int dailyAvailable;
  final int weeklySaved;
  final int savingProgressRate;

  const HomeDashboard({
    this.accounts = const [],
    this.activeGoals = const [],
    this.totalAsset = 0,
    this.weeklyBudget = 0,
    this.weeklySpent = 0,
    this.weeklyRemaining = 0,
    this.daysRemaining = 0,
    this.dailyAvailable = 0,
    this.weeklySaved = 0,
    this.savingProgressRate = 0,
  });
}
