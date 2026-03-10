import 'package:flutter/material.dart';
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
                padding: const EdgeInsets.symmetric(
                  horizontal: 10,
                  vertical: 4,
                ),
                decoration: BoxDecoration(
                  color: AppColors.orange50,
                  borderRadius: BorderRadius.circular(12),
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
          const SizedBox(height: 12),
          Text(
            CurrencyFormatter.formatKRW(remaining),
            style: AppTextStyles.headingLg,
          ),
          const SizedBox(height: 4),
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
          const SizedBox(height: 16),
          MoreProgressBar(
            progress: spentRatio,
          ),
        ],
      ),
    );
  }
}
