import 'package:more_app/data/datasources/local/app_local_datasource.dart';
import 'package:more_app/data/datasources/remote/auth_remote_datasource.dart';
import 'package:more_app/data/models/user_model.dart';

class SeedAuthRemoteDataSource implements AuthRemoteDataSource {
  SeedAuthRemoteDataSource({required AppLocalDataSource localDataSource})
    : _localDataSource = localDataSource;

  final AppLocalDataSource _localDataSource;

  @override
  Future<SocialLoginResponse> loginWithSocialToken({
    required String provider,
    required String socialToken,
  }) async {
    await Future.delayed(const Duration(milliseconds: 500));

    return SocialLoginResponse(
      status: 'TEMP',
      account: false,
      plan: false,
      accessToken: 'seed_access_token_${DateTime.now().millisecondsSinceEpoch}',
      refreshToken: 'seed_refresh_token_${DateTime.now().millisecondsSinceEpoch}',
    );
  }

  @override
  Future<TokenRefreshResponse> refreshToken({
    required String refreshToken,
  }) async {
    await Future.delayed(const Duration(milliseconds: 250));
    return TokenRefreshResponse(
      accessToken: 'seed_access_token_${DateTime.now().millisecondsSinceEpoch}',
      refreshToken: 'seed_refresh_token_${DateTime.now().millisecondsSinceEpoch}',
    );
  }

  @override
  Future<UserModel> getCurrentUser({required String accessToken}) async {
    final user = await _localDataSource.getUser();
    return UserModel(
      id: user.id,
      name: user.name,
      email: user.email,
      profileImageUrl: user.profileImageUrl,
    );
  }

  @override
  Future<void> logout({required String refreshToken}) async {
    await Future.delayed(const Duration(milliseconds: 150));
  }
}
