import 'package:flutter/foundation.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';
import 'package:more_app/core/utils/app_env.dart';
import 'package:more_app/data/datasources/remote/social_auth_datasource.dart';

class SocialAuthDataSourceImpl implements SocialAuthDataSource {
  GoogleSignIn? _googleSignIn;

  static const String _webGoogleClientId =
      '26025204114-h0lsehum6766elgasajahjtdo79bsrej.apps.googleusercontent.com';

  GoogleSignIn _resolveGoogleSignIn() {
    return _googleSignIn ??= GoogleSignIn(
      clientId: kIsWeb ? _webGoogleClientId : null,
      serverClientId: kIsWeb ? null : AppEnv.optional('GOOGLE_CLIENT_ID'),
      scopes: ['email', 'openid', 'profile'],
    );
  }

  @override
  Future<String> getKakaoToken() async {
    if (!kIsWeb && AppEnv.optional('KAKAO_NATIVE_APP_KEY') == null) {
      throw Exception('KAKAO_NATIVE_APP_KEY가 설정되지 않았습니다.');
    }

    OAuthToken token;
    if (kIsWeb) {
      token = await UserApi.instance.loginWithKakaoAccount();
    } else if (await isKakaoTalkInstalled()) {
      token = await UserApi.instance.loginWithKakaoTalk();
    } else {
      token = await UserApi.instance.loginWithKakaoAccount();
    }

    return token.accessToken;
  }

  @override
  Future<String> getGoogleToken() async {
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

    return token;
  }

  @override
  Future<void> logoutKakao() async {
    try {
      await UserApi.instance.logout();
    } catch (_) {}
  }

  @override
  Future<void> logoutGoogle() async {
    try {
      final googleSignIn = _googleSignIn;
      if (googleSignIn != null) {
        await googleSignIn.signOut();
      }
    } catch (_) {}
  }
}
