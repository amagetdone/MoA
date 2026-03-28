import 'package:flutter/foundation.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

class AlarmNotificationService {
  AlarmNotificationService._();

  static final AlarmNotificationService instance = AlarmNotificationService._();

  static const _channelId = 'bank_alarm_demo';
  static const _channelName = '입금 확인 알림';
  static const _channelDescription = '시연용 인증 및 입금 알림을 표시합니다.';

  final FlutterLocalNotificationsPlugin _plugin =
      FlutterLocalNotificationsPlugin();

  bool _initialized = false;

  Future<void> initialize() async {
    if (_initialized || kIsWeb) {
      return;
    }

    const settings = InitializationSettings(
      android: AndroidInitializationSettings('@mipmap/ic_launcher'),
    );

    await _plugin.initialize(settings);

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
    await androidPlugin?.requestNotificationsPermission();

    _initialized = true;
  }

  Future<void> showVerificationDeposit({
    required String authCode,
    int amount = 1,
  }) async {
    await showAlarm(
      title: '$amount원 입금',
      body: 'AMAGETDONE $authCode 입금',
    );
  }

  Future<void> showRemoteMessage(RemoteMessage message) async {
    await showAlarm(
      title: message.notification?.title ?? '입금 알림',
      body: message.notification?.body ?? message.data['merchantOrMemo']?.toString() ?? '입금 알림이 도착했습니다.',
    );
  }

  Future<void> showAlarm({
    required String title,
    required String body,
  }) async {
    await initialize();

    await _plugin.show(
      DateTime.now().microsecondsSinceEpoch.remainder(1 << 31),
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
          category: AndroidNotificationCategory.message,
          ticker: '인증용 입금 알림',
          styleInformation: BigTextStyleInformation(''),
        ),
      ),
    );
  }
}
