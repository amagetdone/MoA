import 'package:more_app/data/datasources/remote/auth_remote_datasource.dart';
import 'package:more_app/data/models/user_model.dart';

class FakeAuthRemoteDataSource implements AuthRemoteDataSource {
  @override
  Future<({UserModel user, String accessToken, String refreshToken})>
      loginWithSocialToken({
    required String provider,
    required String socialToken,
  }) async {
    // 네트워크 지연 시뮬레이션
    await Future.delayed(const Duration(seconds: 1));

    return (
      user: const UserModel(
        id: '1',
        name: '테스트유저',
        email: 'test@more.com',
        profileImageUrl: null,
      ),
      accessToken: 'fake_access_token_${DateTime.now().millisecondsSinceEpoch}',
      refreshToken:
          'fake_refresh_token_${DateTime.now().millisecondsSinceEpoch}',
    );
  }

  @override
  Future<({String accessToken, String refreshToken})> refreshToken({
    required String refreshToken,
  }) async {
    await Future.delayed(const Duration(milliseconds: 500));

    // fake refresh: 항상 성공
    return (
      accessToken: 'fake_access_token_${DateTime.now().millisecondsSinceEpoch}',
      refreshToken:
          'fake_refresh_token_${DateTime.now().millisecondsSinceEpoch}',
    );
  }

  @override
  Future<void> logout({required String accessToken}) async {
    await Future.delayed(const Duration(milliseconds: 300));
  }
}
