import 'package:more_app/domain/entities/chat_message.dart';

abstract class ChatbotRepository {
  /// 일반 응답 (non-streaming)
  Future<({String conversationId, String answer})> sendMessage({
    required String message,
    String? conversationId,
  });

  /// SSE 스트리밍 응답
  Stream<ChatStreamEvent> sendMessageStream({
    required String message,
    String? conversationId,
  });

  /// 대화 조회 (재진입 복원)
  Future<ChatConversation> fetchConversation(String conversationId);
}

sealed class ChatStreamEvent {
  const ChatStreamEvent();
}

class ChatStreamToken extends ChatStreamEvent {
  final String text;
  const ChatStreamToken(this.text);
}

class ChatStreamDone extends ChatStreamEvent {
  final String conversationId;
  const ChatStreamDone(this.conversationId);
}

class ChatStreamStatus extends ChatStreamEvent {
  final String message;
  const ChatStreamStatus(this.message);
}
