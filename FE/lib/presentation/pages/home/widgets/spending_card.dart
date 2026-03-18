import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';

class SpendingCard extends StatelessWidget {
  const SpendingCard({
    super.key,
    required this.weeklyBudget,
    required this.weeklySpent,
  });

  final int weeklyBudget;
  final int weeklySpent;

  @override
  Widget build(BuildContext context) {
    final remaining = math.max(weeklyBudget - weeklySpent, 0);
    final daysLeft = _remainingWeekDays;
    final dailyAvailable = remaining > 0 ? remaining ~/ daysLeft : 0;
    final progress = weeklyBudget > 0
        ? (weeklySpent / weeklyBudget).clamp(0.0, 1.0)
        : 0.0;

    return MoreCard(
      padding: EdgeInsets.fromLTRB(22.w, 22.h, 22.w, 22.h),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // ── 헤더 ──
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 46.w,
                height: 46.w,
                decoration: BoxDecoration(
                  color: const Color(0xFFFFF3E0),
                  borderRadius: BorderRadius.circular(16.r),
                ),
                child: Icon(
                  Icons.account_balance_wallet_outlined,
                  color: AppColors.orange500,
                  size: 24.w,
                ),
              ),
              SizedBox(width: 14.w),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Text(
                          '여유자금',
                          style: AppTextStyles.bodyLg.copyWith(
                            color: AppColors.textSecondary,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                        SizedBox(width: 8.w),
                        _DayBadge(daysLeft: daysLeft),
                      ],
                    ),
                    SizedBox(height: 4.h),
                    FittedBox(
                      fit: BoxFit.scaleDown,
                      alignment: Alignment.centerLeft,
                      child: Text(
                        CurrencyFormatter.formatKRW(remaining),
                        style: AppTextStyles.headingLg.copyWith(
                          color: AppColors.orange500,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),

          SizedBox(height: 18.h),

          // ── 프로그레스 바 ──
          ClipRRect(
            borderRadius: BorderRadius.circular(999.r),
            child: LinearProgressIndicator(
              value: progress,
              minHeight: 7.h,
              backgroundColor: AppColors.gray100,
              valueColor: AlwaysStoppedAnimation<Color>(
                progress >= 0.9
                    ? Colors.redAccent
                    : AppColors.orange500,
              ),
            ),
          ),

          SizedBox(height: 10.h),

          // ── 예산 현황 ──
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              RichText(
                text: TextSpan(
                  children: [
                    TextSpan(
                      text: CurrencyFormatter.formatKRW(weeklySpent),
                      style: AppTextStyles.bodySm.copyWith(
                        fontWeight: FontWeight.w700,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    TextSpan(
                      text: ' / ${CurrencyFormatter.formatKRW(weeklyBudget)}',
                      style: AppTextStyles.bodySm.copyWith(
                        color: AppColors.textTertiary,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ],
                ),
              ),
              Text(
                '오늘 ${CurrencyFormatter.formatKRW(dailyAvailable)}',
                style: AppTextStyles.bodySm.copyWith(
                  color: AppColors.textTertiary,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  int get _remainingWeekDays {
    final weekday = DateTime.now().weekday;
    final daysUntilSunday = DateTime.sunday - weekday;
    return math.max(daysUntilSunday, 1);
  }
}

class _DayBadge extends StatelessWidget {
  const _DayBadge({required this.daysLeft});

  final int daysLeft;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.symmetric(horizontal: 10.w, vertical: 3.h),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(999.r),
        color: AppColors.orange500.withValues(alpha: 0.12),
      ),
      child: Text(
        'D-$daysLeft',
        style: AppTextStyles.caption.copyWith(
          color: AppColors.orange500,
          fontWeight: FontWeight.w700,
          fontSize: 11.sp,
        ),
      ),
    );
  }
}
