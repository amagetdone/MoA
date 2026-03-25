import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:more_app/domain/entities/app_notification_item.dart';

abstract class NotificationInboxRepository {
  Stream<List<AppNotificationItem>> watchNotifications();
  Future<List<AppNotificationItem>> getNotifications();
  Future<void> initialize();
  Future<void> saveNotification(AppNotificationItem item);
  Future<void> saveRemoteMessage(
    RemoteMessage message, {
    bool markAsRead = false,
  });
  Future<void> markAsRead(String id);
  Future<void> markAllAsRead();
}
