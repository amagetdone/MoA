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
        id: json['accountNo'] as String,
        bankName: json['bankName'] as String,
        accountNumber: json['accountNo'] as String,
        accountName: json['accountName'] as String? ?? '',
        balance: 0,
        isPrimary: entry.key == 0,
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
    throw UnimplementedError('removeAccount API not yet implemented');
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
  Future<bool> checkVerification({required String accountNo, required String authCode}) async {
    final response = await _dio.post(
      'v1/accounts/verification/check',
      data: {'accountNo': accountNo, 'authCode': authCode},
    );
    final data = response.data as Map<String, dynamic>;
    return data['verified'] as bool? ?? false;
  }
}