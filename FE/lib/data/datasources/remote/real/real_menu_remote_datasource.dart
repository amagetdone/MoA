import 'package:dio/dio.dart';
import 'package:more_app/data/datasources/remote/menu_remote_datasource.dart';
import 'package:more_app/domain/entities/menu_summary.dart';
import 'package:more_app/domain/entities/user.dart';

class RealMenuRemoteDataSource implements MenuRemoteDataSource {
  final Dio _dio;

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
        profileImageUrl: data['profileImageUrl'] != null
            ? 'https://amagetdone.s3.ap-northeast-2.amazonaws.com/${data['profileImageUrl']}'
            : null,
      ),
      activeGoalCount: data['activePlanCount'] as int? ?? 0,
      achievedGoalCount: data['completedPlanCount'] as int? ?? 0,
      totalSaved: data['totalSavedAmount'] as int? ?? 0,
    );
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
