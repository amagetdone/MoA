import 'dart:async';

import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:more_app/core/notifications/notification_inbox_persistence.dart';
import 'package:more_app/domain/entities/app_notification_item.dart';
import 'package:more_app/domain/repositories/notification_inbox_repository.dart';

class NotificationInboxRepositoryImpl implements NotificationInboxRepository {
  NotificationInboxRepositoryImpl();

  final StreamController<List<AppNotificationItem>> _controller =
      StreamController<List<AppNotificationItem>>.broadcast();
  bool _initialized = false;

  @override
  Stream<List<AppNotificationItem>> watchNotifications() async* {
    await initialize();
    yield await getNotifications();
    yield* _controller.stream;
  }

  @override
  Future<void> initialize() async {
    if (_initialized) {
      return;
    }

    _initialized = true;
    await NotificationInboxPersistence.ensureReady();
    await _emit();
  }

  @override
  Future<List<AppNotificationItem>> getNotifications() async {
    await NotificationInboxPersistence.ensureReady();
    return NotificationInboxPersistence.getNotifications();
  }

  @override
  Future<void> saveNotification(AppNotificationItem item) async {
    await NotificationInboxPersistence.saveNotification(item);
    await _emit();
  }

  @override
  Future<void> saveRemoteMessage(
    RemoteMessage message, {
    bool markAsRead = false,
  }) async {
    await NotificationInboxPersistence.saveRemoteMessage(
      message,
      markAsRead: markAsRead,
    );
    await _emit();
  }

  @override
  Future<void> markAsRead(String id) async {
    await NotificationInboxPersistence.markAsRead(id);
    await _emit();
  }

  @override
  Future<void> markAllAsRead() async {
    await NotificationInboxPersistence.markAllAsRead();
    await _emit();
  }

  Future<void> _emit() async {
    if (_controller.isClosed) {
      return;
    }
    _controller.add(await getNotifications());
  }
}
