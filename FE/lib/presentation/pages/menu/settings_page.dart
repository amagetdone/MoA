import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/viewmodels/auth_viewmodel.dart';
import 'package:more_app/presentation/viewmodels/theme_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/navigation/app_router.dart';

class SettingsPage extends ConsumerWidget {
  const SettingsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final colors = AppColors.of(context);
    final themeMode = ref.watch(themeProvider);
    final isDark = themeMode == ThemeMode.dark;

    return Scaffold(
      backgroundColor: colors.bgPrimary,
      appBar: const MoreAppBar(title: '설정'),
      body: SingleChildScrollView(
        padding: EdgeInsets.symmetric(horizontal: 20.w, vertical: 12.h),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Dark mode toggle
            MoreCard(
              padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 14.h),
              child: Row(
                children: [
                  Container(
                    width: 40.w,
                    height: 40.w,
                    decoration: BoxDecoration(
                      color: colors.gray50,
                      borderRadius: BorderRadius.circular(10.r),
                      border: Border.all(color: colors.borderDefault),
                    ),
                    child: Icon(
                      isDark ? Icons.dark_mode_outlined : Icons.light_mode_outlined,
                      size: 20.w,
                      color: colors.gray500,
                    ),
                  ),
                  SizedBox(width: 14.w),
                  Expanded(
                    child: Text(
                      '다크 모드',
                      style: AppTextStyles.bodyMd.copyWith(
                        color: colors.textPrimary,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                  Switch(
                    value: isDark,
                    onChanged: (_) => ref.read(themeProvider.notifier).toggle(),
                    activeColor: AppColors.orange500,
                  ),
                ],
              ),
            ),
            SizedBox(height: 10.h),

            // Notification
            _SettingsCard(
              icon: Icons.notifications_outlined,
              title: '알림',
              subtitle: '알림 설정',
              onTap: () {
                // TODO: navigate to notification settings
              },
            ),
            SizedBox(height: 10.h),

            // Security
            _SettingsCard(
              icon: Icons.lock_outline,
              title: '보안',
              subtitle: '보안 설정',
              onTap: () {
                // TODO: navigate to security settings
              },
            ),
            SizedBox(height: 10.h),

            // Logout
            _SettingsCard(
              icon: Icons.logout,
              title: '로그아웃',
              subtitle: '',
              titleColor: AppColors.red500,
              onTap: () => _showLogoutDialog(context, ref),
            ),
            SizedBox(height: 10.h),

            // Delete account
            _SettingsCard(
              icon: Icons.person_remove_outlined,
              title: '회원탈퇴',
              subtitle: '',
              onTap: () => _showDeleteAccountDialog(context),
            ),
          ],
        ),
      ),
    );
  }

  void _showLogoutDialog(BuildContext context, WidgetRef ref) {
    final colors = AppColors.of(context);
    showDialog(
      context: context,
      barrierColor: colors.cardShadow,
      builder: (dialogContext) => Dialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20.r)),
        backgroundColor: colors.bgSurface,
        child: Padding(
          padding: EdgeInsets.fromLTRB(24.w, 28.h, 24.w, 20.h),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 52.w,
                height: 52.w,
                decoration: BoxDecoration(
                  color: AppColors.orange500.withValues(alpha: 0.1),
                  shape: BoxShape.circle,
                ),
                child: Icon(Icons.logout, size: 24.w, color: AppColors.orange500),
              ),
              SizedBox(height: 16.h),
              Text(
                '로그아웃',
                style: AppTextStyles.headingMd.copyWith(
                  color: colors.textPrimary,
                  fontSize: 18.sp,
                  fontWeight: FontWeight.bold,
                ),
              ),
              SizedBox(height: 8.h),
              Text(
                '정말 로그아웃 하시겠습니까?',
                style: AppTextStyles.bodyMd.copyWith(
                  color: colors.textSecondary,
                  fontSize: 14.sp,
                ),
              ),
              SizedBox(height: 24.h),
              Row(
                children: [
                  Expanded(
                    child: SizedBox(
                      height: 48.h,
                      child: OutlinedButton(
                        onPressed: () => Navigator.of(dialogContext).pop(),
                        style: OutlinedButton.styleFrom(
                          side: BorderSide(color: colors.borderDefault),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12.r)),
                        ),
                        child: Text(
                          '취소',
                          style: AppTextStyles.bodyMd.copyWith(
                            color: colors.textSecondary,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ),
                  ),
                  SizedBox(width: 10.w),
                  Expanded(
                    child: SizedBox(
                      height: 48.h,
                      child: FilledButton(
                        onPressed: () {
                          Navigator.of(dialogContext).pop();
                          ref.read(authViewModelProvider.notifier).logout();
                          context.go(AppRoutes.login);
                        },
                        style: FilledButton.styleFrom(
                          backgroundColor: AppColors.orange500,
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12.r)),
                        ),
                        child: Text(
                          '로그아웃',
                          style: AppTextStyles.bodyMd.copyWith(
                            color: colors.white,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _showDeleteAccountDialog(BuildContext context) {
    final colors = AppColors.of(context);
    showDialog(
      context: context,
      barrierColor: colors.cardShadow,
      builder: (dialogContext) => Dialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20.r)),
        backgroundColor: colors.bgSurface,
        child: Padding(
          padding: EdgeInsets.fromLTRB(24.w, 28.h, 24.w, 20.h),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 52.w,
                height: 52.w,
                decoration: BoxDecoration(
                  color: AppColors.red500.withValues(alpha: 0.1),
                  shape: BoxShape.circle,
                ),
                child: Icon(Icons.person_remove_outlined, size: 24.w, color: AppColors.red500),
              ),
              SizedBox(height: 16.h),
              Text(
                '회원탈퇴',
                style: AppTextStyles.headingMd.copyWith(
                  color: colors.textPrimary,
                  fontSize: 18.sp,
                  fontWeight: FontWeight.bold,
                ),
              ),
              SizedBox(height: 8.h),
              Text(
                '탈퇴 시 모든 데이터가 삭제됩니다.\n정말 탈퇴하시겠습니까?',
                textAlign: TextAlign.center,
                style: AppTextStyles.bodyMd.copyWith(
                  color: colors.textSecondary,
                  fontSize: 14.sp,
                ),
              ),
              SizedBox(height: 24.h),
              Row(
                children: [
                  Expanded(
                    child: SizedBox(
                      height: 48.h,
                      child: OutlinedButton(
                        onPressed: () => Navigator.of(dialogContext).pop(),
                        style: OutlinedButton.styleFrom(
                          side: BorderSide(color: colors.borderDefault),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12.r)),
                        ),
                        child: Text(
                          '취소',
                          style: AppTextStyles.bodyMd.copyWith(
                            color: colors.textSecondary,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ),
                  ),
                  SizedBox(width: 10.w),
                  Expanded(
                    child: SizedBox(
                      height: 48.h,
                      child: FilledButton(
                        onPressed: () {
                          Navigator.of(dialogContext).pop();
                          // TODO: Implement account deletion
                        },
                        style: FilledButton.styleFrom(
                          backgroundColor: AppColors.red500,
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12.r)),
                        ),
                        child: Text(
                          '탈퇴',
                          style: AppTextStyles.bodyMd.copyWith(
                            color: colors.white,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _SettingsCard extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;
  final Color? titleColor;

  const _SettingsCard({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.onTap,
    this.titleColor,
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return MoreCard(
      onTap: onTap,
      padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 14.h),
      child: Row(
        children: [
          Container(
            width: 40.w,
            height: 40.w,
            decoration: BoxDecoration(
              color: colors.gray50,
              borderRadius: BorderRadius.circular(10.r),
              border: Border.all(color: colors.borderDefault),
            ),
            child: Icon(icon, size: 20.w, color: colors.gray500),
          ),
          SizedBox(width: 14.w),
          Expanded(
            child: Text(
              title,
              style: AppTextStyles.bodyMd.copyWith(
                color: titleColor ?? colors.textPrimary,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
          if (subtitle.isNotEmpty)
            Text(
              subtitle,
              style: AppTextStyles.caption.copyWith(
                color: colors.textTertiary,
                fontSize: 11.sp,
              ),
            ),
          SizedBox(width: 4.w),
          Icon(
            Icons.chevron_right,
            size: 18.w,
            color: colors.textTertiary,
          ),
        ],
      ),
    );
  }
}
