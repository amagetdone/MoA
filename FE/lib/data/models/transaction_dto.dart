import 'package:more_app/domain/entities/transaction.dart';

class TransactionDto {
  final int transactionUniqueNo;
  final String transactionDate;
  final String transactionTime;
  final String transactionType;
  final String transactionTypeName;
  final String transactionAccountNo;
  final int transactionBalance;
  final int transactionAfterBalance;
  final String transactionSummary;
  final String? transactionMemo;

  const TransactionDto({
    required this.transactionUniqueNo,
    required this.transactionDate,
    required this.transactionTime,
    required this.transactionType,
    required this.transactionTypeName,
    required this.transactionAccountNo,
    required this.transactionBalance,
    required this.transactionAfterBalance,
    required this.transactionSummary,
    this.transactionMemo,
  });

  factory TransactionDto.fromJson(Map<String, dynamic> json) {
    return TransactionDto(
      transactionUniqueNo: json['transactionUniqueNo'] as int,
      transactionDate: json['transactionDate'] as String,
      transactionTime: json['transactionTime'] as String,
      transactionType: json['transactionType'] as String,
      transactionTypeName: json['transactionTypeName'] as String,
      transactionAccountNo: json['transactionAccountNo'] as String,
      transactionBalance: json['transactionBalance'] as int,
      transactionAfterBalance: json['transactionAfterBalance'] as int,
      transactionSummary: json['transactionSummary'] as String,
      transactionMemo: json['transactionMemo'] as String?,
    );
  }

  Transaction toEntity() {
    final dateTime = DateTime.parse('$transactionDate $transactionTime');
    return Transaction(
      id: transactionUniqueNo.toString(),
      title: transactionSummary,
      amount: transactionBalance,
      type: transactionType == '1' ? TransactionType.income : TransactionType.expense,
      category: transactionTypeName,
      dateTime: dateTime,
      balanceAfter: transactionAfterBalance,
      memo: transactionMemo,
      accountId: transactionAccountNo,
    );
  }
}

class TransactionListDto {
  final int totalCount;
  final List<TransactionDto> transactions;

  const TransactionListDto({required this.totalCount, required this.transactions});

  factory TransactionListDto.fromJson(Map<String, dynamic> json) {
    return TransactionListDto(
      totalCount: json['totalCount'] as int,
      transactions: (json['transactions'] as List)
          .map((e) => TransactionDto.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}