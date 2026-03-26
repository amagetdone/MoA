import 'package:dio/dio.dart';
import 'package:more_app/data/datasources/remote/menu_remote_datasource.dart';
import 'package:more_app/domain/entities/menu_summary.dart';
import 'package:more_app/domain/entities/user.dart';

class RealMenuRemoteDataSource implements MenuRemoteDataSource {
  final Dio _dio;
  static const _s3BaseUrl =
      'https://amagetdone.s3.ap-northeast-2.amazonaws.com';

  RealMenuRemoteDataSource({required Dio dio}) : _dio = dio;

  @override
  Future<MenuSummary> fetchMenuDashboard() async {
    final response = await _dio.get('v1/mypage');
    final data = response.data as Map<String, dynamic>;

    return MenuSummary(
      user: User(
        id: '',
        name: data['userName'] as String? ?? '',
        email: data['userEmail'] as String? ?? '',
        profileImageUrl: _resolveProfileImageUrl(data['profileImageUrl']),
      ),
      activeGoalCount: (data['activePlanCount'] as num?)?.toInt() ?? 0,
      achievedGoalCount: (data['completedPlanCount'] as num?)?.toInt() ?? 0,
      totalSaved: (data['totalSavedAmount'] as num?)?.toInt() ?? 0,
    );
  }

  String? _resolveProfileImageUrl(Object? rawValue) {
    final value = (rawValue as String?)?.trim();
    if (value == null || value.isEmpty) {
      return null;
    }

    if (value.startsWith('http://') || value.startsWith('https://')) {
      return value;
    }

    final normalizedPath = value.startsWith('/') ? value.substring(1) : value;
    return '$_s3BaseUrl/$normalizedPath';
  }

  @override
  Future<MenuSummary> updateMenuPreferences({
    bool? notificationEnabled,
    bool? biometricEnabled,
  }) async {
    // TODO: API 연동 필요
    throw UnimplementedError('updateMenuPreferences API not yet implemented');
  }
}
