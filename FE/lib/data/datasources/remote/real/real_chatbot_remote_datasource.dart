import 'dart:async';
import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:more_app/domain/repositories/chatbot_repository.dart';

class RealChatbotRemoteDataSource {
  RealChatbotRemoteDataSource({required Dio dio}) : _dio = dio;

  final Dio _dio;

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

    final response = await _dio.post<ResponseBody>(
      'v1/chatbot/messages/stream',
      data: body,
      options: Options(
        responseType: ResponseType.stream,
        headers: {'Accept': 'text/event-stream'},
        receiveTimeout: const Duration(minutes: 2),
      ),
    );

    final stream = response.data!.stream;
    String buffer = '';

    await for (final chunk in stream) {
      buffer += utf8.decode(chunk, allowMalformed: true).replaceAll('\r\n', '\n');

      // SSE 프레임 단위 처리: 빈 줄 기준으로 분리
      while (buffer.contains('\n\n')) {
        final frameEnd = buffer.indexOf('\n\n');
        final frame = buffer.substring(0, frameEnd);
        buffer = buffer.substring(frameEnd + 2);

        final event = _parseFrame(frame);
        if (event != null) yield event;
      }
    }

    // 버퍼 잔여 처리
    if (buffer.trim().isNotEmpty) {
      final event = _parseFrame(buffer.trim());
      if (event != null) yield event;
    }
  }

  Future<Map<String, dynamic>> fetchConversation(String conversationId) async {
    final response = await _dio.get<Map<String, dynamic>>(
      'v1/chatbot/conversations/$conversationId',
    );
    return response.data ?? <String, dynamic>{};
  }

  ChatStreamEvent? _parseFrame(String frame) {
    String? eventType;
    final dataLines = <String>[];

    for (final rawLine in frame.split('\n')) {
      final line = rawLine.trimRight();
      if (line.startsWith('event: ')) {
        eventType = line.substring(7).trim();
      } else if (line.startsWith('data: ')) {
        dataLines.add(line.substring(6).trimLeft());
      }
    }

    if (eventType == null || dataLines.isEmpty) return null;

    try {
      final rawPayload = jsonDecode(dataLines.join('\n'));
      if (rawPayload is! Map) {
        return null;
      }

      final payload = Map<String, dynamic>.from(rawPayload);
      final data = payload['data'] is Map
          ? Map<String, dynamic>.from(payload['data'] as Map)
          : payload;

      switch (eventType) {
        case 'token':
          return ChatStreamToken(
            _readString(data, const ['text', 'token', 'content']) ?? '',
          );
        case 'done':
          return ChatStreamDone(
            _readString(data, const ['conversation_id', 'conversationId', 'id']) ?? '',
          );
        case 'status':
          return ChatStreamStatus(
            _readString(data, const ['message', 'status', 'text']) ?? '',
          );
        default:
          return null;
      }
    } catch (_) {
      return null;
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
}
