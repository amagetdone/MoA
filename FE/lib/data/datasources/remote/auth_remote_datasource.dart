import 'package:more_app/data/models/user_model.dart';

/// 소셜 로그인 API 응답
class SocialLoginResponse {
  final String status; // "ACTIVE" | "TEMP"
  final bool account;
  final bool plan;
  final String accessToken;
  final String refreshToken;

  SocialLoginResponse({
    required this.status,
    required this.account,
    required this.plan,
    required this.accessToken,
    required this.refreshToken,
  });

  factory SocialLoginResponse.fromJson(Map<String, dynamic> json) {
    return SocialLoginResponse(
      status: json['status'] as String? ?? 'TEMP',
      account: json['account'] as bool? ?? false,
      plan: json['plan'] as bool? ?? false,
      accessToken: json['accessToken'] as String? ?? '',
      refreshToken: json['refreshToken'] as String? ?? '',
    );
  }
}

/// 토큰 재발급 API 응답 (accessToken, refreshToken만 포함)
class TokenRefreshResponse {
  final String accessToken;
  final String refreshToken;

  TokenRefreshResponse({
    required this.accessToken,
    required this.refreshToken,
  });

  factory TokenRefreshResponse.fromJson(Map<String, dynamic> json) {
    return TokenRefreshResponse(
      accessToken: json['accessToken'] as String? ?? '',
      refreshToken: json['refreshToken'] as String? ?? '',
    );
  }
}

abstract class AuthRemoteDataSource {
  /// 소셜 토큰을 백엔드에 전달하여 자체 JWT + 온보딩 상태를 받는다
  Future<SocialLoginResponse> loginWithSocialToken({
    required String provider,
    required String socialToken,
  });

  /// refresh token으로 access token 갱신
  Future<TokenRefreshResponse> refreshToken({
    required String refreshToken,
  });

  Future<UserModel> getCurrentUser({required String accessToken});

  Future<void> logout({required String refreshToken});
}
