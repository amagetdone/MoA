import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/providers/app_data_providers.dart';
import 'package:more_app/presentation/viewmodels/menu_viewmodel.dart';
import 'package:more_app/presentation/viewmodels/auth_viewmodel.dart';
import 'package:more_app/presentation/viewmodels/theme_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/navigation/app_router.dart';

class SettingsPage extends ConsumerStatefulWidget {
  const SettingsPage({super.key});

  @override
  ConsumerState<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends ConsumerState<SettingsPage> {
  bool _isUpdatingBiometric = false;

  Future<String?> _showAppPinDialog({required bool isReset}) {
    return showDialog<String>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) => _AppPinSetupDialog(isReset: isReset),
    );
  }

  Future<void> _showMessage(String message) async {
    if (!mounted) {
      return;
    }

    final messenger = ScaffoldMessenger.of(context);
    messenger
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  Future<void> _handleAppPinReset() async {
    if (_isUpdatingBiometric) {
      return;
    }

    final pin = await _showAppPinDialog(isReset: true);
    if (!mounted || pin == null) {
      return;
    }

    setState(() => _isUpdatingBiometric = true);
    try {
      await ref.read(biometricAuthServiceProvider).saveAppPin(pin);
      ref.invalidate(appLockPinConfiguredProvider);
      await _showMessage('앱 PIN을 다시 설정했어요.');
    } finally {
      if (mounted) {
        setState(() => _isUpdatingBiometric = false);
      }
    }
  }

  Future<void> _handleBiometricToggle(bool enabled) async {
    if (_isUpdatingBiometric) {
      return;
    }

    final service = ref.read(biometricAuthServiceProvider);

    if (!enabled) {
      setState(() => _isUpdatingBiometric = true);
      try {
        await service.clearAppPin();
        await ref.read(menuViewModelProvider.notifier).toggleBiometric(false);
        await ref.read(biometricPreferenceProvider.notifier).setEnabled(false);
        ref.invalidate(appLockPinConfiguredProvider);
      } finally {
        if (mounted) {
          setState(() => _isUpdatingBiometric = false);
        }
      }
      return;
    }

    final pin = await _showAppPinDialog(isReset: false);
    if (!mounted) {
      return;
    }

    if (pin == null) {
      await _showMessage('앱 PIN 설정이 취소되어 앱 잠금을 켜지 않았습니다.');
      return;
    }

    try {
      setState(() => _isUpdatingBiometric = true);
      await service.saveAppPin(pin);
      await ref.read(menuViewModelProvider.notifier).toggleBiometric(true);
      await ref.read(biometricPreferenceProvider.notifier).setEnabled(true);
      ref.invalidate(appLockPinConfiguredProvider);

      final deviceAuthAvailable = await service.isDeviceAuthAvailable();
      if (!mounted) {
        return;
      }

      await _showMessage(
        deviceAuthAvailable
            ? '앱 잠금이 켜졌어요. 앱 PIN이나 생체인증으로 잠금을 해제할 수 있습니다.'
            : '앱 잠금이 켜졌어요. 자동로그인 후 앱 PIN을 입력해야 합니다.',
      );
    } finally {
      if (mounted) {
        setState(() => _isUpdatingBiometric = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final themeMode = ref.watch(themeProvider);
    final isDark = themeMode == ThemeMode.dark;
    final menuState = ref.watch(menuViewModelProvider);
    final biometricEnabled = ref.watch(biometricPreferenceProvider);
    final appLockAvailability = ref.watch(appLockAvailabilityProvider);
    final appLockPinConfigured = ref.watch(appLockPinConfiguredProvider);
    final showBiometric =
        !kIsWeb && defaultTargetPlatform == TargetPlatform.android;
    final deviceAuthAvailable = appLockAvailability.maybeWhen(
      data: (value) => value,
      orElse: () => false,
    );
    final appPinConfigured = appLockPinConfigured.maybeWhen(
      data: (value) => value,
      orElse: () => false,
    );

    ref.listen<String?>(
      menuViewModelProvider.select((state) => state.errorMessage),
      (previous, next) {
        if (next == null || next == previous || !context.mounted) {
          return;
        }

        ScaffoldMessenger.of(context)
          ..hideCurrentSnackBar()
          ..showSnackBar(SnackBar(content: Text(next)));
      },
    );

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
                      color: colors.gray100,
                      borderRadius: BorderRadius.circular(10.r),
                      border: Border.all(color: colors.borderDefault),
                    ),
                    child: Icon(
                      isDark
                          ? Icons.dark_mode_outlined
                          : Icons.light_mode_outlined,
                      size: 20.w,
                      color: colors.textSecondary,
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
                    activeThumbColor: AppColors.orange500,
                  ),
                ],
              ),
            ),
            SizedBox(height: 10.h),

            // Notification
            MoreCard(
              padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 14.h),
              child: Row(
                children: [
                  Container(
                    width: 40.w,
                    height: 40.w,
                    decoration: BoxDecoration(
                      color: colors.gray100,
                      borderRadius: BorderRadius.circular(10.r),
                      border: Border.all(color: colors.borderDefault),
                    ),
                    child: Icon(
                      Icons.notifications_outlined,
                      size: 20.w,
                      color: colors.textSecondary,
                    ),
                  ),
                  SizedBox(width: 14.w),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          '푸시 알림',
                          style: AppTextStyles.bodyMd.copyWith(
                            color: colors.textPrimary,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        SizedBox(height: 2.h),
                        Text(
                          '목표 생성 결과와 거래 알림을 받아보세요',
                          style: AppTextStyles.caption.copyWith(
                            color: colors.textTertiary,
                            fontSize: 11.sp,
                          ),
                        ),
                      ],
                    ),
                  ),
                  Switch(
                    value: menuState.notificationEnabled,
                    onChanged: (value) => ref
                        .read(menuViewModelProvider.notifier)
                        .toggleNotification(value),
                    activeThumbColor: AppColors.orange500,
                  ),
                ],
              ),
            ),
            SizedBox(height: 10.h),

            if (showBiometric) ...[
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
                        Icons.lock_outline,
                        size: 20.w,
                        color: colors.gray500,
                      ),
                    ),
                    SizedBox(width: 14.w),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            '앱 잠금',
                            style: AppTextStyles.bodyMd.copyWith(
                              color: colors.textPrimary,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                          SizedBox(height: 2.h),
                          Text(
                            biometricEnabled && appPinConfigured
                                ? deviceAuthAvailable
                                      ? '자동로그인 후 앱 PIN이나 생체인증으로 잠금을 해제해요'
                                      : '자동로그인 후 6자리 앱 PIN으로 잠금을 해제해요'
                                : '6자리 앱 PIN을 설정하면 자동로그인 뒤 한 번 더 확인해요',
                            style: AppTextStyles.caption.copyWith(
                              color: colors.textTertiary,
                              fontSize: 11.sp,
                            ),
                          ),
                          if (biometricEnabled && appPinConfigured) ...[
                            SizedBox(height: 6.h),
                            InkWell(
                              onTap: _isUpdatingBiometric
                                  ? null
                                  : _handleAppPinReset,
                              child: Text(
                                '앱 PIN 다시 설정',
                                style: AppTextStyles.caption.copyWith(
                                  color: AppColors.orange500,
                                  fontSize: 11.sp,
                                  fontWeight: FontWeight.w700,
                                ),
                              ),
                            ),
                          ],
                        ],
                      ),
                    ),
                    Switch(
                      value: biometricEnabled,
                      onChanged: !_isUpdatingBiometric
                          ? _handleBiometricToggle
                          : null,
                      activeThumbColor: AppColors.orange500,
                    ),
                  ],
                ),
              ),
              SizedBox(height: 10.h),
            ],

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
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(20.r),
        ),
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
                child: Icon(
                  Icons.logout,
                  size: 24.w,
                  color: AppColors.orange500,
                ),
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
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12.r),
                          ),
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
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12.r),
                          ),
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
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(20.r),
        ),
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
                child: Icon(
                  Icons.person_remove_outlined,
                  size: 24.w,
                  color: AppColors.red500,
                ),
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
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12.r),
                          ),
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
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12.r),
                          ),
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

class _AppPinSetupDialog extends StatefulWidget {
  const _AppPinSetupDialog({required this.isReset});

  final bool isReset;

  @override
  State<_AppPinSetupDialog> createState() => _AppPinSetupDialogState();
}

class _AppPinSetupDialogState extends State<_AppPinSetupDialog> {
  final _pinController = TextEditingController();
  final _confirmPinController = TextEditingController();
  String? _error;

  @override
  void dispose() {
    _pinController.dispose();
    _confirmPinController.dispose();
    super.dispose();
  }

  void _submit() {
    final pin = _pinController.text.trim();
    final confirmPin = _confirmPinController.text.trim();

    if (pin.length != 6 || confirmPin.length != 6) {
      setState(() {
        _error = '앱 PIN은 6자리 숫자로 입력해주세요.';
      });
      return;
    }

    if (pin != confirmPin) {
      setState(() {
        _error = '입력한 PIN이 서로 다릅니다.';
      });
      return;
    }

    Navigator.of(context).pop(pin);
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);

    return AlertDialog(
      insetPadding: EdgeInsets.symmetric(horizontal: 28.w),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20.r)),
      titlePadding: EdgeInsets.fromLTRB(24.w, 24.h, 24.w, 8.h),
      contentPadding: EdgeInsets.fromLTRB(24.w, 0, 24.w, 20.h),
      actionsPadding: EdgeInsets.fromLTRB(20.w, 0, 20.w, 20.h),
      title: Text(
        widget.isReset ? '앱 PIN 다시 설정' : '앱 PIN 설정',
        style: AppTextStyles.headingMd.copyWith(
          color: colors.textPrimary,
          fontSize: 18.sp,
          fontWeight: FontWeight.w700,
        ),
      ),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '자동로그인 후 앱을 열 때 사용할 6자리 숫자 PIN을 설정해주세요.',
            style: AppTextStyles.bodyMd.copyWith(
              color: colors.textSecondary,
              height: 1.5,
            ),
          ),
          SizedBox(height: 16.h),
          TextField(
            controller: _pinController,
            obscureText: true,
            keyboardType: TextInputType.number,
            maxLength: 6,
            inputFormatters: [FilteringTextInputFormatter.digitsOnly],
            decoration: const InputDecoration(
              labelText: '앱 PIN',
              counterText: '',
            ),
          ),
          SizedBox(height: 8.h),
          TextField(
            controller: _confirmPinController,
            obscureText: true,
            keyboardType: TextInputType.number,
            maxLength: 6,
            inputFormatters: [FilteringTextInputFormatter.digitsOnly],
            decoration: const InputDecoration(
              labelText: '앱 PIN 확인',
              counterText: '',
            ),
          ),
          if (_error != null) ...[
            SizedBox(height: 8.h),
            Text(
              _error!,
              style: AppTextStyles.caption.copyWith(
                color: AppColors.red500,
                fontSize: 12.sp,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('취소'),
        ),
        FilledButton(
          onPressed: _submit,
          style: FilledButton.styleFrom(
            backgroundColor: AppColors.orange500,
            foregroundColor: colors.white,
          ),
          child: const Text('저장'),
        ),
      ],
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
          Icon(Icons.chevron_right, size: 18.w, color: colors.textTertiary),
        ],
      ),
    );
  }
}
