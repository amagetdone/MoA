import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/domain/entities/chat_message.dart';
import 'package:more_app/presentation/viewmodels/chatbot_viewmodel.dart';

class ChatbotPage extends ConsumerStatefulWidget {
  const ChatbotPage({super.key, this.embedded = false});

  final bool embedded;

  @override
  ConsumerState<ChatbotPage> createState() => _ChatbotPageState();
}

class _ChatbotPageState extends ConsumerState<ChatbotPage> {
  final TextEditingController _inputController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  static const _suggestions = [
    '이번 주 소비 피드백 알려줘',
    '저축 팁 추천해줘',
    '목표를 어떻게 잡는 게 좋을까?',
  ];

  @override
  void dispose() {
    _inputController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 200),
          curve: Curves.easeOut,
        );
      }
    });
  }

  void _sendMessage([String? preset]) {
    final text = (preset ?? _inputController.text).trim();
    if (text.isEmpty) return;
    _inputController.clear();
    ref.read(chatbotViewModelProvider.notifier).sendMessage(text);
    _scrollToBottom();
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final state = ref.watch(chatbotViewModelProvider);
    final viewModel = ref.read(chatbotViewModelProvider.notifier);

    ref.listen(chatbotViewModelProvider, (_, next) {
      if (next.messages.isNotEmpty) _scrollToBottom();
    });

    final body = Column(
      children: [
        if (state.isExpired && state.infoMessage != null)
          _ExpiredBanner(
            message: state.infoMessage!,
            onNewChat: viewModel.startNewConversation,
          ),
        Expanded(
          child: !state.hasInitialized && state.isLoading
              ? _InitialLoading(colors: colors)
              : state.messages.isEmpty
              ? _EmptyState(
                  colors: colors,
                  infoMessage: state.isExpired ? null : state.infoMessage,
                  errorMessage: state.errorMessage,
                  onRetry: viewModel.restoreLatestConversation,
                  suggestions: _suggestions,
                  onSuggestionTap: _sendMessage,
                )
              : ListView.builder(
                  controller: _scrollController,
                  padding: EdgeInsets.symmetric(
                    horizontal: 16.w,
                    vertical: 12.h,
                  ),
                  itemCount: state.messages.length,
                  itemBuilder: (context, index) {
                    return _ChatBubble(message: state.messages[index]);
                  },
                ),
        ),
        if (state.errorMessage != null && state.messages.isNotEmpty)
          Padding(
            padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 4.h),
            child: Text(
              state.errorMessage!,
              style: AppTextStyles.bodySm.copyWith(color: AppColors.red500),
              textAlign: TextAlign.center,
            ),
          ),
        _InputBar(
          controller: _inputController,
          isLoading: state.isLoading,
          onSend: _sendMessage,
          colors: colors,
        ),
      ],
    );

    if (widget.embedded) {
      return ColoredBox(
        color: colors.bgPrimary,
        child: SafeArea(
          top: false,
          child: Column(
            children: [
              _ChatbotHeader(
                colors: colors,
                showClose: true,
                canRefresh:
                    state.messages.isNotEmpty || state.conversationId != null,
                isLoading: state.isLoading,
                onClose: () => Navigator.of(context).pop(),
                onRefresh: viewModel.startNewConversation,
              ),
              Expanded(child: body),
            ],
          ),
        ),
      );
    }

    return Scaffold(
      backgroundColor: colors.bgPrimary,
      appBar: PreferredSize(
        preferredSize: Size.fromHeight(72.h),
        child: SafeArea(
          bottom: false,
          child: _ChatbotHeader(
            colors: colors,
            showClose: false,
            canRefresh:
                state.messages.isNotEmpty || state.conversationId != null,
            isLoading: state.isLoading,
            onClose: () => Navigator.of(context).pop(),
            onRefresh: viewModel.startNewConversation,
          ),
        ),
      ),
      body: body,
    );
  }
}

class _ChatbotHeader extends StatelessWidget {
  const _ChatbotHeader({
    required this.colors,
    required this.showClose,
    required this.canRefresh,
    required this.isLoading,
    required this.onClose,
    required this.onRefresh,
  });

  final AppColorScheme colors;
  final bool showClose;
  final bool canRefresh;
  final bool isLoading;
  final VoidCallback onClose;
  final VoidCallback onRefresh;

  @override
  Widget build(BuildContext context) {
    return Container(
      color: colors.bgPrimary,
      padding: EdgeInsets.fromLTRB(8.w, 10.h, 8.w, 8.h),
      child: Row(
        children: [
          IconButton(
            icon: Icon(
              showClose ? Icons.close_rounded : Icons.arrow_back_ios_new,
              size: 20.w,
              color: colors.textPrimary,
            ),
            onPressed: onClose,
          ),
          Container(
            width: 40.w,
            height: 40.w,
            decoration: BoxDecoration(
              color: AppColors.orange500.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(14.r),
            ),
            child: Icon(
              Icons.smart_toy_outlined,
              color: AppColors.orange500,
              size: 22.w,
            ),
          ),
          SizedBox(width: 12.w),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  '모아 AI 코치',
                  style: AppTextStyles.headingMd.copyWith(
                    color: colors.textPrimary,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                SizedBox(height: 2.h),
                Text(
                  '예산과 소비 패턴을 편하게 물어보세요',
                  style: AppTextStyles.caption.copyWith(
                    color: colors.textTertiary,
                  ),
                ),
              ],
            ),
          ),
          if (canRefresh)
            IconButton(
              icon: Icon(Icons.refresh, size: 20.w, color: colors.textTertiary),
              tooltip: '새 대화 시작',
              onPressed: isLoading ? null : onRefresh,
            ),
        ],
      ),
    );
  }
}

// ── 만료 배너 ──────────────────────────────────────
class _ExpiredBanner extends StatelessWidget {
  const _ExpiredBanner({required this.message, required this.onNewChat});
  final String message;
  final VoidCallback onNewChat;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      color: AppColors.orange500.withValues(alpha: 0.1),
      padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 10.h),
      child: Row(
        children: [
          Icon(Icons.info_outline, size: 16.w, color: AppColors.orange500),
          SizedBox(width: 8.w),
          Expanded(
            child: Text(
              message,
              style: AppTextStyles.bodySm.copyWith(color: AppColors.orange500),
            ),
          ),
          GestureDetector(
            onTap: onNewChat,
            child: Text(
              '새로 시작',
              style: AppTextStyles.bodySm.copyWith(
                color: AppColors.orange500,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _InitialLoading extends StatelessWidget {
  const _InitialLoading({required this.colors});

  final AppColorScheme colors;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          SizedBox(
            width: 28.w,
            height: 28.w,
            child: const CircularProgressIndicator(
              strokeWidth: 2.6,
              color: AppColors.orange500,
            ),
          ),
          SizedBox(height: 14.h),
          Text(
            '최근 대화를 불러오는 중이에요',
            style: AppTextStyles.bodySm.copyWith(color: colors.textTertiary),
          ),
        ],
      ),
    );
  }
}

// ── 빈 상태 ──────────────────────────────────────
class _EmptyState extends StatelessWidget {
  const _EmptyState({
    required this.colors,
    this.infoMessage,
    this.errorMessage,
    required this.onRetry,
    required this.suggestions,
    required this.onSuggestionTap,
  });

  final AppColorScheme colors;
  final String? infoMessage;
  final String? errorMessage;
  final VoidCallback onRetry;
  final List<String> suggestions;
  final void Function(String) onSuggestionTap;

  @override
  Widget build(BuildContext context) {
    final title = errorMessage != null
        ? 'AI 코치를 불러오지 못했어요'
        : (infoMessage ?? 'AI 코치에게 물어보세요');
    final description = errorMessage ?? '예산, 소비 패턴, 목표 달성에 대해\n무엇이든 물어보세요.';

    return LayoutBuilder(
      builder: (context, constraints) {
        return SingleChildScrollView(
          padding: EdgeInsets.symmetric(horizontal: 20.w, vertical: 12.h),
          child: ConstrainedBox(
            constraints: BoxConstraints(
              minHeight: math.max(0, constraints.maxHeight - 24.h),
            ),
            child: Center(
              child: Container(
                width: double.infinity,
                padding: EdgeInsets.fromLTRB(20.w, 24.h, 20.w, 20.h),
                decoration: BoxDecoration(
                  color: colors.bgSurface,
                  borderRadius: BorderRadius.circular(24.r),
                  boxShadow: [
                    BoxShadow(
                      color: colors.cardShadow,
                      blurRadius: 12,
                      offset: const Offset(0, 4),
                    ),
                  ],
                ),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Container(
                      width: 72.w,
                      height: 72.w,
                      decoration: BoxDecoration(
                        color: AppColors.orange500.withValues(alpha: 0.1),
                        borderRadius: BorderRadius.circular(24.r),
                      ),
                      child: Icon(
                        Icons.smart_toy_outlined,
                        size: 36.w,
                        color: AppColors.orange500,
                      ),
                    ),
                    SizedBox(height: 16.h),
                    Text(
                      title,
                      style: AppTextStyles.bodyLg.copyWith(
                        fontWeight: FontWeight.w700,
                        color: colors.textPrimary,
                      ),
                      textAlign: TextAlign.center,
                    ),
                    SizedBox(height: 8.h),
                    Text(
                      description,
                      style: AppTextStyles.bodySm.copyWith(
                        color: colors.textTertiary,
                        height: 1.5,
                      ),
                      textAlign: TextAlign.center,
                    ),
                    if (errorMessage == null) ...[
                      SizedBox(height: 18.h),
                      Wrap(
                        spacing: 8.w,
                        runSpacing: 8.h,
                        alignment: WrapAlignment.center,
                        children: suggestions
                            .map(
                              (suggestion) => ActionChip(
                                label: Text(suggestion),
                                onPressed: () => onSuggestionTap(suggestion),
                                labelStyle: AppTextStyles.caption.copyWith(
                                  color: AppColors.orange500,
                                  fontWeight: FontWeight.w700,
                                ),
                                side: BorderSide(
                                  color: AppColors.orange500.withValues(
                                    alpha: 0.24,
                                  ),
                                ),
                                backgroundColor: AppColors.orange500.withValues(
                                  alpha: 0.06,
                                ),
                              ),
                            )
                            .toList(),
                      ),
                    ],
                    if (errorMessage != null) ...[
                      SizedBox(height: 16.h),
                      OutlinedButton.icon(
                        onPressed: onRetry,
                        icon: const Icon(Icons.refresh),
                        label: const Text('다시 불러오기'),
                      ),
                    ],
                  ],
                ),
              ),
            ),
          ),
        );
      },
    );
  }
}

// ── 채팅 버블 ──────────────────────────────────────
class _ChatBubble extends StatelessWidget {
  const _ChatBubble({required this.message});
  final ChatMessage message;

  @override
  Widget build(BuildContext context) {
    final isUser = message.role == ChatRole.user;
    final colors = AppColors.of(context);

    return Padding(
      padding: EdgeInsets.only(bottom: 12.h),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: isUser
            ? MainAxisAlignment.end
            : MainAxisAlignment.start,
        children: [
          if (!isUser) ...[
            Container(
              width: 32.w,
              height: 32.w,
              decoration: BoxDecoration(
                color: AppColors.orange500.withValues(alpha: 0.12),
                shape: BoxShape.circle,
              ),
              child: Icon(
                Icons.smart_toy_outlined,
                size: 16.w,
                color: AppColors.orange500,
              ),
            ),
            SizedBox(width: 8.w),
          ],
          Flexible(
            child: Container(
              padding: EdgeInsets.symmetric(horizontal: 14.w, vertical: 10.h),
              decoration: BoxDecoration(
                color: isUser ? AppColors.orange500 : colors.gray50,
                borderRadius: BorderRadius.only(
                  topLeft: Radius.circular(isUser ? 16.r : 4.r),
                  topRight: Radius.circular(isUser ? 4.r : 16.r),
                  bottomLeft: Radius.circular(16.r),
                  bottomRight: Radius.circular(16.r),
                ),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withValues(alpha: 0.05),
                    blurRadius: 4,
                    offset: const Offset(0, 2),
                  ),
                ],
              ),
              child: isUser
                  ? Text(
                      message.content,
                      style: AppTextStyles.bodySm.copyWith(color: Colors.white),
                    )
                  : message.isStreaming
                  ? _StreamingContent(content: message.content, colors: colors)
                  : MarkdownBody(
                      data: message.content,
                      styleSheet: MarkdownStyleSheet(
                        p: AppTextStyles.bodySm.copyWith(
                          color: colors.textPrimary,
                        ),
                        h1: AppTextStyles.bodyLg.copyWith(
                          fontWeight: FontWeight.w700,
                          color: colors.textPrimary,
                        ),
                        h2: AppTextStyles.bodyLg.copyWith(
                          fontWeight: FontWeight.w600,
                          color: colors.textPrimary,
                        ),
                        listBullet: AppTextStyles.bodySm.copyWith(
                          color: colors.textPrimary,
                        ),
                        code: AppTextStyles.bodySm.copyWith(
                          fontFamily: 'monospace',
                          backgroundColor: colors.bgPrimary,
                        ),
                      ),
                    ),
            ),
          ),
          if (isUser) SizedBox(width: 8.w),
        ],
      ),
    );
  }
}

// 스트리밍 중 plain text + 커서 애니메이션
class _StreamingContent extends StatefulWidget {
  const _StreamingContent({required this.content, required this.colors});
  final String content;
  final AppColorScheme colors;

  @override
  State<_StreamingContent> createState() => _StreamingContentState();
}

class _StreamingContentState extends State<_StreamingContent>
    with SingleTickerProviderStateMixin {
  late final AnimationController _typingController;

  @override
  void initState() {
    super.initState();
    _typingController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1100),
    )..repeat();
  }

  @override
  void dispose() {
    _typingController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _typingController,
      builder: (context, _) {
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            if (widget.content.isNotEmpty)
              Text(
                widget.content,
                style: AppTextStyles.bodySm.copyWith(
                  color: widget.colors.textPrimary,
                  height: 1.5,
                ),
              ),
            SizedBox(height: widget.content.isEmpty ? 0 : 8.h),
            _TypingDotsIndicator(
              progress: _typingController.value,
              colors: widget.colors,
            ),
          ],
        );
      },
    );
  }
}

class _TypingDotsIndicator extends StatelessWidget {
  const _TypingDotsIndicator({required this.progress, required this.colors});

  final double progress;
  final AppColorScheme colors;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 12.h,
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: List.generate(3, (index) {
          final phase = ((progress + index * 0.18) % 1.0) * 2 * math.pi;
          final normalized = (math.sin(phase - math.pi / 2) + 1) / 2;
          final scale = 0.82 + (normalized * 0.38);
          final opacity = 0.32 + (normalized * 0.68);

          return Padding(
            padding: EdgeInsets.only(right: index == 2 ? 0 : 5.w),
            child: Opacity(
              opacity: opacity,
              child: Transform.scale(
                scale: scale,
                child: Container(
                  width: 7.w,
                  height: 7.w,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    gradient: LinearGradient(
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                      colors: [AppColors.orange300, AppColors.orange500],
                    ),
                    boxShadow: [
                      BoxShadow(
                        color: AppColors.orange500.withValues(alpha: 0.18),
                        blurRadius: 6,
                        offset: const Offset(0, 1),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          );
        }),
      ),
    );
  }
}

// ── 입력창 ──────────────────────────────────────────
class _InputBar extends StatelessWidget {
  const _InputBar({
    required this.controller,
    required this.isLoading,
    required this.onSend,
    required this.colors,
  });

  final TextEditingController controller;
  final bool isLoading;
  final VoidCallback onSend;
  final AppColorScheme colors;

  @override
  Widget build(BuildContext context) {
    return Container(
      color: colors.bgSurface,
      padding: EdgeInsets.fromLTRB(
        16.w,
        8.h,
        16.w,
        16.h + MediaQuery.of(context).padding.bottom,
      ),
      child: Row(
        children: [
          Expanded(
            child: Container(
              decoration: BoxDecoration(
                color: colors.bgPrimary,
                borderRadius: BorderRadius.circular(24.r),
                border: Border.all(color: colors.borderDefault),
              ),
              child: TextField(
                controller: controller,
                maxLines: 4,
                minLines: 1,
                textInputAction: TextInputAction.send,
                onSubmitted: (_) => onSend(),
                style: AppTextStyles.bodySm.copyWith(color: colors.textPrimary),
                decoration: InputDecoration(
                  hintText: '메시지를 입력하세요...',
                  hintStyle: AppTextStyles.bodySm.copyWith(
                    color: colors.textTertiary,
                  ),
                  contentPadding: EdgeInsets.symmetric(
                    horizontal: 16.w,
                    vertical: 10.h,
                  ),
                  border: InputBorder.none,
                ),
              ),
            ),
          ),
          SizedBox(width: 8.w),
          GestureDetector(
            onTap: isLoading ? null : onSend,
            child: Container(
              width: 44.w,
              height: 44.w,
              decoration: BoxDecoration(
                color: isLoading ? colors.borderDefault : AppColors.orange500,
                shape: BoxShape.circle,
              ),
              child: isLoading
                  ? Padding(
                      padding: EdgeInsets.all(12.w),
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: colors.textTertiary,
                      ),
                    )
                  : Icon(Icons.send_rounded, size: 20.w, color: Colors.white),
            ),
          ),
        ],
      ),
    );
  }
}
