import 'package:flutter/material.dart';
import 'package:more_app/domain/entities/account.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/entities/spending.dart';
import 'package:more_app/domain/entities/transaction.dart';
import 'package:more_app/domain/entities/user.dart';

class AppSnapshot {
  final User user;
  final MenuPreferencesSnapshot menuPreferences;
  final List<String> bankNames;
  final List<Account> accounts;
  final List<Goal> goals;
  final List<Transaction> transactions;
  final SpendingSnapshot spending;
  final Map<String, List<DailySpending>> goalSavingsHistoryByGoalId;

  const AppSnapshot({
    required this.user,
    required this.menuPreferences,
    this.bankNames = const [],
    this.accounts = const [],
    this.goals = const [],
    this.transactions = const [],
    required this.spending,
    this.goalSavingsHistoryByGoalId = const {},
  });

  factory AppSnapshot.fromJson(Map<String, dynamic> json) {
    final histories =
        (json['goalSavingsHistoryByGoalId'] as Map<String, dynamic>? ?? {}).map(
          (goalId, value) => MapEntry(
            goalId,
            (value as List<dynamic>? ?? [])
                .map(
                  (item) => DailySpending(
                    date: DateTime.parse(item['date'] as String),
                    amount: item['amount'] as int? ?? 0,
                  ),
                )
                .toList(),
          ),
        );

    return AppSnapshot(
      user: User(
        id: (json['user'] as Map<String, dynamic>)['id'] as String,
        name: (json['user'] as Map<String, dynamic>)['name'] as String,
        email: (json['user'] as Map<String, dynamic>)['email'] as String,
        profileImageUrl:
            (json['user'] as Map<String, dynamic>)['profileImageUrl']
                as String?,
      ),
      menuPreferences: MenuPreferencesSnapshot.fromJson(
        json['menuPreferences'] as Map<String, dynamic>? ?? const {},
      ),
      bankNames: (json['bankNames'] as List<dynamic>? ?? [])
          .map((item) => item as String)
          .toList(),
      accounts: (json['accounts'] as List<dynamic>? ?? [])
          .map((item) => _accountFromJson(item as Map<String, dynamic>))
          .toList(),
      goals: (json['goals'] as List<dynamic>? ?? [])
          .map((item) => _goalFromJson(item as Map<String, dynamic>))
          .toList(),
      transactions: (json['transactions'] as List<dynamic>? ?? [])
          .map((item) => _transactionFromJson(item as Map<String, dynamic>))
          .toList(),
      spending: SpendingSnapshot.fromJson(
        json['spending'] as Map<String, dynamic>? ?? const {},
      ),
      goalSavingsHistoryByGoalId: histories,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'user': {
        'id': user.id,
        'name': user.name,
        'email': user.email,
        'profileImageUrl': user.profileImageUrl,
      },
      'menuPreferences': menuPreferences.toJson(),
      'bankNames': bankNames,
      'accounts': accounts.map(_accountToJson).toList(),
      'goals': goals.map(_goalToJson).toList(),
      'transactions': transactions.map(_transactionToJson).toList(),
      'spending': spending.toJson(),
      'goalSavingsHistoryByGoalId': goalSavingsHistoryByGoalId.map(
        (goalId, history) => MapEntry(
          goalId,
          history
              .map(
                (item) => {
                  'date': item.date.toIso8601String(),
                  'amount': item.amount,
                },
              )
              .toList(),
        ),
      ),
    };
  }

  AppSnapshot copyWith({
    User? user,
    MenuPreferencesSnapshot? menuPreferences,
    List<String>? bankNames,
    List<Account>? accounts,
    List<Goal>? goals,
    List<Transaction>? transactions,
    SpendingSnapshot? spending,
    Map<String, List<DailySpending>>? goalSavingsHistoryByGoalId,
  }) {
    return AppSnapshot(
      user: user ?? this.user,
      menuPreferences: menuPreferences ?? this.menuPreferences,
      bankNames: bankNames ?? this.bankNames,
      accounts: accounts ?? this.accounts,
      goals: goals ?? this.goals,
      transactions: transactions ?? this.transactions,
      spending: spending ?? this.spending,
      goalSavingsHistoryByGoalId:
          goalSavingsHistoryByGoalId ?? this.goalSavingsHistoryByGoalId,
    );
  }
}

class MenuPreferencesSnapshot {
  final bool notificationEnabled;
  final bool biometricEnabled;

  const MenuPreferencesSnapshot({
    this.notificationEnabled = true,
    this.biometricEnabled = false,
  });

  factory MenuPreferencesSnapshot.fromJson(Map<String, dynamic> json) {
    return MenuPreferencesSnapshot(
      notificationEnabled: json['notificationEnabled'] as bool? ?? true,
      biometricEnabled: json['biometricEnabled'] as bool? ?? false,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'notificationEnabled': notificationEnabled,
      'biometricEnabled': biometricEnabled,
    };
  }

  MenuPreferencesSnapshot copyWith({
    bool? notificationEnabled,
    bool? biometricEnabled,
  }) {
    return MenuPreferencesSnapshot(
      notificationEnabled: notificationEnabled ?? this.notificationEnabled,
      biometricEnabled: biometricEnabled ?? this.biometricEnabled,
    );
  }
}

class SpendingSnapshot {
  final List<SpendingCategory> categories;
  final List<DailySpending> dailySpendings;
  final List<DailySpending> monthlySpendings;
  final int weeklyBudget;
  final int monthlyIncome;
  final int monthlySavingsGoal;
  final int monthlyFixedExpense;

  const SpendingSnapshot({
    this.categories = const [],
    this.dailySpendings = const [],
    this.monthlySpendings = const [],
    this.weeklyBudget = 0,
    this.monthlyIncome = 0,
    this.monthlySavingsGoal = 0,
    this.monthlyFixedExpense = 0,
  });

  factory SpendingSnapshot.fromJson(Map<String, dynamic> json) {
    return SpendingSnapshot(
      weeklyBudget: json['weeklyBudget'] as int? ?? 0,
      monthlyIncome: json['monthlyIncome'] as int? ?? 0,
      monthlySavingsGoal: json['monthlySavingsGoal'] as int? ?? 0,
      monthlyFixedExpense: json['monthlyFixedExpense'] as int? ?? 0,
      categories: (json['categories'] as List<dynamic>? ?? [])
          .map(
            (item) => _spendingCategoryFromJson(item as Map<String, dynamic>),
          )
          .toList(),
      dailySpendings: (json['dailySpendings'] as List<dynamic>? ?? [])
          .map(
            (item) => DailySpending(
              date: DateTime.parse(item['date'] as String),
              amount: item['amount'] as int? ?? 0,
            ),
          )
          .toList(),
      monthlySpendings: (json['monthlySpendings'] as List<dynamic>? ?? [])
          .map(
            (item) => DailySpending(
              date: DateTime.parse(item['date'] as String),
              amount: item['amount'] as int? ?? 0,
            ),
          )
          .toList(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'weeklyBudget': weeklyBudget,
      'monthlyIncome': monthlyIncome,
      'monthlySavingsGoal': monthlySavingsGoal,
      'monthlyFixedExpense': monthlyFixedExpense,
      'categories': categories.map(_spendingCategoryToJson).toList(),
      'dailySpendings': dailySpendings
          .map(
            (item) => {
              'date': item.date.toIso8601String(),
              'amount': item.amount,
            },
          )
          .toList(),
      'monthlySpendings': monthlySpendings
          .map(
            (item) => {
              'date': item.date.toIso8601String(),
              'amount': item.amount,
            },
          )
          .toList(),
    };
  }

  SpendingSnapshot copyWith({
    List<SpendingCategory>? categories,
    List<DailySpending>? dailySpendings,
    List<DailySpending>? monthlySpendings,
    int? weeklyBudget,
    int? monthlyIncome,
    int? monthlySavingsGoal,
    int? monthlyFixedExpense,
  }) {
    return SpendingSnapshot(
      categories: categories ?? this.categories,
      dailySpendings: dailySpendings ?? this.dailySpendings,
      monthlySpendings: monthlySpendings ?? this.monthlySpendings,
      weeklyBudget: weeklyBudget ?? this.weeklyBudget,
      monthlyIncome: monthlyIncome ?? this.monthlyIncome,
      monthlySavingsGoal: monthlySavingsGoal ?? this.monthlySavingsGoal,
      monthlyFixedExpense: monthlyFixedExpense ?? this.monthlyFixedExpense,
    );
  }
}

Account _accountFromJson(Map<String, dynamic> json) {
  return Account(
    id: json['id'] as String,
    bankName: json['bankName'] as String,
    accountNumber: json['accountNumber'] as String,
    accountName: json['accountName'] as String,
    balance: json['balance'] as int? ?? 0,
    isPrimary: json['isPrimary'] as bool? ?? false,
  );
}

Map<String, dynamic> _accountToJson(Account account) {
  return {
    'id': account.id,
    'bankName': account.bankName,
    'accountNumber': account.accountNumber,
    'accountName': account.accountName,
    'balance': account.balance,
    'isPrimary': account.isPrimary,
  };
}

Goal _goalFromJson(Map<String, dynamic> json) {
  return Goal(
    id: json['id'] as String,
    name: json['name'] as String,
    description: json['description'] as String?,
    targetAmount: json['targetAmount'] as int? ?? 0,
    currentAmount: json['currentAmount'] as int? ?? 0,
    startDate: DateTime.parse(json['startDate'] as String),
    endDate: DateTime.parse(json['endDate'] as String),
    priority: json['priority'] as int? ?? 0,
    status: _goalStatusFromString(json['status'] as String?),
    iconName: json['iconName'] as String?,
    sketchVideoAssetPath: json['sketchVideoAssetPath'] as String?,
  );
}

Map<String, dynamic> _goalToJson(Goal goal) {
  return {
    'id': goal.id,
    'name': goal.name,
    'description': goal.description,
    'targetAmount': goal.targetAmount,
    'currentAmount': goal.currentAmount,
    'startDate': goal.startDate.toIso8601String(),
    'endDate': goal.endDate.toIso8601String(),
    'priority': goal.priority,
    'status': _goalStatusToString(goal.status),
    'iconName': goal.iconName,
    'sketchVideoAssetPath': goal.sketchVideoAssetPath,
  };
}

GoalStatus _goalStatusFromString(String? value) {
  switch (value) {
    case 'achieved':
      return GoalStatus.achieved;
    case 'failed':
      return GoalStatus.failed;
    case 'inProgress':
    default:
      return GoalStatus.inProgress;
  }
}

String _goalStatusToString(GoalStatus status) {
  switch (status) {
    case GoalStatus.achieved:
      return 'achieved';
    case GoalStatus.failed:
      return 'failed';
    case GoalStatus.inProgress:
      return 'inProgress';
  }
}

Transaction _transactionFromJson(Map<String, dynamic> json) {
  return Transaction(
    id: json['id'] as String,
    title: json['title'] as String,
    amount: json['amount'] as int? ?? 0,
    type: _transactionTypeFromString(json['type'] as String?),
    category: json['category'] as String? ?? '',
    dateTime: DateTime.parse(json['dateTime'] as String),
    balanceAfter: json['balanceAfter'] as int? ?? 0,
    memo: json['memo'] as String?,
    accountId: json['accountId'] as String?,
  );
}

Map<String, dynamic> _transactionToJson(Transaction transaction) {
  return {
    'id': transaction.id,
    'title': transaction.title,
    'amount': transaction.amount,
    'type': _transactionTypeToString(transaction.type),
    'category': transaction.category,
    'dateTime': transaction.dateTime.toIso8601String(),
    'balanceAfter': transaction.balanceAfter,
    'memo': transaction.memo,
    'accountId': transaction.accountId,
  };
}

TransactionType _transactionTypeFromString(String? value) {
  switch (value) {
    case 'income':
      return TransactionType.income;
    case 'transfer':
      return TransactionType.transfer;
    case 'expense':
    default:
      return TransactionType.expense;
  }
}

String _transactionTypeToString(TransactionType type) {
  switch (type) {
    case TransactionType.income:
      return 'income';
    case TransactionType.expense:
      return 'expense';
    case TransactionType.transfer:
      return 'transfer';
  }
}

SpendingCategory _spendingCategoryFromJson(Map<String, dynamic> json) {
  return SpendingCategory(
    name: json['name'] as String? ?? '',
    amount: json['amount'] as int? ?? 0,
    budget: json['budget'] as int? ?? 0,
    color: _colorFromHex(json['color'] as String? ?? '#FF607D8B'),
    icon: _iconFromKey(json['icon'] as String? ?? 'more_horiz'),
  );
}

Map<String, dynamic> _spendingCategoryToJson(SpendingCategory category) {
  return {
    'name': category.name,
    'amount': category.amount,
    'budget': category.budget,
    'color': _colorToHex(category.color),
    'icon': _iconToKey(category.icon),
  };
}

Color _colorFromHex(String value) {
  final normalized = value.replaceFirst('#', '');
  final hex = normalized.length == 6 ? 'FF$normalized' : normalized;
  return Color(int.parse(hex, radix: 16));
}

String _colorToHex(Color color) {
  return '#${color.toARGB32().toRadixString(16).padLeft(8, '0').toUpperCase()}';
}

IconData _iconFromKey(String value) {
  switch (value) {
    case 'restaurant':
      return Icons.restaurant;
    case 'directions_bus':
      return Icons.directions_bus;
    case 'shopping_bag':
      return Icons.shopping_bag;
    case 'subscriptions':
      return Icons.subscriptions;
    case 'sports_esports':
      return Icons.sports_esports;
    case 'more_horiz':
    default:
      return Icons.more_horiz;
  }
}

String _iconToKey(IconData icon) {
  if (icon.codePoint == Icons.restaurant.codePoint) {
    return 'restaurant';
  }
  if (icon.codePoint == Icons.directions_bus.codePoint) {
    return 'directions_bus';
  }
  if (icon.codePoint == Icons.shopping_bag.codePoint) {
    return 'shopping_bag';
  }
  if (icon.codePoint == Icons.subscriptions.codePoint) {
    return 'subscriptions';
  }
  if (icon.codePoint == Icons.sports_esports.codePoint) {
    return 'sports_esports';
  }
  return 'more_horiz';
}
