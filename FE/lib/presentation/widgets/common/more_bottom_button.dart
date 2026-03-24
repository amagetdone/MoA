import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';

class MoreBottomButton extends StatelessWidget {
  final String text;
  final VoidCallback? onPressed;
  final bool enabled;
  final double? progress;

  const MoreBottomButton({
    super.key,
    required this.text,
    this.onPressed,
    this.enabled = true,
    this.progress,
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return SafeArea(
      child: Padding(
        padding: EdgeInsets.fromLTRB(24.w, 8.h, 24.w, 16.h),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (progress != null) ...[
              SizedBox(
                height: 6.h,
                width: double.infinity,
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(4.r),
                  child: Stack(
                    children: [
                      // Background
                      Container(
                        decoration: BoxDecoration(
                          color: colors.gray100,
                          borderRadius: BorderRadius.circular(4.r),
                        ),
                      ),
                      // Gradient fill
                      FractionallySizedBox(
                        widthFactor: progress!.clamp(0.0, 1.0),
                        child: Container(
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(4.r),
                            gradient: LinearGradient(
                              colors: [
                                colors.white,
                                AppColors.orange500,
                              ],
                              stops: const [0.0, 0.7],
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              SizedBox(height: 12.h),
            ],
            SizedBox(
              width: double.infinity,
              height: 52.h,
              child: ElevatedButton(
                onPressed: enabled ? onPressed : null,
                style: ButtonStyle(
                  backgroundColor: WidgetStateProperty.resolveWith((states) {
                    if (!enabled) return colors.gray100;
                    if (states.contains(WidgetState.pressed)) return AppColors.orange600;
                    return colors.buttonPrimary;
                  }),
                  foregroundColor: WidgetStateProperty.all(
                    enabled ? colors.white : colors.textTertiary,
                  ),
                  elevation: WidgetStateProperty.all(0),
                  shape: WidgetStateProperty.all(
                    RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12.r),
                    ),
                  ),
                ),
                child: Text(
                  text,
                  style: AppTextStyles.bodyLg.copyWith(
                    color: enabled ? colors.white : colors.textTertiary,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
