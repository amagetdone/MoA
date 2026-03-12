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

    return LayoutBuilder(
      builder: (context, constraints) {
        final isCompact = constraints.maxWidth < 360;

        return MoreCard(
          color: const Color(0xFFF7ECDD),
          padding: EdgeInsets.fromLTRB(22.w, 22.h, 22.w, 24.h),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _HomeGlyph(
                    icon: Icons.account_balance_wallet_outlined,
                    backgroundColor: const Color(0xFFE8AF74),
                    iconColor: AppColors.textPrimary,
                  ),
                  SizedBox(width: 14.w),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
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
                        SizedBox(height: 4.h),
                        Text(
                          '이번 주 목표 달성을 위해',
                          style: AppTextStyles.bodySm.copyWith(
                            color: AppColors.textSecondary,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              SizedBox(height: 18.h),
              if (isCompact) ...[
                _AmountSummary(
                  alignment: Alignment.centerLeft,
                  child: Text.rich(
                    TextSpan(
                      children: [
                        TextSpan(
                          text: CurrencyFormatter.formatKRW(weeklySpent),
                          style: AppTextStyles.headingLg.copyWith(
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                        TextSpan(
                          text:
                              ' / ${CurrencyFormatter.formatKRW(weeklyBudget)}',
                          style: AppTextStyles.bodyLg.copyWith(
                            color: AppColors.textTertiary,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                SizedBox(height: 8.h),
                _AmountSummary(
                  alignment: Alignment.centerLeft,
                  child: Text(
                    '${CurrencyFormatter.formatKRW(remaining)} 남음',
                    style: AppTextStyles.headingMd.copyWith(
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ),
              ] else
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: _AmountSummary(
                        alignment: Alignment.centerLeft,
                        child: Text.rich(
                          TextSpan(
                            children: [
                              TextSpan(
                                text: CurrencyFormatter.formatKRW(weeklySpent),
                                style: AppTextStyles.headingLg.copyWith(
                                  fontWeight: FontWeight.w800,
                                ),
                              ),
                              TextSpan(
                                text:
                                    ' / ${CurrencyFormatter.formatKRW(weeklyBudget)}',
                                style: AppTextStyles.bodyLg.copyWith(
                                  color: AppColors.textTertiary,
                                  fontWeight: FontWeight.w500,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
                    SizedBox(width: 16.w),
                    Expanded(
                      child: _AmountSummary(
                        alignment: Alignment.centerRight,
                        child: Text(
                          '${CurrencyFormatter.formatKRW(remaining)} 남음',
                          style: AppTextStyles.headingMd.copyWith(
                            fontWeight: FontWeight.w800,
                          ),
                          textAlign: TextAlign.right,
                        ),
                      ),
                    ),
                  ],
                ),
              SizedBox(height: 16.h),
              Container(
                height: 1.h,
                color: AppColors.gray500.withValues(alpha: 0.4),
              ),
              SizedBox(height: 18.h),
              Center(
                child: Column(
                  children: [
                    Text(
                      '금일 사용 가능한 금액 (남은 금액 ÷ $daysLeft일)',
                      textAlign: TextAlign.center,
                      style: AppTextStyles.bodySm.copyWith(
                        color: AppColors.textTertiary,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                    SizedBox(height: 8.h),
                    FittedBox(
                      fit: BoxFit.scaleDown,
                      child: Text(
                        CurrencyFormatter.formatKRW(dailyAvailable),
                        style: AppTextStyles.headingXl.copyWith(
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
        );
      },
    );
  }

  int get _remainingWeekDays {
    final weekday = DateTime.now().weekday;
    final daysUntilSunday = DateTime.sunday - weekday;
    return math.max(daysUntilSunday, 1);
  }
}

class _AmountSummary extends StatelessWidget {
  const _AmountSummary({required this.alignment, required this.child});

  final Alignment alignment;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return FittedBox(fit: BoxFit.scaleDown, alignment: alignment, child: child);
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
