import 'dart:async';
import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:more_app/data/datasources/remote/chatbot_web_stream.dart';
import 'package:more_app/domain/repositories/chatbot_repository.dart';

class RealChatbotRemoteDataSource {
  RealChatbotRemoteDataSource({required Dio dio}) : _dio = dio;

  final Dio _dio;
  final _storage = const FlutterSecureStorage();

  Future<Map<String, dynamic>> fetchLatestConversation() async {
    final response = await _dio.get<Map<String, dynamic>>(
      'v1/chatbot/conversations/latest',
    );
    return response.data ?? <String, dynamic>{};
  }

  Future<({String conversationId, String answer})> sendMessage({
    required String message,
    String? conversationId,
  }) async {
    final body = <String, dynamic>{'message': message};
    if (conversationId != null) body['conversation_id'] = conversationId;

    final response = await _dio.post<Map<String, dynamic>>(
      'v1/chatbot/messages',
      data: body,
    );

    final data = response.data!;
    return (
      conversationId: data['conversation_id'] as String,
      answer: data['answer'] as String,
    );
  }

  Stream<ChatStreamEvent> sendMessageStream({
    required String message,
    String? conversationId,
  }) async* {
    final body = <String, dynamic>{'message': message};
    if (conversationId != null) body['conversation_id'] = conversationId;

    final webStream = await _openWebStream(body);
    if (webStream != null) {
      yield* _parseResponseChunks(webStream);
      return;
    }

    final response = await _dio.post<ResponseBody>(
      'v1/chatbot/messages/stream',
      data: body,
      options: Options(
        responseType: ResponseType.stream,
        headers: {'Accept': 'text/event-stream'},
        receiveTimeout: const Duration(minutes: 2),
      ),
    );

    final stream = response.data!.stream.map(
      (chunk) =>
          utf8.decode(chunk, allowMalformed: true).replaceAll('\r\n', '\n'),
    );
    yield* _parseResponseChunks(stream);
  }

  Future<Stream<String>?> _openWebStream(Map<String, dynamic> body) async {
    final headers = <String, String>{};
    for (final entry in _dio.options.headers.entries) {
      final value = entry.value;
      if (value == null) {
        continue;
      }
      headers[entry.key] = value is Iterable
          ? value.join(', ')
          : value.toString();
    }

    final accessToken = await _storage.read(key: 'access_token');
    if (accessToken != null && accessToken.isNotEmpty) {
      headers['Authorization'] = 'Bearer $accessToken';
    }
    headers['Accept'] = 'text/event-stream';
    headers['Content-Type'] = 'application/json';

    final uri = Uri.parse(
      _dio.options.baseUrl,
    ).resolve('v1/chatbot/messages/stream');
    return openChatbotWebStream(
      uri: uri,
      headers: headers,
      jsonBody: jsonEncode(body),
    );
  }

  Stream<ChatStreamEvent> _parseResponseChunks(Stream<String> stream) async* {
    String buffer = '';

    await for (final chunk in stream) {
      buffer += chunk;

      // SSE 프레임 단위 처리: 빈 줄 기준으로 분리
      while (buffer.contains('\n\n')) {
        final frameEnd = buffer.indexOf('\n\n');
        final frame = buffer.substring(0, frameEnd);
        buffer = buffer.substring(frameEnd + 2);

        for (final event in _parseFrameEvents(frame)) {
          yield event;
        }
      }
    }

    // 버퍼 잔여 처리
    if (buffer.trim().isNotEmpty) {
      for (final event in _parseFrameEvents(buffer)) {
        yield event;
      }
    }
  }

  Future<Map<String, dynamic>> fetchConversation(String conversationId) async {
    final response = await _dio.get<Map<String, dynamic>>(
      'v1/chatbot/conversations/$conversationId',
    );
    return response.data ?? <String, dynamic>{};
  }

  List<ChatStreamEvent> _parseFrameEvents(String frame) {
    String? eventType;
    final dataLines = <String>[];

    for (final rawLine in frame.split('\n')) {
      final line = rawLine.trimRight();
      if (line.startsWith('event: ')) {
        eventType = line.substring(7).trim();
      } else if (line.startsWith('data: ')) {
        dataLines.add(line.substring(6));
      }
    }

    if (dataLines.isEmpty) {
      return const [];
    }

    final joinedData = dataLines.join('\n');

    try {
      final rawPayload = jsonDecode(joinedData);
      if (rawPayload is! Map) {
        return const [];
      }

      final payload = Map<String, dynamic>.from(rawPayload);
      final nativeBuffer = payload['nativeBuffer'];
      if (nativeBuffer is String && nativeBuffer.isNotEmpty) {
        final decoded = utf8.decode(
          base64Decode(nativeBuffer),
          allowMalformed: true,
        );
        final nestedEvents = <ChatStreamEvent>[];
        for (final nestedFrame in decoded.split('\n\n')) {
          if (nestedFrame.trim().isEmpty) {
            continue;
          }
          nestedEvents.addAll(_parseFrameEvents(nestedFrame));
        }
        return nestedEvents;
      }

      eventType ??= _readString(payload, const ['event', 'type']);
      final data = payload['data'] is Map
          ? Map<String, dynamic>.from(payload['data'] as Map)
          : payload;

      switch (eventType) {
        case 'token':
          return [
            ChatStreamToken(
              _readTokenText(data, const ['text', 'token', 'content']) ?? '',
            ),
          ];
        case 'done':
          return [
            ChatStreamDone(
              _readString(data, const [
                    'conversation_id',
                    'conversationId',
                    'id',
                  ]) ??
                  '',
            ),
          ];
        case 'status':
          return [
            ChatStreamStatus(
              _readString(data, const ['message', 'status', 'text']) ?? '',
            ),
          ];
        default:
          return const [];
      }
    } catch (_) {
      return const [];
    }
  }

  String? _readString(Map<String, dynamic> data, List<String> keys) {
    for (final key in keys) {
      final value = data[key];
      if (value is String && value.trim().isNotEmpty) {
        return value.trim();
      }
    }
    return null;
  }

  String? _readTokenText(Map<String, dynamic> data, List<String> keys) {
    for (final key in keys) {
      final value = data[key];
      if (value is String) {
        return value;
      }
    }
    return null;
  }
}
