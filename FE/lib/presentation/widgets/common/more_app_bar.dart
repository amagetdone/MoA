import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/providers/app_data_providers.dart';

class MoreAppBar extends ConsumerWidget implements PreferredSizeWidget {
  final String? title;
  final bool showBack;
  final bool showLogo;
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
    this.showLogo = false,
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
  Widget build(BuildContext context, WidgetRef ref) {
    final colors = AppColors.of(context);
    final unreadCount = ref.watch(unreadNotificationCountProvider);
    final actionWidgets = <Widget>[
      if (skipText != null)
        TextButton(
          onPressed: onSkip,
          child: Text(
            skipText!,
            style: AppTextStyles.bodySm.copyWith(color: colors.textTertiary),
          ),
        ),
      if (showNotification)
        Padding(
          padding: EdgeInsets.only(right: 16.w),
          child: IconButton(
            onPressed: () => context.push(AppRoutes.notifications),
            icon: Stack(
              clipBehavior: Clip.none,
              children: [
                Icon(
                  Icons.notifications_none,
                  size: 24.w,
                  color: colors.textPrimary,
                ),
                if (unreadCount > 0)
                  Positioned(
                    right: -2.w,
                    top: -1.h,
                    child: Container(
                      constraints: BoxConstraints(minWidth: 16.w, minHeight: 16.w),
                      padding: EdgeInsets.symmetric(horizontal: 4.w),
                      decoration: BoxDecoration(
                        color: AppColors.red500,
                        borderRadius: BorderRadius.circular(999.r),
                      ),
                      child: Center(
                        child: Text(
                          unreadCount > 99 ? '99+' : '$unreadCount',
                          style: AppTextStyles.caption.copyWith(
                            color: colors.white,
                            fontSize: 9.sp,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                      ),
                    ),
                  ),
              ],
            ),
          ),
        ),
      ...?actions,
    ];

    return AppBar(
      backgroundColor: backgroundColor ?? colors.bgPrimary,
      elevation: 0,
      scrolledUnderElevation: 0,
      centerTitle: true,
      automaticallyImplyLeading: false,
      leadingWidth: 56.w,
      leading: showBack
          ? Padding(
              padding: EdgeInsets.only(left: 16.w),
              child: IconButton(
                icon: Icon(Icons.arrow_back_ios, size: 20.w, color: colors.textPrimary),
                onPressed: onBack ?? () {
                  if (context.canPop()) {
                    context.pop();
                  }
                },
              ),
            )
          : showLogo
              ? Padding(
                  padding: EdgeInsets.only(left: 24.w),
                  child: Image.asset(
                    'assets/images/logo/applogo.png',
                    width: 24.w,
                    height: 24.w,
                  ),
                )
              : null,
      title: title != null
          ? Text(title!, style: AppTextStyles.headingMd.copyWith(color: colors.textPrimary))
          : null,
      actions: actionWidgets.isNotEmpty ? actionWidgets : null,
    );
  }
}
