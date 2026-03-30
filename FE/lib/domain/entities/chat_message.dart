enum ChatRole { user, assistant }

class ChatMessage {
  final String id;
  final ChatRole role;
  final String content;
  final DateTime createdAt;
  final bool isStreaming;

  const ChatMessage({
    required this.id,
    required this.role,
    required this.content,
    required this.createdAt,
    this.isStreaming = false,
  });

  ChatMessage copyWith({
    String? content,
    bool? isStreaming,
  }) {
    return ChatMessage(
      id: id,
      role: role,
      content: content ?? this.content,
      createdAt: createdAt,
      isStreaming: isStreaming ?? this.isStreaming,
    );
  }
}

class ChatConversation {
  final String conversationId;
  final DateTime createdAt;
  final DateTime lastActivityAt;
  final DateTime expiresAt;
  final List<ChatMessage> messages;

  const ChatConversation({
    required this.conversationId,
    required this.createdAt,
    required this.lastActivityAt,
    required this.expiresAt,
    required this.messages,
  });
}
