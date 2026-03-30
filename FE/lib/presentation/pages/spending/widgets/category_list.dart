import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/spending.dart';
import 'package:more_app/presentation/widgets/common/more_progress_bar.dart';

class CategoryList extends StatefulWidget {
  final List<SpendingCategory> categories;
  final int? totalAmount;

  const CategoryList({super.key, required this.categories, this.totalAmount});

  @override
  State<CategoryList> createState() => _CategoryListState();
}

class _CategoryListState extends State<CategoryList> {
  bool _isExpanded = false;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final textScaler = MediaQuery.textScalerOf(context);
    final labelStyle = AppTextStyles.bodyMd.copyWith(
      color: colors.textSecondary,
      fontWeight: FontWeight.w600,
      fontSize: 14.sp,
    );
    // 금액 순 정렬
    final sortedCategories = [...widget.categories]
      ..sort((a, b) => b.amount.compareTo(a.amount));

    final displayItems = _isExpanded
        ? sortedCategories
        : sortedCategories.take(3).toList();
    final labelWidth = _measureLabelWidth(
      context: context,
      labels: sortedCategories.map((category) => category.name),
      style: labelStyle,
      textScaler: textScaler,
    );
    final contentGap = 12.w;

    return Column(
      children: [
        ...displayItems.map((category) {
          final bool useTotalRatio =
              widget.totalAmount != null && widget.totalAmount! > 0;
          final backendRatio = category.ratio; // 백엔드에서 % 단위로 옴 (예: 25.5)
          final bool useBackendRatio = backendRatio != null && backendRatio > 0;

          // 텍스트 표시용 % 값
          final displayPercent = useBackendRatio
              ? backendRatio
              : useTotalRatio
              ? (category.amount / widget.totalAmount! * 100)
              : category.budget > 0
              ? (category.amount / category.budget * 100)
              : 0.0;
          // 프로그레스 바용 0.0~1.0
          final barRatio = (displayPercent / 100).clamp(0.0, 1.0);

          return Padding(
            padding: EdgeInsets.only(bottom: 24.h),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    SizedBox(
                      width: labelWidth,
                      child: Text(
                        category.name,
                        textAlign: TextAlign.left,
                        style: labelStyle,
                        maxLines: 1,
                        softWrap: false,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    SizedBox(width: contentGap),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Align(
                            alignment: Alignment.centerRight,
                            child: FittedBox(
                              fit: BoxFit.scaleDown,
                              alignment: Alignment.centerRight,
                              child: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Text(
                                    useTotalRatio
                                        ? '${CurrencyFormatter.formatKRW(category.amount)} / ${CurrencyFormatter.formatKRW(widget.totalAmount!)}'
                                        : CurrencyFormatter.formatKRW(
                                            category.amount,
                                          ),
                                    style: AppTextStyles.caption.copyWith(
                                      color: colors.textSecondary,
                                      fontSize: 11.sp,
                                      fontWeight: FontWeight.w600,
                                    ),
                                  ),
                                  if (useTotalRatio) ...[
                                    SizedBox(width: 6.w),
                                    Text(
                                      '(${displayPercent.toStringAsFixed(1)}%)',
                                      style: AppTextStyles.caption.copyWith(
                                        color: AppColors.orange500,
                                        fontSize: 11.sp,
                                        fontWeight: FontWeight.w700,
                                      ),
                                    ),
                                  ],
                                ],
                              ),
                            ),
                          ),
                          SizedBox(height: 10.h),
                          FractionallySizedBox(
                            widthFactor: 0.9,
                            child: MoreProgressBar(
                              progress: barRatio,
                              color: AppColors.orange500,
                              backgroundColor: colors.gray50,
                              height: 14.h,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ],
            ),
          );
        }),

        // 확장/축소 화살표
        if (widget.categories.length > 3)
          GestureDetector(
            onTap: () => setState(() => _isExpanded = !_isExpanded),
            child: Icon(
              _isExpanded ? Icons.keyboard_arrow_up : Icons.keyboard_arrow_down,
              color: colors.gray900,
              size: 28.w,
            ),
          ),
      ],
    );
  }

  double _measureLabelWidth({
    required BuildContext context,
    required Iterable<String> labels,
    required TextStyle style,
    required TextScaler textScaler,
  }) {
    final textDirection = Directionality.of(context);
    var maxWidth = 0.0;

    for (final label in labels) {
      final painter = TextPainter(
        text: TextSpan(text: label, style: style),
        textDirection: textDirection,
        textScaler: textScaler,
        maxLines: 1,
      )..layout();
      if (painter.width > maxWidth) {
        maxWidth = painter.width;
      }
    }

    return maxWidth + 16.w;
  }
}
