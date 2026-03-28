import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/security/biometric_auth_service.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/providers/app_data_providers.dart';
import 'package:more_app/presentation/viewmodels/auth_viewmodel.dart';
import 'package:more_app/presentation/widgets/security/app_pin_keypad.dart';

class BiometricAppLock extends ConsumerStatefulWidget {
  const BiometricAppLock({super.key, required this.child});

  final Widget child;

  @override
  ConsumerState<BiometricAppLock> createState() => _BiometricAppLockState();
}

class _BiometricAppLockState extends ConsumerState<BiometricAppLock>
    with WidgetsBindingObserver {
  static const Duration _resumeLockThreshold = Duration(minutes: 1);

  bool _isLockVisible = false;
  bool _isAuthenticating = false;
  bool _didAttemptInitialUnlock = false;
  bool _shouldLockOnResume = false;
  bool _canUseDeviceAuth = false;
  String? _availabilityNotice;
  String? _message;
  String? _pinError;
  DateTime? _backgroundedAt;
  String _enteredPin = '';
  int _pinShuffleSeed = 0;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (_isAuthenticating) {
      return;
    }

    if (!_requiresProtection()) {
      return;
    }

    if (state == AppLifecycleState.paused ||
        state == AppLifecycleState.hidden) {
      _shouldLockOnResume = true;
      _backgroundedAt = DateTime.now();
      return;
    }

    if (state == AppLifecycleState.resumed && _shouldLockOnResume) {
      final backgroundedAt = _backgroundedAt;
      _backgroundedAt = null;
      if (backgroundedAt == null ||
          DateTime.now().difference(backgroundedAt) < _resumeLockThreshold) {
        _shouldLockOnResume = false;
        return;
      }
      _startUnlockFlow();
    }
  }

  bool _hasAuthenticatedSession(AuthState state) {
    return state is AuthAuthenticated || state is AuthAutoLoginSuccess;
  }

  bool _shouldAttemptInitialUnlock(AuthState state) {
    return state is AuthAutoLoginSuccess;
  }

  bool _requiresProtection() {
    final authState = ref.read(authViewModelProvider);
    final biometricEnabled = ref.read(biometricPreferenceProvider);
    return biometricEnabled && _hasAuthenticatedSession(authState);
  }

  void _scheduleInitialUnlockIfNeeded() {
    if (_didAttemptInitialUnlock ||
        _isLockVisible ||
        _isAuthenticating ||
        !_requiresProtection() ||
        !_shouldAttemptInitialUnlock(ref.read(authViewModelProvider))) {
      return;
    }

    _didAttemptInitialUnlock = true;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) {
        return;
      }
      _startUnlockFlow();
    });
  }

  Future<void> _startUnlockFlow() async {
    if (_isAuthenticating || !_requiresProtection()) {
      return;
    }

    final service = ref.read(biometricAuthServiceProvider);
    final hasPin = await service.hasAppPin();
    if (!mounted) {
      return;
    }

    if (!hasPin) {
      await ref.read(biometricPreferenceProvider.notifier).setEnabled(false);
      if (!mounted) {
        return;
      }
      setState(() {
        _isLockVisible = false;
        _isAuthenticating = false;
        _shouldLockOnResume = false;
        _backgroundedAt = null;
        _canUseDeviceAuth = false;
        _availabilityNotice = '앱 PIN이 설정되어 있지 않아 앱 잠금을 해제했어요.';
        _message = null;
        _pinError = null;
        _enteredPin = '';
        _pinShuffleSeed++;
      });
      return;
    }

    final canUseDeviceAuth = await service.isDeviceAuthAvailable();
    if (!mounted) {
      return;
    }

    setState(() {
      _isLockVisible = true;
      _isAuthenticating = canUseDeviceAuth;
      _canUseDeviceAuth = canUseDeviceAuth;
      _availabilityNotice = null;
      _message = null;
      _pinError = null;
    });

    if (canUseDeviceAuth) {
      final unlocked = await service.authenticate(
        reason: '앱 잠금을 해제하려면 본인 확인을 진행해주세요.',
      );
      if (!mounted) {
        return;
      }

      if (unlocked) {
        _completeUnlock();
        return;
      }
    }

    setState(() {
      _isAuthenticating = false;
      _isLockVisible = true;
      _shouldLockOnResume = true;
      _message = canUseDeviceAuth
          ? '생체인증 대신 앱 PIN으로도 잠금을 해제할 수 있어요.'
          : '앱 PIN을 입력해 잠금을 해제해주세요.';
      _enteredPin = '';
      _pinShuffleSeed++;
    });
  }

  Future<void> _unlockWithPin([String? pin]) async {
    if (_isAuthenticating) {
      return;
    }

    final resolvedPin = (pin ?? _enteredPin).trim();
    if (resolvedPin.length != BiometricAuthService.appPinLength) {
      setState(() {
        _pinError = '앱 PIN 4자리를 입력해주세요.';
      });
      return;
    }

    setState(() {
      _isAuthenticating = true;
      _pinError = null;
    });

    final unlocked = await ref
        .read(biometricAuthServiceProvider)
        .verifyAppPin(resolvedPin);
    if (!mounted) {
      return;
    }

    if (unlocked) {
      _completeUnlock();
      return;
    }

    setState(() {
      _isAuthenticating = false;
      _pinError = '앱 PIN이 올바르지 않습니다.';
      _message = null;
      _enteredPin = '';
      _pinShuffleSeed++;
    });
  }

  void _handlePinDigitPressed(String digit) {
    if (_isAuthenticating || _enteredPin.length >= BiometricAuthService.appPinLength) {
      return;
    }

    final nextPin = '$_enteredPin$digit';
    setState(() {
      _enteredPin = nextPin;
      _pinError = null;
    });

    if (nextPin.length == BiometricAuthService.appPinLength) {
      unawaited(_unlockWithPin(nextPin));
    }
  }

  void _handlePinBackspace() {
    if (_isAuthenticating || _enteredPin.isEmpty) {
      return;
    }

    setState(() {
      _enteredPin = _enteredPin.substring(0, _enteredPin.length - 1);
      _pinError = null;
    });
  }

  void _completeUnlock() {
    setState(() {
      _isLockVisible = false;
      _isAuthenticating = false;
      _shouldLockOnResume = false;
      _backgroundedAt = null;
      _canUseDeviceAuth = false;
      _availabilityNotice = null;
      _message = null;
      _pinError = null;
      _enteredPin = '';
      _pinShuffleSeed++;
    });
  }

  Future<void> _logout() async {
    await ref.read(authViewModelProvider.notifier).logout();
    if (!mounted) {
      return;
    }
    appRouter.go(AppRoutes.login);
  }

  @override
  Widget build(BuildContext context) {
    ref.listen<bool>(biometricPreferenceProvider, (previous, next) {
      if (!mounted) {
        return;
      }

      if (!next) {
        setState(() {
          _isLockVisible = false;
          _isAuthenticating = false;
          _shouldLockOnResume = false;
          _backgroundedAt = null;
          _didAttemptInitialUnlock = false;
          _canUseDeviceAuth = false;
          _availabilityNotice = null;
          _message = null;
          _pinError = null;
          _enteredPin = '';
          _pinShuffleSeed++;
        });
        return;
      }

      if (_hasAuthenticatedSession(ref.read(authViewModelProvider))) {
        _didAttemptInitialUnlock = false;
        _scheduleInitialUnlockIfNeeded();
      }
    });

    ref.listen<AuthState>(authViewModelProvider, (previous, next) {
      if (!mounted) {
        return;
      }

      if (!_hasAuthenticatedSession(next)) {
        setState(() {
          _isLockVisible = false;
          _isAuthenticating = false;
          _shouldLockOnResume = false;
          _backgroundedAt = null;
          _didAttemptInitialUnlock = false;
          _canUseDeviceAuth = false;
          _availabilityNotice = null;
          _message = null;
          _pinError = null;
          _enteredPin = '';
          _pinShuffleSeed++;
        });
        return;
      }

      if (_shouldAttemptInitialUnlock(next)) {
        _scheduleInitialUnlockIfNeeded();
      }
    });

    _scheduleInitialUnlockIfNeeded();

    final colors = AppColors.of(context);

    return Stack(
      children: [
        widget.child,
        if (_availabilityNotice != null)
          Positioned.fill(
            child: Material(
              color: colors.bgPrimary.withValues(alpha: 0.88),
              child: SafeArea(
                child: Center(
                  child: Padding(
                    padding: EdgeInsets.symmetric(horizontal: 24.w),
                    child: DecoratedBox(
                      decoration: BoxDecoration(
                        color: colors.bgSurface,
                        borderRadius: BorderRadius.circular(28.r),
                        boxShadow: [
                          BoxShadow(
                            color: colors.cardShadow,
                            blurRadius: 24.r,
                            offset: Offset(0, 12.h),
                          ),
                        ],
                      ),
                      child: Padding(
                        padding: EdgeInsets.fromLTRB(24.w, 28.h, 24.w, 24.h),
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Container(
                              width: 68.w,
                              height: 68.w,
                              decoration: const BoxDecoration(
                                color: AppColors.orange50,
                                shape: BoxShape.circle,
                              ),
                              child: Icon(
                                Icons.lock_reset,
                                size: 34.w,
                                color: AppColors.orange500,
                              ),
                            ),
                            SizedBox(height: 18.h),
                            Text(
                              '앱 잠금을 해제했어요',
                              textAlign: TextAlign.center,
                              style: AppTextStyles.headingLg.copyWith(
                                color: colors.textPrimary,
                                fontWeight: FontWeight.w800,
                                fontSize: 28.sp,
                              ),
                            ),
                            SizedBox(height: 10.h),
                            Text(
                              _availabilityNotice!,
                              textAlign: TextAlign.center,
                              style: AppTextStyles.bodyMd.copyWith(
                                color: colors.textSecondary,
                                height: 1.5,
                              ),
                            ),
                            SizedBox(height: 10.h),
                            Text(
                              '설정 페이지에서 앱 잠금을 다시 켜고 4자리 앱 PIN을 새로 설정해주세요.',
                              textAlign: TextAlign.center,
                              style: AppTextStyles.caption.copyWith(
                                color: colors.textTertiary,
                                fontSize: 12.sp,
                                height: 1.5,
                              ),
                            ),
                            SizedBox(height: 24.h),
                            SizedBox(
                              width: double.infinity,
                              height: 52.h,
                              child: FilledButton(
                                onPressed: () {
                                  setState(() {
                                    _availabilityNotice = null;
                                  });
                                },
                                style: FilledButton.styleFrom(
                                  backgroundColor: AppColors.orange500,
                                  shape: RoundedRectangleBorder(
                                    borderRadius: BorderRadius.circular(16.r),
                                  ),
                                ),
                                child: Text(
                                  '확인',
                                  style: AppTextStyles.bodyMd.copyWith(
                                    color: colors.white,
                                    fontWeight: FontWeight.w700,
                                  ),
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        if (_isLockVisible)
          Positioned.fill(
            child: Material(
              color: colors.bgPrimary.withValues(alpha: 0.96),
              child: SafeArea(
                child: Center(
                  child: Padding(
                    padding: EdgeInsets.symmetric(horizontal: 24.w),
                    child: DecoratedBox(
                      decoration: BoxDecoration(
                        color: colors.bgSurface,
                        borderRadius: BorderRadius.circular(28.r),
                        boxShadow: [
                          BoxShadow(
                            color: colors.cardShadow,
                            blurRadius: 24.r,
                            offset: Offset(0, 12.h),
                          ),
                        ],
                      ),
                      child: Padding(
                        padding: EdgeInsets.fromLTRB(24.w, 28.h, 24.w, 24.h),
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Container(
                              width: 68.w,
                              height: 68.w,
                              decoration: const BoxDecoration(
                                color: AppColors.orange50,
                                shape: BoxShape.circle,
                              ),
                              child: Icon(
                                Icons.lock_outline,
                                size: 34.w,
                                color: AppColors.orange500,
                              ),
                            ),
                            SizedBox(height: 18.h),
                            Text(
                              '앱 잠금',
                              style: AppTextStyles.headingLg.copyWith(
                                color: colors.textPrimary,
                                fontWeight: FontWeight.w800,
                                fontSize: 28.sp,
                              ),
                            ),
                            SizedBox(height: 10.h),
                            if (_isAuthenticating) ...[
                              SizedBox(height: 20.h),
                              SizedBox(
                                width: 32.w,
                                height: 32.w,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2.5,
                                  color: AppColors.orange500,
                                ),
                              ),
                              SizedBox(height: 16.h),
                              Text(
                                '생체인증을 확인하고 있어요.',
                                textAlign: TextAlign.center,
                                style: AppTextStyles.bodyMd.copyWith(
                                  color: colors.textSecondary,
                                  height: 1.5,
                                ),
                              ),
                            ] else
                              AppPinKeypad(
                                pin: _enteredPin,
                                pinLength: BiometricAuthService.appPinLength,
                                title: '앱 잠금',
                                subtitle: _canUseDeviceAuth
                                    ? '4자리 앱 PIN을 입력하거나 생체인증으로 잠금을 해제해주세요.'
                                    : '4자리 앱 PIN을 입력해 앱 잠금을 해제해주세요.',
                                errorText: _pinError,
                                shuffleKey: _pinShuffleSeed,
                                onDigitPressed: _handlePinDigitPressed,
                                onBackspace: _handlePinBackspace,
                                bottomContent: Column(
                                  children: [
                                    if (_message != null) ...[
                                      Text(
                                        _message!,
                                        textAlign: TextAlign.center,
                                        style: AppTextStyles.caption.copyWith(
                                          color: colors.textSecondary,
                                          fontSize: 12.sp,
                                          fontWeight: FontWeight.w600,
                                        ),
                                      ),
                                      SizedBox(height: 12.h),
                                    ],
                                    if (_canUseDeviceAuth) ...[
                                      SizedBox(
                                        width: double.infinity,
                                        height: 52.h,
                                        child: OutlinedButton(
                                          onPressed: _startUnlockFlow,
                                          style: OutlinedButton.styleFrom(
                                            side: const BorderSide(
                                              color: AppColors.orange500,
                                            ),
                                            shape: RoundedRectangleBorder(
                                              borderRadius: BorderRadius.circular(16.r),
                                            ),
                                          ),
                                          child: Text(
                                            '생체인증으로 해제',
                                            style: AppTextStyles.bodyMd.copyWith(
                                              color: AppColors.orange500,
                                              fontWeight: FontWeight.w700,
                                            ),
                                          ),
                                        ),
                                      ),
                                      SizedBox(height: 10.h),
                                    ],
                                    TextButton(
                                      onPressed: _logout,
                                      child: Text(
                                        '로그아웃',
                                        style: AppTextStyles.bodyMd.copyWith(
                                          color: colors.textSecondary,
                                          fontWeight: FontWeight.w600,
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
      ],
    );
  }
}
