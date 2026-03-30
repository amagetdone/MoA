import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/spending.dart';
import 'package:more_app/presentation/utils/spending_ui_mapper.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';

class SpendingDonutChart extends StatelessWidget {
  final List<SpendingCategory> categories;

  const SpendingDonutChart({
    super.key,
    required this.categories,
  });

  int get _totalAmount {
    int total = 0;
    for (final category in categories) {
      total += category.amount;
    }
    return total;
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return MoreCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '소비 패턴',
            style: AppTextStyles.headingMd.copyWith(color: colors.textPrimary, fontSize: 16.sp),
          ),
          SizedBox(height: 16.h),
          SizedBox(
            height: 200.h,
            child: Stack(
              alignment: Alignment.center,
              children: [
                PieChart(
                  PieChartData(
                    sections: _buildSections(colors),
                    centerSpaceRadius: 60.r,
                    sectionsSpace: 2,
                    borderData: FlBorderData(show: false),
                    pieTouchData: PieTouchData(enabled: false),
                  ),
                ),
                Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      CurrencyFormatter.formatKRW(_totalAmount),
                      style: AppTextStyles.bodyMd.copyWith(
                        fontWeight: FontWeight.bold,
                        color: colors.textPrimary,
                        fontSize: 14.sp,
                      ),
                    ),
                    SizedBox(height: 2.h),
                    Text(
                      '총 지출',
                      style: AppTextStyles.caption.copyWith(
                        color: colors.textTertiary,
                        fontSize: 11.sp,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
          SizedBox(height: 12.h),
          GestureDetector(
            onTap: () {
              // TODO: navigate to category detail
            },
            child: Text(
              '이 카테고리가 궁금한가요?',
              style: AppTextStyles.bodySm.copyWith(
                color: AppColors.orange500,
                fontSize: 12.sp,
              ),
            ),
          ),
          SizedBox(height: 12.h),
          Container(
            width: double.infinity,
            padding: EdgeInsets.symmetric(horizontal: 12.w, vertical: 10.h),
            decoration: BoxDecoration(
              color: AppColors.orange50,
              borderRadius: BorderRadius.circular(8.r),
            ),
            child: Text(
              '식비 절약이 필요해요.',
              style: AppTextStyles.bodySm.copyWith(
                color: AppColors.orange500,
                fontWeight: FontWeight.w600,
                fontSize: 13.sp,
              ),
            ),
          ),
          SizedBox(height: 16.h),
          Text(
            '카테고리별 지출 100% 기준',
            style: AppTextStyles.caption.copyWith(
              color: colors.textTertiary,
              fontSize: 11.sp,
            ),
          ),
          SizedBox(height: 8.h),
          ...categories.map((category) {
            final percentage = _totalAmount > 0
                ? (category.amount / _totalAmount * 100).round()
                : 0;
            return Padding(
              padding: EdgeInsets.only(bottom: 8.h),
              child: Row(
                children: [
                  Container(
                    width: 10.w,
                    height: 10.w,
                    decoration: BoxDecoration(
                      color: SpendingUiMapper.colorFrom(category.colorValue),
                      shape: BoxShape.circle,
                    ),
                  ),
                  SizedBox(width: 8.w),
                  SizedBox(
                    width: 50.w,
                    child: Text(
                      category.name,
                      style: AppTextStyles.caption.copyWith(
                        color: colors.textSecondary,
                        fontSize: 12.sp,
                      ),
                    ),
                  ),
                  SizedBox(width: 8.w),
                  Expanded(
                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(3.r),
                      child: LinearProgressIndicator(
                        value: percentage / 100,
                        backgroundColor: colors.borderDefault,
                        valueColor: AlwaysStoppedAnimation(SpendingUiMapper.colorFrom(category.colorValue)),
                        minHeight: 6.h,
                      ),
                    ),
                  ),
                  SizedBox(width: 8.w),
                  Text(
                    '$percentage%',
                    style: AppTextStyles.caption.copyWith(
                      color: colors.textSecondary,
                      fontSize: 12.sp,
                    ),
                  ),
                ],
              ),
            );
          }),
        ],
      ),
    );
  }

  List<PieChartSectionData> _buildSections(AppColorScheme colors) {
    if (categories.isEmpty) {
      return [
        PieChartSectionData(
          value: 1,
          color: colors.borderDefault,
          radius: 30.r,
          showTitle: false,
        ),
      ];
    }

    return categories.map((category) {
      return PieChartSectionData(
        value: category.amount.toDouble(),
        color: SpendingUiMapper.colorFrom(category.colorValue),
        radius: 30.r,
        showTitle: false,
      );
    }).toList();
  }
}
