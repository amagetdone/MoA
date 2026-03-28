import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/presentation/pages/goal/widgets/goal_charts.dart';
import 'package:more_app/presentation/viewmodels/goal_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/goal_video_thumb.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/widgets/common/more_progress_bar.dart';

class GoalDetailPage extends ConsumerStatefulWidget {
  const GoalDetailPage({super.key, required this.goalId});

  final String goalId;

  @override
  ConsumerState<GoalDetailPage> createState() => _GoalDetailPageState();
}

class _GoalDetailPageState extends ConsumerState<GoalDetailPage> {
  bool _isRetryingVideo = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(goalViewModelProvider.notifier).loadGoalDetail(widget.goalId);
    });
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final goalState = ref.watch(goalViewModelProvider);
    final goal = goalState.findGoalById(widget.goalId);
    final isDetailLoading = goalState.isDetailLoading(widget.goalId);
    final history = goalState.savingsHistoryFor(widget.goalId);

    if (goalState.isLoading || (goal == null && isDetailLoading)) {
      return Scaffold(
        backgroundColor: colors.bgPrimary,
        body: const Center(child: CircularProgressIndicator()),
      );
    }

    if (goal == null) {
      return Scaffold(
        backgroundColor: colors.bgPrimary,
        appBar: AppBar(backgroundColor: colors.bgPrimary, elevation: 0),
        body: const Center(child: Text('목표를 찾을 수 없습니다.')),
      );
    }

    return Scaffold(
      backgroundColor: colors.bgPrimary,
      body: SafeArea(
        child: Column(
          children: [
            Padding(
              padding: EdgeInsets.fromLTRB(4.w, 4.h, 12.w, 0),
              child: Row(
                children: [
                  IconButton(
                    icon: Icon(
                      Icons.chevron_left,
                      size: 28.r,
                      color: colors.textPrimary,
                    ),
                    onPressed: () => context.pop(),
                  ),
                  Expanded(
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.center,
                      children: [
                        Flexible(
                          child: Text(
                            goal.name,
                            style: AppTextStyles.headingMd.copyWith(
                              fontWeight: FontWeight.w800,
                              color: colors.textPrimary,
                            ),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                        SizedBox(width: 10.w),
                        if (goal.isEffectivelyAchieved) ...[
                          Text(
                            '${goal.currentWeekNumber}주차',
                            style: AppTextStyles.caption.copyWith(
                              color: colors.textPrimary,
                              fontSize: 12.sp,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ] else ...[
                          _Badge(
                            label: goal.remainingDays >= 0
                                ? 'D-${goal.remainingDays}'
                                : 'D+${-goal.remainingDays}',
                            bgColor: goal.remainingDays < 0
                                ? AppColors.red500
                                : AppColors.orange500,
                            textColor: Colors.white,
                          ),
                        ],
                      ],
                    ),
                  ),
                ],
              ),
            ),
            Expanded(
              child: SingleChildScrollView(
                padding: EdgeInsets.fromLTRB(16.w, 12.h, 16.w, 28.h),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    _HeroCard(
                      goal: goal,
                      showVideoLoading:
                          goal.isAiPending ||
                          (isDetailLoading &&
                              goal.sketchVideoAssetPath == null),
                      isRetryingVideo: _isRetryingVideo,
                      onRetryVideo: goal.isAiFailed
                          ? () => _retryGoalVideo(goal)
                          : null,
                    ),
                    SizedBox(height: 14.h),
                    if (goal.description != null &&
                        goal.description!.isNotEmpty) ...[
                      MoreCard(
                        padding: EdgeInsets.all(18.w),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '목표 설명',
                              style: AppTextStyles.bodyMd.copyWith(
                                fontWeight: FontWeight.w700,
                                color: colors.textPrimary,
                              ),
                            ),
                            SizedBox(height: 6.h),
                            Text(
                              goal.description!,
                              style: AppTextStyles.bodyMd.copyWith(
                                color: colors.textSecondary,
                              ),
                            ),
                          ],
                        ),
                      ),
                      SizedBox(height: 14.h),
                    ],
                    GoalLineChart(history: history, goal: goal),
                    SizedBox(height: 14.h),
                    GoalBarChart(history: history, goal: goal),
                    if (goal.weeklyMission != null ||
                        goal.weeklyBudget != null) ...[
                      SizedBox(height: 14.h),
                      _WeeklySummaryCard(goal: goal),
                    ],
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _retryGoalVideo(Goal goal) async {
    if (_isRetryingVideo) {
      return;
    }

    setState(() => _isRetryingVideo = true);
    try {
      await ref
          .read(goalViewModelProvider.notifier)
          .retryGoalVideoGeneration(goal.id);
    } catch (error) {
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('AI 영상 재시도에 실패했습니다: $error')));
    } finally {
      if (mounted) {
        setState(() => _isRetryingVideo = false);
      }
    }
  }
}

class _HeroCard extends StatelessWidget {
  const _HeroCard({
    required this.goal,
    this.showVideoLoading = false,
    this.isRetryingVideo = false,
    this.onRetryVideo,
  });

  final Goal goal;
  final bool showVideoLoading;
  final bool isRetryingVideo;
  final VoidCallback? onRetryVideo;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final remaining = CurrencyFormatter.formatKRW(goal.remainingAmount);
    final current = CurrencyFormatter.formatKRW(goal.currentAmount);
    final target = CurrencyFormatter.formatKRW(goal.targetAmount);
    final overallProgress = goal.overallProgressRate.clamp(0.0, 1.0);

    return MoreCard(
      padding: EdgeInsets.zero,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Stack(
            clipBehavior: Clip.none,
            children: [
              ClipRRect(
                borderRadius: BorderRadius.vertical(top: Radius.circular(16.r)),
                child: GoalVideoThumb(
                  key: ValueKey('detail_${goal.id}'),
                  goalId: goal.id,
                  assetPath: goal.isAiPending || goal.isAiFailed
                      ? null
                      : goal.sketchVideoAssetPath,
                  iconName: goal.iconName,
                  progressRate: overallProgress,
                  size: 200.w,
                  borderRadius: 0,
                  fillWidth: true,
                  playAnimation: true,
                  forceLoading: showVideoLoading,
                ),
              ),
              Positioned(
                top: 12.h,
                left: 14.w,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _Badge(
                      label: goal.isEffectivelyAchieved
                          ? '목표 완료'
                          : '우선순위 ${goal.priority}',
                      bgColor: goal.isEffectivelyAchieved
                          ? AppColors.green500
                          : AppColors.orange500,
                      textColor: Colors.white,
                    ),
                  ],
                ),
              ),
            ],
          ),
          Padding(
            padding: EdgeInsets.fromLTRB(18.w, 18.h, 18.w, 18.h),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (goal.isAiPending || goal.isAiFailed) ...[
                  Container(
                    width: double.infinity,
                    padding: EdgeInsets.symmetric(
                      horizontal: 14.w,
                      vertical: 12.h,
                    ),
                    decoration: BoxDecoration(
                      color: goal.isAiPending
                          ? AppColors.orange500.withValues(alpha: 0.10)
                          : AppColors.red500.withValues(alpha: 0.08),
                      borderRadius: BorderRadius.circular(14.r),
                    ),
                    child: Row(
                      children: [
                        Icon(
                          goal.isAiPending
                              ? Icons.hourglass_top_rounded
                              : Icons.refresh_rounded,
                          color: goal.isAiPending
                              ? AppColors.orange500
                              : AppColors.red500,
                          size: 18.w,
                        ),
                        SizedBox(width: 10.w),
                        Expanded(
                          child: Text(
                            goal.isAiPending
                                ? 'AI 스케치 영상을 생성 중입니다.'
                                : 'AI 영상 생성에 실패했습니다.\n재시도할 수 있습니다.',
                            style: AppTextStyles.bodySm.copyWith(
                              color: goal.isAiPending
                                  ? AppColors.orange500
                                  : AppColors.red500,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ),
                        if (goal.isAiFailed && onRetryVideo != null) ...[
                          SizedBox(width: 12.w),
                          TextButton(
                            onPressed: isRetryingVideo ? null : onRetryVideo,
                            style: TextButton.styleFrom(
                              padding: EdgeInsets.symmetric(
                                horizontal: 12.w,
                                vertical: 8.h,
                              ),
                              backgroundColor: AppColors.red500,
                              foregroundColor: Colors.white,
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(999.r),
                              ),
                            ),
                            child: Text(
                              isRetryingVideo ? '요청 중' : '재시도',
                              style: AppTextStyles.caption.copyWith(
                                color: Colors.white,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                          ),
                        ],
                      ],
                    ),
                  ),
                  SizedBox(height: 14.h),
                ],
                Row(
                  children: [
                    Container(
                      width: 38.w,
                      height: 38.w,
                      decoration: BoxDecoration(
                        color: AppColors.green500.withValues(alpha: 0.12),
                        shape: BoxShape.circle,
                        border: Border.all(
                          color: AppColors.green500.withValues(alpha: 0.3),
                        ),
                      ),
                      child: Icon(
                        Icons.savings_outlined,
                        color: AppColors.green500,
                        size: 20.w,
                      ),
                    ),
                    SizedBox(width: 12.w),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            crossAxisAlignment: CrossAxisAlignment.baseline,
                            textBaseline: TextBaseline.alphabetic,
                            children: [
                              Text(
                                current,
                                style: AppTextStyles.headingMd.copyWith(
                                  fontWeight: FontWeight.w800,
                                  color: AppColors.green500,
                                ),
                              ),
                              Text(
                                '총 $target',
                                style: AppTextStyles.bodySm.copyWith(
                                  color: colors.textTertiary,
                                ),
                              ),
                            ],
                          ),
                          SizedBox(height: 2.h),
                          Text(
                            '현재 저축액',
                            style: AppTextStyles.bodySm.copyWith(
                              color: colors.textTertiary,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                SizedBox(height: 14.h),
                MoreProgressBar(
                  progress: overallProgress,
                  color: AppColors.green500,
                  backgroundColor: colors.gray500.withValues(alpha: 0.12),
                  height: 8.h,
                ),
                SizedBox(height: 8.h),
                Text(
                  goal.isEffectivelyAchieved
                      ? '축하해요! 목표 금액을 모두 달성했어요.'
                      : '목표까지 $remaining 남음',
                  style: AppTextStyles.bodySm.copyWith(
                    color: goal.isEffectivelyAchieved
                        ? AppColors.green500
                        : colors.textTertiary,
                    fontWeight: goal.isEffectivelyAchieved
                        ? FontWeight.w700
                        : FontWeight.w400,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _WeeklySummaryCard extends StatelessWidget {
  const _WeeklySummaryCard({required this.goal});

  final Goal goal;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final weeklyMission = goal.weeklyMission;
    final weeklyBudget = goal.weeklyBudget;

    return MoreCard(
      padding: EdgeInsets.all(18.w),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '주간 계획 요약',
            style: AppTextStyles.bodyMd.copyWith(
              fontWeight: FontWeight.w700,
              color: colors.textPrimary,
            ),
          ),
          SizedBox(height: 12.h),
          if (weeklyMission != null) ...[
            _SummaryRow(
              label: '주간 미션 저축액',
              value: CurrencyFormatter.formatKRW(
                weeklyMission.weeklySavingsAmount,
              ),
            ),
            _SummaryRow(
              label: '목표까지 남은 주차',
              value: '${weeklyMission.weeksToGoal}주',
            ),
          ],
          if (weeklyBudget != null) ...[
            _SummaryRow(
              label: '주간 총수입',
              value: CurrencyFormatter.formatKRW(
                weeklyBudget.totalIncomeAmount,
              ),
            ),
            _SummaryRow(
              label: '주간 저축액',
              value: CurrencyFormatter.formatKRW(weeklyBudget.savingAmount),
            ),
            _SummaryRow(
              label: '자유 금액',
              value: CurrencyFormatter.formatKRW(weeklyBudget.freeAmount),
            ),
          ],
        ],
      ),
    );
  }
}

class _SummaryRow extends StatelessWidget {
  const _SummaryRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return Padding(
      padding: EdgeInsets.only(bottom: 8.h),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            label,
            style: AppTextStyles.bodySm.copyWith(color: colors.textTertiary),
          ),
          Text(
            value,
            style: AppTextStyles.bodyMd.copyWith(
              color: colors.textPrimary,
              fontWeight: FontWeight.w700,
            ),
          ),
        ],
      ),
    );
  }
}

class _Badge extends StatelessWidget {
  const _Badge({
    required this.label,
    required this.bgColor,
    required this.textColor,
  });

  final String label;
  final Color bgColor;
  final Color textColor;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.symmetric(horizontal: 10.w, vertical: 4.h),
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: BorderRadius.circular(999.r),
      ),
      child: Text(
        label,
        style: AppTextStyles.caption.copyWith(
          color: textColor,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}
