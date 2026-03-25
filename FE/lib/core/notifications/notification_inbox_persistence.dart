import 'dart:convert';

import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:hive_flutter/hive_flutter.dart';
import 'package:more_app/domain/entities/app_notification_item.dart';
import 'package:more_app/presentation/navigation/app_router.dart';

class NotificationInboxPersistence {
  static const _boxName = 'more_notification_inbox_box';
  static const _storagePath = 'more_app';
  static const _maxItems = 100;

  static bool _initialized = false;
  static Box<String>? _box;

  static Future<void> ensureReady() async {
    if (_initialized && _box != null && _box!.isOpen) {
      return;
    }

    await Hive.initFlutter(_storagePath);
    _box = await Hive.openBox<String>(_boxName);
    _initialized = true;
  }

  static Future<List<AppNotificationItem>> getNotifications() async {
    await ensureReady();
    final box = _box!;
    final items = box.values
        .map((value) => AppNotificationItem.fromJson(
              jsonDecode(value) as Map<String, dynamic>,
            ))
        .toList()
      ..sort((a, b) => b.createdAt.compareTo(a.createdAt));
    return items;
  }

  static Future<void> saveNotification(AppNotificationItem item) async {
    await ensureReady();
    final box = _box!;
    await box.put(item.id, jsonEncode(item.toJson()));

    final items = await getNotifications();
    if (items.length > _maxItems) {
      for (final overflow in items.skip(_maxItems)) {
        await box.delete(overflow.id);
      }
    }
  }

  static Future<void> saveRemoteMessage(
    RemoteMessage message, {
    bool markAsRead = false,
  }) async {
    final item = mapRemoteMessageToInboxItem(
      message,
      markAsRead: markAsRead,
    );
    if (item == null) {
      return;
    }
    await saveNotification(item);
  }

  static Future<void> markAsRead(String id) async {
    await ensureReady();
    final box = _box!;
    final value = box.get(id);
    if (value == null) {
      return;
    }

    final item = AppNotificationItem.fromJson(
      jsonDecode(value) as Map<String, dynamic>,
    );
    await box.put(id, jsonEncode(item.copyWith(isRead: true).toJson()));
  }

  static Future<void> markAllAsRead() async {
    final items = await getNotifications();
    for (final item in items) {
      if (!item.isRead) {
        await markAsRead(item.id);
      }
    }
  }

  static AppNotificationItem? mapRemoteMessageToInboxItem(
    RemoteMessage message, {
    bool markAsRead = false,
  }) {
    final title =
        message.notification?.title ??
        message.data['title']?.toString() ??
        '';
    final body =
        message.notification?.body ??
        message.data['body']?.toString() ??
        '';

    if (title.isEmpty && body.isEmpty) {
      return null;
    }

    final createdAt = message.sentTime ?? DateTime.now();
    final id =
        message.messageId ??
        '${createdAt.microsecondsSinceEpoch}-${title.hashCode}-${body.hashCode}';

    final payload = message.data.map(
      (key, value) => MapEntry(key, value.toString()),
    );

    return AppNotificationItem(
      id: id,
      title: title,
      body: body,
      createdAt: createdAt,
      type: payload['type'] ?? 'general',
      route: _resolveRoute(payload),
      payload: payload,
      isRead: markAsRead,
    );
  }

  static String? _resolveRoute(Map<String, String> payload) {
    final explicitRoute = payload['route'];
    if (explicitRoute != null && explicitRoute.isNotEmpty) {
      return explicitRoute;
    }

    final type = payload['type']?.toUpperCase();
    final planId = payload['planId'];

    if (type == 'PLAN_AI_COMPLETED' && planId != null && planId.isNotEmpty) {
      final status = payload['aiPipelineStatus']?.toUpperCase();
      if (status == 'SUCCEEDED') {
        return AppRoutes.goalPreviewLocation(planId);
      }
      return AppRoutes.goalDetailLocation(planId);
    }

    return null;
  }
}
