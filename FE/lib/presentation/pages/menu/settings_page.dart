import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/viewmodels/auth_viewmodel.dart';
import 'package:more_app/presentation/viewmodels/menu_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/navigation/app_router.dart';

class SettingsPage extends ConsumerWidget {
  const SettingsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final menuState = ref.watch(menuViewModelProvider);
    final menuViewModel = ref.read(menuViewModelProvider.notifier);

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: const MoreAppBar(title: '설정'),
      body: ListView(
        children: [
          const SizedBox(height: 8),

          // Notification section
          _SectionHeader(title: '알림'),
          Container(
            color: AppColors.bgSurface,
            child: SwitchListTile(
              title: Text(
                '알림 설정',
                style: AppTextStyles.bodyMd.copyWith(
                  color: AppColors.textPrimary,
                ),
              ),
              subtitle: Text(
                '목표 달성, 소비 리포트 등의 알림을 받습니다',
                style: AppTextStyles.bodySm.copyWith(
                  color: AppColors.textTertiary,
                ),
              ),
              value: menuState.notificationEnabled,
              onChanged: (value) {
                menuViewModel.toggleNotification(value);
              },
              activeColor: AppColors.orange500,
            ),
          ),

          const SizedBox(height: 16),

          // Security section
          _SectionHeader(title: '보안'),
          Container(
            color: AppColors.bgSurface,
            child: SwitchListTile(
              title: Text(
                '생체 인증',
                style: AppTextStyles.bodyMd.copyWith(
                  color: AppColors.textPrimary,
                ),
              ),
              subtitle: Text(
                '앱 잠금 해제 시 생체 인증을 사용합니다',
                style: AppTextStyles.bodySm.copyWith(
                  color: AppColors.textTertiary,
                ),
              ),
              value: menuState.biometricEnabled,
              onChanged: (value) {
                menuViewModel.toggleBiometric(value);
              },
              activeColor: AppColors.orange500,
            ),
          ),

          const SizedBox(height: 16),
          const Divider(height: 1, color: AppColors.borderDefault),
          const SizedBox(height: 8),

          // Logout
          Container(
            color: AppColors.bgSurface,
            child: ListTile(
              title: Text(
                '로그아웃',
                style: AppTextStyles.bodyMd.copyWith(
                  color: AppColors.red500,
                ),
              ),
              trailing: const Icon(
                Icons.chevron_right,
                color: AppColors.textTertiary,
                size: 20,
              ),
              onTap: () => _showLogoutDialog(context, ref),
            ),
          ),

          const Divider(height: 1, color: AppColors.borderDefault),

          // Delete account
          Container(
            color: AppColors.bgSurface,
            child: ListTile(
              title: Text(
                '회원탈퇴',
                style: AppTextStyles.bodyMd.copyWith(
                  color: AppColors.textTertiary,
                ),
              ),
              trailing: const Icon(
                Icons.chevron_right,
                color: AppColors.textTertiary,
                size: 20,
              ),
              onTap: () => _showDeleteAccountDialog(context),
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
          ),
        ),
        content: Text(
          '정말 로그아웃 하시겠습니까?',
          style: AppTextStyles.bodyMd.copyWith(
            color: AppColors.textSecondary,
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: Text(
              '취소',
              style: AppTextStyles.bodyMd.copyWith(
                color: AppColors.textTertiary,
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
          ),
        ),
        content: Text(
          '탈퇴 시 모든 데이터가 삭제됩니다.\n정말 탈퇴하시겠습니까?',
          style: AppTextStyles.bodyMd.copyWith(
            color: AppColors.textSecondary,
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: Text(
              '취소',
              style: AppTextStyles.bodyMd.copyWith(
                color: AppColors.textTertiary,
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
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _SectionHeader extends StatelessWidget {
  final String title;

  const _SectionHeader({required this.title});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Text(
        title,
        style: AppTextStyles.bodySm.copyWith(
          color: AppColors.textTertiary,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}
