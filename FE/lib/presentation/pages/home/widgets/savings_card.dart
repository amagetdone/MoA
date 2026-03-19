import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/widgets/common/more_progress_bar.dart';

class SavingsCard extends StatelessWidget {
  const SavingsCard({
    super.key,
    required this.weeklySaved,
    required this.savingProgressRate,
  });

  final int weeklySaved;
  final int savingProgressRate;

  @override
  Widget build(BuildContext context) {
    final progressRatio = (savingProgressRate / 100).clamp(0.0, 1.0);

    return MoreCard(
      padding: EdgeInsets.fromLTRB(22.w, 22.h, 22.w, 18.h),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 46.w,
                height: 46.w,
                decoration: BoxDecoration(
                  color: const Color(0xFFD7EEDD),
                  borderRadius: BorderRadius.circular(16.r),
                ),
                child: Icon(
                  Icons.savings_outlined,
                  color: AppColors.textPrimary,
                  size: 24.w,
                ),
              ),
              SizedBox(width: 14.w),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '이번 주 저축액',
                      style: AppTextStyles.bodyLg.copyWith(
                        color: AppColors.textSecondary,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                    SizedBox(height: 4.h),
                    FittedBox(
                      fit: BoxFit.scaleDown,
                      alignment: Alignment.centerLeft,
                      child: Text(
                        CurrencyFormatter.formatKRW(weeklySaved),
                        style: AppTextStyles.headingLg.copyWith(
                          color: const Color(0xFF53A65A),
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
          MoreProgressBar(
            progress: progressRatio,
            color: const Color(0xFF69BD5F),
            backgroundColor: AppColors.gray100.withValues(alpha: 0.9),
            height: 14.h,
          ),
          SizedBox(height: 12.h),
          Text(
            '$savingProgressRate% 달성',
            style: AppTextStyles.bodySm.copyWith(
              color: AppColors.textTertiary,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }
}
