import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/providers/app_data_providers.dart';
import 'package:more_app/presentation/viewmodels/auth_viewmodel.dart';

class BiometricAppLock extends ConsumerStatefulWidget {
  const BiometricAppLock({super.key, required this.child});

  final Widget child;

  @override
  ConsumerState<BiometricAppLock> createState() => _BiometricAppLockState();
}

class _BiometricAppLockState extends ConsumerState<BiometricAppLock>
    with WidgetsBindingObserver {
  bool _isLockVisible = false;
  bool _isAuthenticating = false;
  bool _didAttemptInitialUnlock = false;
  bool _shouldLockOnResume = false;
  String? _message;

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

    if (state == AppLifecycleState.inactive ||
        state == AppLifecycleState.paused ||
        state == AppLifecycleState.hidden) {
      _shouldLockOnResume = true;
      return;
    }

    if (state == AppLifecycleState.resumed && _shouldLockOnResume) {
      _startUnlockFlow();
    }
  }

  bool _hasAuthenticatedSession(AuthState state) {
    return state is AuthAuthenticated || state is AuthAutoLoginSuccess;
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
        !_requiresProtection()) {
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
    final available = await service.isBiometricAvailable();
    if (!mounted) {
      return;
    }

    if (!available) {
      await ref.read(biometricPreferenceProvider.notifier).setEnabled(false);
      if (!mounted) {
        return;
      }
      setState(() {
        _isLockVisible = false;
        _isAuthenticating = false;
        _shouldLockOnResume = false;
        _message = null;
      });
      return;
    }

    setState(() {
      _isLockVisible = true;
      _isAuthenticating = true;
      _message = null;
    });

    final unlocked = await service.authenticate(
      reason: '지문 인증으로 앱 잠금을 해제해주세요.',
    );
    if (!mounted) {
      return;
    }

    setState(() {
      _isAuthenticating = false;
      _isLockVisible = !unlocked;
      _shouldLockOnResume = !unlocked;
      _message = unlocked ? null : '앱을 계속 사용하려면 지문 인증이 필요해요.';
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
          _didAttemptInitialUnlock = false;
          _message = null;
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
          _didAttemptInitialUnlock = false;
          _message = null;
        });
        return;
      }

      _scheduleInitialUnlockIfNeeded();
    });

    _scheduleInitialUnlockIfNeeded();

    final colors = AppColors.of(context);

    return Stack(
      children: [
        widget.child,
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
                              decoration: BoxDecoration(
                                color: AppColors.orange50,
                                shape: BoxShape.circle,
                              ),
                              child: Icon(
                                Icons.fingerprint,
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
                            Text(
                              _isAuthenticating
                                  ? '지문 인증을 확인하고 있어요.'
                                  : '지문 인증으로 앱 잠금을 해제해주세요.',
                              textAlign: TextAlign.center,
                              style: AppTextStyles.bodyMd.copyWith(
                                color: colors.textSecondary,
                                height: 1.5,
                              ),
                            ),
                            if (_message != null) ...[
                              SizedBox(height: 12.h),
                              Text(
                                _message!,
                                textAlign: TextAlign.center,
                                style: AppTextStyles.caption.copyWith(
                                  color: AppColors.red500,
                                  fontSize: 12.sp,
                                  fontWeight: FontWeight.w600,
                                ),
                              ),
                            ],
                            SizedBox(height: 24.h),
                            SizedBox(
                              width: double.infinity,
                              height: 52.h,
                              child: FilledButton(
                                onPressed: _isAuthenticating
                                    ? null
                                    : _startUnlockFlow,
                                style: FilledButton.styleFrom(
                                  backgroundColor: AppColors.orange500,
                                  shape: RoundedRectangleBorder(
                                    borderRadius: BorderRadius.circular(16.r),
                                  ),
                                ),
                                child: _isAuthenticating
                                    ? SizedBox(
                                        width: 20.w,
                                        height: 20.w,
                                        child: CircularProgressIndicator(
                                          strokeWidth: 2.w,
                                          color: colors.white,
                                        ),
                                      )
                                    : Text(
                                        '지문 인증하기',
                                        style: AppTextStyles.bodyMd.copyWith(
                                          color: colors.white,
                                          fontWeight: FontWeight.w700,
                                        ),
                                      ),
                              ),
                            ),
                            SizedBox(height: 10.h),
                            TextButton(
                              onPressed: _isAuthenticating ? null : _logout,
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
