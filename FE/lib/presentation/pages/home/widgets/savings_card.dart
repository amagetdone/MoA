import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/presentation/pages/home/widgets/spending_card.dart';

class SavingsCard extends StatelessWidget {
  const SavingsCard({
    super.key,
    required this.weeklySaved,
    required this.savingTargetAmount,
    required this.savingProgressRate,
  });

  final int weeklySaved;
  final int savingTargetAmount;
  final int savingProgressRate;

  @override
  Widget build(BuildContext context) {
    // 저축액 / 총 저축 목표 비율 → 아래에서 위로 초록 차오름
    final fillRatio = savingTargetAmount > 0
        ? (weeklySaved / savingTargetAmount).clamp(0.0, 1.0)
        : 0.0;

    return GaugeCard(
      fillRatio: fillRatio,
      fillColor: const Color(0xFF69BD5F).withValues(alpha: 0.15),
      fillFromTop: false,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '이번 주 저축',
                style: AppTextStyles.bodySm.copyWith(
                  color: AppColors.textSecondary,
                  fontWeight: FontWeight.w500,
                ),
              ),
              SizedBox(height: 6.h),
              FittedBox(
                fit: BoxFit.scaleDown,
                alignment: Alignment.centerLeft,
                child: Text(
                  CurrencyFormatter.formatKRW(weeklySaved),
                  style: AppTextStyles.headingMd.copyWith(
                    color: const Color(0xFF53A65A),
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
            ],
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '/ ${CurrencyFormatter.formatKRW(savingTargetAmount)}',
                style: TextStyle(
                  color: AppColors.textTertiary,
                  fontSize: 11.sp,
                ),
              ),
              SizedBox(height: 2.h),
              Text(
                '$savingProgressRate% 달성',
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
