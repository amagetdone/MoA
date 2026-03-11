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
import 'package:more_app/presentation/widgets/common/more_card.dart';

class OnboardingPlanPage extends ConsumerWidget {
  const OnboardingPlanPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(spendingViewModelProvider);

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
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
              style: AppTextStyles.bodyMd.copyWith(
                color: AppColors.gray500,
                fontSize: 14.sp,
              ),
            ),
            SizedBox(height: 20.h),

            // 주간 미션 card
            MoreCard(
              color: const Color(0xFFFFF8E1),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '주간 미션',
                    style: AppTextStyles.bodySm.copyWith(
                      color: AppColors.orange500,
                      fontWeight: FontWeight.w700,
                      fontSize: 13.sp,
                    ),
                  ),
                  SizedBox(height: 8.h),
                  RichText(
                    text: TextSpan(
                      style: AppTextStyles.bodyMd.copyWith(
                        color: AppColors.gray900,
                        fontSize: 15.sp,
                        fontWeight: FontWeight.w600,
                      ),
                      children: [
                        const TextSpan(text: '매주 '),
                        TextSpan(
                          text: '300,000원',
                          style: TextStyle(color: AppColors.orange500),
                        ),
                        const TextSpan(text: '만 저축하면\n24주 후 유럽여행 갈 수 있어요 !'),
                      ],
                    ),
                  ),
                  SizedBox(height: 12.h),
                  Divider(height: 1, color: AppColors.borderDefault),
                  SizedBox(height: 12.h),
                  Text(
                    '여유 자금',
                    style: AppTextStyles.bodySm.copyWith(
                      color: AppColors.gray500,
                      fontSize: 13.sp,
                    ),
                  ),
                  SizedBox(height: 4.h),
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.baseline,
                    textBaseline: TextBaseline.alphabetic,
                    children: [
                      Text(
                        '200,000원',
                        style: AppTextStyles.headingLg.copyWith(
                          color: AppColors.orange500,
                          fontSize: 24.sp,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      SizedBox(width: 8.w),
                      Text(
                        '(자유소비 140,000원 ÷ 7일)',
                        style: AppTextStyles.bodySm.copyWith(
                          color: AppColors.gray500,
                          fontSize: 12.sp,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            SizedBox(height: 16.h),

            // 주간 예산 card
            MoreCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '주간 예산',
                    style: AppTextStyles.headingMd.copyWith(
                      color: AppColors.gray900,
                      fontSize: 16.sp,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  SizedBox(height: 16.h),

                  _BudgetRow(
                      label: '총 예산 (수입)',
                      value: '1,500,000원',
                      isHeader: true),
                  SizedBox(height: 16.h),
                  Divider(height: 1, color: AppColors.gray100),
                  SizedBox(height: 16.h),

                  _BudgetRow(
                      label: '→ 저축비',
                      value: '300,000원',
                      color: AppColors.orange500),
                  _BudgetSubLabel(text: '목표를 위해 저축하는 금액'),
                  SizedBox(height: 16.h),
                  Divider(height: 1, color: AppColors.gray100),
                  SizedBox(height: 16.h),

                  _BudgetRow(label: '→ 고정비', value: '700,000원'),
                  _BudgetSubLabel(text: '매주 고정으로 나가는 비용'),
                  SizedBox(height: 8.h),
                  Padding(
                    padding: EdgeInsets.only(left: 16.w),
                    child: Column(
                      children: [
                        _BudgetRow(
                            label: '주거비',
                            value: '350,000원',
                            isSubItem: true),
                        SizedBox(height: 6.h),
                        _BudgetRow(
                            label: '통신비',
                            value: '200,000원',
                            isSubItem: true),
                        SizedBox(height: 6.h),
                        _BudgetRow(
                            label: '구독비',
                            value: '150,000원',
                            isSubItem: true),
                      ],
                    ),
                  ),
                  SizedBox(height: 16.h),
                  Divider(height: 1, color: AppColors.gray100),
                  SizedBox(height: 16.h),

                  _BudgetRow(label: '→ 변동비', value: '300,000원'),
                  _BudgetSubLabel(text: '식비, 여가비 등 생활에 필요한 비용'),
                  SizedBox(height: 16.h),
                  Divider(height: 1, color: AppColors.gray100),
                  SizedBox(height: 16.h),

                  _BudgetRow(
                    label: '= 여유 자금',
                    value: '200,000원',
                    isHeader: true,
                    color: AppColors.orange500,
                  ),
                  _BudgetSubLabel(text: '고정/변동/저축비를 제외한 자유롭게 사용 가능한 금액'),
                ],
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: MoreBottomButton(
        text: '다음',
        progress: 6 / 6,
        onPressed: () => context.go(AppRoutes.home),
      ),
    );
  }
}

class _BudgetSubLabel extends StatelessWidget {
  final String text;

  const _BudgetSubLabel({required this.text});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(top: 2.h),
      child: Text(
        text,
        style: AppTextStyles.bodySm.copyWith(
          color: AppColors.gray500,
          fontSize: 11.sp,
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
