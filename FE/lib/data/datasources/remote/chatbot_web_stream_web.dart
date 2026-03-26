import 'dart:async';
import 'dart:convert';
import 'dart:js_interop';

import 'package:more_app/core/errors/exceptions.dart';
import 'package:web/web.dart' as web;

Stream<String>? openChatbotWebStream({
  required Uri uri,
  required Map<String, String> headers,
  required String jsonBody,
}) {
  final controller = StreamController<String>();
  final xhr = web.XMLHttpRequest();
  var emittedLength = 0;
  var settled = false;
  var abortedByCaller = false;

  void emitUnreadResponseText() {
    if (settled || xhr.readyState < web.XMLHttpRequest.LOADING) {
      return;
    }
    if (xhr.status != 0 && xhr.status >= 400) {
      return;
    }

    final responseText = xhr.responseText;
    if (responseText.length <= emittedLength) {
      return;
    }

    final nextChunk = responseText.substring(emittedLength);
    emittedLength = responseText.length;
    if (nextChunk.isNotEmpty) {
      controller.add(nextChunk.replaceAll('\r\n', '\n'));
    }
  }

  void fail(Object error) {
    if (settled) {
      return;
    }
    settled = true;
    controller.addError(error);
    unawaited(controller.close());
  }

  xhr.open('POST', uri.toString());
  headers.forEach((key, value) {
    xhr.setRequestHeader(key, value);
  });

  xhr.onProgress.listen((_) => emitUnreadResponseText());

  xhr.onLoad.first.then((_) {
    if (settled) {
      return;
    }

    emitUnreadResponseText();
    final statusCode = xhr.status;
    if (statusCode >= 200 && statusCode < 300) {
      settled = true;
      unawaited(controller.close());
      return;
    }

    fail(
      ServerException(
        message: _readServerMessage(xhr.responseText),
        statusCode: statusCode,
      ),
    );
  });

  xhr.onError.first.then((_) {
    fail(const NetworkException(message: '챗봇 응답을 불러오지 못했습니다.'));
  });

  web.EventStreamProviders.abortEvent.forTarget(xhr).first.then((_) {
    if (abortedByCaller) {
      return;
    }
    fail(const NetworkException(message: '챗봇 응답이 중단되었습니다.'));
  });

  controller.onCancel = () {
    if (settled) {
      return;
    }
    abortedByCaller = true;
    settled = true;
    if (xhr.readyState < web.XMLHttpRequest.DONE &&
        xhr.readyState > web.XMLHttpRequest.UNSENT) {
      try {
        xhr.abort();
      } catch (_) {}
    }
  };

  xhr.send(jsonBody.toJS);
  return controller.stream;
}

String _readServerMessage(String rawBody) {
  final trimmed = rawBody.trim();
  if (trimmed.isEmpty) {
    return '챗봇 응답에 실패했습니다.';
  }

  try {
    final decoded = jsonDecode(trimmed);
    if (decoded is Map<String, dynamic>) {
      final message = decoded['message'];
      if (message is String && message.trim().isNotEmpty) {
        return message.trim();
      }
    }
  } catch (_) {}

  return trimmed;
}
