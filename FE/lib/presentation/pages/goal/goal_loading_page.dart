import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';

class GoalLoadingPage extends StatefulWidget {
  const GoalLoadingPage({super.key});

  @override
  State<GoalLoadingPage> createState() => _GoalLoadingPageState();
}

class _GoalLoadingPageState extends State<GoalLoadingPage>
    with SingleTickerProviderStateMixin {
  late final AnimationController _animController;
  int _phase = 0;

  @override
  void initState() {
    super.initState();
    _animController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1200),
    )..repeat();
    WidgetsBinding.instance.addPostFrameCallback((_) => _runPhases());
  }

  Future<void> _runPhases() async {
    await Future.delayed(const Duration(seconds: 2));
    if (!mounted) return;

    setState(() => _phase = 1);
    await Future.delayed(const Duration(seconds: 2));
    if (!mounted) return;

    context.go(AppRoutes.goalPlan);
  }

  @override
  void dispose() {
    _animController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return PopScope(
      canPop: false,
      child: Scaffold(
        backgroundColor: colors.bgSurface,
        body: SafeArea(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Padding(
                padding: EdgeInsets.only(left: 24.w, top: 24.h),
                child: Text(
                  '분석 중',
                  style: AppTextStyles.headingLg.copyWith(
                    color: colors.gray900,
                    fontSize: 22.sp,
                  ),
                ),
              ),
              Expanded(
                child: Center(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
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
                            ? '소비패턴 기반으로\n목표 계획을 수립 중입니다.'
                            : '거의 다 됐어요!\n잠시만 기다려주세요.',
                        textAlign: TextAlign.center,
                        style: AppTextStyles.headingMd.copyWith(
                          color: colors.gray900,
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
      ),
    );
  }

  double _barHeight(double value) {
    return (1 - (2 * value - 1).abs());
  }
}
