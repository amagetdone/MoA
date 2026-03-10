import 'package:flutter/material.dart';
import 'package:more_app/domain/entities/account.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/entities/spending.dart';
import 'package:more_app/domain/entities/transaction.dart';

abstract class MockData {
  // ── Accounts ──
  static const List<Account> accounts = [
    Account(
      id: 'acc1',
      bankName: '카카오뱅크',
      accountNumber: '3333-01-1234567',
      accountName: '생활비 통장',
      balance: 1250000,
      isPrimary: true,
    ),
    Account(
      id: 'acc2',
      bankName: '신한은행',
      accountNumber: '110-123-456789',
      accountName: '월급 통장',
      balance: 3500000,
    ),
    Account(
      id: 'acc3',
      bankName: '토스뱅크',
      accountNumber: '1000-1234-5678',
      accountName: '저축 통장',
      balance: 5000000,
    ),
  ];

  static int get totalAsset =>
      accounts.fold(0, (sum, a) => sum + a.balance);

  // ── Goals ──
  static final List<Goal> goals = [
    Goal(
      id: 'goal1',
      name: '제주도 여행',
      description: '친구들과 제주도 여행 자금 모으기',
      targetAmount: 500000,
      currentAmount: 350000,
      startDate: DateTime(2026, 1, 1),
      endDate: DateTime(2026, 4, 30),
      priority: 1,
      status: GoalStatus.inProgress,
      iconName: 'flight',
    ),
    Goal(
      id: 'goal2',
      name: '맥북 프로',
      description: '새 맥북 프로 구매 자금',
      targetAmount: 3000000,
      currentAmount: 1200000,
      startDate: DateTime(2026, 1, 1),
      endDate: DateTime(2026, 12, 31),
      priority: 2,
      status: GoalStatus.inProgress,
      iconName: 'laptop',
    ),
    Goal(
      id: 'goal3',
      name: '비상금',
      description: '만일의 사태를 대비한 비상금',
      targetAmount: 1000000,
      currentAmount: 1000000,
      startDate: DateTime(2025, 6, 1),
      endDate: DateTime(2025, 12, 31),
      priority: 3,
      status: GoalStatus.achieved,
      iconName: 'savings',
    ),
  ];

  static List<Goal> get activeGoals =>
      goals.where((g) => g.status == GoalStatus.inProgress).toList();

  static List<Goal> get achievedGoals =>
      goals.where((g) => g.status == GoalStatus.achieved).toList();

  // ── Transactions ──
  static final List<Transaction> transactions = [
    Transaction(
      id: 'tx1',
      title: '스타벅스',
      amount: 5500,
      type: TransactionType.expense,
      category: '식비',
      dateTime: DateTime(2026, 3, 10, 8, 30),
      balanceAfter: 1244500,
      accountId: 'acc1',
    ),
    Transaction(
      id: 'tx2',
      title: '급여',
      amount: 3500000,
      type: TransactionType.income,
      category: '급여',
      dateTime: DateTime(2026, 3, 10, 9, 0),
      balanceAfter: 4744500,
      accountId: 'acc2',
    ),
    Transaction(
      id: 'tx3',
      title: '네이버페이 결제',
      amount: 32000,
      type: TransactionType.expense,
      category: '쇼핑',
      dateTime: DateTime(2026, 3, 9, 14, 20),
      balanceAfter: 1218500,
      accountId: 'acc1',
    ),
    Transaction(
      id: 'tx4',
      title: '교통카드 충전',
      amount: 50000,
      type: TransactionType.expense,
      category: '교통',
      dateTime: DateTime(2026, 3, 9, 7, 45),
      balanceAfter: 1250500,
      accountId: 'acc1',
    ),
    Transaction(
      id: 'tx5',
      title: 'CU 편의점',
      amount: 3200,
      type: TransactionType.expense,
      category: '식비',
      dateTime: DateTime(2026, 3, 8, 22, 10),
      balanceAfter: 1300500,
      accountId: 'acc1',
    ),
    Transaction(
      id: 'tx6',
      title: '넷플릭스',
      amount: 17000,
      type: TransactionType.expense,
      category: '구독',
      dateTime: DateTime(2026, 3, 8, 0, 0),
      balanceAfter: 1303700,
      accountId: 'acc1',
    ),
    Transaction(
      id: 'tx7',
      title: '배달의민족',
      amount: 28500,
      type: TransactionType.expense,
      category: '식비',
      dateTime: DateTime(2026, 3, 7, 19, 30),
      balanceAfter: 1320700,
      accountId: 'acc1',
    ),
    Transaction(
      id: 'tx8',
      title: '목표 저축',
      amount: 100000,
      type: TransactionType.transfer,
      category: '저축',
      dateTime: DateTime(2026, 3, 7, 10, 0),
      balanceAfter: 1349200,
      accountId: 'acc1',
    ),
    Transaction(
      id: 'tx9',
      title: '올리브영',
      amount: 45000,
      type: TransactionType.expense,
      category: '쇼핑',
      dateTime: DateTime(2026, 3, 6, 16, 0),
      balanceAfter: 1449200,
      accountId: 'acc1',
    ),
    Transaction(
      id: 'tx10',
      title: '주차비',
      amount: 5000,
      type: TransactionType.expense,
      category: '교통',
      dateTime: DateTime(2026, 3, 6, 12, 0),
      balanceAfter: 1494200,
      accountId: 'acc1',
    ),
  ];

  // ── Spending Categories ──
  static const List<SpendingCategory> spendingCategories = [
    SpendingCategory(
      name: '식비',
      amount: 185000,
      budget: 300000,
      color: Color(0xFFFF6B00),
      icon: Icons.restaurant,
    ),
    SpendingCategory(
      name: '교통',
      amount: 55000,
      budget: 100000,
      color: Color(0xFF4CAF50),
      icon: Icons.directions_bus,
    ),
    SpendingCategory(
      name: '쇼핑',
      amount: 77000,
      budget: 150000,
      color: Color(0xFF2196F3),
      icon: Icons.shopping_bag,
    ),
    SpendingCategory(
      name: '구독',
      amount: 17000,
      budget: 30000,
      color: Color(0xFF9C27B0),
      icon: Icons.subscriptions,
    ),
    SpendingCategory(
      name: '여가',
      amount: 45000,
      budget: 100000,
      color: Color(0xFFFF9800),
      icon: Icons.sports_esports,
    ),
    SpendingCategory(
      name: '기타',
      amount: 21000,
      budget: 50000,
      color: Color(0xFF607D8B),
      icon: Icons.more_horiz,
    ),
  ];

  static int get totalWeeklySpent =>
      spendingCategories.fold(0, (sum, c) => sum + c.amount);

  static const int weeklyBudget = 730000;
  static const int monthlyIncome = 3500000;
  static const int monthlySavingsGoal = 500000;
  static const int monthlyFixedExpense = 850000;

  // ── Daily spending for chart ──
  static final List<DailySpending> dailySpendings = [
    DailySpending(date: DateTime(2026, 3, 4), amount: 45000),
    DailySpending(date: DateTime(2026, 3, 5), amount: 32000),
    DailySpending(date: DateTime(2026, 3, 6), amount: 50000),
    DailySpending(date: DateTime(2026, 3, 7), amount: 128500),
    DailySpending(date: DateTime(2026, 3, 8), amount: 20200),
    DailySpending(date: DateTime(2026, 3, 9), amount: 82000),
    DailySpending(date: DateTime(2026, 3, 10), amount: 5500),
  ];

  // ── Monthly spending for chart ──
  static final List<DailySpending> monthlySpendings = [
    DailySpending(date: DateTime(2025, 10, 1), amount: 1850000),
    DailySpending(date: DateTime(2025, 11, 1), amount: 1620000),
    DailySpending(date: DateTime(2025, 12, 1), amount: 2100000),
    DailySpending(date: DateTime(2026, 1, 1), amount: 1780000),
    DailySpending(date: DateTime(2026, 2, 1), amount: 1950000),
    DailySpending(date: DateTime(2026, 3, 1), amount: 400000),
  ];

  // ── Savings history for goal chart ──
  static final List<DailySpending> savingsHistory = [
    DailySpending(date: DateTime(2026, 1, 7), amount: 50000),
    DailySpending(date: DateTime(2026, 1, 14), amount: 50000),
    DailySpending(date: DateTime(2026, 1, 21), amount: 50000),
    DailySpending(date: DateTime(2026, 1, 28), amount: 50000),
    DailySpending(date: DateTime(2026, 2, 4), amount: 50000),
    DailySpending(date: DateTime(2026, 2, 11), amount: 50000),
    DailySpending(date: DateTime(2026, 2, 18), amount: 50000),
    DailySpending(date: DateTime(2026, 2, 25), amount: 50000),
    DailySpending(date: DateTime(2026, 3, 4), amount: 50000),
    DailySpending(date: DateTime(2026, 3, 10), amount: 50000),
  ];

  // ── Bank list ──
  static const List<String> bankNames = [
    '카카오뱅크',
    '토스뱅크',
    '신한은행',
    '국민은행',
    'NH농협은행',
    '우리은행',
    '하나은행',
    '기업은행',
    'SC제일은행',
    '씨티은행',
  ];
}
