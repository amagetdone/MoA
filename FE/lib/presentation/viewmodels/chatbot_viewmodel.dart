import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:more_app/core/errors/exceptions.dart';
import 'package:more_app/domain/entities/chat_message.dart';
import 'package:more_app/domain/repositories/chatbot_repository.dart';
import 'package:more_app/presentation/providers/app_data_providers.dart';

class ChatbotState {
  final List<ChatMessage> messages;
  final String? conversationId;
  final bool isLoading;
  final bool hasInitialized;
  final String? errorMessage;
  final String? infoMessage;
  final bool isExpired;

  const ChatbotState({
    this.messages = const [],
    this.conversationId,
    this.isLoading = false,
    this.hasInitialized = false,
    this.errorMessage,
    this.infoMessage,
    this.isExpired = false,
  });

  ChatbotState copyWith({
    List<ChatMessage>? messages,
    String? conversationId,
    bool clearConversationId = false,
    bool? isLoading,
    bool? hasInitialized,
    String? errorMessage,
    bool clearError = false,
    String? infoMessage,
    bool clearInfo = false,
    bool? isExpired,
  }) {
    return ChatbotState(
      messages: messages ?? this.messages,
      conversationId: clearConversationId ? null : conversationId ?? this.conversationId,
      isLoading: isLoading ?? this.isLoading,
      hasInitialized: hasInitialized ?? this.hasInitialized,
      errorMessage: clearError ? null : errorMessage ?? this.errorMessage,
      infoMessage: clearInfo ? null : infoMessage ?? this.infoMessage,
      isExpired: isExpired ?? this.isExpired,
    );
  }
}

class ChatbotViewModel extends StateNotifier<ChatbotState> {
  ChatbotViewModel(this._repository) : super(const ChatbotState()) {
    unawaited(restoreLatestConversation());
  }

  final ChatbotRepository _repository;
  StreamSubscription<ChatStreamEvent>? _streamSubscription;
  static const _streamingMessageId = 'assistant_streaming';

  @override
  void dispose() {
    _streamSubscription?.cancel();
    super.dispose();
  }

  Future<void> restoreLatestConversation() async {
    state = state.copyWith(
      isLoading: true,
      hasInitialized: state.hasInitialized,
      clearError: true,
      clearInfo: true,
      isExpired: false,
    );

    try {
      final latestConversationId = await _repository.fetchLatestConversationId();
      if (!mounted) {
        return;
      }

      if (latestConversationId == null || latestConversationId.isEmpty) {
        state = const ChatbotState(
          hasInitialized: true,
          infoMessage: '이어갈 최근 대화가 없어요. 새 대화를 시작해보세요.',
        );
        return;
      }

      final conv = await _repository.fetchConversation(latestConversationId);
      if (!mounted) {
        return;
      }

      state = state.copyWith(
        isLoading: false,
        hasInitialized: true,
        conversationId: conv.conversationId,
        messages: conv.messages,
        clearError: true,
        clearInfo: true,
        isExpired: false,
      );
    } catch (error) {
      if (!mounted) {
        return;
      }
      _handleRestoreError(error);
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
      id: _streamingMessageId,
      role: ChatRole.assistant,
      content: '',
      createdAt: DateTime.now(),
      isStreaming: true,
    );

    state = state.copyWith(
      messages: [...state.messages, userMsg, streamingMsg],
      isLoading: true,
      hasInitialized: true,
      clearError: true,
      clearInfo: true,
      isExpired: false,
    );

    String accumulated = '';

    await _streamSubscription?.cancel();
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
            _handleSendError(e, accumulated);
          },
          cancelOnError: true,
        );
  }

  void _updateStreamingMessage(String content) {
    final msgs = List<ChatMessage>.from(state.messages);
    final idx = msgs.indexWhere((m) => m.id == _streamingMessageId);
    if (idx != -1) {
      msgs[idx] = msgs[idx].copyWith(content: content);
      state = state.copyWith(messages: msgs);
    }
  }

  void _finalizeMessage(String content, String conversationId) {
    final msgs = List<ChatMessage>.from(state.messages);
    final idx = msgs.indexWhere((m) => m.id == _streamingMessageId);
    if (idx != -1) {
      msgs[idx] = ChatMessage(
        id: 'assistant_${DateTime.now().millisecondsSinceEpoch}',
        role: ChatRole.assistant,
        content: content.isEmpty ? '응답을 받지 못했어요.' : content,
        createdAt: DateTime.now(),
        isStreaming: false,
      );
    }
    state = state.copyWith(
      messages: msgs,
      conversationId: conversationId.trim().isEmpty ? state.conversationId : conversationId,
      isLoading: false,
      hasInitialized: true,
      clearError: true,
      clearInfo: true,
      isExpired: false,
    );
  }

  void startNewConversation() {
    unawaited(_streamSubscription?.cancel());
    state = const ChatbotState(hasInitialized: true);
  }

  void _handleRestoreError(Object error) {
    final statusCode = _extractStatusCode(error);

    switch (statusCode) {
      case 404:
        state = const ChatbotState(
          hasInitialized: true,
          infoMessage: '이어갈 최근 대화가 없어요. 새 대화를 시작해보세요.',
        );
        return;
      case 410:
        state = const ChatbotState(
          hasInitialized: true,
          infoMessage: '이전 대화가 만료되어 새로 시작할게요.',
          isExpired: true,
        );
        return;
      case 503:
        state = const ChatbotState(
          hasInitialized: true,
          errorMessage: '챗봇 준비 중이거나 설정에 문제가 있어요. 잠시 후 다시 시도해주세요.',
        );
        return;
      case 500:
        state = const ChatbotState(
          hasInitialized: true,
          errorMessage: '일시적 오류가 발생했어요. 잠시 후 다시 시도해주세요.',
        );
        return;
      default:
        state = const ChatbotState(
          hasInitialized: true,
          errorMessage: '챗봇을 불러오지 못했어요. 다시 시도해주세요.',
        );
    }
  }

  void _handleSendError(Object error, String accumulated) {
    final statusCode = _extractStatusCode(error);
    final messages = List<ChatMessage>.from(state.messages);
    final streamingIndex = messages.indexWhere((message) => message.id == _streamingMessageId);
    if (streamingIndex != -1) {
      if (accumulated.isEmpty) {
        messages.removeAt(streamingIndex);
      } else {
        messages[streamingIndex] = messages[streamingIndex].copyWith(
          content: accumulated,
          isStreaming: false,
        );
      }
    }

    String? errorMessage;
    String? infoMessage;
    var clearConversationId = false;
    var isExpired = false;

    switch (statusCode) {
      case 404:
        infoMessage = '이어갈 대화를 찾지 못했어요. 새 대화를 시작할게요.';
        clearConversationId = true;
        break;
      case 410:
        infoMessage = '이전 대화가 만료되어 새 대화를 시작할게요. 같은 질문을 다시 보내주세요.';
        clearConversationId = true;
        isExpired = true;
        break;
      case 503:
        errorMessage = '챗봇 준비 중이거나 설정에 문제가 있어요. 잠시 후 다시 시도해주세요.';
        break;
      case 500:
        errorMessage = '일시적 오류가 발생했어요. 잠시 후 다시 시도해주세요.';
        break;
      default:
        errorMessage = accumulated.isEmpty ? '챗봇 응답에 실패했습니다.' : null;
    }

    state = state.copyWith(
      messages: messages,
      isLoading: false,
      hasInitialized: true,
      errorMessage: errorMessage,
      clearError: errorMessage == null,
      infoMessage: infoMessage,
      clearInfo: infoMessage == null,
      clearConversationId: clearConversationId,
      isExpired: isExpired,
    );
  }

  int? _extractStatusCode(Object error) {
    if (error is DioException) {
      return error.response?.statusCode;
    }
    if (error is ServerException) {
      return error.statusCode;
    }
    return null;
  }
}

final chatbotViewModelProvider =
    StateNotifierProvider.autoDispose<ChatbotViewModel, ChatbotState>((ref) {
  return ChatbotViewModel(ref.read(chatbotRepositoryProvider));
});
