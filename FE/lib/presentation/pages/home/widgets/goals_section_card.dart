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
  });

  final List<Goal> goals;
  final VoidCallback onViewAll;

  @override
  State<GoalsSectionCard> createState() => _GoalsSectionCardState();
}

class _GoalsSectionCardState extends State<GoalsSectionCard> {
  int _currentPage = 0;

  @override
  Widget build(BuildContext context) {
    if (widget.goals.isEmpty) return const SizedBox.shrink();

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
                      '목표',
                      style: AppTextStyles.bodyMd.copyWith(
                        fontWeight: FontWeight.w700,
                        color: colors.textPrimary,
                      ),
                    ),
                    const Spacer(),
                    GestureDetector(
                      onTap: widget.onViewAll,
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

                // PageView for goals
                SizedBox(
                  height: 90.h,
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

class _GoalItem extends StatelessWidget {
  const _GoalItem({required this.goal});
  final Goal goal;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final percent = (goal.progressRate * 100).toStringAsFixed(0);

    return GestureDetector(
      onTap: () => context.push(AppRoutes.goalDetailLocation(goal.id)),
      behavior: HitTestBehavior.opaque,
      child: Column(
        children: [
          // Goal info
          Row(
            children: [
              GoalVideoThumb(
                goalId: goal.id,
                assetPath: goal.isAiPending || goal.isAiFailed
                    ? null
                    : goal.sketchVideoAssetPath,
                iconName: goal.iconName,
                progressRate: goal.progressRate,
                size: 48.w,
                borderRadius: 14.r,
                forceLoading: goal.isAiPending,
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
                      '${CurrencyFormatter.formatKRW(goal.currentAmount)} / ${CurrencyFormatter.formatKRW(goal.targetAmount)}',
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
                  ],
                ),
              ),
            ],
          ),
          SizedBox(height: 8.h),

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
    );
  }
}
