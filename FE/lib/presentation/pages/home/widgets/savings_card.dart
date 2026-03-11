import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/widgets/common/more_progress_bar.dart';

class SavingsCard extends StatelessWidget {
  final int weeklySaved;
  final int weeklyTarget;

  const SavingsCard({
    super.key,
    required this.weeklySaved,
    this.weeklyTarget = 200000,
  });

  @override
  Widget build(BuildContext context) {
    final progressRatio = weeklyTarget > 0
        ? (weeklySaved / weeklyTarget).clamp(0.0, 1.0)
        : 0.0;
    final percentText = (progressRatio * 100).toInt();

    return MoreCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '이번 주 저축액',
            style: AppTextStyles.bodyLg.copyWith(
              fontWeight: FontWeight.w600,
            ),
          ),
          SizedBox(height: 12.h),
          Text(
            CurrencyFormatter.formatKRW(weeklySaved),
            style: AppTextStyles.headingLg.copyWith(
              color: AppColors.statusSuccess,
            ),
          ),
          SizedBox(height: 4.h),
          Text(
            '목표 대비 $percentText%',
            style: AppTextStyles.bodySm.copyWith(
              color: AppColors.textTertiary,
            ),
          ),
          SizedBox(height: 16.h),
          MoreProgressBar(
            progress: progressRatio,
            color: AppColors.statusSuccess,
          ),
        ],
      ),
    );
  }
}
