import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';

class MoreBottomSheet extends StatelessWidget {
  final String? title;
  final Widget child;

  const MoreBottomSheet({
    super.key,
    this.title,
    required this.child,
  });

  static Future<T?> show<T>({
    required BuildContext context,
    String? title,
    required Widget child,
  }) {
    return showModalBottomSheet<T>(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.bgSurface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20.r)),
      ),
      builder: (_) => MoreBottomSheet(title: title, child: child),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).viewInsets.bottom,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          SizedBox(height: 12.h),
          Container(
            width: 40.w,
            height: 4.h,
            decoration: BoxDecoration(
              color: AppColors.gray100,
              borderRadius: BorderRadius.circular(2.r),
            ),
          ),
          if (title != null) ...[
            SizedBox(height: 20.h),
            Text(title!, style: AppTextStyles.headingMd),
          ],
          SizedBox(height: 16.h),
          child,
          SizedBox(height: 16.h),
        ],
      ),
    );
  }
}
