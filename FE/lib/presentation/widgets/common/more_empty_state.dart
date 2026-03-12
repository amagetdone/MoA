import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';

class MoreEmptyState extends StatelessWidget {
  final String message;
  final String? buttonText;
  final VoidCallback? onTap;
  final IconData icon;

  const MoreEmptyState({
    super.key,
    required this.message,
    this.buttonText,
    this.onTap,
    this.icon = Icons.add_circle_outline,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: double.infinity,
        padding: EdgeInsets.symmetric(vertical: 40.h, horizontal: 24.w),
        decoration: BoxDecoration(
          color: AppColors.bgSurface,
          borderRadius: BorderRadius.circular(16.r),
          border: Border.all(color: AppColors.borderDefault),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 48.w, color: AppColors.textTertiary),
            SizedBox(height: 12.h),
            Text(
              message,
              style: AppTextStyles.bodySm.copyWith(color: AppColors.textTertiary),
              textAlign: TextAlign.center,
            ),
            if (buttonText != null) ...[
              SizedBox(height: 16.h),
              Container(
                padding: EdgeInsets.symmetric(horizontal: 20.w, vertical: 10.h),
                decoration: BoxDecoration(
                  color: AppColors.orange50,
                  borderRadius: BorderRadius.circular(8.r),
                ),
                child: Text(
                  buttonText!,
                  style: AppTextStyles.bodySm.copyWith(
                    color: AppColors.orange500,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
