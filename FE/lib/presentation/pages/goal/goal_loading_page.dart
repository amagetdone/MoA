import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/viewmodels/goal_viewmodel.dart';
import 'package:more_app/presentation/viewmodels/menu_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';
import 'package:more_app/presentation/widgets/common/more_loading_bars.dart';

class GoalLoadingPage extends ConsumerStatefulWidget {
  const GoalLoadingPage({
    super.key,
    required this.goalId,
    this.source = 'create',
  });

  final String goalId;
  final String source;

  @override
  ConsumerState<GoalLoadingPage> createState() => _GoalLoadingPageState();
}

class _GoalLoadingPageState extends ConsumerState<GoalLoadingPage>
    with SingleTickerProviderStateMixin {
  late final AnimationController _animController;
  Timer? _pollTimer;
  bool _isCheckingStatus = false;
  bool _isRetrying = false;
  bool _hasNavigated = false;

  @override
  void initState() {
    super.initState();
    _animController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1200),
    )..repeat();

    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (widget.goalId.isEmpty) {
        context.go(AppRoutes.goalTab);
        return;
      }
      _startPolling();
    });
  }

  @override
  void dispose() {
    _pollTimer?.cancel();
    _animController.dispose();
    super.dispose();
  }

  Future<void> _startPolling() async {
    await _pollGoalStatus();
    _pollTimer ??= Timer.periodic(
      const Duration(seconds: 4),
      (_) => _pollGoalStatus(),
    );
  }

  Future<void> _pollGoalStatus() async {
    if (_isCheckingStatus || _hasNavigated || !mounted) {
      return;
    }

    _isCheckingStatus = true;
    try {
      final goal = await ref
          .read(goalViewModelProvider.notifier)
          .refreshGoalStatus(widget.goalId);

      if (!mounted || goal == null) {
        return;
      }

      if (goal.isAiSucceeded) {
        _pollTimer?.cancel();
        _hasNavigated = true;
        unawaited(
          ref.read(goalViewModelProvider.notifier).loadGoalDetail(goal.id),
        );
        context.go(AppRoutes.goalPreviewLocation(goal.id));
        return;
      }

      if (goal.isAiFailed) {
        _pollTimer?.cancel();
      }
    } finally {
      _isCheckingStatus = false;
    }
  }

  Future<void> _enableNotificationsAndBrowse() async {
    final menuNotifier = ref.read(menuViewModelProvider.notifier);
    var currentMenuState = ref.read(menuViewModelProvider);

    if (!currentMenuState.notificationEnabled) {
      await menuNotifier.toggleNotification(true);
      if (!mounted) {
        return;
      }
      currentMenuState = ref.read(menuViewModelProvider);
      if (!currentMenuState.notificationEnabled) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              currentMenuState.errorMessage ?? '알림 권한을 확인해주세요.',
            ),
          ),
        );
        return;
      }
    }

    if (!mounted) {
      return;
    }
    context.go(AppRoutes.goalTab);
  }

  Future<void> _retryGeneration() async {
    if (_isRetrying) {
      return;
    }

    setState(() => _isRetrying = true);
    try {
      await ref
          .read(goalViewModelProvider.notifier)
          .retryGoalVideoGeneration(widget.goalId);
      _pollTimer?.cancel();
      _pollTimer = null;
      _hasNavigated = false;
      await _startPolling();
    } catch (error) {
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('이미지 재생성 요청에 실패했습니다: $error')),
      );
    } finally {
      if (mounted) {
        setState(() => _isRetrying = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final goalState = ref.watch(goalViewModelProvider);
    final menuState = ref.watch(menuViewModelProvider);
    final goal = goalState.findGoalById(widget.goalId);
    final isFailed = goal?.isAiFailed ?? false;
    final isPending = goal == null || goal.isAiPending || _isCheckingStatus;
    final goalName = goal?.name ?? '목표';
    final isEditFlow = widget.source == 'edit';
    final titleText = isFailed
        ? (isEditFlow ? '이미지 재생성 실패' : '이미지 생성 실패')
        : (isEditFlow ? '이미지 다시 생성 중' : '이미지 생성 중');
    final primaryMessage = isFailed
        ? (isEditFlow
            ? '변경된 목표로 이미지를 다시 만드는 중 문제가 생겼어요.\n다시 시도하거나 나중에 확인해주세요.'
            : 'AI가 이미지를 만드는 중 문제가 생겼어요.\n다시 시도하거나 나중에 확인해주세요.')
        : 'AI가 목표 이미지를 생성하고 있어요.\n완성되면 바로 보여드릴게요.';
    final secondaryMessage = isFailed
        ? '알림을 켜 두면 재시도 후 완료될 때 바로 확인할 수 있어요.'
        : '현재 화면에서 기다리거나, 나중에 다시 확인할 수 있어요.';

    return PopScope(
      canPop: false,
      child: Scaffold(
        backgroundColor: colors.bgSurface,
        body: SafeArea(
          child: Padding(
            padding: EdgeInsets.fromLTRB(24.w, 24.h, 24.w, 16.h),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  titleText,
                  style: AppTextStyles.headingLg.copyWith(
                    color: colors.gray900,
                    fontSize: 22.sp,
                  ),
                ),
                SizedBox(height: 8.h),
                Text(
                  goalName,
                  style: AppTextStyles.bodyMd.copyWith(
                    color: AppColors.orange500,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                Expanded(
                  child: Center(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                        children: [
                        MoreLoadingBars(
                          animation: _animController,
                          color: isFailed
                              ? AppColors.red500
                              : AppColors.orange500,
                        ),
                        SizedBox(height: 32.h),
                        Text(
                          primaryMessage,
                          textAlign: TextAlign.center,
                          style: AppTextStyles.headingMd.copyWith(
                            color: colors.gray900,
                            fontSize: 18.sp,
                          ),
                        ),
                        SizedBox(height: 18.h),
                        Text(
                          secondaryMessage,
                          textAlign: TextAlign.center,
                          style: AppTextStyles.bodySm.copyWith(
                            color: colors.textSecondary,
                            height: 1.5,
                          ),
                        ),
                        SizedBox(height: 24.h),
                        if (isFailed)
                          AnimatedSwitcher(
                            duration: const Duration(milliseconds: 220),
                            child: FilledButton.icon(
                              key: const ValueKey('retry'),
                              onPressed: _isRetrying ? null : _retryGeneration,
                              icon: const Icon(Icons.refresh_rounded),
                              label: Text(_isRetrying ? '요청 중...' : '다시 생성하기'),
                            ),
                          ),
                        if (isPending) ...[
                          SizedBox(height: 16.h),
                          Text(
                            '상태를 실시간으로 확인하고 있습니다.',
                            style: AppTextStyles.caption.copyWith(
                              color: colors.textTertiary,
                            ),
                          ),
                        ],
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
        bottomNavigationBar: MoreBottomButton(
          text: isFailed ? '목표 상세로 이동' : '알림으로 받고, 앱을 더 둘러볼래요.',
          onPressed: isFailed
              ? () => context.go(AppRoutes.goalDetailLocation(widget.goalId))
              : (menuState.isLoading ? null : _enableNotificationsAndBrowse),
        ),
      ),
    );
  }

}
