import 'package:flutter/material.dart';

class SpendingCategory {
  final String name;
  final int amount;
  final int budget;
  final Color color;
  final IconData icon;

  const SpendingCategory({
    required this.name,
    required this.amount,
    required this.budget,
    required this.color,
    required this.icon,
  });

  double get usageRate => budget > 0 ? (amount / budget).clamp(0.0, 1.0) : 0.0;

  SpendingCategory copyWith({
    String? name,
    int? amount,
    int? budget,
    Color? color,
    IconData? icon,
  }) {
    return SpendingCategory(
      name: name ?? this.name,
      amount: amount ?? this.amount,
      budget: budget ?? this.budget,
      color: color ?? this.color,
      icon: icon ?? this.icon,
    );
  }
}

class WeeklySpending {
  final DateTime weekStart;
  final int totalBudget;
  final int totalSpent;
  final List<SpendingCategory> categories;

  const WeeklySpending({
    required this.weekStart,
    required this.totalBudget,
    required this.totalSpent,
    required this.categories,
  });

  int get remainingBudget => totalBudget - totalSpent;
}

class DailySpending {
  final DateTime date;
  final int amount;

  const DailySpending({required this.date, required this.amount});
}
