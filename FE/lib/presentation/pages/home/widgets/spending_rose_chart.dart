import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
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
  int? _tappedIndex;

  List<_RoseEntry> _buildEntries() {
    if (widget.categories.isEmpty) return [];

    // 금액 기준 내림차순 정렬
    final sorted = [...widget.categories]..sort((a, b) => b.amount.compareTo(a.amount));

    final entries = <_RoseEntry>[];
    int othersAmount = 0;

    for (int i = 0; i < sorted.length; i++) {
      if (i < 5) {
        entries.add(_RoseEntry(
          name: sorted[i].name,
          amount: sorted[i].amount,
          color: _petalColors[i],
        ));
      } else {
        othersAmount += sorted[i].amount;
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

          // Rose chart
          SizedBox(
            height: 220.h,
            child: GestureDetector(
              onTapDown: (details) => _handleTap(details, entries, total),
              onTapUp: (_) {},
              child: CustomPaint(
                size: Size.infinite,
                painter: _RosePainter(
                  entries: entries,
                  total: total,
                  tappedIndex: _tappedIndex,
                ),
              ),
            ),
          ),

          // 선택된 카테고리 정보
          if (_tappedIndex != null && _tappedIndex! < entries.length) ...[
            SizedBox(height: 12.h),
            _SelectedInfo(entry: entries[_tappedIndex!], total: total),
          ],

          SizedBox(height: 12.h),

          // 범례
          Wrap(
            spacing: 12.w,
            runSpacing: 6.h,
            children: entries.map((e) {
              final percent = total > 0 ? (e.amount / total * 100).toStringAsFixed(0) : '0';
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

  void _handleTap(TapDownDetails details, List<_RoseEntry> entries, int total) {
    final box = context.findRenderObject() as RenderBox;
    final size = box.size;
    // chart area: 16.w padding, starts after header (~40h)
    final chartCenter = Offset(size.width / 2, 16.w + 40.h + 220.h / 2);
    final tapPos = details.localPosition;
    final dx = tapPos.dx - chartCenter.dx;
    final dy = tapPos.dy - chartCenter.dy;
    final dist = math.sqrt(dx * dx + dy * dy);
    final maxRadius = math.min(220.h, size.width - 32.w) / 2 * 0.85;

    if (dist > maxRadius || dist < 15) {
      setState(() => _tappedIndex = null);
      return;
    }

    var angle = math.atan2(dy, dx);
    if (angle < 0) angle += 2 * math.pi;
    // painter starts at -pi/2, so adjust
    angle = (angle + math.pi / 2) % (2 * math.pi);

    final sweepAngle = 2 * math.pi / entries.length;
    final index = (angle / sweepAngle).floor();

    setState(() {
      _tappedIndex = index == _tappedIndex ? null : index;
    });
  }
}

class _SelectedInfo extends StatelessWidget {
  const _SelectedInfo({required this.entry, required this.total});
  final _RoseEntry entry;
  final int total;

  @override
  Widget build(BuildContext context) {
    final percent = total > 0 ? (entry.amount / total * 100).toStringAsFixed(1) : '0';
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
            decoration: BoxDecoration(color: entry.color, shape: BoxShape.circle),
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
  const _RoseEntry({required this.name, required this.amount, required this.color});
}

class _RosePainter extends CustomPainter {
  _RosePainter({
    required this.entries,
    required this.total,
    this.tappedIndex,
  });

  final List<_RoseEntry> entries;
  final int total;
  final int? tappedIndex;

  @override
  void paint(Canvas canvas, Size size) {
    if (entries.isEmpty || total == 0) return;

    final center = Offset(size.width / 2, size.height / 2);
    final maxRadius = math.min(size.width, size.height) / 2 * 0.85;
    final minRadius = maxRadius * 0.2;
    final sweepAngle = 2 * math.pi / entries.length;
    final gap = 0.04; // 꽃잎 사이 간격 (radian)

    // 최대 금액 기준으로 반지름 계산
    final maxAmount = entries.map((e) => e.amount).reduce(math.max);

    for (int i = 0; i < entries.length; i++) {
      final entry = entries[i];
      final ratio = maxAmount > 0 ? entry.amount / maxAmount : 0.0;
      final radius = minRadius + (maxRadius - minRadius) * ratio;
      final isTapped = tappedIndex == i;
      final startAngle = -math.pi / 2 + sweepAngle * i + gap / 2;
      final actualSweep = sweepAngle - gap;

      final paint = Paint()
        ..color = isTapped ? entry.color : entry.color.withValues(alpha: 0.75)
        ..style = PaintingStyle.fill;

      // 선택 시 살짝 바깥으로
      var drawCenter = center;
      if (isTapped) {
        final midAngle = startAngle + actualSweep / 2;
        drawCenter = Offset(
          center.dx + math.cos(midAngle) * 6,
          center.dy + math.sin(midAngle) * 6,
        );
      }

      final path = Path()
        ..moveTo(drawCenter.dx, drawCenter.dy)
        ..arcTo(
          Rect.fromCircle(center: drawCenter, radius: isTapped ? radius + 4 : radius),
          startAngle,
          actualSweep,
          false,
        )
        ..close();

      // 그림자
      if (isTapped) {
        canvas.drawShadow(path, Colors.black, 4, false);
      }

      canvas.drawPath(path, paint);
    }
  }

  @override
  bool shouldRepaint(covariant _RosePainter oldDelegate) {
    return oldDelegate.tappedIndex != tappedIndex ||
        oldDelegate.entries != entries;
  }
}
