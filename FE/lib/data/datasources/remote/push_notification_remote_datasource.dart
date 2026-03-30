abstract class PushNotificationRemoteDataSource {
  Future<void> upsertDeviceToken({
    required String installationId,
    required String fcmToken,
    required String platform,
  });
}
