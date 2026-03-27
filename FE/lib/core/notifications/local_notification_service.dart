import 'dart:convert';

import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:more_app/core/notifications/notification_inbox_persistence.dart';
import 'package:more_app/presentation/navigation/app_router.dart';

class LocalNotificationService {
  static const _channelId = 'more_fcm_foreground';
  static const _channelName = 'MORE 알림';
  static const _channelDescription = '거래 및 목표 관련 알림을 표시합니다.';

  final FlutterLocalNotificationsPlugin _plugin =
      FlutterLocalNotificationsPlugin();

  bool _initialized = false;

  Future<void> initialize() async {
    if (_initialized || kIsWeb) {
      return;
    }

    const initializationSettings = InitializationSettings(
      android: AndroidInitializationSettings('@mipmap/ic_launcher'),
      iOS: DarwinInitializationSettings(),
    );

    await _plugin.initialize(
      initializationSettings,
      onDidReceiveNotificationResponse: _onNotificationResponse,
    );

    final androidPlugin = _plugin
        .resolvePlatformSpecificImplementation<
          AndroidFlutterLocalNotificationsPlugin
        >();
    await androidPlugin?.createNotificationChannel(
      const AndroidNotificationChannel(
        _channelId,
        _channelName,
        description: _channelDescription,
        importance: Importance.max,
      ),
    );

    _initialized = true;
  }

  Future<void> showRemoteMessage(RemoteMessage message) async {
    if (kIsWeb) {
      return;
    }

    await initialize();

    final item = NotificationInboxPersistence.mapRemoteMessageToInboxItem(
      message,
    );
    final title = item?.title ?? message.notification?.title ?? '';
    final body = item?.body ?? message.notification?.body ?? '';
    if (title.isEmpty && body.isEmpty) {
      return;
    }

    final route = item == null
        ? null
        : NotificationInboxPersistence.resolveRouteForItem(item);
    final payload = item == null
        ? null
        : jsonEncode({'route': route, 'messageId': item.id});

    await _plugin.show(
      _notificationId(message, item?.id),
      title,
      body,
      const NotificationDetails(
        android: AndroidNotificationDetails(
          _channelId,
          _channelName,
          channelDescription: _channelDescription,
          importance: Importance.max,
          priority: Priority.high,
          icon: '@mipmap/ic_launcher',
        ),
        iOS: DarwinNotificationDetails(
          presentAlert: true,
          presentBadge: true,
          presentSound: true,
        ),
      ),
      payload: payload,
    );
  }

  void _onNotificationResponse(NotificationResponse response) {
    final payload = response.payload;
    if (payload == null || payload.isEmpty) {
      return;
    }

    try {
      final decoded = jsonDecode(payload) as Map<String, dynamic>;
      final route = decoded['route']?.toString();
      if (route != null && route.isNotEmpty) {
        appRouter.go(route);
      }
    } catch (_) {}
  }

  int _notificationId(RemoteMessage message, String? itemId) {
    final source =
        message.messageId ??
        itemId ??
        DateTime.now().microsecondsSinceEpoch.toString();
    return source.hashCode & 0x7fffffff;
  }
}
