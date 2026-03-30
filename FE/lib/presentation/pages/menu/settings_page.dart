import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/security/biometric_auth_service.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/providers/app_data_providers.dart';
import 'package:more_app/presentation/viewmodels/menu_viewmodel.dart';
import 'package:more_app/presentation/viewmodels/auth_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/widgets/security/app_pin_keypad.dart';

class SettingsPage extends ConsumerStatefulWidget {
  const SettingsPage({super.key});

  @override
  ConsumerState<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends ConsumerState<SettingsPage> {
  bool _isUpdatingBiometric = false;

  Future<AppLockMethod?> _showAppLockMethodDialog() {
    return showDialog<AppLockMethod>(
      context: context,
      builder: (dialogContext) {
        final colors = AppColors.of(dialogContext);
        return AlertDialog(
          backgroundColor: colors.bgSurface,
          title: Text(
            '앱 잠금 방식 선택',
            style: AppTextStyles.bodyMd.copyWith(
              color: colors.textPrimary,
              fontWeight: FontWeight.w800,
            ),
          ),
          content: Text(
            '어떤 방식으로 앱 잠금을 해제할지 선택해주세요.',
            style: AppTextStyles.bodySm.copyWith(
              color: colors.textSecondary,
              height: 1.5,
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(),
              child: const Text('취소'),
            ),
            TextButton(
              onPressed: () =>
                  Navigator.of(dialogContext).pop(AppLockMethod.pin),
              child: const Text('앱 PIN'),
            ),
            FilledButton(
              onPressed: () =>
                  Navigator.of(dialogContext).pop(AppLockMethod.biometric),
              style: FilledButton.styleFrom(
                backgroundColor: AppColors.orange500,
              ),
              child: const Text('생체인증'),
            ),
          ],
        );
      },
    );
  }

  Future<String?> _showAppPinDialog({required bool isReset}) {
    return showDialog<String>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) => _AppPinSetupDialog(isReset: isReset),
    );
  }

  Future<bool> _showAppPinVerificationDialog({
    required String title,
    required String subtitle,
  }) async {
    final result = await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) => _AppPinVerificationDialog(
        title: title,
        subtitle: subtitle,
        validator: ref.read(biometricAuthServiceProvider).verifyAppPin,
      ),
    );

    return result ?? false;
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

  Future<bool> _reauthenticateForAppLockChange({
    required String reason,
    required String failureMessage,
    bool biometricOnly = false,
  }) async {
    final service = ref.read(biometricAuthServiceProvider);
    final authAvailable = biometricOnly
        ? await service.isBiometricAvailable()
        : await service.isDeviceAuthAvailable();
    if (!authAvailable) {
      if (biometricOnly) {
        await _showMessage(failureMessage);
        return false;
      }
      return true;
    }

    final authenticated = await service.authenticate(
      reason: reason,
      biometricOnly: biometricOnly,
    );
    if (authenticated || !mounted) {
      return authenticated;
    }

    await _showMessage(failureMessage);
    return false;
  }

  Future<bool> _confirmCurrentAppLockAccess({
    required String biometricReason,
    required String failureMessage,
    required String pinTitle,
    required String pinSubtitle,
  }) async {
    final service = ref.read(biometricAuthServiceProvider);
    final hasPin = await service.hasAppPin();
    final currentMethod =
        await service.getAppLockMethod() ??
        (hasPin ? AppLockMethod.pin : AppLockMethod.biometric);

    if (currentMethod == AppLockMethod.pin) {
      if (!hasPin) {
        await _showMessage('앱 PIN 정보를 찾을 수 없어 잠금 설정을 변경할 수 없습니다.');
        return false;
      }

      final verified = await _showAppPinVerificationDialog(
        title: pinTitle,
        subtitle: pinSubtitle,
      );
      if (!verified && mounted) {
        await _showMessage(failureMessage);
      }
      return verified;
    }

    return _reauthenticateForAppLockChange(
      reason: biometricReason,
      failureMessage: failureMessage,
      biometricOnly: true,
    );
  }

  Future<void> _handleAppPinReset() async {
    if (_isUpdatingBiometric) {
      return;
    }

    final authenticated = await _confirmCurrentAppLockAccess(
      biometricReason: '앱 PIN을 다시 설정하려면 본인 확인을 진행해주세요.',
      failureMessage: '본인 확인이 완료되지 않아 앱 PIN을 변경하지 않았습니다.',
      pinTitle: '앱 PIN 확인',
      pinSubtitle: '현재 앱 PIN을 입력하면 새 PIN으로 다시 설정할 수 있어요.',
    );
    if (!authenticated || !mounted) {
      return;
    }

    final pin = await _showAppPinDialog(isReset: true);
    if (!mounted || pin == null) {
      return;
    }

    setState(() => _isUpdatingBiometric = true);
    try {
      await ref.read(biometricAuthServiceProvider).saveAppPin(pin);
      await ref.read(biometricAuthServiceProvider).saveAppLockMethod(
        AppLockMethod.pin,
      );
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
      final authenticated = await _confirmCurrentAppLockAccess(
        biometricReason: '앱 잠금을 해제하려면 본인 확인을 진행해주세요.',
        failureMessage: '본인 확인이 완료되지 않아 앱 잠금을 해제하지 않았습니다.',
        pinTitle: '앱 잠금 해제',
        pinSubtitle: '현재 앱 PIN을 입력하면 앱 잠금이 해제돼요.',
      );
      if (!authenticated || !mounted) {
        return;
      }

      setState(() => _isUpdatingBiometric = true);
      try {
        await service.clearAppPin();
        await service.clearAppLockMethod();
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

    final biometricAvailable = await service.isBiometricAvailable();
    if (!mounted) {
      return;
    }

    final selectedMethod = biometricAvailable
        ? await _showAppLockMethodDialog()
        : AppLockMethod.pin;
    if (!mounted) {
      return;
    }

    if (selectedMethod == null) {
      await _showMessage('앱 잠금 설정이 취소되었습니다.');
      return;
    }

    if (selectedMethod == AppLockMethod.biometric) {
      final authenticated = await _reauthenticateForAppLockChange(
        reason: '앱 잠금을 설정하려면 본인 확인을 진행해주세요.',
        failureMessage: '본인 확인이 완료되지 않아 앱 잠금을 켜지 않았습니다.',
        biometricOnly: true,
      );
      if (!authenticated || !mounted) {
        return;
      }
    }

    try {
      setState(() => _isUpdatingBiometric = true);
      if (selectedMethod == AppLockMethod.pin) {
        final pin = await _showAppPinDialog(isReset: false);
        if (!mounted) {
          return;
        }

        if (pin == null) {
          await _showMessage('앱 PIN 설정이 취소되어 앱 잠금을 켜지 않았습니다.');
          return;
        }

        await service.saveAppPin(pin);
        await service.saveAppLockMethod(AppLockMethod.pin);
      } else {
        await service.clearAppPin();
        await service.saveAppLockMethod(AppLockMethod.biometric);
      }
      await ref.read(menuViewModelProvider.notifier).toggleBiometric(true);
      await ref.read(biometricPreferenceProvider.notifier).setEnabled(true);
      ref.invalidate(appLockPinConfiguredProvider);

      if (!mounted) {
        return;
      }

      await _showMessage(
        selectedMethod == AppLockMethod.biometric
            ? '앱 잠금이 켜졌어요. 생체인증으로 잠금을 해제할 수 있습니다.'
            : '앱 잠금이 켜졌어요. 4자리 앱 PIN으로 잠금을 해제할 수 있습니다.',
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
    final menuState = ref.watch(menuViewModelProvider);
    final biometricEnabled = ref.watch(biometricPreferenceProvider);
    final appLockAvailability = ref.watch(appLockAvailabilityProvider);
    final appLockPinConfigured = ref.watch(appLockPinConfiguredProvider);
    final showBiometric =
        !kIsWeb && defaultTargetPlatform == TargetPlatform.android;
    final biometricAvailable = appLockAvailability.maybeWhen(
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
                            biometricEnabled
                                ? '자동로그인 뒤 설정한 방식으로 한 번 더 확인해요'
                                : biometricAvailable
                                ? '생체인증 또는 4자리 앱 PIN으로 앱 잠금을 설정할 수 있어요'
                                : '4자리 앱 PIN으로 앱 잠금을 설정할 수 있어요',
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
  String _enteredPin = '';
  String? _firstPin;
  String? _error;
  int _shuffleSeed = 0;

  void _handleDigitPressed(String digit) {
    if (_enteredPin.length >= BiometricAuthService.appPinLength) {
      return;
    }

    final nextPin = '$_enteredPin$digit';
    setState(() {
      _enteredPin = nextPin;
      _error = null;
    });

    if (nextPin.length < BiometricAuthService.appPinLength) {
      return;
    }

    if (_firstPin == null) {
      setState(() {
        _firstPin = nextPin;
        _enteredPin = '';
        _shuffleSeed++;
      });
      return;
    }

    if (_firstPin == nextPin) {
      Navigator.of(context).pop(nextPin);
      return;
    }

    setState(() {
      _enteredPin = '';
      _error = '입력한 PIN이 서로 다릅니다. 다시 입력해주세요.';
      _shuffleSeed++;
    });
  }

  void _handleBackspace() {
    if (_enteredPin.isEmpty) {
      return;
    }

    setState(() {
      _enteredPin = _enteredPin.substring(0, _enteredPin.length - 1);
      _error = null;
    });
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final isConfirmStep = _firstPin != null;

    return Dialog(
      insetPadding: EdgeInsets.symmetric(horizontal: 20.w),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28.r)),
      backgroundColor: colors.bgSurface,
      child: Padding(
        padding: EdgeInsets.fromLTRB(20.w, 24.h, 20.w, 20.h),
        child: AppPinKeypad(
          pin: _enteredPin,
          pinLength: BiometricAuthService.appPinLength,
          title: widget.isReset ? '앱 PIN 다시 설정' : '앱 PIN 설정',
          subtitle: isConfirmStep
              ? '같은 4자리 PIN을 한 번 더 입력해주세요.'
              : '자동로그인 뒤 잠금 해제에 사용할 4자리 PIN을 입력해주세요.',
          errorText: _error,
          shuffleKey: _shuffleSeed,
          onDigitPressed: _handleDigitPressed,
          onBackspace: _handleBackspace,
          bottomContent: Align(
            alignment: Alignment.centerRight,
            child: TextButton(
              onPressed: () => Navigator.of(context).pop(),
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
      ),
    );
  }
}

class _AppPinVerificationDialog extends StatefulWidget {
  const _AppPinVerificationDialog({
    required this.title,
    required this.subtitle,
    required this.validator,
  });

  final String title;
  final String subtitle;
  final Future<bool> Function(String pin) validator;

  @override
  State<_AppPinVerificationDialog> createState() =>
      _AppPinVerificationDialogState();
}

class _AppPinVerificationDialogState extends State<_AppPinVerificationDialog> {
  String _enteredPin = '';
  String? _error;
  int _shuffleSeed = 0;
  bool _isVerifying = false;

  Future<void> _verifyPin(String pin) async {
    setState(() {
      _isVerifying = true;
      _error = null;
    });

    final isValid = await widget.validator(pin);
    if (!mounted) {
      return;
    }

    if (isValid) {
      Navigator.of(context).pop(true);
      return;
    }

    setState(() {
      _isVerifying = false;
      _enteredPin = '';
      _error = '앱 PIN이 올바르지 않습니다.';
      _shuffleSeed++;
    });
  }

  void _handleDigitPressed(String digit) {
    if (_isVerifying || _enteredPin.length >= BiometricAuthService.appPinLength) {
      return;
    }

    final nextPin = '$_enteredPin$digit';
    setState(() {
      _enteredPin = nextPin;
      _error = null;
    });

    if (nextPin.length == BiometricAuthService.appPinLength) {
      _verifyPin(nextPin);
    }
  }

  void _handleBackspace() {
    if (_isVerifying || _enteredPin.isEmpty) {
      return;
    }

    setState(() {
      _enteredPin = _enteredPin.substring(0, _enteredPin.length - 1);
      _error = null;
    });
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);

    return Dialog(
      insetPadding: EdgeInsets.symmetric(horizontal: 20.w),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28.r)),
      backgroundColor: colors.bgSurface,
      child: Padding(
        padding: EdgeInsets.fromLTRB(20.w, 24.h, 20.w, 20.h),
        child: AppPinKeypad(
          pin: _enteredPin,
          pinLength: BiometricAuthService.appPinLength,
          title: widget.title,
          subtitle: widget.subtitle,
          errorText: _error,
          shuffleKey: _shuffleSeed,
          enabled: !_isVerifying,
          onDigitPressed: _handleDigitPressed,
          onBackspace: _handleBackspace,
          bottomContent: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              if (_isVerifying) ...[
                SizedBox(
                  width: 24.w,
                  height: 24.w,
                  child: CircularProgressIndicator(
                    strokeWidth: 2.5,
                    color: AppColors.orange500,
                  ),
                ),
                SizedBox(height: 12.h),
              ],
              Align(
                alignment: Alignment.centerRight,
                child: TextButton(
                  onPressed: _isVerifying
                      ? null
                      : () => Navigator.of(context).pop(false),
                  child: Text(
                    '취소',
                    style: AppTextStyles.bodyMd.copyWith(
                      color: colors.textSecondary,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
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
          Icon(Icons.chevron_right, size: 18.w, color: colors.textTertiary),
        ],
      ),
    );
  }
}
