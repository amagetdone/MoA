import 'package:flutter/foundation.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

class AppEnv {
  static bool _loaded = false;

  static const Map<String, String> _placeholderValues = {
    'KAKAO_NATIVE_APP_KEY': 'your_kakao_native_app_key',
    'GOOGLE_CLIENT_ID': 'your_google_client_id',
  };

  static const Map<String, String> _dartDefineValues = {
    'KAKAO_NATIVE_APP_KEY': String.fromEnvironment('KAKAO_NATIVE_APP_KEY'),
    'GOOGLE_CLIENT_ID': String.fromEnvironment('GOOGLE_CLIENT_ID'),
  };

  static Future<void> load() async {
    if (kIsWeb) {
      return;
    }

    await dotenv.load(fileName: '.env');
    _loaded = true;
  }

  static String? optional(String key) {
    final defineValue = _dartDefineValues[key]?.trim();
    if (defineValue != null &&
        defineValue.isNotEmpty &&
        defineValue != _placeholderValues[key]) {
      return defineValue;
    }

    if (!_loaded) {
      return null;
    }

    final value = dotenv.env[key]?.trim();

    if (value == null || value.isEmpty || value == _placeholderValues[key]) {
      return null;
    }

    return value;
  }
}
