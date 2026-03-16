import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';

class OnboardingPage extends StatefulWidget {
  const OnboardingPage({super.key});

  @override
  State<OnboardingPage> createState() => _OnboardingPageState();
}

class _OnboardingPageState extends State<OnboardingPage> {
  final PageController _pageController = PageController();
  int _currentPage = 0;

  final List<_OnboardingData> _pages = const [
    _OnboardingData(
      image: 'assets/images/onboarding1.png',
      title: '큰 목표를 작은 계획으로',
      description: '막연한 저축 목표를 주 단위로 쪼개서 달성하기 쉽게',
    ),
    _OnboardingData(
      image: 'assets/images/onboarding2.png',
      title: '소비를 알아야 모을 수 있다',
      description: '소비패턴 분석으로 현실적인 저축계획 수립',
    ),
    _OnboardingData(
      image: 'assets/images/onboarding3.png',
      title: '매주 확인, 매주 성장',
      description: '주/월 단위로 진행상황 체크',
    ),
  ];

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  void _onNext() {
    if (_currentPage < _pages.length - 1) {
      _pageController.nextPage(
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeInOut,
      );
    } else {
      _onComplete();
    }
  }

  void _onComplete() {
    context.go(AppRoutes.onboardingAccount);
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: false,
      child: Scaffold(
      backgroundColor: Colors.white,
      body: SafeArea(
        child: Column(
          children: [
            // Skip button
            Align(
              alignment: Alignment.centerRight,
              child: Padding(
                padding: EdgeInsets.only(top: 16.h, right: 16.w),
                child: GestureDetector(
                  onTap: _onComplete,
                  child: Text(
                    '건너뛰기',
                    style: AppTextStyles.bodySm.copyWith(
                      color: AppColors.gray500,
                      fontSize: 14.sp,
                    ),
                  ),
                ),
              ),
            ),
            // PageView
            Expanded(
              child: PageView.builder(
                controller: _pageController,
                itemCount: _pages.length,
                onPageChanged: (index) {
                  setState(() {
                    _currentPage = index;
                  });
                },
                itemBuilder: (context, index) {
                  final page = _pages[index];
                  return Padding(
                    padding: EdgeInsets.symmetric(horizontal: 32.w),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Image.asset(
                          page.image,
                          height: 280.h,
                          fit: BoxFit.contain,
                          errorBuilder: (context, error, stackTrace) {
                            return Container(
                              height: 280.h,
                              width: 280.w,
                              decoration: BoxDecoration(
                                color: AppColors.gray50,
                                borderRadius: BorderRadius.circular(24.r),
                              ),
                              child: Icon(
                                Icons.image_outlined,
                                size: 64.w,
                                color: AppColors.gray100,
                              ),
                            );
                          },
                        ),
                        SizedBox(height: 48.h),
                        Text(
                          page.title,
                          style: AppTextStyles.headingLg.copyWith(
                            color: AppColors.gray900,
                            fontSize: 22.sp,
                          ),
                          textAlign: TextAlign.center,
                        ),
                        SizedBox(height: 12.h),
                        Text(
                          page.description,
                          style: AppTextStyles.bodyMd.copyWith(
                            color: AppColors.gray500,
                            fontSize: 15.sp,
                          ),
                          textAlign: TextAlign.center,
                        ),
                      ],
                    ),
                  );
                },
              ),
            ),
            // Dot indicators
            Padding(
              padding: EdgeInsets.only(bottom: 32.h),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: List.generate(
                  _pages.length,
                  (index) => AnimatedContainer(
                    duration: const Duration(milliseconds: 300),
                    margin: EdgeInsets.symmetric(horizontal: 4.w),
                    height: 8.h,
                    width: _currentPage == index ? 24.w : 8.w,
                    decoration: BoxDecoration(
                      color: _currentPage == index
                          ? AppColors.orange500
                          : AppColors.gray100,
                      borderRadius: BorderRadius.circular(4.r),
                    ),
                  ),
                ),
              ),
            ),
            // Progress bar
            Padding(
              padding: EdgeInsets.fromLTRB(24.w, 0, 24.w, 12.h),
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
                        widthFactor: (_currentPage + 1) / 7,
                        child: Container(
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(4.r),
                            gradient: const LinearGradient(
                              colors: [
                                AppColors.white,
                                AppColors.orange500,
                              ],
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
            // Bottom button
            Padding(
              padding: EdgeInsets.fromLTRB(24.w, 0, 24.w, 16.h),
              child: SizedBox(
                width: double.infinity,
                height: 56.h,
                child: ElevatedButton(
                  onPressed: _onNext,
                  style: ButtonStyle(
                    backgroundColor: WidgetStateProperty.resolveWith((states) {
                      if (states.contains(WidgetState.pressed)) return AppColors.orange600;
                      return AppColors.orange500;
                    }),
                    foregroundColor: WidgetStateProperty.all(Colors.white),
                    shape: WidgetStateProperty.all(
                      RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(16.r),
                      ),
                    ),
                    elevation: WidgetStateProperty.all(0),
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
    ),
    );
  }
}

class _OnboardingData {
  final String image;
  final String title;
  final String description;

  const _OnboardingData({
    required this.image,
    required this.title,
    required this.description,
  });
}
