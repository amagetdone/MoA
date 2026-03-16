import 'package:more_app/data/datasources/local/app_local_datasource.dart';
import 'package:more_app/data/datasources/remote/auth_remote_datasource.dart';
import 'package:more_app/data/models/social_login_response_dto.dart';
import 'package:more_app/data/models/token_refresh_response_dto.dart';
import 'package:more_app/data/models/user_dto.dart';

class SeedAuthRemoteDataSource implements AuthRemoteDataSource {
  SeedAuthRemoteDataSource({required AppLocalDataSource localDataSource})
    : _localDataSource = localDataSource;

  final AppLocalDataSource _localDataSource;

  @override
  Future<SocialLoginResponseDto> loginWithSocialToken({
    required String provider,
    required String socialToken,
  }) async {
    await Future.delayed(const Duration(milliseconds: 500));

    return SocialLoginResponseDto(
      status: 'TEMP',
      account: false,
      plan: false,
      accessToken: 'seed_access_token_${DateTime.now().millisecondsSinceEpoch}',
      refreshToken: 'seed_refresh_token_${DateTime.now().millisecondsSinceEpoch}',
    );
  }

  @override
  Future<TokenRefreshResponseDto> refreshToken({
    required String refreshToken,
  }) async {
    await Future.delayed(const Duration(milliseconds: 250));
    return TokenRefreshResponseDto(
      accessToken: 'seed_access_token_${DateTime.now().millisecondsSinceEpoch}',
      refreshToken: 'seed_refresh_token_${DateTime.now().millisecondsSinceEpoch}',
      status: 'ACTIVE',
      account: true,
      plan: true,
    );
  }

  @override
  Future<UserDto> getCurrentUser({required String accessToken}) async {
    final user = await _localDataSource.getUser();
    return UserDto(
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
