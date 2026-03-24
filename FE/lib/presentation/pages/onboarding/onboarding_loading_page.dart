import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/viewmodels/onboarding_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_text_field.dart';

class OnboardingLoadingPage extends ConsumerStatefulWidget {
  final String? nextRoute;

  const OnboardingLoadingPage({super.key, this.nextRoute});

  @override
  ConsumerState<OnboardingLoadingPage> createState() =>
      _OnboardingLoadingPageState();
}

class _OnboardingLoadingPageState extends ConsumerState<OnboardingLoadingPage>
    with TickerProviderStateMixin {
  late final AnimationController _animController;
  late final TextEditingController _incomeController;
  int _phase = 0;

  @override
  void initState() {
    super.initState();
    _incomeController = TextEditingController();
    _animController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1200),
    )..repeat();
    WidgetsBinding.instance.addPostFrameCallback((_) => _runPhases());
  }

  Future<void> _runPhases() async {
    final nextRoute = widget.nextRoute ?? AppRoutes.onboardingAnalysis;

    if (nextRoute == AppRoutes.onboardingAnalysis) {
      // phase 0: "연동된 계좌를 분석 중입니다." - sync+analysis 완료 대기
      final syncFuture =
          ref.read(onboardingViewModelProvider.notifier).syncAndAnalyze();

      try {
        await syncFuture.timeout(
          const Duration(seconds: 30),
          onTimeout: () {},
        );
      } catch (_) {}
      if (!mounted) return;

      // 분석된 수입으로 다이얼로그 기본값 세팅
      final analysisIncome =
          ref.read(onboardingViewModelProvider).analysisResult?.averageIncome;
      if (analysisIncome != null) {
        _incomeController.text = _formatNumber(analysisIncome);
      } else {
        try {
          final income =
              await ref.read(onboardingViewModelProvider.notifier).loadIncome();
          if (mounted) _incomeController.text = _formatNumber(income);
        } catch (_) {
          _incomeController.text = '3,000,000';
        }
      }

      // 수입 다이얼로그
      final income = await _showIncomeDialog();
      if (!mounted) return;
      try {
        await ref
            .read(onboardingViewModelProvider.notifier)
            .saveIncome(income ?? 0);
      } catch (_) {}
      if (!mounted) return;

      // phase 1: "지난 3개월 거래내역 기반으로 소비패턴을 분석 중입니다."
      setState(() => _phase = 1);
      await Future.delayed(const Duration(seconds: 2));
      if (!mounted) return;
    } else {
      await Future.delayed(const Duration(seconds: 2));
      if (!mounted) return;

      setState(() => _phase = 1);
      await Future.delayed(const Duration(seconds: 2));
      if (!mounted) return;
    }

    context.go(nextRoute);
  }

  String _formatNumber(int amount) {
    return amount.toString().replaceAllMapped(
          RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))'),
          (m) => '${m[1]},',
        );
  }

  Future<int?> _showIncomeDialog() async {
    return showDialog<int>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) {
        final colors = AppColors.of(dialogContext);
        return AlertDialog(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(20.r),
          ),
          backgroundColor: colors.bgSurface,
          title: Text(
            '월급을 확인해주세요',
            style: AppTextStyles.headingMd.copyWith(
              color: colors.gray900,
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
                  color: colors.gray500,
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
                onPressed: () {
                  final income =
                      CurrencyFormatter.parseFormatted(_incomeController.text) ??
                          0;
                  Navigator.of(dialogContext).pop(income);
                },
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
                        widget.nextRoute == AppRoutes.onboardingPlan
                            ? '소비패턴 기반으로\n목표 계획을 수립 중입니다.'
                            : _phase == 0
                                ? '연동된 계좌를 분석 중입니다.'
                                : '지난 3개월 거래내역 기반으로\n소비패턴을 분석 중입니다.',
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
                        color: colors.gray100,
                        borderRadius: BorderRadius.circular(4.r),
                      ),
                    ),
                    FractionallySizedBox(
                      widthFactor:
                          widget.nextRoute == AppRoutes.onboardingPlan
                              ? 6 / 7
                              : 4 / 7,
                      child: Container(
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(4.r),
                          gradient: LinearGradient(
                            colors: [colors.white, AppColors.orange500],
                            stops: const [0.0, 0.7],
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
