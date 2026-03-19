import 'dart:js_interop';

import 'package:web/web.dart' as web;

class WebWindowFocusListener {
  WebWindowFocusListener({
    required void Function() onFocus,
    required void Function() onBlur,
  })  : _onFocus = onFocus,
        _onBlur = onBlur;

  final void Function() _onFocus;
  final void Function() _onBlur;
  JSFunction? _focusListener;
  JSFunction? _blurListener;

  void attach() {
    _focusListener ??= (() => _onFocus()).toJS;
    _blurListener ??= (() => _onBlur()).toJS;

    web.window.addEventListener('focus', _focusListener);
    web.window.addEventListener('blur', _blurListener);
  }

  void detach() {
    if (_focusListener != null) {
      web.window.removeEventListener('focus', _focusListener);
    }
    if (_blurListener != null) {
      web.window.removeEventListener('blur', _blurListener);
    }
  }
}
