import 'package:more_app/data/models/user_model.dart';

abstract class AuthRemoteDataSource {
  /// 소셜 토큰을 백엔드에 전달하여 자체 JWT + 유저 정보를 받는다
  Future<({UserModel user, String accessToken, String refreshToken})>
      loginWithSocialToken({
    required String provider,
    required String socialToken,
  });

  /// refresh token으로 access token 갱신
  Future<({String accessToken, String refreshToken})> refreshToken({
    required String refreshToken,
  });

  Future<void> logout({required String accessToken});
}
