import 'package:flutter/material.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/spending.dart';
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
    return MoreCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '소비 패턴',
            style: AppTextStyles.headingMd,
          ),
          const SizedBox(height: 16),
          SizedBox(
            height: 200,
            child: Stack(
              alignment: Alignment.center,
              children: [
                PieChart(
                  PieChartData(
                    sections: _buildSections(),
                    centerSpaceRadius: 60,
                    sectionsSpace: 2,
                    borderData: FlBorderData(show: false),
                  ),
                ),
                Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      '총 지출',
                      style: AppTextStyles.caption.copyWith(
                        color: AppColors.textTertiary,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      CurrencyFormatter.formatKRW(_totalAmount),
                      style: AppTextStyles.bodyMd.copyWith(
                        fontWeight: FontWeight.bold,
                        color: AppColors.textPrimary,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          Wrap(
            spacing: 12,
            runSpacing: 8,
            children: categories.map((category) {
              return Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Container(
                    width: 10,
                    height: 10,
                    decoration: BoxDecoration(
                      color: category.color,
                      shape: BoxShape.circle,
                    ),
                  ),
                  const SizedBox(width: 4),
                  Text(
                    category.name,
                    style: AppTextStyles.caption.copyWith(
                      color: AppColors.textSecondary,
                    ),
                  ),
                ],
              );
            }).toList(),
          ),
        ],
      ),
    );
  }

  List<PieChartSectionData> _buildSections() {
    if (categories.isEmpty) {
      return [
        PieChartSectionData(
          value: 1,
          color: AppColors.borderDefault,
          radius: 30,
          showTitle: false,
        ),
      ];
    }

    return categories.map((category) {
      return PieChartSectionData(
        value: category.amount.toDouble(),
        color: category.color,
        radius: 30,
        showTitle: false,
      );
    }).toList();
  }
}
