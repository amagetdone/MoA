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
      color: const Color(0xFFF7ECDD),
      padding: EdgeInsets.fromLTRB(22.w, 22.h, 22.w, 24.h),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // ── 헤더 ──
          Row(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              _HomeGlyph(
                icon: Icons.account_balance_wallet_outlined,
                backgroundColor: const Color(0xFFE8AF74),
                iconColor: AppColors.textPrimary,
              ),
              SizedBox(width: 14.w),
              Wrap(
                spacing: 10.w,
                runSpacing: 6.h,
                crossAxisAlignment: WrapCrossAlignment.center,
                children: [
                  Text(
                    '자유소비',
                    style: AppTextStyles.headingMd.copyWith(
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  _DayBadge(daysLeft: daysLeft),
                ],
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
              backgroundColor: AppColors.gray500.withValues(alpha: 0.2),
              valueColor: AlwaysStoppedAnimation<Color>(
                progress >= 0.9
                    ? Colors.redAccent
                    : const Color(0xFFD98235),
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
                '${CurrencyFormatter.formatKRW(remaining)} 남음',
                style: AppTextStyles.bodySm.copyWith(
                  color: AppColors.textTertiary,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ],
          ),

          SizedBox(height: 16.h),

          // ── 구분선 ──
          Container(
            height: 1.h,
            color: AppColors.gray500.withValues(alpha: 0.4),
          ),

          SizedBox(height: 14.h),

          // ── 오늘 사용 가능 금액 ──
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.baseline,
            textBaseline: TextBaseline.alphabetic,
            children: [
              Text(
                '오늘 사용 가능한 금액',
                style: AppTextStyles.bodySm.copyWith(
                  color: AppColors.textTertiary,
                  fontWeight: FontWeight.w500,
                ),
              ),
              FittedBox(
                fit: BoxFit.scaleDown,
                child: Text(
                  CurrencyFormatter.formatKRW(dailyAvailable),
                  style: AppTextStyles.headingMd.copyWith(
                    color: AppColors.orange500,
                    fontWeight: FontWeight.w800,
                  ),
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

class _HomeGlyph extends StatelessWidget {
  const _HomeGlyph({
    required this.icon,
    required this.backgroundColor,
    required this.iconColor,
  });

  final IconData icon;
  final Color backgroundColor;
  final Color iconColor;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 46.w,
      height: 46.w,
      decoration: BoxDecoration(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(16.r),
      ),
      child: Icon(icon, color: iconColor, size: 24.w),
    );
  }
}

class _DayBadge extends StatelessWidget {
  const _DayBadge({required this.daysLeft});

  final int daysLeft;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.symmetric(horizontal: 12.w, vertical: 4.h),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(999.r),
        gradient: const LinearGradient(
          colors: [Color(0xFFF4C596), Color(0xFFD98235)],
        ),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFFE08D43).withValues(alpha: 0.45),
            blurRadius: 12.r,
            offset: Offset(0, 4.h),
          ),
        ],
      ),
      child: Text(
        'D-$daysLeft',
        style: AppTextStyles.bodySm.copyWith(
          color: Colors.white,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}
