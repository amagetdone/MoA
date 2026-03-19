import 'package:more_app/data/models/social_login_response_dto.dart';
import 'package:more_app/data/models/token_refresh_response_dto.dart';
import 'package:more_app/data/models/user_dto.dart';

abstract class AuthRemoteDataSource {
  /// 소셜 토큰을 백엔드에 전달하여 자체 JWT + 온보딩 상태를 받는다
  Future<SocialLoginResponseDto> loginWithSocialToken({
    required String provider,
    required String socialToken,
  });

  /// refresh token으로 access token 갱신
  Future<TokenRefreshResponseDto> refreshToken({
    required String refreshToken,
  });

  Future<UserDto> getCurrentUser({required String accessToken});

  Future<void> logout({required String refreshToken});

  Future<SocialLoginResponseDto> loginWithTest({required int id});
}
