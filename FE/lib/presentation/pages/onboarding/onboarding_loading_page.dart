import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';

class OnboardingLoadingPage extends StatefulWidget {
  const OnboardingLoadingPage({super.key});

  @override
  State<OnboardingLoadingPage> createState() => _OnboardingLoadingPageState();
}

class _OnboardingLoadingPageState extends State<OnboardingLoadingPage>
    with TickerProviderStateMixin {
  late final AnimationController _animController;
  int _phase = 0; // 0 = analyzing, 1 = planning
  bool _canProceed = false;

  @override
  void initState() {
    super.initState();
    _animController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1200),
    )..repeat();
    _runPhases();
  }

  Future<void> _runPhases() async {
    await Future.delayed(const Duration(seconds: 2));
    if (!mounted) return;
    setState(() => _phase = 1);
    await Future.delayed(const Duration(seconds: 2));
    if (!mounted) return;
    setState(() => _canProceed = true);
  }

  @override
  void dispose() {
    _animController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Padding(
              padding: EdgeInsets.only(left: 24.w, top: 24.h),
              child: Text(
                '분석 중',
                style: AppTextStyles.headingLg.copyWith(
                  color: AppColors.gray900,
                  fontSize: 22.sp,
                ),
              ),
            ),
            Expanded(
              child: Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    // Animated loading bars
                    SizedBox(
                      width: 80.w,
                      height: 48.h,
                      child: AnimatedBuilder(
                        animation: _animController,
                        builder: (context, child) {
                          return Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: List.generate(5, (index) {
                              final delay = index * 0.15;
                              final value = (_animController.value + delay) % 1.0;
                              final height = 12.h + 24.h * _barHeight(value);
                              return Container(
                                width: 6.w,
                                height: height,
                                margin: EdgeInsets.symmetric(horizontal: 3.w),
                                decoration: BoxDecoration(
                                  color: AppColors.orange500,
                                  borderRadius: BorderRadius.circular(3.r),
                                ),
                              );
                            }),
                          );
                        },
                      ),
                    ),
                    SizedBox(height: 32.h),
                    Text(
                      _phase == 0
                          ? '소비패턴을 분석중이에요'
                          : '계획을 수립 중이에요',
                      style: AppTextStyles.headingMd.copyWith(
                        color: AppColors.gray900,
                        fontSize: 18.sp,
                      ),
                    ),
                  ],
                ),
              ),
            ),
            // Bottom button
            Padding(
              padding: EdgeInsets.fromLTRB(24.w, 0, 24.w, 24.h),
              child: SizedBox(
                width: double.infinity,
                height: 56.h,
                child: ElevatedButton(
                  onPressed: _canProceed
                      ? () => context.go(AppRoutes.onboardingIncome)
                      : null,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.orange500,
                    disabledBackgroundColor: AppColors.gray100,
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16.r),
                    ),
                    elevation: 0,
                  ),
                  child: Text(
                    '다음',
                    style: AppTextStyles.bodyMd.copyWith(
                      color: Colors.white,
                      fontWeight: FontWeight.w600,
                      fontSize: 16.sp,
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  double _barHeight(double value) {
    // Simple sine-like bounce
    return (1 - (2 * value - 1).abs());
  }
}
