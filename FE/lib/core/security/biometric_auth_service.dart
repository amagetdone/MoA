import 'dart:convert';
import 'dart:math';

import 'package:crypto/crypto.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:local_auth/local_auth.dart';

class BiometricAuthService {
  BiometricAuthService({
    LocalAuthentication? localAuth,
    FlutterSecureStorage? secureStorage,
  }) : _localAuth = localAuth ?? LocalAuthentication(),
       _secureStorage = secureStorage ?? const FlutterSecureStorage();

  final LocalAuthentication _localAuth;
  final FlutterSecureStorage _secureStorage;

  static const _appPinHashKey = 'app_lock_pin_hash';
  static const _appPinSaltKey = 'app_lock_pin_salt';

  bool get isAndroidOnlySupported =>
      !kIsWeb && defaultTargetPlatform == TargetPlatform.android;

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

  Future<bool> authenticate({required String reason}) async {
    if (!await isDeviceAuthAvailable()) {
      return false;
    }

    try {
      return await _localAuth.authenticate(
        localizedReason: reason,
        options: const AuthenticationOptions(
          biometricOnly: false,
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
    final salt = _generateSalt();
    final hash = _hashPin(pin, salt);
    await _secureStorage.write(key: _appPinSaltKey, value: salt);
    await _secureStorage.write(key: _appPinHashKey, value: hash);
  }

  Future<bool> verifyAppPin(String pin) async {
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

  String _generateSalt() {
    final random = Random();
    final bytes = List<int>.generate(16, (_) => random.nextInt(256));
    return base64UrlEncode(bytes);
  }

  String _hashPin(String pin, String salt) {
    return sha256.convert(utf8.encode('$salt:$pin')).toString();
  }
}
