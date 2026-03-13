import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';

class MoreAppBar extends StatelessWidget implements PreferredSizeWidget {
  final String? title;
  final bool showBack;
  final bool showNotification;
  final String? skipText;
  final VoidCallback? onSkip;
  final VoidCallback? onBack;
  final List<Widget>? actions;
  final Color? backgroundColor;

  const MoreAppBar({
    super.key,
    this.title,
    this.showBack = true,
    this.showNotification = false,
    this.skipText,
    this.onSkip,
    this.onBack,
    this.actions,
    this.backgroundColor,
  });

  @override
  Size get preferredSize => Size.fromHeight(50.h);

  @override
  Widget build(BuildContext context) {
    final actionWidgets = <Widget>[
      if (skipText != null)
        TextButton(
          onPressed: onSkip,
          child: Text(
            skipText!,
            style: AppTextStyles.bodySm.copyWith(color: AppColors.textTertiary),
          ),
        ),
      if (showNotification)
        IconButton(
          icon: Icon(Icons.notifications_none, size: 24.w, color: AppColors.textPrimary),
          onPressed: () {},
        ),
      ...?actions,
    ];

    return AppBar(
      backgroundColor: backgroundColor ?? AppColors.bgPrimary,
      elevation: 0,
      scrolledUnderElevation: 0,
      centerTitle: true,
      automaticallyImplyLeading: false,
      leading: showBack
          ? IconButton(
              icon: Icon(Icons.arrow_back_ios, size: 20.w, color: AppColors.textPrimary),
              onPressed: onBack ?? () {
                if (context.canPop()) {
                  context.pop();
                }
              },
            )
          : null,
      title: title != null
          ? Text(title!, style: AppTextStyles.headingMd)
          : null,
      actions: actionWidgets.isNotEmpty ? actionWidgets : null,
    );
  }
}
