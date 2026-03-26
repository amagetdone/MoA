import 'dart:async';
import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:more_app/domain/repositories/chatbot_repository.dart';

class RealChatbotRemoteDataSource {
  RealChatbotRemoteDataSource({required Dio dio}) : _dio = dio;

  final Dio _dio;

  Future<({String conversationId, String answer})> sendMessage({
    required String message,
    String? conversationId,
  }) async {
    final body = <String, dynamic>{'message': message};
    if (conversationId != null) body['conversation_id'] = conversationId;

    final response = await _dio.post<Map<String, dynamic>>(
      'api/chatbot/messages',
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
      'api/chatbot/messages/stream',
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
      buffer += utf8.decode(chunk, allowMalformed: true);

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
      'api/chatbot/conversations/$conversationId',
    );
    return response.data!;
  }

  ChatStreamEvent? _parseFrame(String frame) {
    String? eventType;
    String? dataLine;

    for (final line in frame.split('\n')) {
      if (line.startsWith('event: ')) {
        eventType = line.substring(7).trim();
      } else if (line.startsWith('data: ')) {
        dataLine = line.substring(6).trim();
      }
    }

    if (eventType == null || dataLine == null) return null;

    try {
      final json = jsonDecode(dataLine) as Map<String, dynamic>;
      switch (eventType) {
        case 'token':
          return ChatStreamToken(json['text'] as String? ?? '');
        case 'done':
          return ChatStreamDone(json['conversation_id'] as String? ?? '');
        case 'status':
          return ChatStreamStatus(json['message'] as String? ?? '');
        default:
          return null;
      }
    } catch (_) {
      return null;
    }
  }
}
