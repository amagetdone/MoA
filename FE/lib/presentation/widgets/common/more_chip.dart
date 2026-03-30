import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';

class MoreChip extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback? onTap;

  const MoreChip({
    super.key,
    required this.label,
    this.selected = false,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 8.h),
        decoration: BoxDecoration(
          color: selected ? AppColors.orange500 : colors.bgSurface,
          borderRadius: BorderRadius.circular(20.r),
          border: Border.all(
            color: selected ? AppColors.orange500 : colors.borderDefault,
          ),
        ),
        child: Text(
          label,
          style: AppTextStyles.bodySm.copyWith(
            color: selected ? colors.white : colors.textSecondary,
            fontWeight: selected ? FontWeight.w600 : FontWeight.w400,
          ),
        ),
      ),
    );
  }
}
