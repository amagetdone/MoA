import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/viewmodels/auth_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/navigation/app_router.dart';

class SettingsPage extends ConsumerWidget {
  const SettingsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: const MoreAppBar(title: '설정'),
      body: ListView(
        children: [
          SizedBox(height: 8.h),

          // Notification
          Container(
            color: AppColors.bgSurface,
            child: ListTile(
              title: Text(
                '알림',
                style: AppTextStyles.bodyMd.copyWith(
                  color: AppColors.textPrimary,
                  fontSize: 14.sp,
                ),
              ),
              trailing: Icon(
                Icons.chevron_right,
                color: AppColors.textTertiary,
                size: 20.w,
              ),
              onTap: () {
                // TODO: navigate to notification settings
              },
              contentPadding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 2.h),
            ),
          ),

          Divider(height: 1, color: AppColors.borderDefault),

          // Security
          Container(
            color: AppColors.bgSurface,
            child: ListTile(
              title: Text(
                '보안',
                style: AppTextStyles.bodyMd.copyWith(
                  color: AppColors.textPrimary,
                  fontSize: 14.sp,
                ),
              ),
              trailing: Icon(
                Icons.chevron_right,
                color: AppColors.textTertiary,
                size: 20.w,
              ),
              onTap: () {
                // TODO: navigate to security settings
              },
              contentPadding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 2.h),
            ),
          ),

          Divider(height: 1, color: AppColors.borderDefault),
          SizedBox(height: 16.h),
          Divider(height: 1, color: AppColors.borderDefault),

          // Logout
          Container(
            color: AppColors.bgSurface,
            child: ListTile(
              title: Text(
                '로그아웃',
                style: AppTextStyles.bodyMd.copyWith(
                  color: AppColors.red500,
                  fontSize: 14.sp,
                ),
              ),
              trailing: Icon(
                Icons.chevron_right,
                color: AppColors.textTertiary,
                size: 20.w,
              ),
              onTap: () => _showLogoutDialog(context, ref),
              contentPadding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 2.h),
            ),
          ),

          Divider(height: 1, color: AppColors.borderDefault),

          // Delete account
          Container(
            color: AppColors.bgSurface,
            child: ListTile(
              title: Text(
                '회원탈퇴',
                style: AppTextStyles.bodyMd.copyWith(
                  color: AppColors.textTertiary,
                  fontSize: 14.sp,
                ),
              ),
              trailing: Icon(
                Icons.chevron_right,
                color: AppColors.textTertiary,
                size: 20.w,
              ),
              onTap: () => _showDeleteAccountDialog(context),
              contentPadding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 2.h),
            ),
          ),
        ],
      ),
    );
  }

  void _showLogoutDialog(BuildContext context, WidgetRef ref) {
    showDialog(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text(
          '로그아웃',
          style: AppTextStyles.headingMd.copyWith(
            color: AppColors.textPrimary,
            fontSize: 18.sp,
          ),
        ),
        content: Text(
          '정말 로그아웃 하시겠습니까?',
          style: AppTextStyles.bodyMd.copyWith(
            color: AppColors.textSecondary,
            fontSize: 14.sp,
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: Text(
              '취소',
              style: AppTextStyles.bodyMd.copyWith(
                color: AppColors.textTertiary,
                fontSize: 14.sp,
              ),
            ),
          ),
          TextButton(
            onPressed: () {
              Navigator.of(dialogContext).pop();
              ref.read(authViewModelProvider.notifier).logout();
              context.go(AppRoutes.login);
            },
            child: Text(
              '로그아웃',
              style: AppTextStyles.bodyMd.copyWith(
                color: AppColors.red500,
                fontSize: 14.sp,
              ),
            ),
          ),
        ],
      ),
    );
  }

  void _showDeleteAccountDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text(
          '회원탈퇴',
          style: AppTextStyles.headingMd.copyWith(
            color: AppColors.textPrimary,
            fontSize: 18.sp,
          ),
        ),
        content: Text(
          '탈퇴 시 모든 데이터가 삭제됩니다.\n정말 탈퇴하시겠습니까?',
          style: AppTextStyles.bodyMd.copyWith(
            color: AppColors.textSecondary,
            fontSize: 14.sp,
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: Text(
              '취소',
              style: AppTextStyles.bodyMd.copyWith(
                color: AppColors.textTertiary,
                fontSize: 14.sp,
              ),
            ),
          ),
          TextButton(
            onPressed: () {
              Navigator.of(dialogContext).pop();
              // TODO: Implement account deletion
            },
            child: Text(
              '탈퇴',
              style: AppTextStyles.bodyMd.copyWith(
                color: AppColors.red500,
                fontSize: 14.sp,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
