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
    final colors = AppColors.of(context);
    final isEmpty = savingTargetAmount <= 0;
    final fillRatio = savingTargetAmount > 0
        ? (weeklySaved / savingTargetAmount).clamp(0.0, 1.0)
        : 0.0;
    final todayTarget = DateTime.now().weekday / 7;
    // 저축: 구분선보다 높거나 같으면 초록, 낮으면 빨간
    final isOnTrack = fillRatio >= todayTarget;
    final amountColor = isOnTrack ? const Color(0xFF53A65A) : AppColors.red500;

    return GaugeCard(
      fillRatio: fillRatio,
      fillColor: const Color(0xFF69BD5F).withValues(alpha: 0.15),
      fillFromTop: false,
      todayLineColor: amountColor,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Row(
            children: [
              Icon(Icons.savings_outlined, size: 14.w, color: amountColor),
              SizedBox(width: 4.w),
              Text(
                '저축',
                style: AppTextStyles.caption.copyWith(
                  color: colors.textSecondary,
                  fontWeight: FontWeight.w600,
                  fontSize: 11.sp,
                ),
              ),
            ],
          ),
          SizedBox(height: 10.h),
          FittedBox(
            fit: BoxFit.scaleDown,
            alignment: Alignment.centerLeft,
            child: Text(
              CurrencyFormatter.formatKRW(weeklySaved),
              style: AppTextStyles.headingMd.copyWith(
                color: amountColor,
                fontWeight: FontWeight.w800,
                fontSize: 17.sp,
              ),
            ),
          ),
          SizedBox(height: 6.h),
          Text(
            isEmpty
                ? '저축 목표를 추가해주세요'
                : '/ ${CurrencyFormatter.formatKRW(savingTargetAmount)}',
            style: TextStyle(
              color: isEmpty ? AppColors.orange500 : colors.textTertiary,
              fontSize: 10.sp,
              fontWeight: isEmpty ? FontWeight.w700 : FontWeight.w400,
            ),
          ),
        ],
      ),
    );
  }
}
