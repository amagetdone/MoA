import 'package:more_app/data/datasources/local/auth_local_datasource.dart';
import 'package:more_app/data/datasources/remote/auth_remote_datasource.dart';
import 'package:more_app/data/datasources/remote/social_auth_datasource.dart';
import 'package:more_app/data/models/social_login_response_dto.dart';
import 'package:more_app/data/models/user_dto.dart';
import 'package:more_app/domain/entities/auth_result.dart';
import 'package:more_app/domain/entities/user.dart' as domain;
import 'package:more_app/domain/repositories/auth_repository.dart';

class AuthRepositoryImpl implements AuthRepository {
  final AuthRemoteDataSource remoteDataSource;
  final AuthLocalDataSource localDataSource;
  final SocialAuthDataSource socialAuthDataSource;

  AuthRepositoryImpl({
    required this.remoteDataSource,
    required this.localDataSource,
    required this.socialAuthDataSource,
  });

  AuthResult _toAuthResult(SocialLoginResponseDto response) {
    return AuthResult(
      status: response.status,
      account: response.account,
      plan: response.plan,
    );
  }

  Future<AuthResult> _saveTokensAndConvert(SocialLoginResponseDto response) async {
    await localDataSource.saveAccessToken(response.accessToken);
    await localDataSource.saveRefreshToken(response.refreshToken);
    return _toAuthResult(response);
  }

  @override
  Future<AuthResult> loginWithKakao() async {
    final socialToken = await socialAuthDataSource.getKakaoToken();
    final result = await remoteDataSource.loginWithSocialToken(
      provider: 'kakao',
      socialToken: socialToken,
    );
    return _saveTokensAndConvert(result);
  }

  @override
  Future<AuthResult> loginWithGoogle() async {
    final socialToken = await socialAuthDataSource.getGoogleToken();
    final result = await remoteDataSource.loginWithSocialToken(
      provider: 'google',
      socialToken: socialToken,
    );
    return _saveTokensAndConvert(result);
  }

  @override
  Future<void> logout() async {
    final refreshTokenValue = await localDataSource.getRefreshToken();
    if (refreshTokenValue != null) {
      await remoteDataSource.logout(refreshToken: refreshTokenValue);
    }
    await socialAuthDataSource.logoutKakao();
    await socialAuthDataSource.logoutGoogle();
    await localDataSource.deleteTokens();
  }

  @override
  Future<domain.User?> getCurrentUser() async {
    final accessToken = await localDataSource.getAccessToken();
    if (accessToken == null) {
      return null;
    }

    final user = await remoteDataSource.getCurrentUser(
      accessToken: accessToken,
    );
    return user.toEntity();
  }

  @override
  Future<bool> isLoggedIn() => localDataSource.hasTokens();

  @override
  Future<AuthResult?> autoLogin() async {
    final hasTokens = await localDataSource.hasTokens();
    if (!hasTokens) return null;

    try {
      final refreshTokenValue = await localDataSource.getRefreshToken();
      if (refreshTokenValue == null) return null;

      final result = await remoteDataSource.refreshToken(
        refreshToken: refreshTokenValue,
      );

      await localDataSource.saveAccessToken(result.accessToken);
      await localDataSource.saveRefreshToken(result.refreshToken);

      return AuthResult(
        status: result.status,
        account: result.account,
        plan: result.plan,
      );
    } catch (_) {
      await localDataSource.deleteTokens();
      return null;
    }
  }
}
