import 'package:flutter/foundation.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';
import 'package:more_app/core/utils/app_env.dart';
import 'package:more_app/data/datasources/local/auth_local_datasource.dart';
import 'package:more_app/data/datasources/remote/auth_remote_datasource.dart';
import 'package:more_app/data/models/user_model.dart';
import 'package:more_app/domain/entities/auth_result.dart';
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

  static const String _webGoogleClientId =
      '26025204114-h0lsehum6766elgasajahjtdo79bsrej.apps.googleusercontent.com';

  GoogleSignIn _resolveGoogleSignIn() {
    return _googleSignIn ??= GoogleSignIn(
      clientId: kIsWeb ? _webGoogleClientId : null,
      serverClientId: kIsWeb ? null : AppEnv.optional('GOOGLE_CLIENT_ID'),
      scopes: ['email', 'openid', 'profile'],
    );
  }

  AuthResult _toAuthResult(SocialLoginResponse response) {
    return AuthResult(
      status: response.status,
      account: response.account,
      plan: response.plan,
    );
  }

  Future<AuthResult> _saveTokensAndConvert(SocialLoginResponse response) async {
    await localDataSource.saveAccessToken(response.accessToken);
    await localDataSource.saveRefreshToken(response.refreshToken);
    return _toAuthResult(response);
  }

  @override
  Future<AuthResult> loginWithKakao() async {
    if (!kIsWeb && AppEnv.optional('KAKAO_NATIVE_APP_KEY') == null) {
      throw Exception('KAKAO_NATIVE_APP_KEY가 설정되지 않았습니다.');
    }

    // 웹에서는 카카오 계정 로그인, 앱에서는 카카오톡 설치 여부에 따라 분기
    OAuthToken token;
    if (kIsWeb) {
      token = await UserApi.instance.loginWithKakaoAccount();
    } else if (await isKakaoTalkInstalled()) {
      token = await UserApi.instance.loginWithKakaoTalk();
    } else {
      token = await UserApi.instance.loginWithKakaoAccount();
    }

    // 소셜 토큰을 백엔드에 전달하여 JWT 발급
    final result = await remoteDataSource.loginWithSocialToken(
      provider: 'kakao',
      socialToken: token.accessToken,
    );

    return _saveTokensAndConvert(result);
  }

  @override
  Future<AuthResult> loginWithGoogle() async {
    if (!kIsWeb && AppEnv.optional('GOOGLE_CLIENT_ID') == null) {
      throw Exception('GOOGLE_CLIENT_ID가 설정되지 않았습니다.');
    }

    final googleUser = await _resolveGoogleSignIn().signIn();
    if (googleUser == null) {
      throw Exception('구글 로그인이 취소되었습니다.');
    }

    final googleAuth = await googleUser.authentication;
    final token = googleAuth.idToken ?? googleAuth.accessToken;
    if (token == null) {
      throw Exception('구글 인증 토큰을 가져올 수 없습니다.');
    }

    // 소셜 토큰을 백엔드에 전달하여 JWT 발급
    final result = await remoteDataSource.loginWithSocialToken(
      provider: 'google',
      socialToken: token,
    );

    return _saveTokensAndConvert(result);
  }

  @override
  Future<void> logout() async {
    final refreshTokenValue = await localDataSource.getRefreshToken();
    if (refreshTokenValue != null) {
      await remoteDataSource.logout(refreshToken: refreshTokenValue);
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
  Future<bool> autoLogin() async {
    final hasTokens = await localDataSource.hasTokens();
    if (!hasTokens) return false;

    try {
      final refreshTokenValue = await localDataSource.getRefreshToken();
      if (refreshTokenValue == null) return false;

      // refresh token으로 새 access token 발급
      final result = await remoteDataSource.refreshToken(
        refreshToken: refreshTokenValue,
      );

      await localDataSource.saveAccessToken(result.accessToken);
      await localDataSource.saveRefreshToken(result.refreshToken);

      return true;
    } catch (_) {
      // 토큰 갱신 실패 시 로그아웃 처리
      await localDataSource.deleteTokens();
      return false;
    }
  }
}
