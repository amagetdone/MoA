import 'package:more_app/data/datasources/remote/real/real_chatbot_remote_datasource.dart';
import 'package:more_app/domain/entities/chat_message.dart';
import 'package:more_app/domain/repositories/chatbot_repository.dart';

class ChatbotRepositoryImpl implements ChatbotRepository {
  ChatbotRepositoryImpl({required RealChatbotRemoteDataSource dataSource})
      : _dataSource = dataSource;

  final RealChatbotRemoteDataSource _dataSource;

  @override
  Future<String?> fetchLatestConversationId() async {
    final data = await _dataSource.fetchLatestConversation();
    return _readConversationId(data);
  }

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
    final conversationMeta = data['conversation'] is Map
        ? Map<String, dynamic>.from(data['conversation'] as Map)
        : data;
    final rawMessages =
        (data['messages'] as List<dynamic>?) ??
        (conversationMeta['messages'] as List<dynamic>?) ??
        const [];
    final resolvedConversationId =
        _readConversationId(conversationMeta) ?? conversationId;
    final messages = rawMessages
        .map((rawMessage) => _parseMessage(rawMessage as Map<dynamic, dynamic>))
        .toList()
      ..sort((left, right) => left.createdAt.compareTo(right.createdAt));

    return ChatConversation(
      conversationId: resolvedConversationId,
      createdAt: _readDateTime(conversationMeta, 'created_at'),
      lastActivityAt: _readDateTime(conversationMeta, 'last_activity_at'),
      expiresAt: _readDateTime(conversationMeta, 'expires_at'),
      messages: messages,
    );
  }

  ChatMessage _parseMessage(Map<dynamic, dynamic> rawMessage) {
    final message = Map<String, dynamic>.from(rawMessage);
    final createdAt = _readDateTime(message, 'created_at');
    final role = (message['role'] as String?)?.trim().toLowerCase();

    return ChatMessage(
      id: '${role ?? 'assistant'}_${message['created_at'] ?? createdAt.toIso8601String()}',
      role: role == 'user' ? ChatRole.user : ChatRole.assistant,
      content: (message['content'] as String? ?? '').trim(),
      createdAt: createdAt,
    );
  }

  String? _readConversationId(Map<String, dynamic> data) {
    final direct = _readString(data, const ['conversation_id', 'conversationId', 'id']);
    if (direct != null) {
      return direct;
    }

    final nestedConversation = data['conversation'];
    if (nestedConversation is Map) {
      return _readConversationId(Map<String, dynamic>.from(nestedConversation));
    }

    return null;
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

  DateTime _readDateTime(Map<String, dynamic> data, String key) {
    final rawValue = data[key];
    if (rawValue is String) {
      final parsed = DateTime.tryParse(rawValue);
      if (parsed != null) {
        return parsed;
      }
    }
    return DateTime.now();
  }
}
