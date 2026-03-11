import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';

class OnboardingIncomePage extends StatefulWidget {
  const OnboardingIncomePage({super.key});

  @override
  State<OnboardingIncomePage> createState() => _OnboardingIncomePageState();
}

class _OnboardingIncomePageState extends State<OnboardingIncomePage> {
  final TextEditingController _incomeController =
      TextEditingController(text: '3,000,000');
  bool _isEditing = false;

  @override
  void dispose() {
    _incomeController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: MoreAppBar(
        title: '계획 수립 중',
        showBack: true,
      ),
      body: SingleChildScrollView(
        padding: EdgeInsets.all(24.w),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Card
            Container(
              width: double.infinity,
              padding: EdgeInsets.all(20.w),
              decoration: BoxDecoration(
                color: AppColors.gray50,
                borderRadius: BorderRadius.circular(16.r),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '수입을 확인해주세요.',
                    style: AppTextStyles.headingMd.copyWith(
                      color: AppColors.gray900,
                      fontSize: 18.sp,
                    ),
                  ),
                  SizedBox(height: 8.h),
                  Text(
                    '분석한 월 평균 수입이에요. 다르다면 직접 수정해주세요.',
                    style: AppTextStyles.bodySm.copyWith(
                      color: AppColors.gray500,
                      fontSize: 13.sp,
                    ),
                  ),
                ],
              ),
            ),
            SizedBox(height: 32.h),

            // 월 평균 수입
            Text(
              '월 평균 수입',
              style: AppTextStyles.bodySm.copyWith(
                color: AppColors.gray700,
                fontWeight: FontWeight.w600,
                fontSize: 13.sp,
              ),
            ),
            SizedBox(height: 8.h),
            TextField(
              controller: _incomeController,
              keyboardType: TextInputType.number,
              readOnly: !_isEditing,
              style: AppTextStyles.headingLg.copyWith(
                color: AppColors.gray900,
                fontSize: 24.sp,
              ),
              decoration: InputDecoration(
                suffixText: '원',
                suffixStyle: AppTextStyles.headingMd.copyWith(
                  color: AppColors.gray500,
                  fontSize: 18.sp,
                ),
                filled: true,
                fillColor: AppColors.gray50,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12.r),
                  borderSide: BorderSide.none,
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12.r),
                  borderSide: BorderSide(
                    color: AppColors.orange500,
                    width: 1.5,
                  ),
                ),
                contentPadding: EdgeInsets.symmetric(
                  horizontal: 16.w,
                  vertical: 16.h,
                ),
              ),
              onChanged: (_) => setState(() {}),
            ),
          ],
        ),
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: EdgeInsets.all(24.w),
          child: Row(
            children: [
              Expanded(
                child: SizedBox(
                  height: 56.h,
                  child: OutlinedButton(
                    onPressed: () {
                      if (_isEditing) {
                        setState(() => _isEditing = false);
                      } else {
                        context.push(AppRoutes.onboardingAnalysis);
                      }
                    },
                    style: OutlinedButton.styleFrom(
                      side: BorderSide(color: AppColors.borderDefault),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(16.r),
                      ),
                    ),
                    child: Text(
                      '이대로 진행',
                      style: AppTextStyles.bodyMd.copyWith(
                        color: AppColors.gray700,
                        fontWeight: FontWeight.w600,
                        fontSize: 15.sp,
                      ),
                    ),
                  ),
                ),
              ),
              SizedBox(width: 12.w),
              Expanded(
                child: SizedBox(
                  height: 56.h,
                  child: ElevatedButton(
                    onPressed: () {
                      if (_isEditing) {
                        setState(() => _isEditing = false);
                        context.push(AppRoutes.onboardingAnalysis);
                      } else {
                        setState(() => _isEditing = true);
                      }
                    },
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.orange500,
                      foregroundColor: Colors.white,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(16.r),
                      ),
                      elevation: 0,
                    ),
                    child: Text(
                      _isEditing ? '수정 완료' : '수정하기',
                      style: AppTextStyles.bodyMd.copyWith(
                        color: Colors.white,
                        fontWeight: FontWeight.w600,
                        fontSize: 15.sp,
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
