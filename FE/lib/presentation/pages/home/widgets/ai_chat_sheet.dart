import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/viewmodels/ai_chat_viewmodel.dart';

class AiChatSheet extends ConsumerStatefulWidget {
  const AiChatSheet({super.key});

  @override
  ConsumerState<AiChatSheet> createState() => _AiChatSheetState();
}

class _AiChatSheetState extends ConsumerState<AiChatSheet> {
  final TextEditingController _messageController = TextEditingController();
  final ScrollController _scrollController = ScrollController();

  int _lastMessageCount = 0;
  bool _lastIsSending = false;

  @override
  void dispose() {
    _messageController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  Future<void> _sendMessage([String? message]) async {
    final text = message ?? _messageController.text;
    if (text.trim().isEmpty) {
      return;
    }
    _messageController.clear();
    await ref.read(aiChatViewModelProvider.notifier).sendMessage(text);
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!_scrollController.hasClients) {
        return;
      }
      _scrollController.animateTo(
        _scrollController.position.maxScrollExtent,
        duration: const Duration(milliseconds: 220),
        curve: Curves.easeOut,
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final chatState = ref.watch(aiChatViewModelProvider);

    if (_lastMessageCount != chatState.messages.length ||
        _lastIsSending != chatState.isSending) {
      _lastMessageCount = chatState.messages.length;
      _lastIsSending = chatState.isSending;
      _scrollToBottom();
    }

    final suggestions = const [
      '이번 주 소비 피드백 알려줘',
      '저축 팁 추천해줘',
      '목표를 어떻게 잡는 게 좋을까?',
    ];

    return SafeArea(
      top: false,
      child: Container(
        decoration: BoxDecoration(
          color: colors.bgSurface,
          borderRadius: BorderRadius.vertical(top: Radius.circular(28.r)),
        ),
        child: Column(
          children: [
            SizedBox(height: 12.h),
            Container(
              width: 44.w,
              height: 4.h,
              decoration: BoxDecoration(
                color: colors.borderDefault,
                borderRadius: BorderRadius.circular(999.r),
              ),
            ),
            Padding(
              padding: EdgeInsets.fromLTRB(20.w, 18.h, 20.w, 14.h),
              child: Row(
                children: [
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
                      children: [
                        Text(
                          '모아 AI 코치',
                          style: AppTextStyles.headingMd.copyWith(
                            color: colors.textPrimary,
                          ),
                        ),
                        SizedBox(height: 2.h),
                        Text(
                          '지금은 목업 응답으로 동작해요',
                          style: AppTextStyles.caption.copyWith(
                            color: colors.textTertiary,
                          ),
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    onPressed: () => Navigator.of(context).pop(),
                    icon: Icon(
                      Icons.close_rounded,
                      color: colors.textSecondary,
                    ),
                  ),
                ],
              ),
            ),
            Divider(height: 1, color: colors.borderDefault),
            Expanded(
              child: ListView(
                controller: _scrollController,
                padding: EdgeInsets.fromLTRB(20.w, 18.h, 20.w, 16.h),
                children: [
                  Wrap(
                    spacing: 8.w,
                    runSpacing: 8.h,
                    children: suggestions.map((suggestion) {
                      return ActionChip(
                        label: Text(suggestion),
                        onPressed: chatState.isSending
                            ? null
                            : () => _sendMessage(suggestion),
                        labelStyle: AppTextStyles.caption.copyWith(
                          color: AppColors.orange500,
                          fontWeight: FontWeight.w700,
                        ),
                        side: BorderSide(
                          color: AppColors.orange500.withValues(alpha: 0.28),
                        ),
                        backgroundColor: AppColors.orange500.withValues(
                          alpha: 0.06,
                        ),
                      );
                    }).toList(),
                  ),
                  SizedBox(height: 18.h),
                  ...chatState.messages.map((message) {
                    final isUser = message.role == AiChatRole.user;
                    return Padding(
                      padding: EdgeInsets.only(bottom: 12.h),
                      child: Row(
                        mainAxisAlignment: isUser
                            ? MainAxisAlignment.end
                            : MainAxisAlignment.start,
                        children: [
                          ConstrainedBox(
                            constraints: BoxConstraints(maxWidth: 0.78.sw),
                            child: Container(
                              padding: EdgeInsets.symmetric(
                                horizontal: 14.w,
                                vertical: 12.h,
                              ),
                              decoration: BoxDecoration(
                                color: isUser
                                    ? AppColors.orange500
                                    : colors.gray50,
                                borderRadius: BorderRadius.only(
                                  topLeft: Radius.circular(18.r),
                                  topRight: Radius.circular(18.r),
                                  bottomLeft: Radius.circular(
                                    isUser ? 18.r : 6.r,
                                  ),
                                  bottomRight: Radius.circular(
                                    isUser ? 6.r : 18.r,
                                  ),
                                ),
                              ),
                              child: Text(
                                message.text,
                                style: AppTextStyles.bodySm.copyWith(
                                  color: isUser
                                      ? Colors.white
                                      : colors.textPrimary,
                                  height: 1.5,
                                ),
                              ),
                            ),
                          ),
                        ],
                      ),
                    );
                  }),
                  if (chatState.isSending)
                    Padding(
                      padding: EdgeInsets.only(bottom: 12.h),
                      child: Row(
                        children: [
                          Container(
                            padding: EdgeInsets.symmetric(
                              horizontal: 14.w,
                              vertical: 12.h,
                            ),
                            decoration: BoxDecoration(
                              color: colors.gray50,
                              borderRadius: BorderRadius.circular(18.r),
                            ),
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: List.generate(3, (index) {
                                return Padding(
                                  padding: EdgeInsets.symmetric(horizontal: 2.w),
                                  child: SizedBox(
                                    width: 6.w,
                                    height: 6.w,
                                    child: CircularProgressIndicator(
                                      strokeWidth: 1.6,
                                      valueColor:
                                          AlwaysStoppedAnimation<Color>(
                                        AppColors.orange500.withValues(
                                          alpha: 0.85,
                                        ),
                                      ),
                                    ),
                                  ),
                                );
                              }),
                            ),
                          ),
                        ],
                      ),
                    ),
                ],
              ),
            ),
            Container(
              padding: EdgeInsets.fromLTRB(20.w, 12.h, 20.w, 20.h),
              decoration: BoxDecoration(
                color: colors.bgSurface,
                border: Border(
                  top: BorderSide(color: colors.borderDefault),
                ),
              ),
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _messageController,
                      textInputAction: TextInputAction.send,
                      minLines: 1,
                      maxLines: 4,
                      onSubmitted: (_) => _sendMessage(),
                      decoration: InputDecoration(
                        hintText: '궁금한 점을 입력해보세요',
                        hintStyle: AppTextStyles.bodySm.copyWith(
                          color: colors.textTertiary,
                        ),
                        filled: true,
                        fillColor: colors.gray50,
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(18.r),
                          borderSide: BorderSide.none,
                        ),
                        contentPadding: EdgeInsets.symmetric(
                          horizontal: 16.w,
                          vertical: 12.h,
                        ),
                      ),
                    ),
                  ),
                  SizedBox(width: 10.w),
                  SizedBox(
                    width: 48.w,
                    height: 48.w,
                    child: FilledButton(
                      onPressed: chatState.isSending ? null : _sendMessage,
                      style: FilledButton.styleFrom(
                        padding: EdgeInsets.zero,
                        backgroundColor: AppColors.orange500,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(16.r),
                        ),
                      ),
                      child: Icon(Icons.arrow_upward_rounded, size: 20.w),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
