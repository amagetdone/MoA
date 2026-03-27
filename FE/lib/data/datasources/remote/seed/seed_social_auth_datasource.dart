import 'package:more_app/data/datasources/remote/social_auth_datasource.dart';

class SeedSocialAuthDataSource implements SocialAuthDataSource {
  @override
  Future<String> getKakaoToken() async {
    await Future.delayed(const Duration(milliseconds: 300));
    return 'seed_kakao_token_${DateTime.now().millisecondsSinceEpoch}';
  }

  @override
  Future<GoogleSocialTokens> getGoogleToken() async {
    await Future.delayed(const Duration(milliseconds: 300));
    final token = 'seed_google_token_${DateTime.now().millisecondsSinceEpoch}';
    return GoogleSocialTokens(idToken: token, accessToken: token);
  }

  @override
  Future<void> resetPendingLogins() async {}

  @override
  Future<void> logoutKakao() async {}

  @override
  Future<void> logoutGoogle() async {}
}
