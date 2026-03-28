import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/widgets/common/goal_video_thumb.dart';

class GoalsSectionCard extends StatefulWidget {
  const GoalsSectionCard({
    super.key,
    required this.goals,
    required this.onViewAll,
    this.onAddGoal,
  });

  final List<Goal> goals;
  final VoidCallback onViewAll;
  final VoidCallback? onAddGoal;

  @override
  State<GoalsSectionCard> createState() => _GoalsSectionCardState();
}

class _GoalsSectionCardState extends State<GoalsSectionCard> {
  int _currentPage = 0;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final pageCount = widget.goals.length;

    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(20.r),
        color: colors.bgSurface,
        boxShadow: [
          BoxShadow(
            color: colors.cardShadow,
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Stack(
        children: [
          // Bottom inner glow
          Positioned(
            bottom: 0,
            left: 0,
            right: 0,
            height: 80.h,
            child: Container(
              decoration: BoxDecoration(
                borderRadius: BorderRadius.vertical(
                  bottom: Radius.circular(20.r),
                ),
                gradient: LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: isDark
                      ? [
                          Colors.transparent,
                          const Color(0xFF5B8DEF).withValues(alpha: 0.04),
                          const Color(0xFFE8873B).withValues(alpha: 0.06),
                        ]
                      : [
                          Colors.transparent,
                          const Color(0xFF5B8DEF).withValues(alpha: 0.03),
                          const Color(0xFFE8873B).withValues(alpha: 0.05),
                        ],
                ),
              ),
            ),
          ),
          // Content
          Padding(
            padding: EdgeInsets.fromLTRB(16.w, 16.w, 16.w, 14.w),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Header
                Row(
                  children: [
                    Text(
                      '이번 주 목표',
                      style: AppTextStyles.bodyMd.copyWith(
                        fontWeight: FontWeight.w800,
                        fontSize: 15.sp,
                        color: AppColors.orange500,
                      ),
                    ),
                    const Spacer(),
                    GestureDetector(
                      onTap: pageCount == 0
                          ? widget.onAddGoal
                          : widget.onViewAll,
                      child: Row(
                        children: [
                          Text(
                            pageCount == 0 ? '추가하기' : '전체보기',
                            style: AppTextStyles.caption.copyWith(
                              color: colors.textTertiary,
                              fontWeight: FontWeight.w500,
                              fontSize: 11.sp,
                            ),
                          ),
                          Icon(
                            Icons.chevron_right,
                            size: 14.w,
                            color: colors.textTertiary,
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                SizedBox(height: 6.h),

                if (pageCount == 0)
                  _GoalEmptyBody(onTap: widget.onAddGoal)
                else
                  SizedBox(
                    height: 128.h,
                    child: PageView.builder(
                      itemCount: pageCount,
                      onPageChanged: (i) => setState(() => _currentPage = i),
                      physics: const BouncingScrollPhysics(),
                      itemBuilder: (context, index) {
                        return _GoalItem(goal: widget.goals[index]);
                      },
                    ),
                  ),
                // Page dots
                if (pageCount > 1) ...[
                  SizedBox(height: 6.h),
                  Center(
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: List.generate(pageCount, (i) {
                        return Container(
                          width: i == _currentPage ? 14.w : 5.w,
                          height: 5.w,
                          margin: EdgeInsets.symmetric(horizontal: 2.w),
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(3.r),
                            color: i == _currentPage
                                ? AppColors.orange500
                                : colors.borderDefault,
                          ),
                        );
                      }),
                    ),
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _GoalEmptyBody extends StatelessWidget {
  const _GoalEmptyBody({this.onTap});

  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);

    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: double.infinity,
        padding: EdgeInsets.symmetric(horizontal: 14.w, vertical: 18.h),
        decoration: BoxDecoration(
          color: AppColors.orange50.withValues(alpha: 0.45),
          borderRadius: BorderRadius.circular(16.r),
          border: Border.all(
            color: AppColors.orange300.withValues(alpha: 0.45),
          ),
        ),
        child: Column(
          children: [
            Container(
              width: 42.w,
              height: 42.w,
              decoration: BoxDecoration(
                color: AppColors.orange500,
                borderRadius: BorderRadius.circular(12.r),
              ),
              child: Icon(Icons.flag_outlined, color: Colors.white, size: 22.w),
            ),
            SizedBox(height: 10.h),
            Text(
              '아직 목표가 없어요',
              style: AppTextStyles.bodySm.copyWith(
                color: colors.textPrimary,
                fontWeight: FontWeight.w700,
              ),
            ),
            SizedBox(height: 4.h),
            Text(
              '첫 번째 저축 목표를 추가해주세요',
              style: AppTextStyles.caption.copyWith(
                color: colors.textTertiary,
                fontSize: 11.sp,
              ),
              textAlign: TextAlign.center,
            ),
            SizedBox(height: 10.h),
            Container(
              padding: EdgeInsets.symmetric(horizontal: 14.w, vertical: 7.h),
              decoration: BoxDecoration(
                color: colors.bgPrimary,
                borderRadius: BorderRadius.circular(999.r),
              ),
              child: Text(
                '목표 추가하기',
                style: AppTextStyles.caption.copyWith(
                  color: AppColors.orange500,
                  fontWeight: FontWeight.w700,
                  fontSize: 11.sp,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _GoalItem extends StatelessWidget {
  const _GoalItem({required this.goal});
  final Goal goal;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final overallProgress = goal.overallProgressRate.clamp(0.0, 1.0);
    final percentValue = overallProgress * 100;
    final percent = percentValue > 0 && percentValue < 1
        ? percentValue.toStringAsFixed(1)
        : percentValue.toStringAsFixed(0);
    final weeklySaved = goal.weeklySavedAmount;
    final weeklyTarget = goal.weeklyTargetAmount;
    final remainingWeeklyTarget = (weeklyTarget - weeklySaved).clamp(
      0,
      weeklyTarget,
    );
    final weeklyTargetMarkerProgress = goal.targetAmount > 0
        ? ((goal.currentAmount + remainingWeeklyTarget) / goal.targetAmount)
              .clamp(0.0, 1.0)
        : 0.0;

    return GestureDetector(
      onTap: () => context.push(AppRoutes.goalDetailLocation(goal.id)),
      behavior: HitTestBehavior.opaque,
      child: Column(
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Padding(
                padding: EdgeInsets.only(top: 2.h),
                child: GoalVideoThumb(
                  goalId: goal.id,
                  assetPath: goal.isAiPending || goal.isAiFailed
                      ? null
                      : goal.sketchVideoAssetPath,
                  iconName: goal.iconName,
                  progressRate: overallProgress,
                  size: 52.w,
                  borderRadius: 14.r,
                  forceLoading: goal.isAiPending,
                ),
              ),
              SizedBox(width: 14.w),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      goal.name,
                      style: AppTextStyles.bodyMd.copyWith(
                        fontWeight: FontWeight.w700,
                        color: colors.textPrimary,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    SizedBox(height: 2.h),
                    Text(
                      '이번 주 ${CurrencyFormatter.formatKRW(weeklySaved)} / ${CurrencyFormatter.formatKRW(weeklyTarget)}',
                      style: AppTextStyles.caption.copyWith(
                        color: colors.textSecondary,
                        fontSize: 11.sp,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    SizedBox(height: 2.h),
                    Text(
                      '전체 기간 ${CurrencyFormatter.formatKRW(goal.currentAmount)} / ${CurrencyFormatter.formatKRW(goal.targetAmount)}',
                      style: AppTextStyles.caption.copyWith(
                        color: colors.textTertiary,
                        fontSize: 11.sp,
                      ),
                    ),
                    if (goal.isAiPending || goal.isAiFailed) ...[
                      SizedBox(height: 3.h),
                      Text(
                        goal.isAiPending ? 'AI 영상 생성 중' : 'AI 영상 재시도 필요',
                        style: AppTextStyles.caption.copyWith(
                          color: goal.isAiPending
                              ? AppColors.orange500
                              : AppColors.red500,
                          fontWeight: FontWeight.w700,
                          fontSize: 10.sp,
                        ),
                      ),
                    ],
                    SizedBox(height: 8.h),
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        Expanded(
                          child: SizedBox(
                            height: 16.h,
                            child: Align(
                              alignment: Alignment.centerLeft,
                              child: _GoalProgressBar(
                                progress: overallProgress,
                                markerProgress: weeklyTargetMarkerProgress,
                              ),
                            ),
                          ),
                        ),
                        SizedBox(width: 10.w),
                        SizedBox(
                          width: 36.w,
                          height: 20.h,
                          child: Align(
                            alignment: Alignment.centerRight,
                            child: Padding(
                              padding: EdgeInsets.only(top: 3.h),
                              child: Text(
                                '$percent%',
                                style: AppTextStyles.bodySm.copyWith(
                                  color: AppColors.orange500,
                                  fontWeight: FontWeight.w800,
                                  fontSize: 12.sp,
                                  height: 1,
                                ),
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _GoalProgressBar extends StatelessWidget {
  const _GoalProgressBar({
    required this.progress,
    required this.markerProgress,
  });

  final double progress;
  final double markerProgress;

  @override
  Widget build(BuildContext context) {
    final normalizedProgress = progress.clamp(0.0, 1.0);
    final normalizedMarker = markerProgress.clamp(0.0, 1.0);

    return SizedBox(
      height: 6.h,
      child: LayoutBuilder(
        builder: (context, constraints) {
          final markerLeft = normalizedMarker <= 0
              ? 0.0
              : (constraints.maxWidth * normalizedMarker).clamp(
                  1.w,
                  constraints.maxWidth - 1.w,
                );
          final progressWidth = constraints.maxWidth * normalizedProgress;

          return Stack(
            clipBehavior: Clip.none,
            children: [
              Container(
                height: 6.h,
                decoration: BoxDecoration(
                  color: AppColors.orange500.withValues(alpha: 0.1),
                  borderRadius: BorderRadius.circular(3.r),
                ),
              ),
              if (normalizedProgress > 0)
                Positioned(
                  left: 0,
                  bottom: 0,
                  child: Container(
                    width: progressWidth.clamp(2.w, constraints.maxWidth),
                    height: 6.h,
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(3.r),
                      gradient: const LinearGradient(
                        colors: [AppColors.orange300, AppColors.orange500],
                      ),
                    ),
                  ),
                ),
              Positioned(
                left: markerLeft - 0.5.w,
                bottom: 0,
                child: Container(
                  width: 1.w,
                  height: 6.h,
                  decoration: BoxDecoration(
                    color: AppColors.orange500,
                    borderRadius: BorderRadius.circular(999.r),
                  ),
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}
