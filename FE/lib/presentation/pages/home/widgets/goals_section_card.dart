import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/widgets/common/goal_video_thumb.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/widgets/common/more_progress_bar.dart';

class GoalsSectionCard extends StatelessWidget {
  const GoalsSectionCard({
    super.key,
    required this.goals,
    required this.onViewAll,
  });

  /// 달성률 순 정렬된 상위 목표 리스트 (최대 3개)
  final List<Goal> goals;
  final VoidCallback onViewAll;

  @override
  Widget build(BuildContext context) {
    return MoreCard(
      padding: EdgeInsets.fromLTRB(22.w, 20.h, 22.w, 20.h),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // ── 헤더 ──
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              Text(
                '목표',
                style: AppTextStyles.headingMd.copyWith(
                  fontWeight: FontWeight.w800,
                ),
              ),
              GestureDetector(
                onTap: onViewAll,
                child: Row(
                  children: [
                    Text(
                      '전체보기',
                      style: AppTextStyles.bodySm.copyWith(
                        color: AppColors.textTertiary,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                    SizedBox(width: 2.w),
                    Icon(
                      Icons.chevron_right,
                      size: 16.w,
                      color: AppColors.textTertiary,
                    ),
                  ],
                ),
              ),
            ],
          ),

          SizedBox(height: 16.h),

          // ── 목표 리스트 ──
          for (int i = 0; i < goals.length; i++) ...[
            _GoalItem(key: ValueKey(goals[i].id), goal: goals[i]),
            if (i < goals.length - 1) ...[
              SizedBox(height: 12.h),
              Divider(
                height: 1.h,
                thickness: 1.h,
                color: AppColors.gray500.withValues(alpha: 0.15),
              ),
              SizedBox(height: 12.h),
            ],
          ],
        ],
      ),
    );
  }
}

class _GoalItem extends StatelessWidget {
  const _GoalItem({super.key, required this.goal});

  final Goal goal;

  @override
  Widget build(BuildContext context) {
    final percent = (goal.progressRate * 100).toStringAsFixed(0);
    final dDayText = goal.remainingDays >= 0
        ? 'D-${goal.remainingDays}'
        : 'D+${-goal.remainingDays}';

    return InkWell(
      onTap: () => context.push(AppRoutes.goalDetailLocation(goal.id)),
      borderRadius: BorderRadius.circular(10.r),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          // 목표 영상 썸네일
          GoalVideoThumb(
            assetPath: goal.sketchVideoAssetPath,
            iconName: goal.iconName,
            progressRate: goal.progressRate,
            size: 48.w,
            borderRadius: 14.r,
          ),

          SizedBox(width: 14.w),

          // 우측: 이름 / 프로그레스 / 금액
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // 목표명 + D-day + 달성률
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    Expanded(
                      child: Text(
                        goal.name,
                        style: AppTextStyles.bodyLg.copyWith(
                          fontWeight: FontWeight.w700,
                          color: AppColors.textPrimary,
                        ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    SizedBox(width: 8.w),
                    Row(
                      children: [
                        Text(
                          '$percent%',
                          style: AppTextStyles.bodySm.copyWith(
                            color: AppColors.orange500,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        SizedBox(width: 6.w),
                        Container(
                          padding: EdgeInsets.symmetric(
                            horizontal: 7.w,
                            vertical: 2.h,
                          ),
                          decoration: BoxDecoration(
                            color: AppColors.gray500.withValues(alpha: 0.12),
                            borderRadius: BorderRadius.circular(999.r),
                          ),
                          child: Text(
                            dDayText,
                            style: AppTextStyles.bodySm.copyWith(
                              color: AppColors.textTertiary,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),

                SizedBox(height: 8.h),

                // 프로그레스 바
                MoreProgressBar(
                  progress: goal.progressRate,
                  color: AppColors.orange500,
                  backgroundColor: AppColors.gray500.withValues(alpha: 0.15),
                  height: 6.h,
                ),

                SizedBox(height: 5.h),

                // 금액
                Text(
                  '${CurrencyFormatter.formatKRW(goal.currentAmount)} / ${CurrencyFormatter.formatKRW(goal.targetAmount)}',
                  style: AppTextStyles.bodySm.copyWith(
                    color: AppColors.textTertiary,
                    fontWeight: FontWeight.w500,
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
