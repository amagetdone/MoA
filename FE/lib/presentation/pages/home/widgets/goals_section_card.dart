import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/widgets/common/goal_video_thumb.dart';

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
    if (goals.isEmpty) return const SizedBox.shrink();

    final goal = goals.first;
    final colors = AppColors.of(context);
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final percent = (goal.progressRate * 100).toStringAsFixed(0);
    final dDayText = goal.remainingDays >= 0
        ? 'D-${goal.remainingDays}'
        : 'D+${-goal.remainingDays}';

    return GestureDetector(
      onTap: () => context.push(AppRoutes.goalDetailLocation(goal.id)),
      child: Container(
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
                        '내 목표',
                        style: AppTextStyles.bodyMd.copyWith(
                          fontWeight: FontWeight.w700,
                          color: colors.textPrimary,
                        ),
                      ),
                      const Spacer(),
                      GestureDetector(
                        onTap: onViewAll,
                        child: Row(
                          children: [
                            Text(
                              '전체보기',
                              style: AppTextStyles.caption.copyWith(
                                color: colors.textTertiary,
                                fontWeight: FontWeight.w500,
                                fontSize: 11.sp,
                              ),
                            ),
                            Icon(Icons.chevron_right, size: 14.w, color: colors.textTertiary),
                          ],
                        ),
                      ),
                    ],
                  ),
                  SizedBox(height: 6.h),

                  // Goal info
                  Row(
                    children: [
                      GoalVideoThumb(
                        goalId: goal.id,
                        assetPath: goal.sketchVideoAssetPath,
                        iconName: goal.iconName,
                        progressRate: goal.progressRate,
                        size: 52.w,
                        borderRadius: 16.r,
                      ),
                      SizedBox(width: 14.w),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Expanded(
                                  child: Text(
                                    goal.name,
                                    style: AppTextStyles.bodyMd.copyWith(
                                      fontWeight: FontWeight.w700,
                                      color: colors.textPrimary,
                                    ),
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                  ),
                                ),
                                Container(
                                  padding: EdgeInsets.symmetric(horizontal: 8.w, vertical: 3.h),
                                  decoration: BoxDecoration(
                                    color: colors.textPrimary.withValues(alpha: 0.06),
                                    borderRadius: BorderRadius.circular(999.r),
                                  ),
                                  child: Text(
                                    dDayText,
                                    style: AppTextStyles.caption.copyWith(
                                      color: colors.textSecondary,
                                      fontWeight: FontWeight.w600,
                                      fontSize: 10.sp,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                            SizedBox(height: 2.h),
                            Text(
                              '${CurrencyFormatter.formatKRW(goal.currentAmount)} / ${CurrencyFormatter.formatKRW(goal.targetAmount)}',
                              style: AppTextStyles.caption.copyWith(
                                color: colors.textTertiary,
                                fontSize: 11.sp,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  SizedBox(height: 6.h),

                  // Progress bar + percent
                  Row(
                    children: [
                      Expanded(
                        child: Stack(
                          children: [
                            Container(
                              height: 6.h,
                              decoration: BoxDecoration(
                                color: AppColors.orange500.withValues(alpha: 0.1),
                                borderRadius: BorderRadius.circular(3.r),
                              ),
                            ),
                            FractionallySizedBox(
                              widthFactor: goal.progressRate.clamp(0.0, 1.0),
                              child: Container(
                                height: 6.h,
                                decoration: BoxDecoration(
                                  borderRadius: BorderRadius.circular(3.r),
                                  gradient: const LinearGradient(
                                    colors: [AppColors.orange300, AppColors.orange500],
                                  ),
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                      SizedBox(width: 10.w),
                      Text(
                        '$percent%',
                        style: AppTextStyles.bodySm.copyWith(
                          color: AppColors.orange500,
                          fontWeight: FontWeight.w800,
                          fontSize: 12.sp,
                        ),
                      ),
                    ],
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
