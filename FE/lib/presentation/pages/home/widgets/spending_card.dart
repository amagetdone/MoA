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
    // 남은 비율 → 오렌지 배경이 위에서 아래로 줄어듦
    final remainRatio = totalAmount > 0
        ? (remainingAmount / totalAmount).clamp(0.0, 1.0)
        : 0.0;

    return GaugeCard(
      fillRatio: remainRatio,
      fillColor: AppColors.orange500.withValues(alpha: 0.15),
      fillFromTop: false,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Text(
                    '여유자금',
                    style: AppTextStyles.bodySm.copyWith(
                      color: AppColors.textSecondary,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  SizedBox(width: 4.w),
                  Container(
                    padding: EdgeInsets.symmetric(horizontal: 6.w, vertical: 1.h),
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(999.r),
                      color: AppColors.orange500.withValues(alpha: 0.15),
                    ),
                    child: Text(
                      'D-$daysRemaining',
                      style: TextStyle(
                        color: AppColors.orange500,
                        fontWeight: FontWeight.w700,
                        fontSize: 9.sp,
                      ),
                    ),
                  ),
                ],
              ),
              SizedBox(height: 6.h),
              FittedBox(
                fit: BoxFit.scaleDown,
                alignment: Alignment.centerLeft,
                child: Row(
                  children: [
                    Text(
                      CurrencyFormatter.formatKRW(remainingAmount),
                      style: AppTextStyles.headingMd.copyWith(
                        color: AppColors.orange500,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    SizedBox(width: 4.w),
                    Text(
                      '남음',
                      style: TextStyle(
                        color: AppColors.textTertiary,
                        fontSize: 11.sp,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '/ ${CurrencyFormatter.formatKRW(totalAmount)} (${CurrencyFormatter.formatKRW(usedAmount)} 사용)',
                style: TextStyle(
                  color: AppColors.textTertiary,
                  fontSize: 10.sp,
                ),
              ),
              SizedBox(height: 2.h),
              Text(
                '하루 ${CurrencyFormatter.formatKRW(dailyAvailable)} 사용 가능',
                style: TextStyle(
                  color: AppColors.textTertiary,
                  fontSize: 10.sp,
                ),
              ),
            ],
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
  /// true: 위에서 아래로 채움 (소비 → 줄어듦), false: 아래에서 위로 채움 (저축 → 차오름)
  final bool fillFromTop;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.bgSurface,
        borderRadius: BorderRadius.circular(16.r),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.06),
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
