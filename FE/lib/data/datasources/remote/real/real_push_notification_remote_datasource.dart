import 'package:dio/dio.dart';
import 'package:more_app/data/datasources/remote/push_notification_remote_datasource.dart';

class RealPushNotificationRemoteDataSource
    implements PushNotificationRemoteDataSource {
  RealPushNotificationRemoteDataSource({required Dio dio}) : _dio = dio;

  final Dio _dio;

  @override
  Future<void> upsertDeviceToken({
    required String installationId,
    required String fcmToken,
    required String platform,
  }) async {
    await _dio.post(
      'v1/device-tokens',
      data: {
        'installationId': installationId,
        'fcmToken': fcmToken,
        'platform': platform,
      },
    );
  }
}
