import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/widgets/common/more_progress_bar.dart';

class GoalSummaryCard extends StatelessWidget {
  const GoalSummaryCard({super.key, required this.goal, this.onViewAll});

  final Goal goal;
  final VoidCallback? onViewAll;

  @override
  Widget build(BuildContext context) {
    final dDayText = goal.remainingDays >= 0
        ? 'D-${goal.remainingDays}'
        : 'D+${-goal.remainingDays}';
    final weekText = '${_currentWeek(goal)}주차 ($dDayText)';
    final percentText = (goal.progressRate * 100).toStringAsFixed(1);

    return LayoutBuilder(
      builder: (context, constraints) {
        final isCompact = constraints.maxWidth < 360;

        return MoreCard(
          onTap: () => context.push(AppRoutes.goalDetailLocation(goal.id)),
          padding: EdgeInsets.fromLTRB(22.w, 22.h, 22.w, 20.h),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (isCompact)
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        _GoalHeaderIcon(),
                        SizedBox(width: 14.w),
                        Expanded(child: _GoalHeaderText(goalName: goal.name)),
                      ],
                    ),
                    SizedBox(height: 10.h),
                    Align(
                      alignment: Alignment.centerRight,
                      child: _ViewAllButton(onPressed: onViewAll),
                    ),
                  ],
                )
              else
                Row(
                  children: [
                    _GoalHeaderIcon(),
                    SizedBox(width: 14.w),
                    Expanded(child: _GoalHeaderText(goalName: goal.name)),
                    _ViewAllButton(onPressed: onViewAll),
                  ],
                ),
              SizedBox(height: 18.h),
              if (isCompact)
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Center(
                      child: _GoalIllustration(
                        assetPath: _illustrationAssetPath,
                      ),
                    ),
                    SizedBox(height: 16.h),
                    _GoalProgressContent(
                      weekText: weekText,
                      amountText:
                          '${CurrencyFormatter.formatKRW(goal.currentAmount)} / ${CurrencyFormatter.formatKRW(goal.targetAmount)} ($percentText%)',
                      progressRate: goal.progressRate,
                    ),
                  ],
                )
              else
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _GoalIllustration(assetPath: _illustrationAssetPath),
                    SizedBox(width: 18.w),
                    Expanded(
                      child: _GoalProgressContent(
                        weekText: weekText,
                        amountText:
                            '${CurrencyFormatter.formatKRW(goal.currentAmount)} / ${CurrencyFormatter.formatKRW(goal.targetAmount)} ($percentText%)',
                        progressRate: goal.progressRate,
                      ),
                    ),
                  ],
                ),
            ],
          ),
        );
      },
    );
  }

  int _currentWeek(Goal goal) {
    final now = DateTime.now();
    final elapsedDays = now.difference(goal.startDate).inDays;
    return math.max(1, (elapsedDays ~/ 7) + 1);
  }

  String get _illustrationAssetPath {
    switch (goal.iconName) {
      case 'travel':
        return 'assets/images/onboarding1.png';
      case 'laptop':
      case 'electronics':
        return 'assets/images/onboarding3.png';
      default:
        return 'assets/images/onboarding2.png';
    }
  }
}

class _GoalHeaderIcon extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(
      width: 42.w,
      height: 42.w,
      decoration: BoxDecoration(
        color: const Color(0xFFF5F5F5),
        borderRadius: BorderRadius.circular(14.r),
      ),
      child: Icon(
        Icons.wb_sunny_outlined,
        color: AppColors.textPrimary,
        size: 22.w,
      ),
    );
  }
}

class _GoalHeaderText extends StatelessWidget {
  const _GoalHeaderText({required this.goalName});

  final String goalName;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '목표',
          style: AppTextStyles.headingMd.copyWith(fontWeight: FontWeight.w800),
        ),
        Text(
          goalName,
          style: AppTextStyles.bodyLg.copyWith(
            color: AppColors.textSecondary,
            fontWeight: FontWeight.w500,
          ),
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
      ],
    );
  }
}

class _ViewAllButton extends StatelessWidget {
  const _ViewAllButton({required this.onPressed});

  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    return TextButton(
      onPressed: onPressed ?? () => context.go(AppRoutes.goalTab),
      style: TextButton.styleFrom(
        padding: EdgeInsets.symmetric(horizontal: 8.w, vertical: 4.h),
        minimumSize: Size.zero,
        tapTargetSize: MaterialTapTargetSize.shrinkWrap,
      ),
      child: Text(
        '전체보기',
        style: AppTextStyles.bodyLg.copyWith(
          color: AppColors.textSecondary,
          fontWeight: FontWeight.w500,
        ),
      ),
    );
  }
}

class _GoalIllustration extends StatelessWidget {
  const _GoalIllustration({required this.assetPath});

  final String assetPath;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 104.w,
      height: 104.w,
      padding: EdgeInsets.all(10.w),
      decoration: BoxDecoration(
        color: const Color(0xFFF7F7F7),
        borderRadius: BorderRadius.circular(20.r),
        border: Border.all(color: AppColors.borderDefault),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.04),
            blurRadius: 16.r,
            offset: Offset(0, 4.h),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(14.r),
        child: DecoratedBox(
          decoration: BoxDecoration(
            gradient: const LinearGradient(
              begin: Alignment.topCenter,
              end: Alignment.bottomCenter,
              colors: [Color(0xFFFFF2E0), Color(0xFFF6FAEF)],
            ),
            borderRadius: BorderRadius.circular(14.r),
          ),
          child: Image.asset(assetPath, fit: BoxFit.cover),
        ),
      ),
    );
  }
}

class _GoalProgressContent extends StatelessWidget {
  const _GoalProgressContent({
    required this.weekText,
    required this.amountText,
    required this.progressRate,
  });

  final String weekText;
  final String amountText;
  final double progressRate;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          weekText,
          style: AppTextStyles.bodyLg.copyWith(
            color: AppColors.textSecondary,
            fontWeight: FontWeight.w600,
          ),
        ),
        SizedBox(height: 4.h),
        FittedBox(
          fit: BoxFit.scaleDown,
          alignment: Alignment.centerLeft,
          child: Text(
            amountText,
            style: AppTextStyles.bodyLg.copyWith(
              color: AppColors.textTertiary,
              fontWeight: FontWeight.w500,
            ),
          ),
        ),
        SizedBox(height: 12.h),
        MoreProgressBar(
          progress: progressRate,
          color: AppColors.orange500,
          backgroundColor: AppColors.gray100.withValues(alpha: 0.95),
          height: 14.h,
        ),
      ],
    );
  }
}
