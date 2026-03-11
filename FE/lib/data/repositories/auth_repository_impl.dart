import 'package:flutter/foundation.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';
import 'package:more_app/core/utils/app_env.dart';
import 'package:more_app/data/datasources/local/auth_local_datasource.dart';
import 'package:more_app/data/datasources/remote/auth_remote_datasource.dart';
import 'package:more_app/data/models/user_model.dart';
import 'package:more_app/domain/entities/user.dart' as domain;
import 'package:more_app/domain/repositories/auth_repository.dart';

class AuthRepositoryImpl implements AuthRepository {
  final AuthRemoteDataSource remoteDataSource;
  final AuthLocalDataSource localDataSource;
  GoogleSignIn? _googleSignIn;

  AuthRepositoryImpl({
    required this.remoteDataSource,
    required this.localDataSource,
    GoogleSignIn? googleSignIn,
  }) : _googleSignIn = googleSignIn;

  GoogleSignIn _resolveGoogleSignIn() {
    return _googleSignIn ??= GoogleSignIn(
      clientId: kIsWeb ? AppEnv.optional('GOOGLE_CLIENT_ID') : null,
      serverClientId: kIsWeb ? null : AppEnv.optional('GOOGLE_CLIENT_ID'),
    );
  }

  @override
  Future<domain.User> loginWithKakao() async {
    if (AppEnv.optional('KAKAO_NATIVE_APP_KEY') == null) {
      throw Exception('KAKAO_NATIVE_APP_KEY가 설정되지 않았습니다.');
    }

    // 카카오톡 앱 설치 여부에 따라 분기
    OAuthToken token;
    if (await isKakaoTalkInstalled()) {
      token = await UserApi.instance.loginWithKakaoTalk();
    } else {
      token = await UserApi.instance.loginWithKakaoAccount();
    }

    // 소셜 토큰을 백엔드에 전달하여 JWT 발급
    final result = await remoteDataSource.loginWithSocialToken(
      provider: 'kakao',
      socialToken: token.accessToken,
    );

    // JWT를 로컬에 저장
    await localDataSource.saveAccessToken(result.accessToken);
    await localDataSource.saveRefreshToken(result.refreshToken);

    return result.user.toEntity();
  }

  @override
  Future<domain.User> loginWithGoogle() async {
    if (AppEnv.optional('GOOGLE_CLIENT_ID') == null) {
      throw Exception('GOOGLE_CLIENT_ID가 설정되지 않았습니다.');
    }

    final googleUser = await _resolveGoogleSignIn().signIn();
    if (googleUser == null) {
      throw Exception('구글 로그인이 취소되었습니다.');
    }

    final googleAuth = await googleUser.authentication;
    final idToken = googleAuth.idToken;
    if (idToken == null) {
      throw Exception('구글 인증 토큰을 가져올 수 없습니다.');
    }

    // 소셜 토큰을 백엔드에 전달하여 JWT 발급
    final result = await remoteDataSource.loginWithSocialToken(
      provider: 'google',
      socialToken: idToken,
    );

    await localDataSource.saveAccessToken(result.accessToken);
    await localDataSource.saveRefreshToken(result.refreshToken);

    return result.user.toEntity();
  }

  @override
  Future<void> logout() async {
    final accessToken = await localDataSource.getAccessToken();
    if (accessToken != null) {
      await remoteDataSource.logout(accessToken: accessToken);
    }

    // 소셜 로그아웃
    try {
      await UserApi.instance.logout();
    } catch (_) {}
    try {
      final googleSignIn = _googleSignIn;
      if (googleSignIn != null) {
        await googleSignIn.signOut();
      }
    } catch (_) {}

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
  Future<domain.User?> autoLogin() async {
    final hasTokens = await localDataSource.hasTokens();
    if (!hasTokens) return null;

    try {
      final refreshTokenValue = await localDataSource.getRefreshToken();
      if (refreshTokenValue == null) return null;

      // refresh token으로 새 access token 발급
      final result = await remoteDataSource.refreshToken(
        refreshToken: refreshTokenValue,
      );

      await localDataSource.saveAccessToken(result.accessToken);
      await localDataSource.saveRefreshToken(result.refreshToken);

      final user = await remoteDataSource.getCurrentUser(
        accessToken: result.accessToken,
      );
      return user.toEntity();
    } catch (_) {
      // 토큰 갱신 실패 시 로그아웃 처리
      await localDataSource.deleteTokens();
      return null;
    }
  }
}
