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
    final fillRatio = savingTargetAmount > 0
        ? (weeklySaved / savingTargetAmount).clamp(0.0, 1.0)
        : 0.0;

    return GaugeCard(
      fillRatio: fillRatio,
      fillColor: const Color(0xFF69BD5F).withValues(alpha: 0.15),
      fillFromTop: false,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Text(
            '이번 주 저축',
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
              CurrencyFormatter.formatKRW(weeklySaved),
              style: AppTextStyles.headingMd.copyWith(
                color: const Color(0xFF53A65A),
                fontWeight: FontWeight.w800,
              ),
            ),
          ),
          SizedBox(height: 4.h),
          Text(
            '/ ${CurrencyFormatter.formatKRW(savingTargetAmount)}',
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
