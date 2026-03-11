import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/domain/entities/spending.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';

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
    return MoreCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text(
                '지출 그래프',
                style: AppTextStyles.headingMd.copyWith(fontSize: 16.sp),
              ),
              SizedBox(width: 8.w),
              Container(
                padding: EdgeInsets.symmetric(horizontal: 8.w, vertical: 2.h),
                decoration: BoxDecoration(
                  color: AppColors.red500.withValues(alpha: 0.1),
                  borderRadius: BorderRadius.circular(10.r),
                ),
                child: Text(
                  '10% 증가',
                  style: AppTextStyles.caption.copyWith(
                    color: AppColors.red500,
                    fontSize: 11.sp,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ],
          ),
          SizedBox(height: 4.h),
          Text(
            '총 지출',
            style: AppTextStyles.bodySm.copyWith(
              color: AppColors.textTertiary,
              fontSize: 12.sp,
            ),
          ),
          SizedBox(height: 16.h),
          SizedBox(
            height: 200.h,
            child: data.isEmpty
                ? Center(
                    child: Text(
                      '데이터가 없습니다',
                      style: AppTextStyles.bodyMd.copyWith(
                        color: AppColors.textTertiary,
                        fontSize: 14.sp,
                      ),
                    ),
                  )
                : LineChart(
                    _buildChartData(),
                  ),
          ),
        ],
      ),
    );
  }

  LineChartData _buildChartData() {
    final spots = <FlSpot>[];
    for (int i = 0; i < data.length; i++) {
      spots.add(FlSpot(i.toDouble(), data[i].amount.toDouble()));
    }

    double maxY = 0;
    for (final spending in data) {
      if (spending.amount > maxY) {
        maxY = spending.amount.toDouble();
      }
    }
    if (maxY == 0) maxY = 100000;
    maxY = maxY * 1.2;

    return LineChartData(
      gridData: FlGridData(
        show: true,
        drawVerticalLine: false,
        horizontalInterval: maxY / 4,
        getDrawingHorizontalLine: (value) {
          return FlLine(
            color: AppColors.borderDefault,
            strokeWidth: 1,
          );
        },
      ),
      titlesData: FlTitlesData(
        topTitles: const AxisTitles(
          sideTitles: SideTitles(showTitles: false),
        ),
        rightTitles: const AxisTitles(
          sideTitles: SideTitles(showTitles: false),
        ),
        leftTitles: AxisTitles(
          sideTitles: SideTitles(
            showTitles: true,
            reservedSize: 50.w,
            getTitlesWidget: (value, meta) {
              if (value == meta.max || value == meta.min) {
                return const SizedBox.shrink();
              }
              final display = (value / 10000).toStringAsFixed(0);
              return Padding(
                padding: EdgeInsets.only(right: 4.w),
                child: Text(
                  '${display}만',
                  style: AppTextStyles.caption.copyWith(
                    color: AppColors.textTertiary,
                    fontSize: 10.sp,
                  ),
                ),
              );
            },
          ),
        ),
        bottomTitles: AxisTitles(
          sideTitles: SideTitles(
            showTitles: true,
            reservedSize: 28.h,
            interval: 1,
            getTitlesWidget: (value, meta) {
              final index = value.toInt();
              if (index < 0 || index >= data.length) {
                return const SizedBox.shrink();
              }

              final showLabel = data.length <= 7 ||
                  index % (data.length ~/ 5).clamp(1, data.length) == 0 ||
                  index == data.length - 1;

              if (!showLabel) {
                return const SizedBox.shrink();
              }

              final date = data[index].date;
              final label = isWeekly
                  ? '${date.month}/${date.day}'
                  : '${date.day}일';

              return Padding(
                padding: EdgeInsets.only(top: 8.h),
                child: Text(
                  label,
                  style: AppTextStyles.caption.copyWith(
                    color: AppColors.textTertiary,
                    fontSize: 10.sp,
                  ),
                ),
              );
            },
          ),
        ),
      ),
      borderData: FlBorderData(show: false),
      minX: 0,
      maxX: (data.length - 1).toDouble().clamp(0, double.infinity),
      minY: 0,
      maxY: maxY,
      lineBarsData: [
        LineChartBarData(
          spots: spots,
          isCurved: true,
          curveSmoothness: 0.3,
          color: AppColors.orange500,
          barWidth: 2.5,
          isStrokeCapRound: true,
          dotData: FlDotData(
            show: data.length <= 7,
            getDotPainter: (spot, percent, barData, index) {
              return FlDotCirclePainter(
                radius: 3.r,
                color: Colors.white,
                strokeWidth: 2,
                strokeColor: AppColors.orange500,
              );
            },
          ),
          belowBarData: BarAreaData(
            show: true,
            gradient: LinearGradient(
              begin: Alignment.topCenter,
              end: Alignment.bottomCenter,
              colors: [
                AppColors.orange500.withValues(alpha: 0.3),
                AppColors.orange500.withValues(alpha: 0.0),
              ],
            ),
          ),
        ),
      ],
      lineTouchData: LineTouchData(
        touchTooltipData: LineTouchTooltipData(
          getTooltipItems: (touchedSpots) {
            return touchedSpots.map((spot) {
              final amount = spot.y.toInt();
              final formatted = _formatAmount(amount);
              return LineTooltipItem(
                formatted,
                AppTextStyles.caption.copyWith(
                  color: Colors.white,
                  fontWeight: FontWeight.w600,
                  fontSize: 11.sp,
                ),
              );
            }).toList();
          },
        ),
      ),
    );
  }

  String _formatAmount(int amount) {
    if (amount >= 10000) {
      final man = amount ~/ 10000;
      final remainder = amount % 10000;
      if (remainder == 0) {
        return '${man}만원';
      }
      return '${man}만${remainder}원';
    }
    return '${amount}원';
  }
}
