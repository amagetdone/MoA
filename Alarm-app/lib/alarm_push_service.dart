import 'dart:async';

import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';

class AlarmPushService {
  AlarmPushService._();

  static final AlarmPushService instance = AlarmPushService._();

  static const verificationAlarmTopic = 'verification_alarm_demo';

  StreamSubscription<RemoteMessage>? _foregroundSubscription;

  Future<void> initialize({
    required Future<void> Function(RemoteMessage message) onForegroundMessage,
  }) async {
    if (Firebase.apps.isEmpty) {
      await Firebase.initializeApp();
    }

    await FirebaseMessaging.instance.requestPermission(
      alert: true,
      badge: true,
      sound: true,
    );

    await FirebaseMessaging.instance.subscribeToTopic(verificationAlarmTopic);

    await _foregroundSubscription?.cancel();
    _foregroundSubscription = FirebaseMessaging.onMessage.listen((message) {
      unawaited(onForegroundMessage(message));
    });
  }

  Future<String?> getToken() async {
    if (Firebase.apps.isEmpty) {
      await Firebase.initializeApp();
    }
    return FirebaseMessaging.instance.getToken();
  }

  Future<void> dispose() async {
    await _foregroundSubscription?.cancel();
    _foregroundSubscription = null;
  }
}
