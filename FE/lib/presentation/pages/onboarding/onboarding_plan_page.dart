import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/viewmodels/spending_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';

class OnboardingPlanPage extends ConsumerWidget {
  const OnboardingPlanPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(spendingViewModelProvider);

    return Scaffold(
      backgroundColor: Colors.white,
      appBar: MoreAppBar(title: '목표 계획 수립', showBack: true),
      body: SingleChildScrollView(
        padding: EdgeInsets.all(24.w),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (state.isLoading)
              Padding(
                padding: EdgeInsets.symmetric(vertical: 48.h),
                child: const Center(child: CircularProgressIndicator()),
              )
            else if (state.errorMessage != null)
              Padding(
                padding: EdgeInsets.only(bottom: 24.h),
                child: Text(
                  '계획 데이터를 불러오지 못했습니다.\n${state.errorMessage!}',
                  style: AppTextStyles.bodySm.copyWith(color: AppColors.red500),
                ),
              ),

            Text(
              '소비패턴 결과를 반영하여\n유럽여행 목표 계획을 수립합니다.',
              style: AppTextStyles.headingMd.copyWith(
                color: AppColors.gray900,
                fontSize: 18.sp,
              ),
            ),
            SizedBox(height: 24.h),

            // 주간 미션 card (orange)
            Container(
              width: double.infinity,
              padding: EdgeInsets.all(20.w),
              decoration: BoxDecoration(
                color: AppColors.orange500,
                borderRadius: BorderRadius.circular(16.r),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '주간 미션',
                    style: AppTextStyles.bodySm.copyWith(
                      color: Colors.white.withValues(alpha: 0.8),
                      fontSize: 13.sp,
                    ),
                  ),
                  SizedBox(height: 8.h),
                  Text(
                    '매주 300,000원만 저축하면\n24주 후 유럽여행 갈 수 있어요!',
                    style: AppTextStyles.headingMd.copyWith(
                      color: Colors.white,
                      fontSize: 16.sp,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ],
              ),
            ),
            SizedBox(height: 24.h),

            // 하루 생활비 한도
            Text(
              '하루 생활비 한도',
              style: AppTextStyles.bodySm.copyWith(
                color: AppColors.gray500,
                fontSize: 13.sp,
              ),
            ),
            SizedBox(height: 8.h),
            Text(
              '20,000원',
              style: AppTextStyles.headingXl.copyWith(
                color: AppColors.gray900,
                fontSize: 28.sp,
                fontWeight: FontWeight.w700,
              ),
            ),
            SizedBox(height: 4.h),
            Text(
              '자유소비 140,000원 ÷ 7일',
              style: AppTextStyles.bodySm.copyWith(
                color: AppColors.gray500,
                fontSize: 12.sp,
              ),
            ),
            SizedBox(height: 32.h),

            // 주간 예산 section
            Text(
              '주간 예산',
              style: AppTextStyles.headingMd.copyWith(
                color: AppColors.gray900,
                fontSize: 16.sp,
                fontWeight: FontWeight.w700,
              ),
            ),
            SizedBox(height: 16.h),

            _BudgetRow(label: '총 예산(수입)', value: '1,500,000원', isHeader: true),
            SizedBox(height: 12.h),
            _BudgetRow(label: '→ 저축비', value: '300,000원', color: AppColors.orange500),
            SizedBox(height: 12.h),
            _BudgetRow(label: '→ 고정비', value: '700,000원'),
            // Sub-categories
            Padding(
              padding: EdgeInsets.only(left: 24.w),
              child: Column(
                children: [
                  SizedBox(height: 8.h),
                  _BudgetRow(label: '식비', value: '350,000원', isSubItem: true),
                  SizedBox(height: 6.h),
                  _BudgetRow(label: '교통비', value: '200,000원', isSubItem: true),
                  SizedBox(height: 6.h),
                  _BudgetRow(label: '여가비', value: '150,000원', isSubItem: true),
                ],
              ),
            ),
            SizedBox(height: 12.h),
            _BudgetRow(label: '→ 변동비', value: '360,000원'),
            SizedBox(height: 12.h),
            Divider(height: 1.h, color: AppColors.gray100),
            SizedBox(height: 12.h),
            _BudgetRow(
              label: '= 자유 사용 금액',
              value: '200,000원',
              isHeader: true,
              color: AppColors.orange500,
            ),
          ],
        ),
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: EdgeInsets.all(24.w),
          child: MoreBottomButton(
            text: '다음',
            onPressed: () => context.go(AppRoutes.home),
            enabled: true,
          ),
        ),
      ),
    );
  }
}

class _BudgetRow extends StatelessWidget {
  final String label;
  final String value;
  final bool isHeader;
  final bool isSubItem;
  final Color? color;

  const _BudgetRow({
    required this.label,
    required this.value,
    this.isHeader = false,
    this.isSubItem = false,
    this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          label,
          style: AppTextStyles.bodySm.copyWith(
            color: isSubItem ? AppColors.gray500 : AppColors.gray700,
            fontWeight: isHeader ? FontWeight.w700 : FontWeight.w400,
            fontSize: isSubItem ? 12.sp : 14.sp,
          ),
        ),
        Text(
          value,
          style: AppTextStyles.bodySm.copyWith(
            color: color ?? (isSubItem ? AppColors.gray500 : AppColors.gray900),
            fontWeight: isHeader ? FontWeight.w700 : FontWeight.w500,
            fontSize: isSubItem ? 12.sp : 14.sp,
          ),
        ),
      ],
    );
  }
}
