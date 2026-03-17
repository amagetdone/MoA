import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/viewmodels/auth_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/navigation/app_router.dart';

class SettingsPage extends ConsumerWidget {
  const SettingsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: const MoreAppBar(title: '설정'),
      body: SingleChildScrollView(
        padding: EdgeInsets.symmetric(horizontal: 20.w, vertical: 12.h),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
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
              titleColor: AppColors.textTertiary,
              onTap: () => _showDeleteAccountDialog(context),
            ),
          ],
        ),
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
    return MoreCard(
      onTap: onTap,
      padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 14.h),
      child: Row(
        children: [
          Container(
            width: 40.w,
            height: 40.w,
            decoration: BoxDecoration(
              color: AppColors.gray50,
              borderRadius: BorderRadius.circular(10.r),
              border: Border.all(color: AppColors.borderDefault),
            ),
            child: Icon(icon, size: 20.w, color: AppColors.gray500),
          ),
          SizedBox(width: 14.w),
          Expanded(
            child: Text(
              title,
              style: AppTextStyles.bodyMd.copyWith(
                color: titleColor ?? AppColors.textPrimary,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
          if (subtitle.isNotEmpty)
            Text(
              subtitle,
              style: AppTextStyles.caption.copyWith(
                color: AppColors.textTertiary,
                fontSize: 11.sp,
              ),
            ),
          SizedBox(width: 4.w),
          Icon(
            Icons.chevron_right,
            size: 18.w,
            color: AppColors.textTertiary,
          ),
        ],
      ),
    );
  }
}
