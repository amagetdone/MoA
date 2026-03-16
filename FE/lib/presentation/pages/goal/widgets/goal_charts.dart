import 'dart:math' as math;

import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/entities/spending.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';

// ─── 달성 현황 라인 차트 ──────────────────────────────────────
class GoalLineChart extends StatelessWidget {
  final List<DailySpending> history;
  final Goal goal;

  const GoalLineChart({super.key, required this.history, required this.goal});

  @override
  Widget build(BuildContext context) {
    if (history.isEmpty) return const SizedBox.shrink();

    final totalDays =
        goal.endDate.difference(goal.startDate).inDays.toDouble();
    final targetAmount = goal.targetAmount.toDouble();

    // 누적 실적 spot (X = startDate 기준 경과일)
    final actualSpots = <FlSpot>[];
    var cumulative = 0;
    for (final item in history) {
      cumulative += item.amount;
      final x = item.date.difference(goal.startDate).inDays
          .toDouble()
          .clamp(0.0, totalDays);
      actualSpots.add(FlSpot(x, cumulative.toDouble()));
    }

    final lastSpot = actualSpots.last;
    final projectionSpots = <FlSpot>[
      lastSpot,
      FlSpot(totalDays, targetAmount),
    ];

    final maxY = math.max(targetAmount, lastSpot.y) * 1.2;
    // X축 틱: 월 경계(매월 1일) 기준으로 직접 계산
    final monthTicks = <double>[];
    var tickDate = DateTime(goal.startDate.year, goal.startDate.month + 1, 1);
    while (tickDate.isBefore(goal.endDate)) {
      monthTicks.add(tickDate.difference(goal.startDate).inDays.toDouble());
      tickDate = DateTime(tickDate.year, tickDate.month + 1, 1);
    }
    // 시작·끝 추가 (중복 제거 후 정렬)
    monthTicks.insert(0, 0);
    if (!monthTicks.contains(totalDays)) monthTicks.add(totalDays);
    monthTicks.sort();

    final tickInterval = totalDays / 8; // dummy: getTitlesWidget에서 필터링

    return MoreCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 타이틀 + 목표 금액
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.baseline,
            textBaseline: TextBaseline.alphabetic,
            children: [
              Text(
                '목표 달성 현황',
                style: AppTextStyles.headingMd.copyWith(
                  color: AppColors.textPrimary,
                  fontWeight: FontWeight.w700,
                ),
              ),
              Text(
                _fmtAmount(goal.targetAmount),
                style: AppTextStyles.caption.copyWith(
                  color: AppColors.textTertiary,
                ),
              ),
            ],
          ),
          SizedBox(height: 16.h),
          SizedBox(
            height: 200.h,
            child: LineChart(
              LineChartData(
                clipData: const FlClipData.all(),
                gridData: FlGridData(
                  show: true,
                  drawVerticalLine: false,
                  horizontalInterval: maxY / 4,
                  getDrawingHorizontalLine: (_) => FlLine(
                    color: AppColors.borderDefault,
                    strokeWidth: 1,
                  ),
                ),
                titlesData: FlTitlesData(
                  topTitles: const AxisTitles(
                    sideTitles: SideTitles(showTitles: false),
                  ),
                  rightTitles: const AxisTitles(
                    sideTitles: SideTitles(showTitles: false),
                  ),
                  leftTitles: const AxisTitles(
                    sideTitles: SideTitles(showTitles: false),
                  ),
                  bottomTitles: AxisTitles(
                    sideTitles: SideTitles(
                      showTitles: true,
                      reservedSize: 28.h,
                      interval: tickInterval,
                      getTitlesWidget: (value, meta) {
                        // 월 경계 틱만 표시
                        final nearest = monthTicks.fold<double>(
                          monthTicks.first,
                          (prev, t) =>
                              (t - value).abs() < (prev - value).abs()
                                  ? t
                                  : prev,
                        );
                        if ((nearest - value).abs() > tickInterval * 0.5) {
                          return const SizedBox.shrink();
                        }
                        final date = goal.startDate
                            .add(Duration(days: nearest.toInt()));
                        return Padding(
                          padding: EdgeInsets.only(top: 4.h),
                          child: Text(
                            '${date.month}월',
                            style: AppTextStyles.caption.copyWith(
                              color: AppColors.textTertiary,
                            ),
                          ),
                        );
                      },
                    ),
                  ),
                ),
                borderData: FlBorderData(show: false),
                minX: 0,
                maxX: totalDays,
                minY: 0,
                maxY: maxY,
                lineBarsData: [
                  // ① 실적 라인 (초록 solid)
                  LineChartBarData(
                    spots: actualSpots,
                    isCurved: true,
                    curveSmoothness: 0.15,
                    color: AppColors.green500,
                    barWidth: 2.5,
                    isStrokeCapRound: true,
                    dotData: FlDotData(
                      show: true,
                      checkToShowDot: (spot, _) =>
                          spot.x == lastSpot.x && spot.y == lastSpot.y,
                      getDotPainter: (_, __, ___, ____) =>
                          FlDotCirclePainter(
                            radius: 4,
                            color: AppColors.green500,
                            strokeColor: Colors.white,
                            strokeWidth: 2,
                          ),
                    ),
                    belowBarData: BarAreaData(
                      show: true,
                      gradient: LinearGradient(
                        begin: Alignment.topCenter,
                        end: Alignment.bottomCenter,
                        colors: [
                          AppColors.green500.withValues(alpha: 0.25),
                          AppColors.green500.withValues(alpha: 0),
                        ],
                      ),
                    ),
                  ),
                  // ② 프로젝션 라인 (점선)
                  LineChartBarData(
                    spots: projectionSpots,
                    isCurved: false,
                    color: AppColors.textTertiary.withValues(alpha: 0.45),
                    barWidth: 1.8,
                    dashArray: [6, 4],
                    dotData: const FlDotData(show: false),
                    belowBarData: BarAreaData(show: false),
                  ),
                ],
                lineTouchData: LineTouchData(
                  touchTooltipData: LineTouchTooltipData(
                    getTooltipColor: (_) => AppColors.textPrimary,
                    getTooltipItems: (touched) => touched
                        .map(
                          (s) => LineTooltipItem(
                            _fmtAmount(s.y.toInt()),
                            AppTextStyles.caption
                                .copyWith(color: Colors.white),
                          ),
                        )
                        .toList(),
                  ),
                ),
              ),
            ),
          ),
          // 현재 저축액 레이블 (라인 끝 아래)
          SizedBox(height: 4.h),
          Text(
            _fmtAmount(cumulative),
            style: AppTextStyles.caption.copyWith(
              color: AppColors.green500,
              fontWeight: FontWeight.w700,
            ),
          ),
        ],
      ),
    );
  }

  String _fmtAmount(int amount) {
    if (amount >= 10000) {
      final man = amount ~/ 10000;
      final rest = amount % 10000;
      return rest == 0 ? '$man만원' : '${(amount / 10000).toStringAsFixed(1)}만원';
    }
    return '$amount원';
  }
}

// ─── 저축 이력 바 차트 ────────────────────────────────────────
class GoalBarChart extends StatelessWidget {
  final List<DailySpending> history;
  final Goal goal;

  const GoalBarChart({super.key, required this.history, required this.goal});

  @override
  Widget build(BuildContext context) {
    if (history.isEmpty) return const SizedBox.shrink();

    // ① 주차별 합산
    final Map<int, int> weeklyTotals = {};
    for (final item in history) {
      final w = item.date.difference(goal.startDate).inDays ~/ 7;
      weeklyTotals[w] = (weeklyTotals[w] ?? 0) + item.amount;
    }

    // ② 메타 계산
    final totalWeeks =
        (goal.endDate.difference(goal.startDate).inDays / 7).ceil();
    final currentWeek =
        DateTime.now().difference(goal.startDate).inDays ~/ 7;
    final weeklyTarget =
        totalWeeks > 0 ? goal.targetAmount / totalWeeks : 0.0;

    // 표시할 주차 수 (최대 8주)
    final displayWeeks = math.min(totalWeeks, 8);

    // ③ 최대 Y
    final maxWeekly = weeklyTotals.values.fold<int>(
      0,
      (prev, v) => v > prev ? v : prev,
    );
    final maxY = math.max(weeklyTarget, maxWeekly.toDouble()) * 1.35;

    // ④ 바 그룹
    final barGroups = List.generate(displayWeeks, (i) {
      final amount = (weeklyTotals[i] ?? 0).toDouble();
      final isFuture = i > currentWeek;
      final Color barColor;
      if (isFuture) {
        barColor = AppColors.gray500.withValues(alpha: 0.15);
      } else if (!isFuture && amount >= weeklyTarget) {
        barColor = AppColors.green500;
      } else {
        barColor = AppColors.red500;
      }
      return BarChartGroupData(
        x: i,
        barRods: [
          BarChartRodData(
            toY: isFuture ? weeklyTarget * 0.05 : amount,
            color: barColor,
            width: displayWeeks > 6 ? 24.w : 32.w,
            borderRadius: BorderRadius.only(
              topLeft: Radius.circular(4.r),
              topRight: Radius.circular(4.r),
            ),
          ),
        ],
      );
    });

    return MoreCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '저축 이력',
            style: AppTextStyles.headingMd.copyWith(
              color: AppColors.textPrimary,
              fontWeight: FontWeight.w700,
            ),
          ),
          SizedBox(height: 14.h),

          // ── 주차 서클 ──────────────────────────────────────
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: List.generate(displayWeeks, (i) {
              final amount = weeklyTotals[i] ?? 0;
              final isFuture = i > currentWeek;
              final isCurrent = i == currentWeek;
              final metTarget = !isFuture && amount >= weeklyTarget;

              final Color bgColor;
              final Color txtColor;
              if (isFuture) {
                bgColor = AppColors.gray500.withValues(alpha: 0.15);
                txtColor = AppColors.textTertiary;
              } else if (metTarget) {
                bgColor = AppColors.green500;
                txtColor = Colors.white;
              } else {
                bgColor = AppColors.red500;
                txtColor = Colors.white;
              }

              return Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Container(
                    width: 28.w,
                    height: 28.w,
                    decoration: BoxDecoration(
                      color: bgColor,
                      shape: BoxShape.circle,
                    ),
                    alignment: Alignment.center,
                    child: Text(
                      '${i + 1}',
                      style: AppTextStyles.caption.copyWith(
                        color: txtColor,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                  if (isCurrent) ...[
                    SizedBox(height: 2.h),
                    Text(
                      '진행중',
                      style: AppTextStyles.caption.copyWith(
                        color: AppColors.textTertiary,
                        fontSize: 9.sp,
                      ),
                    ),
                  ],
                ],
              );
            }),
          ),

          SizedBox(height: 12.h),

          // ── 바 차트 ────────────────────────────────────────
          SizedBox(
            height: 180.h,
            child: BarChart(
              BarChartData(
                alignment: BarChartAlignment.spaceAround,
                maxY: maxY,
                gridData: const FlGridData(show: false),
                titlesData: FlTitlesData(
                  topTitles: const AxisTitles(
                    sideTitles: SideTitles(showTitles: false),
                  ),
                  rightTitles: const AxisTitles(
                    sideTitles: SideTitles(showTitles: false),
                  ),
                  leftTitles: const AxisTitles(
                    sideTitles: SideTitles(showTitles: false),
                  ),
                  bottomTitles: AxisTitles(
                    sideTitles: SideTitles(
                      showTitles: true,
                      reservedSize: 24.h,
                      getTitlesWidget: (value, meta) {
                        final i = value.toInt();
                        if (i < 0 || i >= displayWeeks) {
                          return const SizedBox.shrink();
                        }
                        return Padding(
                          padding: EdgeInsets.only(top: 4.h),
                          child: Text(
                            '${i + 1}주차',
                            style: AppTextStyles.caption.copyWith(
                              color: AppColors.textTertiary,
                            ),
                          ),
                        );
                      },
                    ),
                  ),
                ),
                borderData: FlBorderData(show: false),
                barGroups: barGroups,
                // 주 목표 점선
                extraLinesData: weeklyTarget > 0
                    ? ExtraLinesData(
                        horizontalLines: [
                          HorizontalLine(
                            y: weeklyTarget,
                            color: AppColors.textTertiary
                                .withValues(alpha: 0.5),
                            strokeWidth: 1.5,
                            dashArray: [6, 4],
                            label: HorizontalLineLabel(
                              show: true,
                              alignment: Alignment.topRight,
                              labelResolver: (_) =>
                                  _fmtAmount(weeklyTarget.toInt()),
                              style: AppTextStyles.caption.copyWith(
                                color: AppColors.textTertiary,
                              ),
                            ),
                          ),
                        ],
                      )
                    : null,
                barTouchData: BarTouchData(
                  touchTooltipData: BarTouchTooltipData(
                    getTooltipColor: (_) => AppColors.textPrimary,
                    getTooltipItem: (_, __, rod, ___) => BarTooltipItem(
                      _fmtAmount(rod.toY.toInt()),
                      AppTextStyles.caption.copyWith(color: Colors.white),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  String _fmtAmount(int amount) {
    if (amount >= 10000) {
      final man = amount ~/ 10000;
      final rest = amount % 10000;
      return rest == 0 ? '$man만원' : '${(amount / 10000).toStringAsFixed(1)}만원';
    }
    return '$amount원';
  }
}
