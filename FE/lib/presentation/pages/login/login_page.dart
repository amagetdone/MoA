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
import 'package:more_app/presentation/viewmodels/auth_viewmodel.dart';

class LoginPage extends ConsumerStatefulWidget {
  const LoginPage({super.key});

  @override
  ConsumerState<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends ConsumerState<LoginPage>
    with WidgetsBindingObserver {
  Timer? _interruptedLoginTimer;
  bool _backgroundedDuringLogin = false;
  WebWindowFocusListener? _webWindowFocusListener;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);

    if (kIsWeb) {
      _webWindowFocusListener = WebWindowFocusListener(
        onFocus: _handleWindowFocus,
        onBlur: _handleWindowBlur,
      )..attach();
    }
  }

  @override
  void dispose() {
    _interruptedLoginTimer?.cancel();
    _webWindowFocusListener?.detach();
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  void _scheduleInterruptedLoginReset() {
    _interruptedLoginTimer?.cancel();
    _interruptedLoginTimer = Timer(const Duration(milliseconds: 600), () {
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
    ref.listen<AuthState>(authViewModelProvider, (prev, next) {
      if (next is! AuthLoading) {
        _interruptedLoginTimer?.cancel();
        _backgroundedDuringLogin = false;
      }

      if (next is AuthAuthenticated) {
        final result = next.authResult;
        if (result.status == 'TEMP') {
          context.go(AppRoutes.onboardingProfile);
        } else if (next.isTest) {
          context.go(AppRoutes.onboarding);
        } else {
          context.go(AppRoutes.home);
        }
      }
    });

    final authState = ref.watch(authViewModelProvider);
    final isLoading = authState is AuthLoading;

    return Scaffold(
      backgroundColor: Colors.white,
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
                  fontWeight: FontWeight.w700,
                ),
              ),
              const Spacer(flex: 4),
              _SocialLoginButton(
                onPressed:
                    isLoading
                        ? null
                        : () =>
                            ref
                                .read(authViewModelProvider.notifier)
                                .loginWithKakao(),
                backgroundColor: const Color(0xFFFEE500),
                textColor: AppColors.textPrimary,
                text: '카카오로 시작하기',
                iconPath: 'assets/images/logo/kakaologo.png',
              ),
              SizedBox(height: 12.h),
              _SocialLoginButton(
                onPressed:
                    isLoading
                        ? null
                        : () =>
                            ref
                                .read(authViewModelProvider.notifier)
                                .loginWithGoogle(),
                backgroundColor: Colors.white,
                textColor: AppColors.textPrimary,
                text: '구글로 시작하기',
                iconPath: 'assets/images/logo/googlelogo.png',
                borderColor: AppColors.borderDefault,
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
                          ref.read(authViewModelProvider.notifier).loginWithTest(id: id);
                        }
                      },
                child: Text(
                  '테스트 계정으로 로그인',
                  style: AppTextStyles.bodySm.copyWith(
                    color: AppColors.textTertiary,
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
          side: BorderSide(
            color: borderColor ?? backgroundColor,
          ),
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
