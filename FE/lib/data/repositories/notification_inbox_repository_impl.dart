import 'dart:async';
import 'dart:convert';

import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:more_app/core/notifications/notification_inbox_persistence.dart';
import 'package:more_app/data/datasources/local/auth_local_datasource.dart';
import 'package:more_app/domain/entities/app_notification_item.dart';
import 'package:more_app/domain/repositories/notification_inbox_repository.dart';

class NotificationInboxRepositoryImpl implements NotificationInboxRepository {
  NotificationInboxRepositoryImpl({
    required AuthLocalDataSource authLocalDataSource,
  }) : _authLocalDataSource = authLocalDataSource;

  final StreamController<List<AppNotificationItem>> _controller =
      StreamController<List<AppNotificationItem>>.broadcast();
  final AuthLocalDataSource _authLocalDataSource;
  bool _initialized = false;

  @override
  Stream<List<AppNotificationItem>> watchNotifications() async* {
    await initialize();
    yield await getNotifications();
    yield* _controller.stream;
  }

  @override
  Future<void> initialize() async {
    await NotificationInboxPersistence.ensureReady();
    await _syncOwner();
    if (_initialized) {
      return;
    }

    _initialized = true;
    await _emit();
  }

  @override
  Future<List<AppNotificationItem>> getNotifications() async {
    await initialize();
    return NotificationInboxPersistence.getNotifications();
  }

  @override
  Future<void> saveNotification(AppNotificationItem item) async {
    await initialize();
    await NotificationInboxPersistence.saveNotification(item);
    await _emit();
  }

  @override
  Future<void> saveRemoteMessage(
    RemoteMessage message, {
    bool markAsRead = false,
  }) async {
    await initialize();
    await NotificationInboxPersistence.saveRemoteMessage(
      message,
      markAsRead: markAsRead,
    );
    await _emit();
  }

  @override
  Future<void> markAsRead(String id) async {
    await initialize();
    await NotificationInboxPersistence.markAsRead(id);
    await _emit();
  }

  @override
  Future<void> markAllAsRead() async {
    await initialize();
    await NotificationInboxPersistence.markAllAsRead();
    await _emit();
  }

  @override
  Future<void> deleteNotification(String id) async {
    await initialize();
    await NotificationInboxPersistence.deleteNotification(id);
    await _emit();
  }

  @override
  Future<void> clearNotifications() async {
    await NotificationInboxPersistence.clearNotifications();
    await _emit();
  }

  Future<void> _emit() async {
    if (_controller.isClosed) {
      return;
    }
    _controller.add(await NotificationInboxPersistence.getNotifications());
  }

  Future<void> _syncOwner() async {
    final accessToken = await _authLocalDataSource.getAccessToken();
    await NotificationInboxPersistence.syncOwner(
      _decodeUserIdFromAccessToken(accessToken),
    );
  }

  String? _decodeUserIdFromAccessToken(String? accessToken) {
    if (accessToken == null || accessToken.isEmpty) {
      return null;
    }

    try {
      final parts = accessToken.split('.');
      if (parts.length < 2) {
        return null;
      }

      final payload =
          jsonDecode(
                utf8.decode(base64Url.decode(base64Url.normalize(parts[1]))),
              )
              as Map<String, dynamic>;
      return payload['sub']?.toString();
    } catch (_) {
      return null;
    }
  }
}
