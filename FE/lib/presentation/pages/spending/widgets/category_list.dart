import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/spending.dart';
import 'package:more_app/presentation/utils/spending_ui_mapper.dart';
import 'package:more_app/presentation/widgets/common/more_progress_bar.dart';

class CategoryList extends StatefulWidget {
  final List<SpendingCategory> categories;
  final int? totalAmount;

  const CategoryList({
    super.key,
    required this.categories,
    this.totalAmount,
  });

  @override
  State<CategoryList> createState() => _CategoryListState();
}

class _CategoryListState extends State<CategoryList> {
  bool _isExpanded = false;

  @override
  Widget build(BuildContext context) {
    // 금액 순 정렬
    final sortedCategories = [...widget.categories]
      ..sort((a, b) => b.amount.compareTo(a.amount));
    
    final displayItems = _isExpanded ? sortedCategories : sortedCategories.take(3).toList();

    return Column(
      children: [
        ...displayItems.map((category) {
          final bool useTotalRatio = widget.totalAmount != null && widget.totalAmount! > 0;
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
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                // 1. 카테고리명 (좌측 하단)
                SizedBox(
                  width: 55.w,
                  child: Padding(
                    padding: EdgeInsets.only(bottom: 4.h),
                    child: Text(
                      category.name,
                      style: AppTextStyles.bodyMd.copyWith(
                        color: AppColors.textSecondary,
                        fontWeight: FontWeight.w600,
                        fontSize: 14.sp,
                      ),
                    ),
                  ),
                ),
                SizedBox(width: 8.w),
                // 2. 우측 금액 및 바 그래프
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          Text(
                            useTotalRatio
                                ? '${CurrencyFormatter.formatKRW(category.amount)} / ${CurrencyFormatter.formatKRW(widget.totalAmount!)}'
                                : CurrencyFormatter.formatKRW(category.amount),
                            style: AppTextStyles.caption.copyWith(
                              color: AppColors.textSecondary,
                              fontSize: 12.sp,
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                          if (useTotalRatio) ...[
                            SizedBox(width: 6.w),
                            Text(
                              '(${displayPercent.toStringAsFixed(1)}%)',
                              style: AppTextStyles.caption.copyWith(
                                color: AppColors.orange500,
                                fontSize: 12.sp,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ],
                        ],
                      ),
                      SizedBox(height: 6.h),
                      MoreProgressBar(
                        progress: barRatio,
                        color: AppColors.orange500,
                        backgroundColor: const Color(0xFFF0F0F0),
                        height: 14.h,
                      ),
                    ],
                  ),
                ),
              ],
            ),
          );
        }).toList(),

        // 확장/축소 화살표
        if (widget.categories.length > 3)
          GestureDetector(
            onTap: () => setState(() => _isExpanded = !_isExpanded),
            child: Icon(
              _isExpanded ? Icons.keyboard_arrow_up : Icons.keyboard_arrow_down,
              color: AppColors.gray900,
              size: 28.w,
            ),
          ),
      ],
    );
  }
}
