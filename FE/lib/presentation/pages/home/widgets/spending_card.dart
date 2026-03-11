import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/widgets/common/more_progress_bar.dart';

class SpendingCard extends StatelessWidget {
  final int weeklyBudget;
  final int weeklySpent;

  const SpendingCard({
    super.key,
    required this.weeklyBudget,
    required this.weeklySpent,
  });

  @override
  Widget build(BuildContext context) {
    final remaining = weeklyBudget - weeklySpent;
    final dailyAvailable = remaining > 0 ? remaining ~/ 5 : 0;
    final spentRatio = weeklyBudget > 0
        ? (weeklySpent / weeklyBudget).clamp(0.0, 1.0)
        : 0.0;

    return MoreCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                '자유 소비',
                style: AppTextStyles.bodyLg.copyWith(
                  fontWeight: FontWeight.w600,
                ),
              ),
              Container(
                padding: EdgeInsets.symmetric(
                  horizontal: 10.w,
                  vertical: 4.h,
                ),
                decoration: BoxDecoration(
                  color: AppColors.orange50,
                  borderRadius: BorderRadius.circular(12.r),
                ),
                child: Text(
                  'D-5',
                  style: AppTextStyles.caption.copyWith(
                    color: AppColors.orange500,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ],
          ),
          SizedBox(height: 12.h),
          Text(
            CurrencyFormatter.formatKRW(remaining),
            style: AppTextStyles.headingLg,
          ),
          SizedBox(height: 4.h),
          Row(
            children: [
              Text(
                '일 사용 가능 금액  ',
                style: AppTextStyles.bodySm.copyWith(
                  color: AppColors.textTertiary,
                ),
              ),
              Text(
                CurrencyFormatter.formatKRW(dailyAvailable),
                style: AppTextStyles.bodySm.copyWith(
                  color: AppColors.textTertiary,
                ),
              ),
            ],
          ),
          SizedBox(height: 16.h),
          MoreProgressBar(
            progress: spentRatio,
          ),
        ],
      ),
    );
  }
}
