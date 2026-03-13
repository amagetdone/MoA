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

@JS('google.accounts.id.renderButton')
external void _gsiRenderButton(web.Element element, _ButtonConfig config);

Future<String> getGoogleIdToken(String clientId) {
  final completer = Completer<String>();

  final config = _IdConfig(
    client_id: clientId,
    callback: ((JSAny response) {
      final credential =
          _CredentialResponse(response as JSObject).credential;
      if (!completer.isCompleted) {
        completer.complete(credential);
      }
    }).toJS,
  );

  _gsiInitialize(config);

  // 숨겨진 div를 만들어서 Google 버튼을 렌더링 후 자동 클릭
  final container = web.document.createElement('div') as web.HTMLDivElement;
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
      click_listener: (() {
        // 클릭 이벤트 감지용 (아무 동작 안 해도 됨)
      }).toJS,
    ),
  );

  // 렌더링 완료 후 버튼 자동 클릭
  Future.delayed(const Duration(milliseconds: 300), () {
    final button = container.querySelector('[role="button"]')
        ?? container.querySelector('div[style]');
    if (button != null) {
      (button as web.HTMLElement).click();
    } else if (!completer.isCompleted) {
      completer.completeError(Exception('구글 로그인 버튼을 찾을 수 없습니다.'));
    }
  });

  // 완료 후 정리
  completer.future.whenComplete(() {
    container.remove();
  });

  return completer.future.timeout(
    const Duration(seconds: 60),
    onTimeout: () {
      container.remove();
      throw Exception('구글 로그인 시간 초과');
    },
  );
}
