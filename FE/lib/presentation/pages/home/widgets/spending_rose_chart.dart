import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/spending.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';

const _donutColors = [
  Color(0xFF5B8DEF),
  Color(0xFFFFB74D),
  Color(0xFF81C784),
  Color(0xFFE57373),
  Color(0xFF90A4AE),
];

class SpendingRoseChart extends StatefulWidget {
  const SpendingRoseChart({
    super.key,
    required this.categories,
    required this.title,
    this.subtitle,
    this.onPrevious,
    this.onNext,
    this.canMoveNext = false,
  });

  final List<SpendingCategory> categories;
  final String title;
  final String? subtitle;
  final VoidCallback? onPrevious;
  final VoidCallback? onNext;
  final bool canMoveNext;

  @override
  State<SpendingRoseChart> createState() => _SpendingRoseChartState();
}

class _SpendingRoseChartState extends State<SpendingRoseChart> {
  static const _collapsedCategoryNames = {'기타', '기타서비스'};
  static const _maxVisibleCategories = 4;

  int? _selectedIndex;

  List<_DonutEntry> _buildEntries() {
    if (widget.categories.isEmpty) return [];

    final merged = <String, int>{};
    for (final c in widget.categories) {
      final normalizedName = c.name.trim();
      merged[normalizedName] = (merged[normalizedName] ?? 0) + c.amount;
    }

    final sorted =
        merged.entries
            .where((entry) => !_collapsedCategoryNames.contains(entry.key))
            .toList()
          ..sort((a, b) => b.value.compareTo(a.value));

    final entries = <_DonutEntry>[];
    int othersAmount = 0;

    for (final categoryName in _collapsedCategoryNames) {
      othersAmount += merged[categoryName] ?? 0;
    }

    for (int i = 0; i < sorted.length; i++) {
      if (i < _maxVisibleCategories) {
        entries.add(
          _DonutEntry(
            name: sorted[i].key,
            amount: sorted[i].value,
            color: _donutColors[i],
          ),
        );
      } else {
        othersAmount += sorted[i].value;
      }
    }

    if (othersAmount > 0) {
      entries.add(
        _DonutEntry(name: '기타', amount: othersAmount, color: _donutColors[4]),
      );
    }

    return entries;
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final entries = _buildEntries();
    final total = entries.fold<int>(0, (s, e) => s + e.amount);

    if (entries.isEmpty || total == 0) {
      return const SizedBox.shrink();
    }

    final centerEntry =
        _selectedIndex != null && _selectedIndex! < entries.length
        ? entries[_selectedIndex!]
        : null;

    return LayoutBuilder(
      builder: (context, constraints) {
        final isCompact = constraints.maxWidth < 340;
        final hasBoundedHeight = constraints.maxHeight.isFinite;
        final hasSelection =
            _selectedIndex != null && _selectedIndex! < entries.length;
        final cardPadding = isCompact ? 14.w : 16.w;
        final contentWidth = constraints.maxWidth - (cardPadding * 2);
        final centerSpaceRadius = isCompact ? 48.w : 55.w;
        final baseRadius = isCompact ? 28.w : 32.w;
        final selectedRadius = isCompact ? 34.w : 38.w;
        final titleFontSize = 11.sp;
        final legendFontSize = hasSelection
            ? (isCompact ? 8.sp : 9.sp)
            : (isCompact ? 9.sp : 10.sp);
        final chartHeight = (contentWidth * 0.72)
            .clamp(180.w, 240.w)
            .toDouble();

        final chart = Stack(
          alignment: Alignment.center,
          children: [
            PieChart(
              PieChartData(
                sectionsSpace: 2,
                centerSpaceRadius: centerSpaceRadius,
                startDegreeOffset: -90,
                pieTouchData: PieTouchData(
                  touchCallback: (event, response) {
                    if (response == null || response.touchedSection == null) {
                      return;
                    }
                    if (event is FlTapUpEvent) {
                      final idx = response.touchedSection!.touchedSectionIndex;
                      if (idx >= 0 && idx < entries.length) {
                        setState(() {
                          _selectedIndex = _selectedIndex == idx ? null : idx;
                        });
                      }
                    }
                  },
                ),
                sections: entries.asMap().entries.map((e) {
                  final idx = e.key;
                  final entry = e.value;
                  final isSelected = _selectedIndex == idx;
                  return PieChartSectionData(
                    value: entry.amount.toDouble(),
                    color: entry.color,
                    radius: isSelected ? selectedRadius : baseRadius,
                    title: '',
                  );
                }).toList(),
              ),
            ),
            IgnorePointer(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    centerEntry?.name ?? '총 소비',
                    style: AppTextStyles.caption.copyWith(
                      color: colors.textTertiary,
                      fontSize: titleFontSize,
                    ),
                  ),
                  SizedBox(height: 2.h),
                  Text(
                    CurrencyFormatter.formatCompact(
                      centerEntry?.amount ?? total,
                    ),
                    style: AppTextStyles.headingMd.copyWith(
                      color: colors.textPrimary,
                      fontSize: isCompact ? 14.sp : 15.sp,
                    ),
                  ),
                ],
              ),
            ),
          ],
        );

        return MoreCard(
          padding: EdgeInsets.all(cardPadding),
          child: Column(
            mainAxisSize: hasBoundedHeight
                ? MainAxisSize.max
                : MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          widget.title,
                          style: AppTextStyles.bodyMd.copyWith(
                            fontWeight: FontWeight.w700,
                            color: colors.textPrimary,
                          ),
                        ),
                        if (widget.subtitle != null) ...[
                          SizedBox(height: 4.h),
                          Text(
                            widget.subtitle!,
                            style: AppTextStyles.caption.copyWith(
                              color: colors.textTertiary,
                              fontSize: titleFontSize,
                            ),
                          ),
                        ],
                      ],
                    ),
                  ),
                ],
              ),
              SizedBox(height: hasSelection ? 8.h : (isCompact ? 10.h : 14.h)),
              if (hasBoundedHeight) Expanded(child: chart),
              if (!hasBoundedHeight)
                SizedBox(height: chartHeight, child: chart),
              if (_selectedIndex != null &&
                  _selectedIndex! < entries.length) ...[
                SizedBox(height: isCompact ? 4.h : 6.h),
                _SelectedInfo(
                  entry: entries[_selectedIndex!],
                  total: total,
                  compact: isCompact,
                ),
              ],
              SizedBox(height: hasSelection ? 4.h : (isCompact ? 8.h : 10.h)),
              Center(
                child: Wrap(
                  alignment: WrapAlignment.center,
                  spacing: hasSelection
                      ? (isCompact ? 8.w : 12.w)
                      : (isCompact ? 10.w : 14.w),
                  runSpacing: hasSelection ? 4.h : 6.h,
                  children: entries.map((e) {
                    final percent = total > 0
                        ? (e.amount / total * 100).toStringAsFixed(0)
                        : '0';
                    return Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Container(
                          width: 8.w,
                          height: 8.w,
                          decoration: BoxDecoration(
                            color: e.color,
                            shape: BoxShape.circle,
                          ),
                        ),
                        SizedBox(width: 4.w),
                        Text(
                          '${e.name} $percent%',
                          style: TextStyle(
                            color: colors.textTertiary,
                            fontSize: legendFontSize,
                          ),
                        ),
                      ],
                    );
                  }).toList(),
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}

class _SelectedInfo extends StatelessWidget {
  const _SelectedInfo({
    required this.entry,
    required this.total,
    required this.compact,
  });
  final _DonutEntry entry;
  final int total;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final percent = total > 0
        ? (entry.amount / total * 100).toStringAsFixed(1)
        : '0';
    return Container(
      padding: EdgeInsets.symmetric(
        horizontal: compact ? 10.w : 14.w,
        vertical: compact ? 7.h : 9.h,
      ),
      decoration: BoxDecoration(
        color: entry.color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(10.r),
      ),
      child: Row(
        children: [
          Container(
            width: 10.w,
            height: 10.w,
            decoration: BoxDecoration(
              color: entry.color,
              shape: BoxShape.circle,
            ),
          ),
          SizedBox(width: 8.w),
          Text(
            entry.name,
            style: AppTextStyles.bodySm.copyWith(
              fontWeight: FontWeight.w700,
              color: colors.textPrimary,
              fontSize: compact ? 12.sp : null,
            ),
          ),
          const Spacer(),
          Text(
            '$percent%',
            style: TextStyle(
              color: entry.color,
              fontSize: compact ? 12.sp : 13.sp,
              fontWeight: FontWeight.w700,
            ),
          ),
          SizedBox(width: 8.w),
          Text(
            CurrencyFormatter.formatKRW(entry.amount),
            style: AppTextStyles.bodySm.copyWith(
              fontWeight: FontWeight.w600,
              color: colors.textPrimary,
              fontSize: compact ? 11.sp : null,
            ),
          ),
        ],
      ),
    );
  }
}

class _DonutEntry {
  final String name;
  final int amount;
  final Color color;
  const _DonutEntry({
    required this.name,
    required this.amount,
    required this.color,
  });
}
