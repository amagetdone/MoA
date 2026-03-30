import 'dart:convert';
import 'dart:math';

import 'package:crypto/crypto.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:local_auth/local_auth.dart';

enum AppLockMethod { biometric, pin }

class BiometricAuthService {
  BiometricAuthService({
    LocalAuthentication? localAuth,
    FlutterSecureStorage? secureStorage,
  }) : _localAuth = localAuth ?? LocalAuthentication(),
       _secureStorage = secureStorage ?? const FlutterSecureStorage();

  final LocalAuthentication _localAuth;
  final FlutterSecureStorage _secureStorage;

  static const int appPinLength = 4;
  static const _appPinHashKey = 'app_lock_pin_hash';
  static const _appPinSaltKey = 'app_lock_pin_salt';
  static const _appLockMethodKey = 'app_lock_method';

  bool get isAndroidOnlySupported =>
      !kIsWeb && defaultTargetPlatform == TargetPlatform.android;

  Future<bool> isBiometricAvailable() async {
    if (!isAndroidOnlySupported) {
      return false;
    }

    try {
      final canCheckBiometrics = await _localAuth.canCheckBiometrics;
      if (!canCheckBiometrics) {
        return false;
      }

      final availableBiometrics = await _localAuth.getAvailableBiometrics();
      return availableBiometrics.isNotEmpty;
    } catch (_) {
      return false;
    }
  }

  Future<bool> isDeviceAuthAvailable() async {
    if (!isAndroidOnlySupported) {
      return false;
    }

    try {
      return await _localAuth.isDeviceSupported();
    } catch (_) {
      return false;
    }
  }

  Future<bool> authenticate({
    required String reason,
    bool biometricOnly = false,
  }) async {
    final isAvailable = biometricOnly
        ? await isBiometricAvailable()
        : await isDeviceAuthAvailable();
    if (!isAvailable) {
      return false;
    }

    try {
      return await _localAuth.authenticate(
        localizedReason: reason,
        options: AuthenticationOptions(
          biometricOnly: biometricOnly,
          stickyAuth: true,
          useErrorDialogs: true,
        ),
      );
    } catch (_) {
      return false;
    }
  }

  Future<bool> hasAppPin() async {
    final hash = await _secureStorage.read(key: _appPinHashKey);
    final salt = await _secureStorage.read(key: _appPinSaltKey);
    return hash != null && hash.isNotEmpty && salt != null && salt.isNotEmpty;
  }

  Future<void> saveAppPin(String pin) async {
    if (!_isValidPin(pin)) {
      throw ArgumentError.value(pin, 'pin', 'App PIN must be 4 digits.');
    }
    final salt = _generateSalt();
    final hash = _hashPin(pin, salt);
    await _secureStorage.write(key: _appPinSaltKey, value: salt);
    await _secureStorage.write(key: _appPinHashKey, value: hash);
  }

  Future<void> saveAppLockMethod(AppLockMethod method) async {
    await _secureStorage.write(key: _appLockMethodKey, value: method.name);
  }

  Future<AppLockMethod?> getAppLockMethod() async {
    final raw = await _secureStorage.read(key: _appLockMethodKey);
    return switch (raw) {
      'biometric' => AppLockMethod.biometric,
      'pin' => AppLockMethod.pin,
      _ => null,
    };
  }

  Future<bool> verifyAppPin(String pin) async {
    if (!_isValidPin(pin)) {
      return false;
    }

    final salt = await _secureStorage.read(key: _appPinSaltKey);
    final savedHash = await _secureStorage.read(key: _appPinHashKey);

    if (salt == null || savedHash == null) {
      return false;
    }

    return _hashPin(pin, salt) == savedHash;
  }

  Future<void> clearAppPin() async {
    await _secureStorage.delete(key: _appPinSaltKey);
    await _secureStorage.delete(key: _appPinHashKey);
  }

  Future<void> clearAppLockMethod() async {
    await _secureStorage.delete(key: _appLockMethodKey);
  }

  String _generateSalt() {
    final random = Random();
    final bytes = List<int>.generate(16, (_) => random.nextInt(256));
    return base64UrlEncode(bytes);
  }

  String _hashPin(String pin, String salt) {
    return sha256.convert(utf8.encode('$salt:$pin')).toString();
  }

  bool _isValidPin(String pin) {
    return RegExp('^\\d{$appPinLength}\$').hasMatch(pin);
  }
}
