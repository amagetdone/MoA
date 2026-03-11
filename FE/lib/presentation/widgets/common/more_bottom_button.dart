import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';

class MoreBottomButton extends StatelessWidget {
  final String text;
  final VoidCallback? onPressed;
  final bool enabled;

  const MoreBottomButton({
    super.key,
    required this.text,
    this.onPressed,
    this.enabled = true,
  });

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: EdgeInsets.fromLTRB(24.w, 8.h, 24.w, 16.h),
        child: SizedBox(
          width: double.infinity,
          height: 52.h,
          child: ElevatedButton(
            onPressed: enabled ? onPressed : null,
            style: ElevatedButton.styleFrom(
              backgroundColor:
                  enabled ? AppColors.buttonPrimary : AppColors.gray100,
              foregroundColor:
                  enabled ? AppColors.white : AppColors.textTertiary,
              elevation: 0,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12.r),
              ),
            ),
            child: Text(
              text,
              style: AppTextStyles.bodyLg.copyWith(
                color: enabled ? AppColors.white : AppColors.textTertiary,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ),
      ),
    );
  }
}
