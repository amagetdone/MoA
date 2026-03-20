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

  final List<Goal> goals;
  final VoidCallback onViewAll;

  @override
  Widget build(BuildContext context) {
    return MoreCard(
      padding: EdgeInsets.all(16.w),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                '목표',
                style: AppTextStyles.bodyMd.copyWith(
                  fontWeight: FontWeight.w800,
                ),
              ),
              GestureDetector(
                onTap: onViewAll,
                child: Row(
                  children: [
                    Text(
                      '전체보기',
                      style: AppTextStyles.caption.copyWith(
                        color: AppColors.textTertiary,
                        fontWeight: FontWeight.w500,
                        fontSize: 11.sp,
                      ),
                    ),
                    Icon(
                      Icons.chevron_right,
                      size: 14.w,
                      color: AppColors.textTertiary,
                    ),
                  ],
                ),
              ),
            ],
          ),
          SizedBox(height: 12.h),
          for (int i = 0; i < goals.length; i++) ...[
            _GoalItem(key: ValueKey(goals[i].id), goal: goals[i]),
            if (i < goals.length - 1) ...[
              SizedBox(height: 8.h),
              Divider(
                height: 1.h,
                thickness: 1.h,
                color: AppColors.gray500.withValues(alpha: 0.1),
              ),
              SizedBox(height: 8.h),
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
      borderRadius: BorderRadius.circular(8.r),
      child: Row(
        children: [
          GoalVideoThumb(
            goalId: goal.id,
            assetPath: goal.sketchVideoAssetPath,
            iconName: goal.iconName,
            progressRate: goal.progressRate,
            size: 40.w,
            borderRadius: 12.r,
          ),
          SizedBox(width: 12.w),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        goal.name,
                        style: AppTextStyles.bodySm.copyWith(
                          fontWeight: FontWeight.w700,
                          color: AppColors.textPrimary,
                        ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    SizedBox(width: 6.w),
                    Text(
                      '$percent%',
                      style: AppTextStyles.caption.copyWith(
                        color: AppColors.orange500,
                        fontWeight: FontWeight.w700,
                        fontSize: 11.sp,
                      ),
                    ),
                    SizedBox(width: 4.w),
                    Container(
                      padding: EdgeInsets.symmetric(horizontal: 6.w, vertical: 1.h),
                      decoration: BoxDecoration(
                        color: AppColors.gray500.withValues(alpha: 0.1),
                        borderRadius: BorderRadius.circular(999.r),
                      ),
                      child: Text(
                        dDayText,
                        style: AppTextStyles.caption.copyWith(
                          color: AppColors.textTertiary,
                          fontWeight: FontWeight.w600,
                          fontSize: 10.sp,
                        ),
                      ),
                    ),
                  ],
                ),
                SizedBox(height: 6.h),
                MoreProgressBar(
                  progress: goal.progressRate,
                  color: AppColors.orange500,
                  backgroundColor: AppColors.gray500.withValues(alpha: 0.12),
                  height: 4.h,
                ),
                SizedBox(height: 4.h),
                Text(
                  '${CurrencyFormatter.formatKRW(goal.currentAmount)} / ${CurrencyFormatter.formatKRW(goal.targetAmount)}',
                  style: AppTextStyles.caption.copyWith(
                    color: AppColors.textTertiary,
                    fontSize: 10.sp,
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
