import 'package:flutter/material.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/domain/entities/spending.dart';
import 'package:more_app/presentation/widgets/common/more_progress_bar.dart';

class CategoryList extends StatelessWidget {
  final List<SpendingCategory> categories;

  const CategoryList({
    super.key,
    required this.categories,
  });

  @override
  Widget build(BuildContext context) {
    return ListView.separated(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      itemCount: categories.length,
      separatorBuilder: (context, index) => const SizedBox(height: 12),
      itemBuilder: (context, index) {
        final category = categories[index];
        return _CategoryRow(category: category);
      },
    );
  }
}

class _CategoryRow extends StatelessWidget {
  final SpendingCategory category;

  const _CategoryRow({
    required this.category,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Row(
          children: [
            Container(
              width: 36,
              height: 36,
              decoration: BoxDecoration(
                color: category.color.withValues(alpha: 0.15),
                shape: BoxShape.circle,
              ),
              child: Icon(
                category.icon,
                size: 18,
                color: category.color,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    category.name,
                    style: AppTextStyles.bodyMd.copyWith(
                      color: AppColors.textPrimary,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ],
              ),
            ),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  CurrencyFormatter.formatKRW(category.amount),
                  style: AppTextStyles.bodyMd.copyWith(
                    color: AppColors.textPrimary,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  '/ ${CurrencyFormatter.formatKRW(category.budget)}',
                  style: AppTextStyles.caption.copyWith(
                    color: AppColors.textTertiary,
                  ),
                ),
              ],
            ),
          ],
        ),
        const SizedBox(height: 8),
        MoreProgressBar(
          progress: category.usageRate.clamp(0.0, 1.0),
          color: category.usageRate > 1.0 ? AppColors.red500 : category.color,
          backgroundColor: AppColors.borderDefault,
          height: 6,
        ),
      ],
    );
  }
}
