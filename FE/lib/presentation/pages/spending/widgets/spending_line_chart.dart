import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/domain/entities/spending.dart';
import 'package:more_app/core/utils/currency_formatter.dart';

class SpendingLineChart extends StatelessWidget {
  final List<DailySpending> data;
  final bool isWeekly;

  const SpendingLineChart({
    super.key,
    required this.data,
    required this.isWeekly,
  });

  @override
  Widget build(BuildContext context) {
    if (data.isEmpty) return const Center(child: Text('데이터가 없습니다'));

    final totalSpent = data.fold(0, (sum, item) => sum + item.amount);
    
    final maxVal = data.map((e) => e.amount).reduce((a, b) => a > b ? a : b);
    final interval = maxVal > 0 ? (maxVal / 4).roundToDouble() : 50000.0;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '총 지출',
          style: AppTextStyles.bodySm.copyWith(
            color: AppColors.textTertiary,
            fontSize: 12.sp,
          ),
        ),
        SizedBox(height: 2.h),
        Text(
          CurrencyFormatter.formatKRW(totalSpent),
          style: AppTextStyles.headingMd.copyWith(
            fontSize: 22.sp,
            color: AppColors.textPrimary,
            fontWeight: FontWeight.bold,
          ),
        ),
        SizedBox(height: 32.h),
        
        Expanded(
          child: LineChart(
            LineChartData(
              gridData: FlGridData(
                show: true,
                drawVerticalLine: false,
                horizontalInterval: interval > 0 ? interval : 50000,
                getDrawingHorizontalLine: (value) => FlLine(
                  color: AppColors.gray100.withValues(alpha: 0.5),
                  strokeWidth: 1,
                ),
              ),
              titlesData: const FlTitlesData(show: false),
              borderData: FlBorderData(show: false),
              lineBarsData: [
                LineChartBarData(
                  spots: data.asMap().entries.map((e) => FlSpot(e.key.toDouble(), e.value.amount.toDouble())).toList(),
                  isCurved: false,
                  color: AppColors.gray900,
                  barWidth: isWeekly ? 2.5 : 2.0,
                  isStrokeCapRound: true,
                  dotData: const FlDotData(show: false),
                  belowBarData: BarAreaData(
                    show: true,
                    gradient: LinearGradient(
                      begin: Alignment.topCenter,
                      end: Alignment.bottomCenter,
                      colors: [
                        AppColors.gray900.withValues(alpha: 0.08),
                        Colors.transparent,
                      ],
                    ),
                  ),
                ),
              ],
              lineTouchData: LineTouchData(
                enabled: true,
                handleBuiltInTouches: true,
                touchTooltipData: LineTouchTooltipData(
                  getTooltipColor: (spot) => AppColors.gray900.withValues(alpha: 0.9),
                  tooltipRoundedRadius: 8.r,
                  tooltipPadding: EdgeInsets.symmetric(horizontal: 12.w, vertical: 8.h),
                  getTooltipItems: (touchedSpots) {
                    return touchedSpots.map((spot) {
                      String trendText = "";
                      if (spot.spotIndex > 0) {
                        final prevY = data[spot.spotIndex - 1].amount;
                        if (prevY > 0) {
                          final diff = ((spot.y - prevY) / prevY * 100).toInt();
                          trendText = diff >= 0 ? "+$diff%" : "$diff%";
                        }
                      }

                      final date = data[spot.spotIndex].date;
                      final dateLabel = isWeekly ? "" : "${date.month}/${date.day}\n";

                      return LineTooltipItem(
                        '$dateLabel${CurrencyFormatter.formatKRW(spot.y.toInt())}\n',
                        AppTextStyles.bodySm.copyWith(
                          color: Colors.white,
                          fontWeight: FontWeight.bold,
                        ),
                        children: [
                          if (trendText.isNotEmpty)
                            TextSpan(
                              text: '이전대비 $trendText',
                              style: AppTextStyles.caption.copyWith(
                                color: trendText.startsWith('+') ? AppColors.orange300 : Colors.greenAccent,
                                fontSize: 10.sp,
                              ),
                            ),
                        ],
                      );
                    }).toList();
                  },
                ),
                getTouchedSpotIndicator: (LineChartBarData barData, List<int> spotIndexes) {
                  return spotIndexes.map((index) {
                    return TouchedSpotIndicatorData(
                      FlLine(color: AppColors.gray500.withValues(alpha: 0.3), strokeWidth: 1.5, dashArray: [4, 4]),
                      FlDotData(
                        getDotPainter: (spot, percent, barData, index) {
                          return FlDotCirclePainter(
                            radius: 5.r,
                            color: AppColors.orange500,
                            strokeWidth: 2,
                            strokeColor: Colors.white,
                          );
                        },
                      ),
                    );
                  }).toList();
                },
              ),
              minY: 0,
              maxY: maxVal > 0 ? (maxVal * 1.3) : 100000,
            ),
          ),
        ),
      ],
    );
  }
}
