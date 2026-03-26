import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:more_app/domain/entities/chat_message.dart';
import 'package:more_app/domain/repositories/chatbot_repository.dart';
import 'package:more_app/presentation/providers/app_data_providers.dart';

class ChatbotState {
  final List<ChatMessage> messages;
  final String? conversationId;
  final bool isLoading;
  final String? errorMessage;
  final bool isExpired;

  const ChatbotState({
    this.messages = const [],
    this.conversationId,
    this.isLoading = false,
    this.errorMessage,
    this.isExpired = false,
  });

  ChatbotState copyWith({
    List<ChatMessage>? messages,
    String? conversationId,
    bool clearConversationId = false,
    bool? isLoading,
    String? errorMessage,
    bool clearError = false,
    bool? isExpired,
  }) {
    return ChatbotState(
      messages: messages ?? this.messages,
      conversationId: clearConversationId ? null : conversationId ?? this.conversationId,
      isLoading: isLoading ?? this.isLoading,
      errorMessage: clearError ? null : errorMessage ?? this.errorMessage,
      isExpired: isExpired ?? this.isExpired,
    );
  }
}

class ChatbotViewModel extends StateNotifier<ChatbotState> {
  ChatbotViewModel(this._repository, String? savedConversationId)
      : super(const ChatbotState()) {
    if (savedConversationId != null) {
      restoreConversation(savedConversationId);
    }
  }

  final ChatbotRepository _repository;
  StreamSubscription<ChatStreamEvent>? _streamSubscription;

  @override
  void dispose() {
    _streamSubscription?.cancel();
    super.dispose();
  }

  Future<void> restoreConversation(String conversationId) async {
    state = state.copyWith(isLoading: true, clearError: true);
    try {
      final conv = await _repository.fetchConversation(conversationId);
      state = state.copyWith(
        isLoading: false,
        conversationId: conv.conversationId,
        messages: conv.messages,
      );
    } on Exception catch (e) {
      final msg = e.toString();
      if (msg.contains('410')) {
        state = state.copyWith(
          isLoading: false,
          isExpired: true,
          clearConversationId: true,
        );
      } else {
        // 조회 실패 시 빈 채팅으로 시작
        state = state.copyWith(isLoading: false, clearError: true);
      }
    }
  }

  Future<void> sendMessage(String text) async {
    if (text.trim().isEmpty || state.isLoading) return;

    final userMsg = ChatMessage(
      id: 'user_${DateTime.now().millisecondsSinceEpoch}',
      role: ChatRole.user,
      content: text.trim(),
      createdAt: DateTime.now(),
    );

    // 임시 assistant 버블 (스트리밍 중)
    final streamingMsg = ChatMessage(
      id: 'assistant_streaming',
      role: ChatRole.assistant,
      content: '',
      createdAt: DateTime.now(),
      isStreaming: true,
    );

    state = state.copyWith(
      messages: [...state.messages, userMsg, streamingMsg],
      isLoading: true,
      clearError: true,
      isExpired: false,
    );

    String accumulated = '';

    _streamSubscription?.cancel();
    _streamSubscription = _repository
        .sendMessageStream(
          message: text.trim(),
          conversationId: state.conversationId,
        )
        .listen(
          (event) {
            if (!mounted) return;
            switch (event) {
              case ChatStreamToken(:final text):
                accumulated += text;
                _updateStreamingMessage(accumulated);
              case ChatStreamDone(:final conversationId):
                _finalizeMessage(accumulated, conversationId);
              case ChatStreamStatus():
                // 상태 이벤트는 로딩 표시로만 처리 (무시)
                break;
            }
          },
          onError: (Object e) {
            if (!mounted) return;
            final msgs = List<ChatMessage>.from(state.messages);
            // 스트리밍 버블을 지금까지 받은 내용으로 확정
            final idx = msgs.indexWhere((m) => m.id == 'assistant_streaming');
            if (idx != -1) {
              msgs[idx] = msgs[idx].copyWith(
                content: accumulated.isEmpty ? '응답이 중간에 끊어졌습니다.' : accumulated,
                isStreaming: false,
              );
            }
            state = state.copyWith(
              messages: msgs,
              isLoading: false,
              errorMessage: accumulated.isEmpty ? '챗봇 응답에 실패했습니다.' : null,
            );
          },
          cancelOnError: true,
        );
  }

  void _updateStreamingMessage(String content) {
    final msgs = List<ChatMessage>.from(state.messages);
    final idx = msgs.indexWhere((m) => m.id == 'assistant_streaming');
    if (idx != -1) {
      msgs[idx] = msgs[idx].copyWith(content: content);
      state = state.copyWith(messages: msgs);
    }
  }

  void _finalizeMessage(String content, String conversationId) {
    final msgs = List<ChatMessage>.from(state.messages);
    final idx = msgs.indexWhere((m) => m.id == 'assistant_streaming');
    if (idx != -1) {
      msgs[idx] = ChatMessage(
        id: 'assistant_${DateTime.now().millisecondsSinceEpoch}',
        role: ChatRole.assistant,
        content: content,
        createdAt: DateTime.now(),
        isStreaming: false,
      );
    }
    state = state.copyWith(
      messages: msgs,
      conversationId: conversationId,
      isLoading: false,
      clearError: true,
    );
  }

  void startNewConversation() {
    _streamSubscription?.cancel();
    state = const ChatbotState();
  }
}

final chatbotViewModelProvider =
    StateNotifierProvider.autoDispose<ChatbotViewModel, ChatbotState>((ref) {
  return ChatbotViewModel(ref.read(chatbotRepositoryProvider), null);
});
