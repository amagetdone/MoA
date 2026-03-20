class Transaction {
  final String id;
  final String title;
  final int amount;
  final TransactionType type;
  final String category;
  final DateTime dateTime;
  final int balanceAfter;
  final String? memo;
  final String? accountId;
  final String? sourceType; // 'ACCOUNT' or 'CARD'

  const Transaction({
    required this.id,
    required this.title,
    required this.amount,
    required this.type,
    required this.category,
    required this.dateTime,
    required this.balanceAfter,
    this.memo,
    this.accountId,
    this.sourceType,
  });
}

enum TransactionType {
  income,
  expense,
  transfer,
}
