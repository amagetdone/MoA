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
    required this.weekLabel,
  });

  final List<SpendingCategory> categories;
  final String weekLabel;

  @override
  State<SpendingRoseChart> createState() => _SpendingRoseChartState();
}

class _SpendingRoseChartState extends State<SpendingRoseChart> {
  int? _selectedIndex;

  List<_DonutEntry> _buildEntries() {
    if (widget.categories.isEmpty) return [];

    final merged = <String, int>{};
    for (final c in widget.categories) {
      merged[c.name] = (merged[c.name] ?? 0) + c.amount;
    }

    final sorted = merged.entries.toList()
      ..sort((a, b) => b.value.compareTo(a.value));

    final entries = <_DonutEntry>[];
    int othersAmount = 0;

    for (int i = 0; i < sorted.length; i++) {
      if (i < 4) {
        entries.add(_DonutEntry(
          name: sorted[i].key,
          amount: sorted[i].value,
          color: _donutColors[i],
        ));
      } else {
        othersAmount += sorted[i].value;
      }
    }

    if (othersAmount > 0) {
      entries.add(_DonutEntry(
        name: '기타',
        amount: othersAmount,
        color: _donutColors[4],
      ));
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

    return MoreCard(
      padding: EdgeInsets.all(16.w),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '소비',
            style: AppTextStyles.bodyMd.copyWith(
              fontWeight: FontWeight.w700,
              color: colors.textPrimary,
            ),
          ),
          SizedBox(height: 16.h),

          // Donut chart
          SizedBox(
            height: 220.h,
            child: Stack(
              alignment: Alignment.center,
              children: [
                PieChart(
                  PieChartData(
                    sectionsSpace: 2,
                    centerSpaceRadius: 55.w,
                    startDegreeOffset: -90,
                    pieTouchData: PieTouchData(
                      touchCallback: (event, response) {
                        if (response == null ||
                            response.touchedSection == null) {
                          return;
                        }
                        if (event is FlTapUpEvent) {
                          final idx =
                              response.touchedSection!.touchedSectionIndex;
                          if (idx >= 0 && idx < entries.length) {
                            setState(() {
                              _selectedIndex =
                                  _selectedIndex == idx ? null : idx;
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
                        radius: isSelected ? 38.w : 32.w,
                        title: '',
                      );
                    }).toList(),
                  ),
                ),
                // Center label
                IgnorePointer(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        centerEntry?.name ?? '총 소비',
                        style: AppTextStyles.caption.copyWith(
                          color: colors.textTertiary,
                          fontSize: 11.sp,
                        ),
                      ),
                      SizedBox(height: 2.h),
                      Text(
                        CurrencyFormatter.formatCompact(
                          centerEntry?.amount ?? total,
                        ),
                        style: AppTextStyles.headingMd.copyWith(
                          color: colors.textPrimary,
                          fontSize: 15.sp,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),

          // Selected category detail
          if (_selectedIndex != null && _selectedIndex! < entries.length) ...[
            SizedBox(height: 8.h),
            _SelectedInfo(entry: entries[_selectedIndex!], total: total),
          ],

          SizedBox(height: 12.h),

          // Legend
          Center(
            child: Wrap(
              alignment: WrapAlignment.center,
              spacing: 14.w,
              runSpacing: 6.h,
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
                        fontSize: 10.sp,
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
  }
}

class _SelectedInfo extends StatelessWidget {
  const _SelectedInfo({required this.entry, required this.total});
  final _DonutEntry entry;
  final int total;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final percent = total > 0
        ? (entry.amount / total * 100).toStringAsFixed(1)
        : '0';
    return Container(
      padding: EdgeInsets.symmetric(horizontal: 14.w, vertical: 10.h),
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
            ),
          ),
          const Spacer(),
          Text(
            '$percent%',
            style: TextStyle(
              color: entry.color,
              fontSize: 13.sp,
              fontWeight: FontWeight.w700,
            ),
          ),
          SizedBox(width: 8.w),
          Text(
            CurrencyFormatter.formatKRW(entry.amount),
            style: AppTextStyles.bodySm.copyWith(
              fontWeight: FontWeight.w600,
              color: colors.textPrimary,
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
