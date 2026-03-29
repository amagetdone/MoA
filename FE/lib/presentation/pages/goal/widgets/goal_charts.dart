import 'dart:math' as math;

import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/goal.dart';
import 'package:more_app/domain/entities/spending.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';

const _minChartZoom = 0.8;
const _maxChartZoom = 2.4;

class GoalLineChart extends StatelessWidget {
  const GoalLineChart({super.key, required this.history, required this.goal});

  final List<DailySpending> history;
  final Goal goal;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final graphData = goal.graphData;
    if (graphData != null &&
        (graphData.weeks.isNotEmpty ||
            graphData.planned.isNotEmpty ||
            graphData.actual.isNotEmpty)) {
      final totalWeekCount = math.max(
        goal.totalWeeks,
        math.max(
          graphData.weeks.length,
          math.max(graphData.planned.length, graphData.actual.length),
        ),
      );
      final actualSeries = _buildCumulativeSeries(
        weeklyValues: graphData.actual,
        totalWeeks: totalWeekCount,
      );
      final plannedSeries = _buildCumulativeSeries(
        weeklyValues: graphData.planned,
        totalWeeks: totalWeekCount,
      );

      return _LineChartCard(
        title: '달성 추이',
        primaryLegend: '실제',
        secondaryLegend: '예상 경로',
        primaryColor: AppColors.green500,
        secondaryColor: colors.textTertiary,
        primarySeries: [
          for (var i = 0; i < totalWeekCount; i++)
            _ChartDatum(
              x: i.toDouble(),
              label: '${i + 1}주차',
              value: actualSeries[i],
            ),
        ],
        secondarySeries: [
          for (var i = 0; i < totalWeekCount; i++)
            _ChartDatum(
              x: i.toDouble(),
              label: '${i + 1}주차',
              value: plannedSeries[i],
            ),
        ],
      );
    }

    final totalWeeks = math.max(goal.totalWeeks, 1);
    final weeklyTotals = List<int>.filled(totalWeeks, 0);
    for (final item in history) {
      final weekIndex = (item.date.difference(goal.startDate).inDays ~/ 7)
          .clamp(0, totalWeeks - 1);
      weeklyTotals[weekIndex] += item.amount;
    }

    if (weeklyTotals.every((value) => value == 0) && goal.targetAmount <= 0) {
      return const SizedBox.shrink();
    }

    final actualSeries = _buildCumulativeSeries(
      weeklyValues: weeklyTotals,
      totalWeeks: totalWeeks,
    );
    final plannedSeries = _buildEvenPlanSeries(
      totalWeeks: totalWeeks,
      targetAmount: goal.targetAmount,
    );

    return _LineChartCard(
      title: '달성 추이',
      primaryLegend: '실제',
      secondaryLegend: '예상 경로',
      primaryColor: AppColors.green500,
      secondaryColor: colors.textTertiary,
      primarySeries: [
        for (var i = 0; i < totalWeeks; i++)
          _ChartDatum(
            x: i.toDouble(),
            label: '${i + 1}주차',
            value: actualSeries[i],
          ),
      ],
      secondarySeries: [
        for (var i = 0; i < totalWeeks; i++)
          _ChartDatum(
            x: i.toDouble(),
            label: '${i + 1}주차',
            value: plannedSeries[i],
          ),
      ],
    );
  }
}

class GoalBarChart extends StatelessWidget {
  const GoalBarChart({super.key, required this.history, required this.goal});

  final List<DailySpending> history;
  final Goal goal;

  @override
  Widget build(BuildContext context) {
    if (goal.weeklyPlans.isNotEmpty) {
      final normalizedPlans = _buildNormalizedWeeklyPlans(
        goal: goal,
        history: history,
      );
      final currentWeek = goal.currentWeekNumber;
      final highlightedIndex = normalizedPlans.indexWhere(
        (plan) => plan.week == currentWeek,
      );
      final safeHighlightedIndex = highlightedIndex >= 0
          ? highlightedIndex
          : (currentWeek - 1).clamp(0, normalizedPlans.length - 1);
      final highlightedPlan = normalizedPlans[safeHighlightedIndex];
      final plannedWeeklyValues = goal.graphData?.planned;
      final weeklyTarget = (plannedWeeklyValues != null && plannedWeeklyValues.isNotEmpty)
          ? plannedWeeklyValues[
              (highlightedPlan.week - 1).clamp(0, plannedWeeklyValues.length - 1)
            ].toDouble()
          : (goal.totalWeeks > 0
              ? (goal.targetAmount / goal.totalWeeks).ceilToDouble()
              : null);

      return _BarChartCard(
        title: '저축 이력',
        leftMetricLabel: goal.status == GoalStatus.achieved ? null : '이번 주 저축 계획',
        leftMetricValue: goal.status == GoalStatus.achieved ? null : CurrencyFormatter.formatKRW(highlightedPlan.amount),
        leftMetricColor: goal.status == GoalStatus.achieved ? null : AppColors.orange500,
        rightMetricLabel: goal.status == GoalStatus.achieved ? null : '주간 목표액',
        rightMetricValue: goal.status == GoalStatus.achieved ? null : CurrencyFormatter.formatKRW(weeklyTarget?.round() ?? 0),
        bars: [
          for (final plan in normalizedPlans)
            _ChartDatum(
              x: (plan.week - 1).toDouble(),
              label: '${plan.week}주차',
              value: plan.amount.toDouble(),
            ),
        ],
        referenceLineY: weeklyTarget,
        highlightedIndex: safeHighlightedIndex,
        baseColor: AppColors.green500,
        highlightedColor: AppColors.orange500,
        tooltipPrefix: '주간 계획',
      );
    }

    final weeklyTotals = <int, int>{};
    for (final item in history) {
      final weekIndex = (item.date.difference(goal.startDate).inDays ~/ 7)
          .clamp(0, 9999);
      weeklyTotals[weekIndex] = (weeklyTotals[weekIndex] ?? 0) + item.amount;
    }

    final fallbackWeekCount = math.max(
      1,
      math.max(goal.currentWeekNumber, goal.totalWeeks),
    );
    final latestWeekIndex = weeklyTotals.isEmpty
        ? fallbackWeekCount - 1
        : (weeklyTotals.keys.toList()..sort()).last;
    final latestWeekAmount = weeklyTotals[latestWeekIndex] ?? 0;
    final weeklyTarget = goal.totalWeeks > 0
        ? goal.targetAmount / goal.totalWeeks
        : 0.0;

    return _BarChartCard(
      title: '저축 이력',
      leftMetricLabel: goal.status == GoalStatus.achieved ? null : '최근 주 저축',
      leftMetricValue: goal.status == GoalStatus.achieved ? null : CurrencyFormatter.formatKRW(latestWeekAmount),
      leftMetricColor: goal.status == GoalStatus.achieved ? null : AppColors.green500,
      rightMetricLabel: goal.status == GoalStatus.achieved ? null : '주간 목표',
      rightMetricValue: goal.status == GoalStatus.achieved ? null : CurrencyFormatter.formatKRW(weeklyTarget.round()),
      bars: [
        for (var i = 0; i <= latestWeekIndex; i++)
          _ChartDatum(
            x: i.toDouble(),
            label: '${i + 1}주차',
            value: (weeklyTotals[i] ?? 0).toDouble(),
          ),
      ],
      referenceLineY: weeklyTarget > 0 ? weeklyTarget : null,
      highlightedIndex: latestWeekIndex,
      baseColor: AppColors.green500,
      highlightedColor: AppColors.green500,
      tooltipPrefix: '저축 금액',
    );
  }
}

class _LineChartCard extends StatefulWidget {
  const _LineChartCard({
    required this.title,
    required this.primaryLegend,
    required this.secondaryLegend,
    required this.primaryColor,
    required this.secondaryColor,
    required this.primarySeries,
    required this.secondarySeries,
  });

  final String title;
  final String primaryLegend;
  final String secondaryLegend;
  final Color primaryColor;
  final Color secondaryColor;
  final List<_ChartDatum> primarySeries;
  final List<_ChartDatum> secondarySeries;

  @override
  State<_LineChartCard> createState() => _LineChartCardState();
}

class _LineChartCardState extends State<_LineChartCard> {
  final Map<int, Offset> _activePointers = <int, Offset>{};

  double _zoom = 1.0;
  double? _pinchStartDistance;
  double _pinchStartZoom = 1.0;
  int? _selectedIndex;

  void _zoomIn() => _setZoom(_zoom + 0.2);

  void _zoomOut() => _setZoom(_zoom - 0.2);

  void _setZoom(double value) {
    setState(() {
      _zoom = value.clamp(_minChartZoom, _maxChartZoom).toDouble();
    });
  }

  void _handlePointerDown(PointerDownEvent event) {
    _activePointers[event.pointer] = event.localPosition;
    if (_activePointers.length == 2) {
      _pinchStartDistance = _calculatePointerDistance(_activePointers);
      _pinchStartZoom = _zoom;
    }
  }

  void _handlePointerMove(PointerMoveEvent event) {
    if (!_activePointers.containsKey(event.pointer)) {
      return;
    }
    _activePointers[event.pointer] = event.localPosition;
    if (_activePointers.length != 2 || _pinchStartDistance == null) {
      return;
    }

    final distance = _calculatePointerDistance(_activePointers);
    if (distance == null || _pinchStartDistance == 0) {
      return;
    }

    _setZoom(_pinchStartZoom * (distance / _pinchStartDistance!));
  }

  void _handlePointerEnd(PointerEvent event) {
    _activePointers.remove(event.pointer);
    if (_activePointers.length < 2) {
      _pinchStartDistance = null;
      _pinchStartZoom = _zoom;
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final pointCount = widget.primarySeries.length;
    final allValues = <double>[
      ...widget.primarySeries.map((e) => e.value),
      ...widget.secondarySeries.map((e) => e.value),
    ];
    final maxY = _resolvePositiveChartMaxY(allValues, paddingFactor: 1.1);
    final latestActual = widget.primarySeries.isEmpty
        ? 0
        : widget.primarySeries.last.value.round();
    final latestPlanned = widget.secondarySeries.isEmpty
        ? 0
        : widget.secondarySeries.last.value.round();
    const secondaryBarIndex = 0;
    const primaryBarIndex = 1;

    final primaryBar = LineChartBarData(
      spots: [
        for (final item in widget.primarySeries) FlSpot(item.x, item.value),
      ],
      isCurved: true,
      color: widget.primaryColor,
      barWidth: 2.8,
      isStrokeCapRound: true,
      dotData: FlDotData(
        show: true,
        getDotPainter: (spot, percent, bar, index) {
          final isSelected = _selectedIndex == spot.x.toInt();
          return FlDotCirclePainter(
            radius: isSelected ? 4.6 : 2.8,
            color: widget.primaryColor,
            strokeWidth: isSelected ? 2 : 1.4,
            strokeColor: Colors.white,
          );
        },
      ),
      belowBarData: BarAreaData(
        show: true,
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [
            widget.primaryColor.withValues(alpha: 0.18),
            widget.primaryColor.withValues(alpha: 0),
          ],
        ),
      ),
    );

    final secondaryBar = LineChartBarData(
      spots: [
        for (final item in widget.secondarySeries) FlSpot(item.x, item.value),
      ],
      isCurved: true,
      color: widget.secondaryColor.withValues(alpha: 0.78),
      barWidth: 2,
      dashArray: const [6, 4],
      dotData: const FlDotData(show: false),
      belowBarData: BarAreaData(show: false),
    );

    final selectedTooltipSpots = _selectedIndex == null
        ? const <LineBarSpot>[]
        : [
            if (_selectedIndex! < primaryBar.spots.length)
              LineBarSpot(
                primaryBar,
                primaryBarIndex,
                primaryBar.spots[_selectedIndex!],
              ),
            if (_selectedIndex! < secondaryBar.spots.length)
              LineBarSpot(
                secondaryBar,
                secondaryBarIndex,
                secondaryBar.spots[_selectedIndex!],
              ),
          ];

    return MoreCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            widget.title,
            style: AppTextStyles.headingMd.copyWith(
              color: colors.textPrimary,
              fontWeight: FontWeight.w700,
            ),
          ),
          SizedBox(height: 10.h),
          _MetricRow(
            leftLabel: '현재 누적',
            leftValue: CurrencyFormatter.formatKRW(latestActual),
            leftColor: widget.primaryColor,
            rightLabel: '예상 누적',
            rightValue: CurrencyFormatter.formatKRW(latestPlanned),
            rightColor: colors.textPrimary,
          ),
          SizedBox(height: 10.h),
          _LegendRow(
            items: [
              _LegendItem(
                label: widget.primaryLegend,
                color: widget.primaryColor,
              ),
              _LegendItem(
                label: widget.secondaryLegend,
                color: widget.secondaryColor,
                dashed: true,
              ),
            ],
          ),
          SizedBox(height: 8.h),
          _ChartControlRow(onZoomIn: _zoomIn, onZoomOut: _zoomOut),
          SizedBox(height: 8.h),
          SizedBox(
            height: 300.h,
            child: LayoutBuilder(
              builder: (context, constraints) {
                final stepWidth = 56.w * _zoom;
                final chartWidth = math.max(
                  constraints.maxWidth,
                  math.max(1, pointCount) * stepWidth,
                );
                final visibleCount = math.max(
                  1,
                  (constraints.maxWidth / stepWidth).ceil(),
                );
                final titleInterval = math.max(1, (visibleCount / 4).ceil());

                return Listener(
                  onPointerDown: _handlePointerDown,
                  onPointerMove: _handlePointerMove,
                  onPointerUp: _handlePointerEnd,
                  onPointerCancel: _handlePointerEnd,
                  child: SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: SizedBox(
                      width: chartWidth,
                      child: LineChart(
                        LineChartData(
                          minX: 0,
                          maxX: math.max(0, pointCount - 1).toDouble(),
                          minY: 0,
                          maxY: maxY,
                          clipData: const FlClipData.all(),
                          borderData: FlBorderData(show: false),
                          gridData: FlGridData(
                            show: true,
                            drawVerticalLine: false,
                            horizontalInterval: _resolveGridInterval(maxY),
                            getDrawingHorizontalLine: (_) => FlLine(
                              color: colors.borderDefault,
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
                            leftTitles: AxisTitles(
                              sideTitles: SideTitles(
                                showTitles: true,
                                reservedSize: 42.w,
                                interval: _resolveGridInterval(maxY),
                                getTitlesWidget: (value, meta) => Padding(
                                  padding: EdgeInsets.only(right: 6.w),
                                  child: Text(
                                    _formatAxisAmount(value),
                                    style: AppTextStyles.caption.copyWith(
                                      color: colors.textTertiary,
                                    ),
                                  ),
                                ),
                              ),
                            ),
                            bottomTitles: AxisTitles(
                              sideTitles: SideTitles(
                                showTitles: true,
                                reservedSize: 28.h,
                                interval: titleInterval.toDouble(),
                                getTitlesWidget: (value, meta) {
                                  final index = value.toInt();
                                  if (index < 0 ||
                                      index >= widget.primarySeries.length) {
                                    return const SizedBox.shrink();
                                  }
                                  return Padding(
                                    padding: EdgeInsets.only(top: 6.h),
                                    child: Text(
                                      widget.primarySeries[index].label,
                                      style: AppTextStyles.caption.copyWith(
                                        color: colors.textTertiary,
                                      ),
                                    ),
                                  );
                                },
                              ),
                            ),
                          ),
                          lineTouchData: LineTouchData(
                            enabled: true,
                            handleBuiltInTouches: false,
                            touchSpotThreshold: 24,
                            touchTooltipData: LineTouchTooltipData(
                              fitInsideHorizontally: true,
                              fitInsideVertically: true,
                              tooltipRoundedRadius: 12.r,
                              tooltipPadding: EdgeInsets.symmetric(
                                horizontal: 10.w,
                                vertical: 6.h,
                              ),
                              tooltipMargin: 8.h,
                              getTooltipColor: (_) => colors.bgSurface,
                              getTooltipItems: (spots) => spots
                                  .map(
                                    (spot) => LineTooltipItem(
                                      '${spot.barIndex == primaryBarIndex ? widget.primaryLegend : widget.secondaryLegend}\n${CurrencyFormatter.formatKRW(spot.y.round())}',
                                      AppTextStyles.caption.copyWith(
                                        color: spot.barIndex == primaryBarIndex
                                            ? widget.primaryColor
                                            : widget.secondaryColor,
                                        fontWeight: FontWeight.w800,
                                      ),
                                    ),
                                  )
                                  .toList(),
                            ),
                            touchCallback: (event, response) {
                              final touchedIndex =
                                  response?.lineBarSpots?.isEmpty ?? true
                                  ? null
                                  : response!.lineBarSpots!.first.spotIndex;
                              if (event is FlTapUpEvent) {
                                setState(() {
                                  _selectedIndex = touchedIndex;
                                });
                              } else if (touchedIndex != null &&
                                  event is! FlPanEndEvent &&
                                  event is! FlLongPressEnd &&
                                  event is! FlPointerExitEvent) {
                                setState(() {
                                  _selectedIndex = touchedIndex;
                                });
                              }
                            },
                          ),
                          showingTooltipIndicators: _selectedIndex == null
                              ? const []
                              : [
                                  ShowingTooltipIndicators(
                                    selectedTooltipSpots,
                                  ),
                                ],
                          lineBarsData: [secondaryBar, primaryBar],
                        ),
                      ),
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class _BarChartCard extends StatefulWidget {
  const _BarChartCard({
    required this.title,
    this.leftMetricLabel,
    this.leftMetricValue,
    this.leftMetricColor,
    this.rightMetricLabel,
    this.rightMetricValue,
    required this.bars,
    required this.baseColor,
    required this.highlightedColor,
    required this.tooltipPrefix,
    this.referenceLineY,
    this.highlightedIndex,
  });

  final String title;
  final String? leftMetricLabel;
  final String? leftMetricValue;
  final Color? leftMetricColor;
  final String? rightMetricLabel;
  final String? rightMetricValue;
  final List<_ChartDatum> bars;
  final double? referenceLineY;
  final int? highlightedIndex;
  final Color baseColor;
  final Color highlightedColor;
  final String tooltipPrefix;

  @override
  State<_BarChartCard> createState() => _BarChartCardState();
}

class _BarChartCardState extends State<_BarChartCard> {
  final ScrollController _scrollController = ScrollController();
  final Map<int, Offset> _activePointers = <int, Offset>{};

  double _zoom = 1.0;
  double? _pinchStartDistance;
  double _pinchStartZoom = 1.0;
  int? _selectedIndex;

  @override
  void initState() {
    super.initState();
    _selectedIndex = widget.highlightedIndex;
    _scrollController.addListener(_handleScroll);
  }

  @override
  void dispose() {
    _scrollController
      ..removeListener(_handleScroll)
      ..dispose();
    super.dispose();
  }

  void _handleScroll() => setState(() {});

  void _zoomIn() => _setZoom(_zoom + 0.2);

  void _zoomOut() => _setZoom(_zoom - 0.2);

  void _setZoom(double value) {
    setState(() {
      _zoom = value.clamp(_minChartZoom, _maxChartZoom).toDouble();
    });
  }

  void _handlePointerDown(PointerDownEvent event) {
    _activePointers[event.pointer] = event.localPosition;
    if (_activePointers.length == 2) {
      _pinchStartDistance = _calculatePointerDistance(_activePointers);
      _pinchStartZoom = _zoom;
    }
  }

  void _handlePointerMove(PointerMoveEvent event) {
    if (!_activePointers.containsKey(event.pointer)) {
      return;
    }
    _activePointers[event.pointer] = event.localPosition;
    if (_activePointers.length != 2 || _pinchStartDistance == null) {
      return;
    }

    final distance = _calculatePointerDistance(_activePointers);
    if (distance == null || _pinchStartDistance == 0) {
      return;
    }

    _setZoom(_pinchStartZoom * (distance / _pinchStartDistance!));
  }

  void _handlePointerEnd(PointerEvent event) {
    _activePointers.remove(event.pointer);
    if (_activePointers.length < 2) {
      _pinchStartDistance = null;
      _pinchStartZoom = _zoom;
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return MoreCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            widget.title,
            style: AppTextStyles.headingMd.copyWith(
              color: colors.textPrimary,
              fontWeight: FontWeight.w700,
            ),
          ),
          if (widget.leftMetricLabel != null && widget.rightMetricLabel != null) ...[
            SizedBox(height: 10.h),
            _MetricRow(
              leftLabel: widget.leftMetricLabel!,
              leftValue: widget.leftMetricValue!,
              leftColor: widget.leftMetricColor!,
              rightLabel: widget.rightMetricLabel!,
              rightValue: widget.rightMetricValue!,
              rightColor: colors.textPrimary,
            ),
          ],
          SizedBox(height: 10.h),
          _ChartControlRow(onZoomIn: _zoomIn, onZoomOut: _zoomOut),
          SizedBox(height: 8.h),
          SizedBox(
            height: 360.h,
            child: LayoutBuilder(
              builder: (context, constraints) {
                final count = widget.bars.length;
                final stepWidth = 56.w * _zoom;
                final chartWidth = math.max(
                  constraints.maxWidth,
                  count * stepWidth,
                );
                final visibleCount = math.max(
                  1,
                  (constraints.maxWidth / stepWidth).ceil(),
                );
                final startIndex = _scrollController.hasClients
                    ? (_scrollController.offset / stepWidth).floor()
                    : 0;
                final clampedStart = startIndex
                    .clamp(0, math.max(0, count - 1))
                    .toInt();
                final endIndex = math.min(
                  count - 1,
                  clampedStart + visibleCount - 1,
                );
                final allValues = <double>[
                  for (final bar in widget.bars) bar.value,
                  if (widget.referenceLineY != null) widget.referenceLineY!,
                ];
                final bounds = _resolveBarChartBounds(allValues);
                final titleInterval = math.max(1, (visibleCount / 4).ceil());
                final barWidth = math.min(stepWidth * 0.48, 28.w).toDouble();

                return Listener(
                  onPointerDown: _handlePointerDown,
                  onPointerMove: _handlePointerMove,
                  onPointerUp: _handlePointerEnd,
                  onPointerCancel: _handlePointerEnd,
                  child: SingleChildScrollView(
                    controller: _scrollController,
                    scrollDirection: Axis.horizontal,
                    child: SizedBox(
                      width: chartWidth,
                      child: BarChart(
                        BarChartData(
                          minY: bounds.minY,
                          maxY: bounds.maxY,
                          alignment: BarChartAlignment.spaceAround,
                          borderData: FlBorderData(show: false),
                          gridData: FlGridData(
                            show: true,
                            drawVerticalLine: false,
                            horizontalInterval: bounds.interval,
                            getDrawingHorizontalLine: (_) => FlLine(
                              color: colors.borderDefault,
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
                            leftTitles: AxisTitles(
                              sideTitles: SideTitles(
                                showTitles: true,
                                reservedSize: 42.w,
                                interval: bounds.interval,
                                getTitlesWidget: (value, meta) => Padding(
                                  padding: EdgeInsets.only(right: 6.w),
                                  child: Text(
                                    _formatAxisAmount(value),
                                    style: AppTextStyles.caption.copyWith(
                                      color: colors.textTertiary,
                                    ),
                                  ),
                                ),
                              ),
                            ),
                            bottomTitles: AxisTitles(
                              sideTitles: SideTitles(
                                showTitles: true,
                                reservedSize: 28.h,
                                interval: titleInterval.toDouble(),
                                getTitlesWidget: (value, meta) {
                                  final index = value.toInt();
                                  if (index < 0 ||
                                      index >= widget.bars.length) {
                                    return const SizedBox.shrink();
                                  }
                                  return Padding(
                                    padding: EdgeInsets.only(top: 6.h),
                                    child: Text(
                                      widget.bars[index].label,
                                      style: AppTextStyles.caption.copyWith(
                                        color: colors.textTertiary,
                                      ),
                                    ),
                                  );
                                },
                              ),
                            ),
                          ),
                          barTouchData: BarTouchData(
                            enabled: true,
                            handleBuiltInTouches: false,
                            touchTooltipData: BarTouchTooltipData(
                              fitInsideHorizontally: true,
                              fitInsideVertically: true,
                              tooltipRoundedRadius: 12.r,
                              tooltipPadding: EdgeInsets.symmetric(
                                horizontal: 10.w,
                                vertical: 6.h,
                              ),
                              tooltipMargin: 8.h,
                              getTooltipColor: (_) => colors.bgSurface,
                              getTooltipItem:
                                  (
                                    group,
                                    groupIndex,
                                    rod,
                                    rodIndex,
                                  ) => BarTooltipItem(
                                    '${widget.tooltipPrefix}\n${CurrencyFormatter.formatKRW(rod.toY.round())}',
                                    AppTextStyles.caption.copyWith(
                                      color: group.x == widget.highlightedIndex
                                          ? widget.highlightedColor
                                          : widget.baseColor,
                                      fontWeight: FontWeight.w800,
                                    ),
                                  ),
                            ),
                            touchCallback: (event, response) {
                              if (event is FlTapUpEvent) {
                                setState(() {
                                  _selectedIndex = response?.spot == null
                                      ? null
                                      : response!.spot!.touchedBarGroupIndex;
                                });
                              }
                            },
                          ),
                          extraLinesData: widget.referenceLineY == null
                              ? null
                              : ExtraLinesData(
                                  horizontalLines: [
                                    HorizontalLine(
                                      y: widget.referenceLineY!,
                                      color: colors.textTertiary.withValues(
                                        alpha: 0.55,
                                      ),
                                      strokeWidth: 1.5,
                                      dashArray: const [6, 4],
                                    ),
                                  ],
                                ),
                          barGroups: List.generate(widget.bars.length, (index) {
                            return BarChartGroupData(
                              x: index,
                              showingTooltipIndicators: _selectedIndex == index
                                  ? const [0]
                                  : const [],
                              barRods: [
                                BarChartRodData(
                                  toY: widget.bars[index].value,
                                  color: index == widget.highlightedIndex
                                      ? widget.highlightedColor
                                      : widget.baseColor,
                                  width: barWidth,
                                  borderRadius: BorderRadius.only(
                                    topLeft: Radius.circular(4.r),
                                    topRight: Radius.circular(4.r),
                                  ),
                                ),
                              ],
                            );
                          }),
                        ),
                      ),
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class _ChartDatum {
  const _ChartDatum({
    required this.x,
    required this.label,
    required this.value,
  });

  final double x;
  final String label;
  final double value;
}

class _MetricRow extends StatelessWidget {
  const _MetricRow({
    required this.leftLabel,
    required this.leftValue,
    required this.leftColor,
    required this.rightLabel,
    required this.rightValue,
    required this.rightColor,
  });

  final String leftLabel;
  final String leftValue;
  final Color leftColor;
  final String rightLabel;
  final String rightValue;
  final Color rightColor;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: _MetricBlock(
            label: leftLabel,
            value: leftValue,
            color: leftColor,
          ),
        ),
        SizedBox(width: 12.w),
        Expanded(
          child: _MetricBlock(
            label: rightLabel,
            value: rightValue,
            color: rightColor,
            crossAxisAlignment: CrossAxisAlignment.end,
            textAlign: TextAlign.end,
          ),
        ),
      ],
    );
  }
}

class _MetricBlock extends StatelessWidget {
  const _MetricBlock({
    required this.label,
    required this.value,
    required this.color,
    this.crossAxisAlignment = CrossAxisAlignment.start,
    this.textAlign = TextAlign.start,
  });

  final String label;
  final String value;
  final Color color;
  final CrossAxisAlignment crossAxisAlignment;
  final TextAlign textAlign;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return Column(
      crossAxisAlignment: crossAxisAlignment,
      children: [
        Text(
          label,
          textAlign: textAlign,
          style: AppTextStyles.caption.copyWith(color: colors.textTertiary),
        ),
        SizedBox(height: 4.h),
        Text(
          value,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          textAlign: textAlign,
          style: AppTextStyles.bodySm.copyWith(
            color: color,
            fontWeight: FontWeight.w700,
          ),
        ),
      ],
    );
  }
}

class _ChartControlRow extends StatelessWidget {
  const _ChartControlRow({required this.onZoomIn, required this.onZoomOut});

  final VoidCallback onZoomIn;
  final VoidCallback onZoomOut;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        _ZoomButton(icon: Icons.remove_rounded, onTap: onZoomOut),
        SizedBox(width: 6.w),
        _ZoomButton(icon: Icons.add_rounded, onTap: onZoomIn),
      ],
    );
  }
}

class _ZoomButton extends StatelessWidget {
  const _ZoomButton({required this.icon, required this.onTap});

  final IconData icon;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return InkWell(
      borderRadius: BorderRadius.circular(999.r),
      onTap: onTap,
      child: Container(
        width: 28.w,
        height: 28.w,
        decoration: BoxDecoration(
          color: colors.gray100,
          borderRadius: BorderRadius.circular(999.r),
        ),
        child: Icon(icon, size: 18.w, color: colors.textPrimary),
      ),
    );
  }
}

class _LegendRow extends StatelessWidget {
  const _LegendRow({required this.items});

  final List<_LegendItem> items;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return Wrap(
      spacing: 12.w,
      runSpacing: 6.h,
      children: [
        for (final item in items)
          Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 12.w,
                height: 12.w,
                decoration: BoxDecoration(
                  color: item.dashed ? Colors.transparent : item.color,
                  borderRadius: BorderRadius.circular(999.r),
                  border: item.dashed
                      ? Border.all(color: item.color, width: 1.4)
                      : null,
                ),
              ),
              SizedBox(width: 6.w),
              Text(
                item.label,
                style: AppTextStyles.caption.copyWith(
                  color: colors.textTertiary,
                ),
              ),
            ],
          ),
      ],
    );
  }
}

class _LegendItem {
  const _LegendItem({
    required this.label,
    required this.color,
    this.dashed = false,
  });

  final String label;
  final Color color;
  final bool dashed;
}

class _BarChartBounds {
  const _BarChartBounds({
    required this.minY,
    required this.maxY,
    required this.interval,
  });

  final double minY;
  final double maxY;
  final double interval;
}

double? _calculatePointerDistance(Map<int, Offset> activePointers) {
  if (activePointers.length < 2) {
    return null;
  }

  final points = activePointers.values.take(2).toList(growable: false);
  return (points.first - points.last).distance;
}

List<GoalWeeklyPlan> _buildNormalizedWeeklyPlans({
  required Goal goal,
  required List<DailySpending> history,
}) {
  final historyByWeek = <int, int>{};
  for (final item in history) {
    final week = (item.date.difference(goal.startDate).inDays ~/ 7) + 1;
    if (week <= 0) {
      continue;
    }
    historyByWeek[week] = (historyByWeek[week] ?? 0) + item.amount;
  }

  final planByWeek = <int, int>{};
  for (final plan in goal.weeklyPlans) {
    if (plan.week <= 0) {
      continue;
    }
    planByWeek[plan.week] = plan.amount;
  }

  final maxPlanWeek = planByWeek.keys.isEmpty
      ? 0
      : planByWeek.keys.reduce(math.max);
  final maxHistoryWeek = historyByWeek.keys.isEmpty
      ? 0
      : historyByWeek.keys.reduce(math.max);
  final lastWeek = math.max(
    1,
    math.max(
      goal.currentWeekNumber,
      maxHistoryWeek,
    ),
  );

  return [
    for (var week = 1; week <= lastWeek; week++)
      GoalWeeklyPlan(
        week: week,
        amount: week < goal.currentWeekNumber
            ? (historyByWeek[week] ?? planByWeek[week] ?? 0)
            : (planByWeek[week] ?? historyByWeek[week] ?? 0),
      ),
  ];
}

List<double> _buildCumulativeSeries({
  required List<int> weeklyValues,
  required int totalWeeks,
}) {
  final safeTotalWeeks = math.max(totalWeeks, weeklyValues.length);
  final result = <double>[];
  var cumulative = 0.0;

  for (var i = 0; i < safeTotalWeeks; i++) {
    if (i < weeklyValues.length) {
      cumulative += weeklyValues[i].toDouble();
    }
    result.add(cumulative);
  }

  return result;
}

List<double> _buildEvenPlanSeries({
  required int totalWeeks,
  required int targetAmount,
}) {
  if (totalWeeks <= 0 || targetAmount <= 0) {
    return List<double>.filled(math.max(totalWeeks, 1), 0);
  }

  final weeklyTarget = targetAmount / totalWeeks;
  final result = <double>[];
  var cumulative = 0.0;

  for (var i = 0; i < totalWeeks; i++) {
    cumulative = math.min(targetAmount.toDouble(), cumulative + weeklyTarget);
    result.add(cumulative);
  }

  return result;
}

double _resolvePositiveChartMaxY(
  List<double> values, {
  double paddingFactor = 1.1,
}) {
  final normalized = values
      .where((value) => value.isFinite && value > 0)
      .toList();
  final maxValue = normalized.isEmpty ? 1.0 : normalized.reduce(math.max);
  return _roundUpAxisValue(maxValue * paddingFactor);
}

_BarChartBounds _resolveBarChartBounds(List<double> values) {
  final normalized = values.where((value) => value.isFinite).toList();
  if (normalized.isEmpty) {
    return const _BarChartBounds(minY: 0, maxY: 100, interval: 25);
  }

  final positiveMax = normalized.reduce(math.max);
  final negativeMin = normalized.reduce(math.min);
  final maxY = _roundUpAxisValue(math.max(positiveMax, 1) * 1.08);
  final minY = negativeMin < 0
      ? -_roundUpAxisValue(negativeMin.abs() * 1.12)
      : 0.0;
  final interval = _resolveGridInterval(maxY - minY);
  return _BarChartBounds(minY: minY, maxY: maxY, interval: interval);
}

double _resolveGridInterval(double rangeOrMax) {
  final safeRange = rangeOrMax <= 0 ? 100.0 : rangeOrMax;
  return _roundUpAxisValue(
    safeRange / 4,
  ).clamp(1.0, double.infinity).toDouble();
}

double _roundUpAxisValue(double value) {
  final safeValue = value <= 0 ? 1.0 : value;
  if (safeValue < 1000) return (safeValue / 50).ceil() * 50.0;
  if (safeValue < 10000) return (safeValue / 500).ceil() * 500.0;
  if (safeValue < 100000) return (safeValue / 5000).ceil() * 5000.0;
  return (safeValue / 10000).ceil() * 10000.0;
}

String _formatAxisAmount(double value) {
  final absValue = value.abs();
  if (absValue >= 100000000) {
    final formatted = (value / 100000000).toStringAsFixed(
      absValue % 100000000 == 0 ? 0 : 1,
    );
    return '$formatted억';
  }
  if (absValue >= 10000) {
    final formatted = (value / 10000).toStringAsFixed(
      absValue % 10000 == 0 ? 0 : 1,
    );
    return '$formatted만';
  }
  if (absValue >= 1000) {
    return '${(value / 1000).toStringAsFixed(0)}천';
  }
  return value.toStringAsFixed(0);
}
