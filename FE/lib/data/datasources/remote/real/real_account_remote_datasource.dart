import 'package:dio/dio.dart';
import 'package:more_app/data/datasources/remote/account_remote_datasource.dart';
import 'package:more_app/domain/entities/account.dart';
import 'package:more_app/domain/entities/account_dashboard.dart';
import 'package:more_app/domain/entities/transaction.dart';

class RealAccountRemoteDataSource implements AccountRemoteDataSource {
  final Dio _dio;

  RealAccountRemoteDataSource({required Dio dio}) : _dio = dio;

  @override
  Future<AccountDashboard> fetchAccountDashboard() async {
    final response = await _dio.get('v1/accounts/me');
    final List<dynamic> dataList = response.data as List<dynamic>;

    final accounts = dataList.asMap().entries.map((entry) {
      final json = entry.value as Map<String, dynamic>;
      return Account(
        id: json['accountNo'] as String? ?? '',
        bankName: json['bankName'] as String? ?? '',
        accountNumber: json['accountNo'] as String? ?? '',
        accountName: json['accountName'] as String? ?? '',
        balance: 0,
        isPrimary: entry.key == 0,
        verified: json['verified'] as bool? ?? true,
      );
    }).toList();

    return AccountDashboard(
      accounts: accounts,
      transactions: [],
      bankNames: accounts.map((a) => a.bankName).toSet().toList(),
      totalAsset: 0,
    );
  }

  @override
  Future<Account> addAccount(Account account) async {
    throw UnimplementedError('addAccount API not yet implemented');
  }

  @override
  Future<void> removeAccount(String id) async {
    await _dio.delete('v1/accounts/$id');
  }

  @override
  Future<Transaction?> findTransactionById(String id) async {
    throw UnimplementedError('findTransactionById API not yet implemented');
  }

  @override
  Future<bool> confirmAccount(String accountNo) async {
    final response = await _dio.post(
      'v1/accounts/confirm',
      data: {'accountNo': accountNo},
    );
    final data = response.data as Map<String, dynamic>;
    return data['confirmed'] as bool? ?? false;
  }

  @override
  Future<Map<String, dynamic>> requestVerification(String accountNo) async {
    final response = await _dio.post(
      'v1/accounts/verification/requests',
      data: {'accountNo': accountNo},
    );
    return response.data as Map<String, dynamic>;
  }

  @override
  Future<bool> checkVerification({
    required String accountNo,
    required String authCode,
  }) async {
    final response = await _dio.post(
      'v1/accounts/verification/check',
      data: {'accountNo': accountNo, 'authCode': authCode},
    );
    final data = response.data as Map<String, dynamic>;
    return data['verified'] as bool? ?? false;
  }

  @override
  Future<List<Transaction>> fetchTransactions({
    String? fromDate,
    String? toDate,
    String? analysisInclusion,
    int page = 0,
    int size = 50,
  }) async {
    final queryParams = <String, dynamic>{'page': page, 'size': size};
    if (fromDate != null) {
      queryParams['fromDate'] = fromDate;
    }
    if (toDate != null) {
      queryParams['toDate'] = toDate;
    }
    if (analysisInclusion != null) {
      queryParams['analysisInclusion'] = analysisInclusion;
    }

    final response = await _dio.get(
      'transactions',
      queryParameters: queryParams,
    );

    final data = response.data as Map<String, dynamic>;
    final items = (data['items'] as List<dynamic>?) ?? [];

    return items.map((json) {
      final item = json as Map<String, dynamic>;
      final direction = item['direction'] as String? ?? 'OUT';
      final category = item['category'] as Map<String, dynamic>?;
      final balanceAfter =
          (item['transactionAfterBalance'] as num?)?.toInt() ??
          (item['balanceAfter'] as num?)?.toInt() ??
          (item['afterBalance'] as num?)?.toInt() ??
          0;

      return Transaction(
        id: (item['transactionId'] as num).toString(),
        title: item['merchantText'] as String? ?? '',
        amount: (item['amount'] as num).toInt(),
        type: direction == 'IN'
            ? TransactionType.income
            : TransactionType.expense,
        category: category?['name'] as String? ?? '기타',
        dateTime: DateTime.parse(item['occurredAt'] as String),
        balanceAfter: balanceAfter,
        memo: item['expenseType'] as String?,
        accountId: item['sourceId'] as String?,
        sourceType: item['sourceType'] as String?,
      );
    }).toList();
  }
}
