import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';

class SpendingCard extends StatelessWidget {
  const SpendingCard({
    super.key,
    required this.totalAmount,
    required this.usedAmount,
    required this.remainingAmount,
    required this.daysRemaining,
    required this.dailyAvailable,
  });

  final int totalAmount;
  final int usedAmount;
  final int remainingAmount;
  final int daysRemaining;
  final int dailyAvailable;

  @override
  Widget build(BuildContext context) {
    final progress = totalAmount > 0
        ? (usedAmount / totalAmount).clamp(0.0, 1.0)
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
                        _DayBadge(daysRemaining: daysRemaining),
                      ],
                    ),
                    SizedBox(height: 4.h),
                    FittedBox(
                      fit: BoxFit.scaleDown,
                      alignment: Alignment.centerLeft,
                      child: Text(
                        CurrencyFormatter.formatKRW(remainingAmount),
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

          // ── 프로그레스 바 (왼쪽=사용(회색), 오른쪽=남은(오렌지)) ──
          ClipRRect(
            borderRadius: BorderRadius.circular(999.r),
            child: LinearProgressIndicator(
              value: progress,
              minHeight: 7.h,
              backgroundColor: AppColors.orange500,
              valueColor: const AlwaysStoppedAnimation<Color>(
                AppColors.gray100,
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
                      text: CurrencyFormatter.formatKRW(usedAmount),
                      style: AppTextStyles.bodySm.copyWith(
                        fontWeight: FontWeight.w700,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    TextSpan(
                      text: ' / ${CurrencyFormatter.formatKRW(totalAmount)}',
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
}

class _DayBadge extends StatelessWidget {
  const _DayBadge({required this.daysRemaining});

  final int daysRemaining;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.symmetric(horizontal: 10.w, vertical: 3.h),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(999.r),
        color: AppColors.orange500.withValues(alpha: 0.12),
      ),
      child: Text(
        'D-$daysRemaining',
        style: AppTextStyles.caption.copyWith(
          color: AppColors.orange500,
          fontWeight: FontWeight.w700,
          fontSize: 11.sp,
        ),
      ),
    );
  }
}
