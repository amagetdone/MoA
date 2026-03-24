import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';

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
    final colors = AppColors.of(context);
    final remainRatio = totalAmount > 0
        ? (remainingAmount / totalAmount).clamp(0.0, 1.0)
        : 0.0;

    return GaugeCard(
      fillRatio: remainRatio,
      fillColor: AppColors.orange500.withValues(alpha: 0.15),
      fillFromTop: false,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Text(
            '여유자금',
            style: AppTextStyles.bodySm.copyWith(
              color: colors.textSecondary,
              fontWeight: FontWeight.w500,
            ),
          ),
          SizedBox(height: 8.h),
          FittedBox(
            fit: BoxFit.scaleDown,
            alignment: Alignment.centerLeft,
            child: Text(
              CurrencyFormatter.formatKRW(remainingAmount),
              style: AppTextStyles.headingMd.copyWith(
                color: AppColors.orange500,
                fontWeight: FontWeight.w800,
              ),
            ),
          ),
          SizedBox(height: 4.h),
          Text(
            '/ ${CurrencyFormatter.formatKRW(totalAmount)}',
            style: TextStyle(
              color: colors.textTertiary,
              fontSize: 11.sp,
            ),
          ),
        ],
      ),
    );
  }
}

/// 카드 배경을 게이지처럼 채우는 공용 위젯
class GaugeCard extends StatelessWidget {
  const GaugeCard({
    super.key,
    required this.fillRatio,
    required this.fillColor,
    required this.child,
    this.fillFromTop = false,
  });

  final double fillRatio;
  final Color fillColor;
  final Widget child;
  final bool fillFromTop;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return Container(
      decoration: BoxDecoration(
        color: colors.bgSurface,
        borderRadius: BorderRadius.circular(16.r),
        boxShadow: [
          BoxShadow(
            color: colors.cardShadow,
            offset: Offset(0, 2.h),
            blurRadius: 8.r,
          ),
        ],
      ),
      clipBehavior: Clip.antiAlias,
      child: Stack(
        children: [
          Positioned.fill(
            child: Align(
              alignment: fillFromTop
                  ? Alignment.topCenter
                  : Alignment.bottomCenter,
              child: FractionallySizedBox(
                heightFactor: fillRatio,
                widthFactor: 1.0,
                child: Container(
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      begin: fillFromTop ? Alignment.bottomCenter : Alignment.topCenter,
                      end: fillFromTop ? Alignment.topCenter : Alignment.bottomCenter,
                      colors: [
                        fillColor.withValues(alpha: 0.0),
                        fillColor,
                      ],
                      stops: const [0.0, 0.3],
                    ),
                  ),
                ),
              ),
            ),
          ),
          Padding(
            padding: EdgeInsets.all(14.w),
            child: child,
          ),
        ],
      ),
    );
  }
}
