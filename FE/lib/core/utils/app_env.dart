import 'package:flutter_dotenv/flutter_dotenv.dart';

class AppEnv {
  static const Map<String, String> _placeholderValues = {
    'KAKAO_NATIVE_APP_KEY': 'your_kakao_native_app_key',
    'GOOGLE_CLIENT_ID': 'your_google_client_id',
  };

  static Future<void> load() => dotenv.load(fileName: '.env.example');

  static String? optional(String key) {
    final value = dotenv.env[key]?.trim();

    if (value == null || value.isEmpty || value == _placeholderValues[key]) {
      return null;
    }

    return value;
  }
}
