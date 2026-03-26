import 'package:more_app/data/datasources/remote/real/real_chatbot_remote_datasource.dart';
import 'package:more_app/domain/entities/chat_message.dart';
import 'package:more_app/domain/repositories/chatbot_repository.dart';

class ChatbotRepositoryImpl implements ChatbotRepository {
  ChatbotRepositoryImpl({required RealChatbotRemoteDataSource dataSource})
      : _dataSource = dataSource;

  final RealChatbotRemoteDataSource _dataSource;

  @override
  Future<({String conversationId, String answer})> sendMessage({
    required String message,
    String? conversationId,
  }) {
    return _dataSource.sendMessage(
      message: message,
      conversationId: conversationId,
    );
  }

  @override
  Stream<ChatStreamEvent> sendMessageStream({
    required String message,
    String? conversationId,
  }) {
    return _dataSource.sendMessageStream(
      message: message,
      conversationId: conversationId,
    );
  }

  @override
  Future<ChatConversation> fetchConversation(String conversationId) async {
    final data = await _dataSource.fetchConversation(conversationId);
    final rawMessages = data['messages'] as List<dynamic>? ?? [];

    return ChatConversation(
      conversationId: data['conversation_id'] as String,
      createdAt: DateTime.parse(data['created_at'] as String),
      lastActivityAt: DateTime.parse(data['last_activity_at'] as String),
      expiresAt: DateTime.parse(data['expires_at'] as String),
      messages: rawMessages.map((m) {
        final map = m as Map<String, dynamic>;
        return ChatMessage(
          id: '${map['role']}_${map['created_at']}',
          role: map['role'] == 'user' ? ChatRole.user : ChatRole.assistant,
          content: map['content'] as String,
          createdAt: DateTime.parse(map['created_at'] as String),
        );
      }).toList(),
    );
  }
}
