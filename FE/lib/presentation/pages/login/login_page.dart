import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/viewmodels/auth_viewmodel.dart';

class LoginPage extends ConsumerWidget {
  const LoginPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    ref.listen<AuthState>(authViewModelProvider, (prev, next) {
      if (next is AuthAuthenticated) {
        final result = next.authResult;
        if (result.status == 'TEMP') {
          context.go(AppRoutes.onboardingProfile);
        } else {
          context.go(AppRoutes.home);
        }
      } else if (next is AuthError) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(next.message)),
        );
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

              // 로고
              Image.asset(
                'assets/images/logo/applogo.png',
                width: 80.w,
                height: 80.w,
              ),
              SizedBox(height: 16.h),

              // 앱 이름
              Text(
                '모아',
                style: AppTextStyles.headingXl.copyWith(
                  fontWeight: FontWeight.w700,
                ),
              ),

              const Spacer(flex: 4),

              // 카카오 로그인 버튼
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

              // 구글 로그인 버튼
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
