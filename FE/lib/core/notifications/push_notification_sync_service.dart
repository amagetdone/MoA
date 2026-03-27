import 'dart:async';
import 'dart:convert';
import 'dart:math';

import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:more_app/core/notifications/firebase_web_push_config.dart';
import 'package:more_app/core/notifications/local_notification_service.dart';
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
    required LocalNotificationService localNotificationService,
    FlutterSecureStorage? secureStorage,
  }) : _remoteDataSource = remoteDataSource,
       _authLocalDataSource = authLocalDataSource,
       _appLocalDataSource = appLocalDataSource,
       _notificationInboxRepository = notificationInboxRepository,
       _localNotificationService = localNotificationService,
       _secureStorage = secureStorage ?? const FlutterSecureStorage();

  static const _installationIdKey = 'push_installation_id';

  final PushNotificationRemoteDataSource _remoteDataSource;
  final AuthLocalDataSource _authLocalDataSource;
  final AppLocalDataSource _appLocalDataSource;
  final NotificationInboxRepository _notificationInboxRepository;
  final LocalNotificationService _localNotificationService;
  final FlutterSecureStorage _secureStorage;

  StreamSubscription<String>? _tokenRefreshSubscription;
  StreamSubscription<RemoteMessage>? _foregroundMessageSubscription;
  StreamSubscription<RemoteMessage>? _messageOpenedSubscription;
  bool _initialized = false;
  bool _firebaseReady = false;
  bool _permissionPromptInProgress = false;
  String? _lastOpenedMessageId;

  bool get _isSupportedPlatform {
    if (kIsWeb) {
      return true;
    }

    return switch (defaultTargetPlatform) {
      TargetPlatform.android || TargetPlatform.iOS => true,
      _ => false,
    };
  }

  String get _platform {
    if (kIsWeb) {
      return 'WEB';
    }

    return switch (defaultTargetPlatform) {
      TargetPlatform.iOS => 'IOS',
      _ => 'ANDROID',
    };
  }

  Future<void> initialize() async {
    if (_initialized || !_isSupportedPlatform) {
      return;
    }

    _initialized = true;

    await _notificationInboxRepository.initialize();
    await _restoreInboxFromLaunchPayload();
    await _localNotificationService.initialize();

    if (!await _ensureFirebaseReady()) {
      return;
    }

    final preferences = await _appLocalDataSource.getMenuPreferences();
    try {
      await FirebaseMessaging.instance.setAutoInitEnabled(
        preferences.notificationEnabled,
      );
    } catch (_) {}

    _tokenRefreshSubscription = FirebaseMessaging.instance.onTokenRefresh
        .listen((token) {
          unawaited(_upsertTokenIfAllowed(token));
        });

    _foregroundMessageSubscription = FirebaseMessaging.onMessage.listen((
      message,
    ) {
      unawaited(_handleForegroundMessage(message));
    });

    _messageOpenedSubscription = FirebaseMessaging.onMessageOpenedApp.listen((
      message,
    ) {
      unawaited(_handleOpenedMessage(message));
    });

    try {
      final initialMessage = await FirebaseMessaging.instance
          .getInitialMessage();
      if (initialMessage != null) {
        await _handleOpenedMessage(initialMessage);
      }
    } catch (_) {}
  }

  Future<bool> _ensureFirebaseReady() async {
    if (!_isSupportedPlatform) {
      return false;
    }

    if (Firebase.apps.isNotEmpty) {
      _firebaseReady = true;
      return true;
    }

    try {
      if (kIsWeb) {
        await Firebase.initializeApp(options: FirebaseWebPushConfig.options);
      } else {
        await Firebase.initializeApp();
        if (defaultTargetPlatform == TargetPlatform.android) {
          FirebaseMessaging.onBackgroundMessage(
            firebaseMessagingBackgroundHandler,
          );
        }
      }
    } catch (_) {}

    _firebaseReady = Firebase.apps.isNotEmpty;
    return _firebaseReady;
  }

  Future<void> _restoreInboxFromLaunchPayload() async {
    if (!kIsWeb) {
      return;
    }

    final encodedPayload = Uri.base.queryParameters['fcmPayload'];
    if (encodedPayload == null || encodedPayload.isEmpty) {
      return;
    }

    try {
      final envelope =
          jsonDecode(
                utf8.decode(base64Url.decode(base64.normalize(encodedPayload))),
              )
              as Map<String, dynamic>;
      await NotificationInboxPersistence.savePayloadEnvelope(
        envelope,
        markAsRead: true,
      );
    } catch (_) {}
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
    if (!_isSupportedPlatform) {
      return;
    }

    await initialize();
    if (!await _ensureFirebaseReady()) {
      return;
    }

    final preferences = await _appLocalDataSource.getMenuPreferences();
    if (!preferences.notificationEnabled) {
      try {
        await FirebaseMessaging.instance.setAutoInitEnabled(false);
      } catch (_) {}
      return;
    }

    final hasTokens = await _authLocalDataSource.hasTokens();
    if (!hasTokens) {
      return;
    }

    try {
      await FirebaseMessaging.instance.setAutoInitEnabled(true);
    } catch (_) {}

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

      final isGranted =
          settings.authorizationStatus == AuthorizationStatus.authorized ||
          settings.authorizationStatus == AuthorizationStatus.provisional;

      if (!isGranted) {
        return;
      }
    } else if (kIsWeb) {
      final settings = await FirebaseMessaging.instance
          .getNotificationSettings();
      final isGranted =
          settings.authorizationStatus == AuthorizationStatus.authorized ||
          settings.authorizationStatus == AuthorizationStatus.provisional;

      if (!isGranted) {
        return;
      }
    }

    final token = await FirebaseMessaging.instance.getToken(
      vapidKey: kIsWeb ? FirebaseWebPushConfig.vapidKey : null,
    );
    if (token == null || token.isEmpty) {
      return;
    }

    await _upsertTokenIfAllowed(token);
  }

  Future<void> promptPermissionIfNeededOnLaunch() async {
    if (!_isSupportedPlatform || _permissionPromptInProgress) {
      return;
    }

    await initialize();
    if (!await _ensureFirebaseReady()) {
      return;
    }

    final preferences = await _appLocalDataSource.getMenuPreferences();

    AuthorizationStatus currentStatus;
    try {
      final settings = await FirebaseMessaging.instance
          .getNotificationSettings();
      currentStatus = settings.authorizationStatus;
    } catch (_) {
      return;
    }

    final isAlreadyResolved =
        currentStatus == AuthorizationStatus.authorized ||
        currentStatus == AuthorizationStatus.provisional ||
        currentStatus == AuthorizationStatus.denied;
    if (isAlreadyResolved) {
      if ((currentStatus == AuthorizationStatus.authorized ||
              currentStatus == AuthorizationStatus.provisional) &&
          preferences.notificationEnabled) {
        await syncTokenIfEnabled(promptPermission: false);
      }
      return;
    }

    _permissionPromptInProgress = true;
    try {
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

      if (isGranted && preferences.notificationEnabled) {
        await syncTokenIfEnabled(promptPermission: false);
      }
    } finally {
      _permissionPromptInProgress = false;
    }
  }

  Future<bool> setNotificationsEnabled(bool enabled) async {
    if (!_isSupportedPlatform) {
      return enabled;
    }

    await initialize();
    if (!await _ensureFirebaseReady()) {
      if (!enabled) {
        return false;
      }

      throw StateError('푸시 알림 기능을 초기화하지 못했습니다. 새로고침 후 다시 시도해주세요.');
    }
    try {
      await FirebaseMessaging.instance.setAutoInitEnabled(enabled);
    } catch (_) {}

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
    await _notificationInboxRepository.clearNotifications();

    if (!_isSupportedPlatform) {
      return;
    }

    await initialize();
    if (!await _ensureFirebaseReady()) {
      return;
    }
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
    _navigateFromNotification(
      NotificationInboxPersistence.resolveRouteForItem(item),
      item.id,
    );
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

  Future<void> _handleForegroundMessage(RemoteMessage message) async {
    await _notificationInboxRepository.saveRemoteMessage(message);
    await _localNotificationService.showRemoteMessage(message);
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

    await _secureStorage.write(key: _installationIdKey, value: installationId);
    return installationId;
  }
}
