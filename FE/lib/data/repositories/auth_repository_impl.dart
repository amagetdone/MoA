import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';
import 'package:more_app/data/datasources/local/auth_local_datasource.dart';
import 'package:more_app/data/datasources/remote/auth_remote_datasource.dart';
import 'package:more_app/data/models/user_model.dart';
import 'package:more_app/domain/entities/user.dart' as domain;
import 'package:more_app/domain/repositories/auth_repository.dart';

class AuthRepositoryImpl implements AuthRepository {
  final AuthRemoteDataSource remoteDataSource;
  final AuthLocalDataSource localDataSource;
  final GoogleSignIn _googleSignIn;

  AuthRepositoryImpl({
    required this.remoteDataSource,
    required this.localDataSource,
    GoogleSignIn? googleSignIn,
  }) : _googleSignIn = googleSignIn ??
            GoogleSignIn(serverClientId: dotenv.env['GOOGLE_CLIENT_ID']);

  @override
  Future<domain.User> loginWithKakao() async {
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
    final googleUser = await _googleSignIn.signIn();
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
      await _googleSignIn.signOut();
    } catch (_) {}

    await localDataSource.deleteTokens();
  }

  @override
  Future<domain.User?> getCurrentUser() async {
    // TODO: 백엔드 연동 시 구현
    return null;
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

      // fake에서는 하드코딩된 유저 반환
      return const domain.User(
        id: '1',
        name: '테스트유저',
        email: 'test@more.com',
      );
    } catch (_) {
      // 토큰 갱신 실패 시 로그아웃 처리
      await localDataSource.deleteTokens();
      return null;
    }
  }
}
