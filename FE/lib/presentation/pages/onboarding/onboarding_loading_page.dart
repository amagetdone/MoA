import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/presentation/widgets/common/more_text_field.dart';

class OnboardingLoadingPage extends StatefulWidget {
  final String? nextRoute;

  const OnboardingLoadingPage({super.key, this.nextRoute});

  @override
  State<OnboardingLoadingPage> createState() => _OnboardingLoadingPageState();
}

class _OnboardingLoadingPageState extends State<OnboardingLoadingPage>
    with TickerProviderStateMixin {
  late final AnimationController _animController;
  int _phase = 0; // 0 = analyzing, 1 = planning
  final TextEditingController _incomeController =
      TextEditingController(text: '3,000,000');

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
    final nextRoute = widget.nextRoute ?? AppRoutes.onboardingAnalysis;

    await Future.delayed(const Duration(seconds: 2));
    if (!mounted) return;

    // 계좌 → 분석 플로우일 때만 수입 확인 다이얼로그 표시
    if (nextRoute == AppRoutes.onboardingAnalysis) {
      await _showIncomeDialog();
      if (!mounted) return;
    }

    setState(() => _phase = 1);
    await Future.delayed(const Duration(seconds: 2));
    if (!mounted) return;

    context.go(nextRoute);
  }

  Future<void> _showIncomeDialog() async {
    await showDialog(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) {
        return AlertDialog(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(20.r),
          ),
          backgroundColor: AppColors.bgSurface,
          title: Text(
            '월급을 확인해주세요',
            style: AppTextStyles.headingMd.copyWith(
              color: AppColors.gray900,
              fontSize: 18.sp,
            ),
          ),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '분석한 월 평균 수입이에요.\n다르다면 직접 수정해주세요.',
                style: AppTextStyles.bodySm.copyWith(
                  color: AppColors.gray500,
                  fontSize: 13.sp,
                ),
              ),
              SizedBox(height: 16.h),
              MoreTextField(
                label: '월 평균 수입',
                hint: '0',
                controller: _incomeController,
                keyboardType: TextInputType.number,
                inputFormatters: [CurrencyInputFormatter()],
                suffix: '원',
              ),
            ],
          ),
          actions: [
            SizedBox(
              width: double.infinity,
              height: 48.h,
              child: ElevatedButton(
                onPressed: () => Navigator.of(dialogContext).pop(),
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.orange500,
                  foregroundColor: Colors.white,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12.r),
                  ),
                  elevation: 0,
                ),
                child: Text(
                  '확인',
                  style: AppTextStyles.bodyMd.copyWith(
                    color: Colors.white,
                    fontWeight: FontWeight.w600,
                    fontSize: 15.sp,
                  ),
                ),
              ),
            ),
          ],
        );
      },
    );
  }

  @override
  void dispose() {
    _animController.dispose();
    _incomeController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: false,
      child: Scaffold(
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
                                final value =
                                    (_animController.value + delay) % 1.0;
                                final height =
                                    12.h + 24.h * _barHeight(value);
                                return Container(
                                  width: 6.w,
                                  height: height,
                                  margin:
                                      EdgeInsets.symmetric(horizontal: 3.w),
                                  decoration: BoxDecoration(
                                    color: AppColors.orange500,
                                    borderRadius:
                                        BorderRadius.circular(3.r),
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
                            : widget.nextRoute == AppRoutes.onboardingPlan
                                ? '소비패턴 분석을 기반으로\n목표를 수립 중이에요'
                                : '소비패턴 분석을 마무리 하고 있어요',
                        style: AppTextStyles.headingMd.copyWith(
                          color: AppColors.gray900,
                          fontSize: 18.sp,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
        bottomNavigationBar: SafeArea(
          child: Padding(
            padding: EdgeInsets.fromLTRB(24.w, 0, 24.w, 16.h),
            child: SizedBox(
              height: 6.h,
              width: double.infinity,
              child: ClipRRect(
                borderRadius: BorderRadius.circular(4.r),
                child: Stack(
                  children: [
                    Container(
                      decoration: BoxDecoration(
                        color: AppColors.gray100,
                        borderRadius: BorderRadius.circular(4.r),
                      ),
                    ),
                    FractionallySizedBox(
                      widthFactor: widget.nextRoute == AppRoutes.onboardingPlan ? 6 / 7 : 4 / 7,
                      child: Container(
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(4.r),
                          gradient: const LinearGradient(
                            colors: [AppColors.white, AppColors.orange500],
                            stops: [0.0, 0.7],
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
    );
  }

  double _barHeight(double value) {
    return (1 - (2 * value - 1).abs());
  }
}
