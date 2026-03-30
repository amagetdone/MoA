class WebWindowFocusListener {
  const WebWindowFocusListener({
    required this.onFocus,
    required this.onBlur,
  });

  final void Function() onFocus;
  final void Function() onBlur;

  void attach() {}

  void detach() {}
}
