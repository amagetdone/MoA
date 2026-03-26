import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/web_window_focus_listener.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/navigation/auth_route_resolver.dart';
import 'package:more_app/presentation/viewmodels/auth_viewmodel.dart';

class LoginPage extends ConsumerStatefulWidget {
  const LoginPage({super.key, this.reason});

  final String? reason;

  @override
  ConsumerState<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends ConsumerState<LoginPage>
    with WidgetsBindingObserver {
  static const _webLoginRecoveryTimeout = Duration(seconds: 3);

  Timer? _interruptedLoginTimer;
  bool _backgroundedDuringLogin = false;
  WebWindowFocusListener? _webWindowFocusListener;
  bool _sessionExpiredDialogShown = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _maybeShowSessionExpiredDialog();

    if (kIsWeb) {
      _webWindowFocusListener = WebWindowFocusListener(
        onFocus: _handleWindowFocus,
        onBlur: _handleWindowBlur,
      )..attach();
    }
  }

  @override
  void didUpdateWidget(covariant LoginPage oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.reason != widget.reason) {
      _maybeShowSessionExpiredDialog();
    }
  }

  @override
  void dispose() {
    _interruptedLoginTimer?.cancel();
    _webWindowFocusListener?.detach();
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  void _maybeShowSessionExpiredDialog() {
    if (widget.reason != AppRoutes.sessionExpiredReason ||
        _sessionExpiredDialogShown) {
      return;
    }

    _sessionExpiredDialogShown = true;
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      if (!mounted) {
        return;
      }

      final colors = AppColors.of(context);
      await showDialog<void>(
        context: context,
        barrierDismissible: true,
        builder: (dialogContext) {
          return AlertDialog(
            insetPadding: EdgeInsets.symmetric(horizontal: 28.w),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(20.r),
            ),
            titlePadding: EdgeInsets.fromLTRB(24.w, 24.h, 24.w, 8.h),
            contentPadding: EdgeInsets.fromLTRB(24.w, 0, 24.w, 20.h),
            actionsPadding: EdgeInsets.fromLTRB(20.w, 0, 20.w, 20.h),
            title: Row(
              children: [
                Container(
                  width: 40.w,
                  height: 40.w,
                  decoration: BoxDecoration(
                    color: AppColors.red500.withValues(alpha: 0.08),
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    Icons.warning_amber_rounded,
                    color: AppColors.red500,
                    size: 22.w,
                  ),
                ),
                SizedBox(width: 12.w),
                Expanded(
                  child: Text(
                    '세션이 종료되었어요',
                    style: AppTextStyles.headingMd.copyWith(
                      color: colors.textPrimary,
                      fontSize: 18.sp,
                    ),
                  ),
                ),
              ],
            ),
            content: Text(
              '다른 기기에서 로그인되어 현재 세션이 종료되었습니다.\n보안을 위해 다시 로그인해주세요.',
              style: AppTextStyles.bodyMd.copyWith(
                color: colors.textSecondary,
                height: 1.5,
              ),
            ),
            actions: [
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: () => Navigator.of(dialogContext).pop(),
                  style: FilledButton.styleFrom(
                    backgroundColor: AppColors.orange500,
                    foregroundColor: colors.white,
                    padding: EdgeInsets.symmetric(vertical: 14.h),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(14.r),
                    ),
                  ),
                  child: const Text('확인'),
                ),
              ),
            ],
          );
        },
      );

      if (!mounted || widget.reason != AppRoutes.sessionExpiredReason) {
        return;
      }

      context.replace(AppRoutes.login);
    });
  }

  void _scheduleInterruptedLoginReset() {
    _interruptedLoginTimer?.cancel();
    // Web social login returns focus to the page before the provider token and
    // backend login finish. A short timeout can incorrectly discard a successful
    // login, so keep this recovery window deliberately loose.
    _interruptedLoginTimer = Timer(_webLoginRecoveryTimeout, () {
      if (!mounted) {
        return;
      }

      if (ref.read(authViewModelProvider) is AuthLoading) {
        ref.read(authViewModelProvider.notifier).resetInterruptedLogin();
      }

      _backgroundedDuringLogin = false;
    });
  }

  void _handleWindowBlur() {
    if (!mounted) {
      return;
    }

    if (ref.read(authViewModelProvider) is AuthLoading) {
      _backgroundedDuringLogin = true;
      _interruptedLoginTimer?.cancel();
    }
  }

  void _handleWindowFocus() {
    if (!mounted || !_backgroundedDuringLogin) {
      return;
    }

    _scheduleInterruptedLoginReset();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (!kIsWeb) {
      return;
    }

    final authState = ref.read(authViewModelProvider);
    final isLoading = authState is AuthLoading;

    if (!isLoading) {
      _backgroundedDuringLogin = false;
      _interruptedLoginTimer?.cancel();
      return;
    }

    switch (state) {
      case AppLifecycleState.hidden:
      case AppLifecycleState.inactive:
      case AppLifecycleState.paused:
        _backgroundedDuringLogin = true;
        _interruptedLoginTimer?.cancel();
        break;
      case AppLifecycleState.resumed:
        if (!_backgroundedDuringLogin) {
          return;
        }

        _scheduleInterruptedLoginReset();
        break;
      case AppLifecycleState.detached:
        break;
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);

    ref.listen<AuthState>(authViewModelProvider, (prev, next) {
      if (next is! AuthLoading) {
        _interruptedLoginTimer?.cancel();
        _backgroundedDuringLogin = false;
      }

      if (next is AuthAuthenticated) {
        context.go(resolvePostAuthRoute(next.authResult, isTest: next.isTest));
      }
    });

    final authState = ref.watch(authViewModelProvider);
    final isLoading = authState is AuthLoading;

    return Scaffold(
      backgroundColor: colors.bgSurface,
      body: SafeArea(
        child: Padding(
          padding: EdgeInsets.symmetric(horizontal: 24.w),
          child: Column(
            children: [
              const Spacer(flex: 3),
              Image.asset(
                'assets/images/logo/applogo.png',
                width: 80.w,
                height: 80.w,
              ),
              SizedBox(height: 16.h),
              Text(
                '모아',
                style: AppTextStyles.headingXl.copyWith(
                  color: colors.textPrimary,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const Spacer(flex: 4),
              _SocialLoginButton(
                onPressed: isLoading
                    ? null
                    : () => ref
                          .read(authViewModelProvider.notifier)
                          .loginWithKakao(),
                backgroundColor: const Color(0xFFFEE500),
                textColor: colors.textPrimary,
                text: '카카오로 시작하기',
                iconPath: 'assets/images/logo/kakaologo.png',
              ),
              SizedBox(height: 12.h),
              _SocialLoginButton(
                onPressed: isLoading
                    ? null
                    : () => ref
                          .read(authViewModelProvider.notifier)
                          .loginWithGoogle(),
                backgroundColor: colors.bgSurface,
                textColor: colors.textPrimary,
                text: '구글로 시작하기',
                iconPath: 'assets/images/logo/googlelogo.png',
                borderColor: colors.borderDefault,
              ),
              SizedBox(height: 12.h),
              GestureDetector(
                onTap: isLoading
                    ? null
                    : () async {
                        final id = await showDialog<int>(
                          context: context,
                          builder: (ctx) => SimpleDialog(
                            title: const Text('테스트 계정 선택'),
                            children: List.generate(5, (i) {
                              final num = i + 1;
                              return SimpleDialogOption(
                                onPressed: () => Navigator.pop(ctx, num),
                                child: Text('계정 $num'),
                              );
                            }),
                          ),
                        );
                        if (id != null) {
                          ref
                              .read(authViewModelProvider.notifier)
                              .loginWithTest(id: id);
                        }
                      },
                child: Text(
                  '테스트 계정으로 로그인',
                  style: AppTextStyles.bodySm.copyWith(
                    color: colors.textTertiary,
                    decoration: TextDecoration.underline,
                  ),
                ),
              ),
              Spacer(flex: 3),
            ],
          ),
        ),
      ),
    );
  }
}

class _SocialLoginButton extends StatelessWidget {
  final VoidCallback? onPressed;
  final Color backgroundColor;
  final Color textColor;
  final String text;
  final String iconPath;
  final Color? borderColor;

  const _SocialLoginButton({
    required this.onPressed,
    required this.backgroundColor,
    required this.textColor,
    required this.text,
    required this.iconPath,
    this.borderColor,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: double.infinity,
      height: 52.h,
      child: OutlinedButton(
        onPressed: onPressed,
        style: OutlinedButton.styleFrom(
          backgroundColor: backgroundColor,
          side: BorderSide(color: borderColor ?? backgroundColor),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12.r),
          ),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Image.asset(iconPath, width: 24.w, height: 24.w),
            SizedBox(width: 8.w),
            Text(
              text,
              style: AppTextStyles.bodyMd.copyWith(
                color: textColor,
                fontWeight: FontWeight.w600,
                fontSize: 15.sp,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
