abstract class SocialAuthDataSource {
  Future<String> getKakaoToken();
  Future<String> getGoogleToken();
  Future<void> logoutKakao();
  Future<void> logoutGoogle();
}
