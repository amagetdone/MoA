import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';
import 'package:more_app/core/utils/app_env.dart';
import 'package:more_app/data/datasources/remote/social_auth_datasource.dart';

class SocialAuthDataSourceImpl implements SocialAuthDataSource {
  static const String _webGoogleClientId =
      '26025204114-h0lsehum6766elgasajahjtdo79bsrej.apps.googleusercontent.com';

  GoogleSignIn? _googleSignIn;
  Future<String>? _pendingGoogleToken;
  Future<String>? _pendingKakaoToken;

  GoogleSignIn _resolveGoogleSignIn() {
    return _googleSignIn ??= GoogleSignIn(
      clientId: kIsWeb ? _webGoogleClientId : null,
      serverClientId: kIsWeb ? null : AppEnv.optional('GOOGLE_CLIENT_ID'),
      scopes: const ['email', 'openid', 'profile'],
    );
  }

  Future<String> _runSingleFlight({
    required Future<String>? currentRequest,
    required void Function(Future<String>? next) setRequest,
    required Future<String> Function() action,
  }) {
    if (currentRequest != null) {
      return currentRequest;
    }

    final future = action();
    setRequest(future);
    future.whenComplete(() => setRequest(null));
    return future;
  }

  @override
  Future<void> resetPendingLogins() async {
    _pendingGoogleToken = null;
    _pendingKakaoToken = null;
    await logoutGoogle();
    await logoutKakao();
  }

  @override
  Future<String> getKakaoToken() {
    return _runSingleFlight(
      currentRequest: _pendingKakaoToken,
      setRequest: (next) => _pendingKakaoToken = next,
      action: () async {
        if (!kIsWeb && AppEnv.optional('KAKAO_NATIVE_APP_KEY') == null) {
          throw Exception('KAKAO_NATIVE_APP_KEY가 설정되지 않았습니다.');
        }

        try {
          OAuthToken token;
          if (kIsWeb) {
            token = await UserApi.instance.loginWithKakaoAccount();
          } else if (await isKakaoTalkInstalled()) {
            token = await UserApi.instance.loginWithKakaoTalk();
          } else {
            token = await UserApi.instance.loginWithKakaoAccount();
          }

          return token.accessToken;
        } catch (_) {
          await logoutKakao();
          rethrow;
        }
      },
    );
  }

  @override
  Future<String> getGoogleToken() {
    return _runSingleFlight(
      currentRequest: _pendingGoogleToken,
      setRequest: (next) => _pendingGoogleToken = next,
      action: () async {
        if (!kIsWeb && AppEnv.optional('GOOGLE_CLIENT_ID') == null) {
          throw Exception('GOOGLE_CLIENT_ID가 설정되지 않았습니다.');
        }

        final googleSignIn = _resolveGoogleSignIn();

        try {
          await googleSignIn.signOut();

          final googleUser = await googleSignIn.signIn();
          if (googleUser == null) {
            throw Exception('구글 로그인이 취소되었습니다.');
          }

          final googleAuth = await googleUser.authentication;
          final token = googleAuth.idToken ?? googleAuth.accessToken;
          if (token == null) {
            throw Exception('구글 인증 토큰을 가져올 수 없습니다.');
          }

          return token;
        } catch (_) {
          await logoutGoogle();
          rethrow;
        }
      },
    );
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
