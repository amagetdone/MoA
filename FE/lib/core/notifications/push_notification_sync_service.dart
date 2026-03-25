import 'dart:async';
import 'dart:math';

import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:more_app/core/notifications/notification_inbox_persistence.dart';
import 'package:more_app/data/datasources/local/app_local_datasource.dart';
import 'package:more_app/data/datasources/local/auth_local_datasource.dart';
import 'package:more_app/data/datasources/remote/push_notification_remote_datasource.dart';
import 'package:more_app/domain/repositories/notification_inbox_repository.dart';
import 'package:more_app/presentation/navigation/app_router.dart';

Future<void> firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  await NotificationInboxPersistence.saveRemoteMessage(message);
}

class PushNotificationSyncService {
  PushNotificationSyncService({
    required PushNotificationRemoteDataSource remoteDataSource,
    required AuthLocalDataSource authLocalDataSource,
    required AppLocalDataSource appLocalDataSource,
    required NotificationInboxRepository notificationInboxRepository,
    FlutterSecureStorage? secureStorage,
  }) : _remoteDataSource = remoteDataSource,
       _authLocalDataSource = authLocalDataSource,
       _appLocalDataSource = appLocalDataSource,
       _notificationInboxRepository = notificationInboxRepository,
       _secureStorage = secureStorage ?? const FlutterSecureStorage();

  static const _installationIdKey = 'push_installation_id';

  final PushNotificationRemoteDataSource _remoteDataSource;
  final AuthLocalDataSource _authLocalDataSource;
  final AppLocalDataSource _appLocalDataSource;
  final NotificationInboxRepository _notificationInboxRepository;
  final FlutterSecureStorage _secureStorage;

  StreamSubscription<String>? _tokenRefreshSubscription;
  StreamSubscription<RemoteMessage>? _foregroundMessageSubscription;
  StreamSubscription<RemoteMessage>? _messageOpenedSubscription;
  bool _initialized = false;
  String? _lastOpenedMessageId;

  bool get _isSupportedMobilePlatform {
    if (kIsWeb) {
      return false;
    }

    return switch (defaultTargetPlatform) {
      TargetPlatform.android || TargetPlatform.iOS => true,
      _ => false,
    };
  }

  String get _platform {
    return switch (defaultTargetPlatform) {
      TargetPlatform.iOS => 'IOS',
      _ => 'ANDROID',
    };
  }

  Future<void> initialize() async {
    if (_initialized || !_isSupportedMobilePlatform) {
      return;
    }

    _initialized = true;

    final preferences = await _appLocalDataSource.getMenuPreferences();
    await FirebaseMessaging.instance.setAutoInitEnabled(
      preferences.notificationEnabled,
    );

    await _notificationInboxRepository.initialize();

    _tokenRefreshSubscription = FirebaseMessaging.instance.onTokenRefresh.listen(
      (token) {
        unawaited(_upsertTokenIfAllowed(token));
      },
    );

    _foregroundMessageSubscription = FirebaseMessaging.onMessage.listen((
      message,
    ) {
      unawaited(_notificationInboxRepository.saveRemoteMessage(message));
    });

    _messageOpenedSubscription = FirebaseMessaging.onMessageOpenedApp.listen((
      message,
    ) {
      unawaited(_handleOpenedMessage(message));
    });

    final initialMessage = await FirebaseMessaging.instance.getInitialMessage();
    if (initialMessage != null) {
      await _handleOpenedMessage(initialMessage);
    }
  }

  Future<void> dispose() async {
    await _tokenRefreshSubscription?.cancel();
    await _foregroundMessageSubscription?.cancel();
    await _messageOpenedSubscription?.cancel();
    _tokenRefreshSubscription = null;
    _foregroundMessageSubscription = null;
    _messageOpenedSubscription = null;
    _initialized = false;
  }

  Future<void> syncTokenIfEnabled({bool promptPermission = true}) async {
    if (!_isSupportedMobilePlatform) {
      return;
    }

    await initialize();

    final preferences = await _appLocalDataSource.getMenuPreferences();
    if (!preferences.notificationEnabled) {
      await FirebaseMessaging.instance.setAutoInitEnabled(false);
      return;
    }

    final hasTokens = await _authLocalDataSource.hasTokens();
    if (!hasTokens) {
      return;
    }

    await FirebaseMessaging.instance.setAutoInitEnabled(true);

    if (promptPermission) {
      final settings = await FirebaseMessaging.instance.requestPermission(
        alert: true,
        announcement: false,
        badge: true,
        carPlay: false,
        criticalAlert: false,
        provisional: false,
        sound: true,
      );

      if (settings.authorizationStatus == AuthorizationStatus.denied) {
        return;
      }
    }

    final token = await FirebaseMessaging.instance.getToken();
    if (token == null || token.isEmpty) {
      return;
    }

    await _upsertTokenIfAllowed(token);
  }

  Future<bool> setNotificationsEnabled(bool enabled) async {
    if (!_isSupportedMobilePlatform) {
      return enabled;
    }

    await initialize();
    await FirebaseMessaging.instance.setAutoInitEnabled(enabled);

    if (!enabled) {
      try {
        await FirebaseMessaging.instance.deleteToken();
      } catch (_) {}
      return false;
    }

    final settings = await FirebaseMessaging.instance.requestPermission(
      alert: true,
      announcement: false,
      badge: true,
      carPlay: false,
      criticalAlert: false,
      provisional: false,
      sound: true,
    );

    final isGranted =
        settings.authorizationStatus == AuthorizationStatus.authorized ||
        settings.authorizationStatus == AuthorizationStatus.provisional;

    if (!isGranted) {
      return false;
    }

    await syncTokenIfEnabled(promptPermission: false);
    return true;
  }

  Future<void> handleLogout() async {
    if (!_isSupportedMobilePlatform) {
      return;
    }

    await initialize();
    try {
      await FirebaseMessaging.instance.deleteToken();
    } catch (_) {}
  }

  Future<void> _upsertTokenIfAllowed(String token) async {
    final preferences = await _appLocalDataSource.getMenuPreferences();
    if (!preferences.notificationEnabled) {
      return;
    }

    final hasTokens = await _authLocalDataSource.hasTokens();
    if (!hasTokens) {
      return;
    }

    final installationId = await _getOrCreateInstallationId();
    await _remoteDataSource.upsertDeviceToken(
      installationId: installationId,
      fcmToken: token,
      platform: _platform,
    );
  }

  Future<void> _handleOpenedMessage(RemoteMessage message) async {
    final item = NotificationInboxPersistence.mapRemoteMessageToInboxItem(
      message,
      markAsRead: true,
    );
    if (item == null) {
      return;
    }

    await _notificationInboxRepository.saveNotification(item);
    _navigateFromNotification(item.route, item.id);
  }

  void _navigateFromNotification(String? route, String messageId) {
    if (route == null || route.isEmpty || _lastOpenedMessageId == messageId) {
      return;
    }

    _lastOpenedMessageId = messageId;
    Future<void>.delayed(const Duration(milliseconds: 250), () {
      appRouter.go(route);
    });
  }

  Future<String> _getOrCreateInstallationId() async {
    final saved = await _secureStorage.read(key: _installationIdKey);
    if (saved != null && saved.isNotEmpty) {
      return saved;
    }

    final random = Random.secure();
    final installationId = [
      DateTime.now().microsecondsSinceEpoch.toRadixString(16),
      random.nextInt(0x7fffffff).toRadixString(16),
      random.nextInt(0x7fffffff).toRadixString(16),
    ].join('-');

    await _secureStorage.write(
      key: _installationIdKey,
      value: installationId,
    );
    return installationId;
  }
}
