import 'dart:async';
import 'dart:math';

import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:more_app/data/datasources/local/app_local_datasource.dart';
import 'package:more_app/data/datasources/local/auth_local_datasource.dart';
import 'package:more_app/data/datasources/remote/push_notification_remote_datasource.dart';

class PushNotificationSyncService {
  PushNotificationSyncService({
    required PushNotificationRemoteDataSource remoteDataSource,
    required AuthLocalDataSource authLocalDataSource,
    required AppLocalDataSource appLocalDataSource,
    FlutterSecureStorage? secureStorage,
  }) : _remoteDataSource = remoteDataSource,
       _authLocalDataSource = authLocalDataSource,
       _appLocalDataSource = appLocalDataSource,
       _secureStorage = secureStorage ?? const FlutterSecureStorage();

  static const _installationIdKey = 'push_installation_id';

  final PushNotificationRemoteDataSource _remoteDataSource;
  final AuthLocalDataSource _authLocalDataSource;
  final AppLocalDataSource _appLocalDataSource;
  final FlutterSecureStorage _secureStorage;

  StreamSubscription<String>? _tokenRefreshSubscription;
  bool _initialized = false;

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

    _tokenRefreshSubscription = FirebaseMessaging.instance.onTokenRefresh.listen(
      (token) {
        unawaited(_upsertTokenIfAllowed(token));
      },
    );
  }

  Future<void> dispose() async {
    await _tokenRefreshSubscription?.cancel();
    _tokenRefreshSubscription = null;
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
