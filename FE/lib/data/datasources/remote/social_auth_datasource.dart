class GoogleSocialTokens {
  const GoogleSocialTokens({
    required this.idToken,
    required this.accessToken,
  });

  final String? idToken;
  final String? accessToken;

  bool get hasToken =>
      (idToken != null && idToken!.isNotEmpty) ||
      (accessToken != null && accessToken!.isNotEmpty);
}

abstract class SocialAuthDataSource {
  Future<String> getKakaoToken();
  Future<GoogleSocialTokens> getGoogleToken();
  Future<void> resetPendingLogins();
  Future<void> logoutKakao();
  Future<void> logoutGoogle();
}
