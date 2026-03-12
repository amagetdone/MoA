import 'dart:async';
import 'dart:js_interop';

extension type _CredentialResponse(JSObject _) implements JSObject {
  external String get credential;
}

extension type _IdConfig._(JSObject _) implements JSObject {
  external factory _IdConfig({
    String client_id,
    JSFunction callback,
    bool use_fedcm_for_prompt,
  });
}

extension type _PromptNotification(JSObject _) implements JSObject {
  external bool isSkippedMoment();
  external bool isDismissedMoment();
}

@JS('google.accounts.id.initialize')
external void _gsiInitialize(_IdConfig config);

@JS('google.accounts.id.prompt')
external void _gsiPrompt([JSFunction? momentListener]);

Future<String> getGoogleIdToken(String clientId) {
  final completer = Completer<String>();

  final config = _IdConfig(
    client_id: clientId,
    use_fedcm_for_prompt: false,
    callback: ((JSAny response) {
      final credential =
          _CredentialResponse(response as JSObject).credential;
      if (!completer.isCompleted) {
        completer.complete(credential);
      }
    }).toJS,
  );

  _gsiInitialize(config);

  _gsiPrompt(((JSAny notification) {
    final n = _PromptNotification(notification as JSObject);
    if ((n.isSkippedMoment() || n.isDismissedMoment()) &&
        !completer.isCompleted) {
      completer.completeError(
        Exception('구글 로그인이 취소되었습니다.'),
      );
    }
  }).toJS);

  return completer.future.timeout(
    const Duration(seconds: 60),
    onTimeout: () => throw Exception('구글 로그인 시간 초과'),
  );
}
