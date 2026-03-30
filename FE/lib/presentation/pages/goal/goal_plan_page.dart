import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/theme/app_text_styles.dart';
import 'package:more_app/core/utils/currency_formatter.dart';
import 'package:more_app/presentation/navigation/app_router.dart';
import 'package:more_app/presentation/viewmodels/onboarding_viewmodel.dart';
import 'package:more_app/presentation/widgets/common/more_app_bar.dart';
import 'package:more_app/presentation/widgets/common/more_bottom_button.dart';
import 'package:more_app/presentation/widgets/common/more_card.dart';

class GoalPlanPage extends ConsumerWidget {
  const GoalPlanPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final colors = AppColors.of(context);
    final plan = ref.watch(onboardingViewModelProvider).planResult;

    if (plan == null) {
      return Scaffold(
        backgroundColor: colors.bgPrimary,
        appBar: const MoreAppBar(title: '', showBack: false),
        body: Center(
          child: Padding(
            padding: EdgeInsets.symmetric(horizontal: 24.w),
            child: MoreCard(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(
                    Icons.insert_chart_outlined_rounded,
                    size: 44.w,
                    color: AppColors.orange500,
                  ),
                  SizedBox(height: 16.h),
                  Text(
                    '표시할 목표 계획이 없어요.',
                    style: AppTextStyles.bodyLg.copyWith(
                      color: colors.textPrimary,
                      fontWeight: FontWeight.w700,
                    ),
                    textAlign: TextAlign.center,
                  ),
                  SizedBox(height: 8.h),
                  Text(
                    '목표를 다시 생성한 뒤 계획을 확인해주세요.',
                    style: AppTextStyles.bodySm.copyWith(
                      color: colors.textTertiary,
                    ),
                    textAlign: TextAlign.center,
                  ),
                ],
              ),
            ),
          ),
        ),
        bottomNavigationBar: MoreBottomButton(
          text: '목표 목록으로 이동',
          onPressed: () => context.go(AppRoutes.goalTab),
        ),
      );
    }

    final planTitle = plan.planTitle;
    final weeklySavings = plan.weeklySavingsAmount;
    final weeksToGoal = plan.weeksToGoal;
    final budget = plan.weeklyBudget;
    final totalIncome = budget.totalIncomeAmount;
    final savingAmount = budget.savingAmount;
    final fixedTotal = budget.fixedExpense.totalAmount;
    final fixedItems = budget.fixedExpense.items;
    final variableTotal = budget.variableExpense.totalAmount;
    final freeAmount = budget.freeAmount;

    return Scaffold(
      backgroundColor: colors.bgPrimary,
      appBar: MoreAppBar(title: '', showBack: false),
      body: SingleChildScrollView(
        padding: EdgeInsets.fromLTRB(24.w, 0, 24.w, 24.w),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '목표 계획 수립',
              style: AppTextStyles.headingLg.copyWith(
                color: colors.textPrimary,
                fontSize: 24.sp,
                fontWeight: FontWeight.bold,
              ),
            ),
            SizedBox(height: 6.h),
            Text(
              '소비패턴 결과를 반영하여\n$planTitle 목표 계획을 수립합니다.',
              style: AppTextStyles.bodyMd.copyWith(
                color: colors.gray500,
                fontSize: 14.sp,
              ),
            ),
            SizedBox(height: 24.h),

            // 주간 미션 card
            MoreCard(
              color: colors.bgInsight,
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
                        color: colors.gray900,
                        fontSize: 15.sp,
                        fontWeight: FontWeight.w600,
                      ),
                      children: [
                        const TextSpan(text: '매주 '),
                        TextSpan(
                          text: CurrencyFormatter.formatKRW(weeklySavings),
                          style: const TextStyle(color: AppColors.orange500),
                        ),
                        TextSpan(
                          text: '만 저축하면\n$weeksToGoal주 후 $planTitle 이룰 수 있어요 !',
                        ),
                      ],
                    ),
                  ),
                  SizedBox(height: 12.h),
                  Divider(height: 1, color: colors.borderDefault),
                  SizedBox(height: 12.h),
                  Text(
                    '여유 자금',
                    style: AppTextStyles.bodySm.copyWith(
                      color: colors.gray500,
                      fontSize: 13.sp,
                    ),
                  ),
                  SizedBox(height: 4.h),
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.baseline,
                    textBaseline: TextBaseline.alphabetic,
                    children: [
                      Text(
                        CurrencyFormatter.formatKRW(freeAmount),
                        style: AppTextStyles.headingLg.copyWith(
                          color: AppColors.orange500,
                          fontSize: 24.sp,
                          fontWeight: FontWeight.w700,
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
                      color: colors.gray900,
                      fontSize: 16.sp,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  SizedBox(height: 16.h),

                  _BudgetRow(
                    label: '총 예산 (수입)',
                    value: CurrencyFormatter.formatKRW(totalIncome),
                    isHeader: true,
                  ),
                  SizedBox(height: 16.h),
                  Divider(height: 1, color: colors.gray100),
                  SizedBox(height: 16.h),

                  _BudgetRow(
                    label: '→ 저축비',
                    value: CurrencyFormatter.formatKRW(savingAmount),
                    color: AppColors.orange500,
                  ),
                  const _BudgetSubLabel(text: '목표를 위해 저축하는 금액'),
                  SizedBox(height: 16.h),
                  Divider(height: 1, color: colors.gray100),
                  SizedBox(height: 16.h),

                  _BudgetRow(
                    label: '→ 고정비',
                    value: CurrencyFormatter.formatKRW(fixedTotal),
                  ),
                  const _BudgetSubLabel(text: '매주 고정으로 나가는 비용'),
                  if (fixedItems.isNotEmpty) ...[
                    SizedBox(height: 8.h),
                    Padding(
                      padding: EdgeInsets.only(left: 16.w),
                      child: Column(
                        children: fixedItems.asMap().entries.map((entry) {
                          return Padding(
                            padding: EdgeInsets.only(
                              bottom: entry.key < fixedItems.length - 1
                                  ? 6.h
                                  : 0,
                            ),
                            child: _BudgetRow(
                              label: entry.value.category,
                              value: CurrencyFormatter.formatKRW(
                                entry.value.amount,
                              ),
                              isSubItem: true,
                            ),
                          );
                        }).toList(),
                      ),
                    ),
                  ],
                  SizedBox(height: 16.h),
                  Divider(height: 1, color: colors.gray100),
                  SizedBox(height: 16.h),

                  _BudgetRow(
                    label: '→ 변동비',
                    value: CurrencyFormatter.formatKRW(variableTotal),
                  ),
                  const _BudgetSubLabel(text: '식비, 여가비 등 생활에 필요한 비용'),
                  SizedBox(height: 16.h),
                  Divider(height: 1, color: colors.gray100),
                  SizedBox(height: 16.h),

                  _BudgetRow(
                    label: '= 여유 자금',
                    value: CurrencyFormatter.formatKRW(freeAmount),
                    isHeader: true,
                    color: AppColors.orange500,
                  ),
                  const _BudgetSubLabel(text: '고정/변동/저축비를 제외한 자유롭게 사용 가능한 금액'),
                ],
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: MoreBottomButton(
        text: '확인',
        onPressed: () => context.go(AppRoutes.goalTab),
      ),
    );
  }
}

class _BudgetSubLabel extends StatelessWidget {
  final String text;

  const _BudgetSubLabel({required this.text});

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return Padding(
      padding: EdgeInsets.only(top: 2.h),
      child: Text(
        text,
        style: AppTextStyles.bodySm.copyWith(
          color: colors.gray500,
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
    final colors = AppColors.of(context);
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          label,
          style: AppTextStyles.bodySm.copyWith(
            color: isSubItem ? colors.gray500 : colors.gray700,
            fontWeight: isHeader ? FontWeight.w700 : FontWeight.w400,
            fontSize: isSubItem ? 12.sp : 14.sp,
          ),
        ),
        Text(
          value,
          style: AppTextStyles.bodySm.copyWith(
            color: color ?? (isSubItem ? colors.gray500 : colors.gray900),
            fontWeight: isHeader ? FontWeight.w700 : FontWeight.w500,
            fontSize: isSubItem ? 12.sp : 14.sp,
          ),
        ),
      ],
    );
  }
}
