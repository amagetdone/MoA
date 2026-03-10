import 'package:flutter/material.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';

class MoreAppBar extends StatelessWidget implements PreferredSizeWidget {
  final String? title;
  final bool showBack;
  final bool showNotification;
  final String? skipText;
  final VoidCallback? onSkip;
  final List<Widget>? actions;

  const MoreAppBar({
    super.key,
    this.title,
    this.showBack = true,
    this.showNotification = false,
    this.skipText,
    this.onSkip,
    this.actions,
  });

  @override
  Size get preferredSize => const Size.fromHeight(kToolbarHeight);

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
          icon: const Icon(Icons.notifications_none, color: AppColors.textPrimary),
          onPressed: () {},
        ),
      ...?actions,
    ];

    return AppBar(
      backgroundColor: AppColors.bgSurface,
      elevation: 0,
      scrolledUnderElevation: 0,
      centerTitle: true,
      automaticallyImplyLeading: false,
      leading: showBack
          ? IconButton(
              icon: const Icon(Icons.arrow_back_ios, size: 20, color: AppColors.textPrimary),
              onPressed: () {
                if (Navigator.of(context).canPop()) {
                  Navigator.of(context).pop();
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
