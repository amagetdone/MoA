import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

enum AiChatRole { assistant, user }

class AiChatMessage {
  const AiChatMessage({
    required this.id,
    required this.role,
    required this.text,
    required this.createdAt,
  });

  final String id;
  final AiChatRole role;
  final String text;
  final DateTime createdAt;
}

class AiChatState {
  const AiChatState({
    this.messages = const [],
    this.isSending = false,
  });

  final List<AiChatMessage> messages;
  final bool isSending;

  AiChatState copyWith({
    List<AiChatMessage>? messages,
    bool? isSending,
  }) {
    return AiChatState(
      messages: messages ?? this.messages,
      isSending: isSending ?? this.isSending,
    );
  }
}

class AiChatViewModel extends StateNotifier<AiChatState> {
  AiChatViewModel()
      : super(
          AiChatState(
            messages: [
              AiChatMessage(
                id: _messageId(),
                role: AiChatRole.assistant,
                text: '안녕하세요. 모아 AI 코치예요.\n소비, 저축, 목표에 대해 편하게 물어보세요.',
                createdAt: DateTime.now(),
              ),
            ],
          ),
        );

  Future<void> sendMessage(String text) async {
    final trimmed = text.trim();
    if (trimmed.isEmpty || state.isSending) {
      return;
    }

    final userMessage = AiChatMessage(
      id: _messageId(),
      role: AiChatRole.user,
      text: trimmed,
      createdAt: DateTime.now(),
    );

    state = state.copyWith(
      messages: [...state.messages, userMessage],
      isSending: true,
    );

    await Future<void>.delayed(const Duration(milliseconds: 700));

    final reply = AiChatMessage(
      id: _messageId(),
      role: AiChatRole.assistant,
      text: _buildMockReply(trimmed),
      createdAt: DateTime.now(),
    );

    state = state.copyWith(
      messages: [...state.messages, reply],
      isSending: false,
    );
  }

  String _buildMockReply(String input) {
    if (_containsAny(input, ['소비', '지출', '아껴'])) {
      return '이번 주 소비는 카테고리별 흐름을 먼저 보고,\n고정비 다음으로 큰 변동비 한 항목만 줄여도 체감이 큽니다.';
    }
    if (_containsAny(input, ['저축', '모아', '예산'])) {
      return '저축은 목표 금액을 주차 단위로 쪼개서 보는 게 좋아요.\n이번 주 여유자금에서 먼저 고정 저축분을 빼 두는 방식으로 시작해보세요.';
    }
    if (_containsAny(input, ['목표', '여행', '집', '차'])) {
      return '목표는 기간, 우선순위, 주간 저축 가능 금액을 같이 봐야 현실적으로 잡힙니다.\n원하는 목표를 말해주시면 가볍게 시뮬레이션해드릴게요.';
    }
    if (_containsAny(input, ['알림', '챗봇', '도와'])) {
      return '지금은 목업 대화지만, 이후엔 서버 AI와 연결해서 더 개인화된 답변으로 바뀔 예정이에요.';
    }
    return '좋아요. 지금은 목업 응답이라 간단히 답하고 있어요.\n나중에 서버 API가 붙으면 사용자 데이터 기준으로 더 정확하게 안내할 수 있습니다.';
  }

  bool _containsAny(String input, List<String> keywords) {
    return keywords.any(input.contains);
  }

  static String _messageId() =>
      DateTime.now().microsecondsSinceEpoch.toString();
}

final aiChatViewModelProvider =
    StateNotifierProvider<AiChatViewModel, AiChatState>(
  (ref) => AiChatViewModel(),
);
