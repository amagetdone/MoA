// ignore_for_file: non_constant_identifier_names

import 'dart:async';
import 'dart:js_interop';

import 'package:web/web.dart' as web;

extension type _CredentialResponse(JSObject _) implements JSObject {
  external String get credential;
}

extension type _IdConfig._(JSObject _) implements JSObject {
  external factory _IdConfig({
    String client_id,
    JSFunction callback,
  });
}

extension type _ButtonConfig._(JSObject _) implements JSObject {
  external factory _ButtonConfig({
    String type,
    String theme,
    String size,
    String text,
    int width,
    JSFunction click_listener,
  });
}

@JS('google.accounts.id.initialize')
external void _gsiInitialize(_IdConfig config);

@JS('google.accounts.id.cancel')
external void _gsiCancel();

@JS('google.accounts.id.renderButton')
external void _gsiRenderButton(web.Element element, _ButtonConfig config);

Future<String> getGoogleIdToken(String clientId) {
  final completer = Completer<String>();
  Timer? timeoutTimer;
  Timer? cancelTimer;
  late final web.HTMLDivElement container;
  bool lostFocusDuringLogin = false;

  void completeError(String message) {
    if (!completer.isCompleted) {
      completer.completeError(Exception(message));
    }
  }

  final onFocus = (() {
    if (!lostFocusDuringLogin || completer.isCompleted) {
      return;
    }

    cancelTimer?.cancel();
    cancelTimer = Timer(const Duration(milliseconds: 900), () {
      completeError('구글 로그인이 취소되었습니다.');
    });
  }).toJS;

  final onBlur = (() {
    lostFocusDuringLogin = true;
  }).toJS;

  final onVisibilityChange = (() {
    if (web.document.visibilityState == 'hidden') {
      lostFocusDuringLogin = true;
      return;
    }

    if (!lostFocusDuringLogin || completer.isCompleted) {
      return;
    }

    cancelTimer?.cancel();
    cancelTimer = Timer(const Duration(milliseconds: 900), () {
      completeError('구글 로그인이 취소되었습니다.');
    });
  }).toJS;

  void cleanup() {
    timeoutTimer?.cancel();
    cancelTimer?.cancel();
    web.window.removeEventListener('focus', onFocus);
    web.window.removeEventListener('blur', onBlur);
    web.document.removeEventListener('visibilitychange', onVisibilityChange);

    if (container.isConnected) {
      container.remove();
    }

    try {
      _gsiCancel();
    } catch (_) {}
  }

  final config = _IdConfig(
    client_id: clientId,
    callback: ((JSAny response) {
      cancelTimer?.cancel();
      final credential = _CredentialResponse(response as JSObject).credential;
      if (credential.isEmpty) {
        completeError('구글 인증 토큰을 가져올 수 없습니다.');
        return;
      }

      if (!completer.isCompleted) {
        completer.complete(credential);
      }
    }).toJS,
  );

  _gsiInitialize(config);
  web.window.addEventListener('focus', onFocus);
  web.window.addEventListener('blur', onBlur);
  web.document.addEventListener('visibilitychange', onVisibilityChange);

  container = web.document.createElement('div') as web.HTMLDivElement;
  container.style.setProperty('position', 'fixed');
  container.style.setProperty('top', '-9999px');
  container.style.setProperty('left', '-9999px');
  web.document.body!.appendChild(container);

  _gsiRenderButton(
    container,
    _ButtonConfig(
      type: 'standard',
      theme: 'outline',
      size: 'large',
      text: 'signin_with',
      width: 300,
      click_listener: (() {}).toJS,
    ),
  );

  Future.delayed(const Duration(milliseconds: 250), () {
    if (completer.isCompleted) {
      return;
    }

    final button = container.querySelector('[role="button"]');
    if (button == null) {
      completeError('구글 로그인 버튼을 찾을 수 없습니다.');
      return;
    }

    (button as web.HTMLElement).click();
  });

  timeoutTimer = Timer(
    const Duration(seconds: 12),
    () => completeError('구글 로그인이 완료되지 않았습니다. 다시 시도해주세요.'),
  );

  completer.future.whenComplete(cleanup);
  return completer.future;
}
