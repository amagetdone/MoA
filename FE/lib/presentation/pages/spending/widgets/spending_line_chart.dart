import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/domain/entities/spending.dart';
import 'package:more_app/core/utils/currency_formatter.dart';

double _niceInterval(double maxVal, bool isWeekly) {
  // 주간: 4칸, 월간 비교: 3칸 기준으로 nice step 계산
  final raw = maxVal / (isWeekly ? 4.0 : 4.0);
  const steps = [10000.0, 20000.0, 50000.0, 100000.0, 200000.0, 500000.0, 1000000.0, 2000000.0, 5000000.0];
  for (final step in steps) {
    if (raw <= step) return step;
  }
  return steps.last;
}

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

    final maxVal = data.map((e) => e.amount).reduce((a, b) => a > b ? a : b);
    final minVal = data.map((e) => e.amount).reduce((a, b) => a < b ? a : b);
    final maxIndex = data.indexWhere((e) => e.amount == maxVal);
    final minIndex = data.indexWhere((e) => e.amount == minVal);

    final spots = data
        .asMap()
        .entries
        .map((e) => FlSpot(e.key.toDouble(), e.value.amount.toDouble()))
        .toList();

    final yInterval = _niceInterval(maxVal.toDouble(), isWeekly);
    final ceilSteps = (maxVal / yInterval).ceil();
    final isExactMultiple = maxVal.toDouble() % yInterval == 0.0;
    final maxY = (isExactMultiple ? ceilSteps + 1 : ceilSteps) * yInterval;

    final barData = LineChartBarData(
      spots: spots,
      isCurved: true,
      curveSmoothness: 0.15,
      color: AppColors.orange500,
      barWidth: 2.5,
      isStrokeCapRound: true,
      dotData: FlDotData(
        show: true,
        checkToShowDot: (spot, _) =>
            spot.x == maxIndex.toDouble() || spot.x == minIndex.toDouble(),
        getDotPainter: (spot, p, b, i) {
          final isMax = spot.x == maxIndex.toDouble();
          return FlDotCirclePainter(
            radius: isMax ? 5 : 3.5,
            color: AppColors.orange500,
            strokeColor: Colors.white,
            strokeWidth: 2,
          );
        },
      ),
      belowBarData: BarAreaData(
        show: true,
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [
            AppColors.orange500.withValues(alpha: 0.2),
            AppColors.orange500.withValues(alpha: 0),
          ],
        ),
      ),
    );

    return Column(
      children: [
        Expanded(
          child: LineChart(
            LineChartData(
              gridData: const FlGridData(show: false),
              titlesData: FlTitlesData(
                topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                leftTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                bottomTitles: AxisTitles(
                  sideTitles: SideTitles(
                    showTitles: true,
                    reservedSize: 28.h,
                    getTitlesWidget: (value, meta) {
                      final i = value.toInt();
                      if (value != i.toDouble() || i < 0 || i >= data.length) {
                        return const SizedBox.shrink();
                      }
                      final date = data[i].date;
                      final label = isWeekly
                          ? ['월', '화', '수', '목', '금', '토', '일'][i % 7]
                          : '${i + 1}주';
                      return Padding(
                        padding: EdgeInsets.only(top: 4.h),
                        child: Text(
                          label,
                          style: AppTextStyles.caption.copyWith(
                            color: i == maxIndex || i == minIndex
                                ? AppColors.textSecondary
                                : AppColors.textTertiary,
                            fontWeight: i == maxIndex || i == minIndex
                                ? FontWeight.w600
                                : FontWeight.normal,
                          ),
                        ),
                      );
                    },
                  ),
                ),
              ),
              borderData: FlBorderData(show: false),
              minX: -0.3,
              maxX: spots.length - 1 + 0.3,
              minY: 0,
              maxY: maxY,
              lineBarsData: [barData],
              showingTooltipIndicators: [
                ShowingTooltipIndicators([
                  LineBarSpot(barData, 0, spots[maxIndex]),
                ]),
                ShowingTooltipIndicators([
                  LineBarSpot(barData, 0, spots[minIndex]),
                ]),
              ],
              lineTouchData: LineTouchData(
                enabled: true,
                handleBuiltInTouches: true,
                touchTooltipData: LineTouchTooltipData(
                  getTooltipColor: (spot) => spot.x == maxIndex.toDouble()
                      ? AppColors.orange500
                      : AppColors.textPrimary,
                  tooltipRoundedRadius: 8.r,
                  tooltipPadding: EdgeInsets.symmetric(
                    horizontal: 10.w,
                    vertical: 6.h,
                  ),
                  getTooltipItems: (touchedSpots) {
                    return touchedSpots.map((spot) {
                      return LineTooltipItem(
                        CurrencyFormatter.formatKRW(spot.y.toInt()),
                        AppTextStyles.caption.copyWith(
                          color: Colors.white,
                          fontWeight: FontWeight.bold,
                        ),
                      );
                    }).toList();
                  },
                ),
                getTouchedSpotIndicator: (barData, spotIndexes) {
                  return spotIndexes.map((_) {
                    return TouchedSpotIndicatorData(
                      FlLine(
                        color: AppColors.orange500.withValues(alpha: 0.4),
                        strokeWidth: 1.5,
                      ),
                      FlDotData(
                        getDotPainter: (spot, percent, bar, i) =>
                            FlDotCirclePainter(
                          radius: 5.r,
                          color: AppColors.orange500,
                          strokeWidth: 2,
                          strokeColor: Colors.white,
                        ),
                      ),
                    );
                  }).toList();
                },
              ),
            ),
          ),
        ),
      ],
    );
  }
}