import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:graphic/graphic.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/spending.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';

const _petalColors = [
  Color(0xFF5B8DEF),
  Color(0xFFFFB74D),
  Color(0xFF81C784),
  Color(0xFFE57373),
  Color(0xFF9575CD),
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

  List<_RoseEntry> _buildEntries() {
    if (widget.categories.isEmpty) return [];

    // 같은 이름 카테고리 합치기
    final merged = <String, int>{};
    for (final c in widget.categories) {
      merged[c.name] = (merged[c.name] ?? 0) + c.amount;
    }

    final sorted = merged.entries.toList()
      ..sort((a, b) => b.value.compareTo(a.value));

    final entries = <_RoseEntry>[];
    int othersAmount = 0;

    for (int i = 0; i < sorted.length; i++) {
      if (i < 5) {
        entries.add(_RoseEntry(
          name: sorted[i].key,
          amount: sorted[i].value,
          color: _petalColors[i],
        ));
      } else {
        othersAmount += sorted[i].value;
      }
    }

    if (othersAmount > 0) {
      entries.add(_RoseEntry(
        name: '기타',
        amount: othersAmount,
        color: _petalColors[5],
      ));
    }

    return entries;
  }

  @override
  Widget build(BuildContext context) {
    final entries = _buildEntries();
    final total = entries.fold<int>(0, (s, e) => s + e.amount);

    if (entries.isEmpty || total == 0) {
      return const SizedBox.shrink();
    }

    final chartData = entries
        .asMap()
        .entries
        .map((e) => {
          'name': e.value.name,
          'value': e.value.amount,
          'index': e.key.toString(),
        })
        .toList();

    return MoreCard(
      padding: EdgeInsets.all(16.w),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text(
                '이번 주 소비',
                style: AppTextStyles.bodyMd.copyWith(fontWeight: FontWeight.w700),
              ),
              const Spacer(),
              Text(
                widget.weekLabel,
                style: TextStyle(
                  color: AppColors.textTertiary,
                  fontSize: 11.sp,
                ),
              ),
            ],
          ),
          SizedBox(height: 16.h),

          SizedBox(
            height: 240.h,
            child: Chart(
              data: chartData,
              variables: {
                'name': Variable(
                  accessor: (Map map) => map['name'] as String,
                ),
                'value': Variable(
                  accessor: (Map map) => map['value'] as num,
                  scale: LinearScale(min: 0, marginMax: 0.1),
                ),
                'index': Variable(
                  accessor: (Map map) => map['index'] as String,
                ),
              },
              marks: [
                IntervalMark(
                  shape: ShapeEncode(
                    value: RectShape(
                      borderRadius: BorderRadius.all(Radius.circular(6.r)),
                    ),
                  ),
                  color: ColorEncode(
                    variable: 'index',
                    values: entries.length >= 2
                        ? entries.map((e) => e.color).toList()
                        : [entries.first.color, entries.first.color],
                  ),
                  elevation: ElevationEncode(value: 3),
                  transition: Transition(
                    duration: const Duration(milliseconds: 800),
                    curve: Curves.elasticOut,
                  ),
                  entrance: {MarkEntrance.y},
                ),
              ],
              coord: PolarCoord(startRadius: 0.15),
              selections: {
                'tap': PointSelection(
                  on: {GestureType.tap},
                  toggle: true,
                ),
              },
              tooltip: TooltipGuide(
                renderer: (size, anchor, selectedTuples) {
                  if (selectedTuples.isEmpty) {
                    WidgetsBinding.instance.addPostFrameCallback((_) {
                      if (mounted && _selectedIndex != null) {
                        setState(() => _selectedIndex = null);
                      }
                    });
                    return [];
                  }

                  final tuple = selectedTuples.values.first;
                  final idxStr = tuple['index']?.toString() ?? '';
                  final idx = int.tryParse(idxStr) ?? -1;

                  WidgetsBinding.instance.addPostFrameCallback((_) {
                    if (mounted && _selectedIndex != idx) {
                      setState(() => _selectedIndex = idx);
                    }
                  });

                  // 투명 요소 반환 (기본 툴팁 숨김, 우리 커스텀 UI 사용)
                  return [];
                },
              ),
            ),
          ),

          // 선택된 카테고리 정보
          if (_selectedIndex != null && _selectedIndex! < entries.length) ...[
            SizedBox(height: 8.h),
            _SelectedInfo(entry: entries[_selectedIndex!], total: total),
          ],

          SizedBox(height: 12.h),

          // 범례
          Wrap(
            spacing: 12.w,
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
                      color: AppColors.textTertiary,
                      fontSize: 10.sp,
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
}

class _SelectedInfo extends StatelessWidget {
  const _SelectedInfo({required this.entry, required this.total});
  final _RoseEntry entry;
  final int total;

  @override
  Widget build(BuildContext context) {
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
            style: AppTextStyles.bodySm.copyWith(fontWeight: FontWeight.w700),
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
            style: AppTextStyles.bodySm.copyWith(fontWeight: FontWeight.w600),
          ),
        ],
      ),
    );
  }
}

class _RoseEntry {
  final String name;
  final int amount;
  final Color color;
  const _RoseEntry({
    required this.name,
    required this.amount,
    required this.color,
  });
}
